/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.xlibb.solace.consumer;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.transaction.TransactedSession;
import io.ballerina.lib.solace.common.BallerinaSolaceDatabindingException;
import io.ballerina.lib.solace.common.CommonUtils;
import io.ballerina.lib.solace.config.ConfigurationUtils;
import io.ballerina.lib.solace.config.ConsumerConfiguration;
import io.ballerina.lib.solace.config.ConsumerSubscriptionConfig;
import io.ballerina.lib.solace.config.QueueConsumerConfig;
import io.ballerina.lib.solace.config.TopicConsumerConfig;
import io.ballerina.lib.solace.observability.SolaceMetricsUtil;
import io.ballerina.lib.solace.observability.SolaceTracingUtil;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static io.ballerina.lib.solace.common.Constants.NATIVE_CLOSED;
import static io.ballerina.lib.solace.common.Constants.NATIVE_CONSUMER;
import static io.ballerina.lib.solace.common.Constants.NATIVE_DESTINATION;
import static io.ballerina.lib.solace.common.Constants.NATIVE_FLOW;
import static io.ballerina.lib.solace.common.Constants.NATIVE_SESSION;
import static io.ballerina.lib.solace.common.Constants.NATIVE_SUBSCRIPTION_TYPE;
import static io.ballerina.lib.solace.common.Constants.NATIVE_TRANSACTED;
import static io.ballerina.lib.solace.common.Constants.NATIVE_TX_SESSION;
import static io.ballerina.lib.solace.common.Constants.NATIVE_URL;
import static io.ballerina.lib.solace.common.MessageFieldConstants.PAYLOAD_KEY;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_DIRECT_TOPIC;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_DURABLE_TOPIC;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_QUEUE;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.createDirectTopicConsumer;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.createDurableTopicConsumer;
import static io.ballerina.lib.solace.consumer.ConsumerUtils.createQueueConsumer;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.CONTEXT_CONSUMER;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_ACKNOWLEDGE;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_CLOSE;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_COMMIT;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_NACK;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_RECEIVE;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_ROLLBACK;

/**
 * Consumer actions - main entry point for Ballerina MessageConsumer interop.
 */
public class ConsumerActions {

    /**
     * Initialize the consumer with connection URL and configuration. Creates either a transacted or non-transacted
     * consumer based on configuration.
     *
     * @param consumer the Ballerina consumer object
     * @param url      the broker URL
     * @param config   the consumer configuration
     * @return null on success, BError on failure
     */
    public static BError init(BObject consumer, BString url, BMap<BString, Object> config) {
        JCSMPSession session = null;
        TransactedSession txSession = null;
        try {
            // Parse configuration
            ConsumerConfiguration consumerConfig = new ConsumerConfiguration(config);
            ConsumerSubscriptionConfig subscriptionConfig = consumerConfig.subscriptionConfig();
            boolean isTransacted = consumerConfig.connectionConfig().transacted();

            // Build JCSMP properties from configuration
            JCSMPProperties jcsmpProps =
                    ConfigurationUtils.buildJCSMPProperties(url.getValue(), consumerConfig.connectionConfig());
            ConfigurationUtils.applyReceiveTimestampProperty(jcsmpProps, consumerConfig.generateReceiveTimestamps());

            // Create and connect base JCSMP session
            session = JCSMPFactory.onlyInstance().createSession(jcsmpProps);
            session.connect();

            // Validate: Direct topic subscriptions cannot be transacted
            if (isTransacted && subscriptionConfig instanceof TopicConsumerConfig topicConfig &&
                    !topicConfig.isDurable()) {
                cleanupOnInitFailure(session, null);
                return CommonUtils.createError("Transacted mode is not supported for direct topic subscriptions. " +
                        "Use DURABLE endpoint type for guaranteed delivery with transactions.");
            }

            // Create TransactedSession if in transacted mode
            txSession = isTransacted ? session.createTransactedSession() : null;
            final JCSMPSession finalSession = session;
            final TransactedSession finalTxSession = txSession;

            // Store session references and transacted flag
            consumer.addNativeData(NATIVE_SESSION, session);
            consumer.addNativeData(NATIVE_TX_SESSION, txSession);
            consumer.addNativeData(NATIVE_TRANSACTED, isTransacted);
            consumer.addNativeData(NATIVE_CLOSED, false);
            consumer.addNativeData(NATIVE_URL, url.getValue());

            // Create appropriate consumer based on subscription type
            if (subscriptionConfig instanceof QueueConsumerConfig queueConfig) {
                FlowReceiverFactory factory = isTransacted
                        ? props -> finalTxSession.createFlow(null, props, null)
                        : props -> finalSession.createFlow(null, props);
                createQueueConsumer(consumer, factory, queueConfig, isTransacted);
            } else if (subscriptionConfig instanceof TopicConsumerConfig topicConfig) {
                topicConfig.validate();
                if (topicConfig.isDurable()) {
                    FlowReceiverFactory factory = isTransacted
                            ? props -> finalTxSession.createFlow(null, props, null)
                            : props -> finalSession.createFlow(null, props);
                    createDurableTopicConsumer(consumer, factory, topicConfig, isTransacted);
                } else {
                    createDirectTopicConsumer(consumer, session, topicConfig);
                }
            } else {
                cleanupOnInitFailure(session, txSession);
                return CommonUtils.createError("Unknown subscription configuration type");
            }

            SolaceMetricsUtil.reportNewConsumer(consumer);
            return null;
        } catch (Exception e) {
            cleanupOnInitFailure(session, txSession);
            SolaceMetricsUtil.reportConnectionError(CONTEXT_CONSUMER);
            return CommonUtils.createError("Failed to initialize consumer", e);
        }
    }

    /**
     * Best-effort cleanup of resources already created by init() when a later step fails, since nothing else
     * will ever call close() on a consumer whose init() returned an error.
     */
    private static void cleanupOnInitFailure(JCSMPSession session, TransactedSession txSession) {
        if (txSession != null) {
            CommonUtils.closeQuietly(txSession::close);
        }
        if (session != null) {
            CommonUtils.closeQuietly(session::closeSession);
        }
    }


    /**
     * Receive a message with timeout.
     *
     * @param env       the Ballerina environment (injected for tracing)
     * @param consumer  the Ballerina consumer object
     * @param timeout   the timeout in seconds, or {@code null} to never expire
     * @param bTypedesc the caller-declared expected message type
     * @return the received message, null if timeout, or BError on failure
     */
    public static Object receive(Environment env, BObject consumer, Object timeout, BTypedesc bTypedesc) {
        SolaceTracingUtil.traceResourceInvocation(env, consumer);
        Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
        if (closed != null && closed) {
            return CommonUtils.createError("Consumer is closed");
        }
        BigDecimal timeoutDecimal = timeout instanceof BDecimal bDecimal ? bDecimal.decimalValue() : BigDecimal.ZERO;
        long timeoutMs = timeoutDecimal.multiply(BigDecimal.valueOf(1000)).longValue();
        String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);

        try {
            Object result = CommonUtils.executeBlocking(() -> {
                BytesXMLMessage message = null;
                if (SUBSCRIPTION_TYPE_QUEUE.equals(subscriptionType) ||
                        SUBSCRIPTION_TYPE_DURABLE_TOPIC.equals(subscriptionType)) {
                    FlowReceiver flowReceiver = (FlowReceiver) consumer.getNativeData(NATIVE_FLOW);
                    if (flowReceiver == null) {
                        return CommonUtils.createError("Consumer flow not initialized");
                    }
                    message = flowReceiver.receive((int) timeoutMs);
                } else if (SUBSCRIPTION_TYPE_DIRECT_TOPIC.equals(subscriptionType)) {
                    XMLMessageConsumer xmlConsumer = (XMLMessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);
                    if (xmlConsumer == null) {
                        return CommonUtils.createError("Consumer not initialized");
                    }
                    message = xmlConsumer.receive((int) timeoutMs);
                }
                if (message == null) {
                    return null; // Timeout - no message available
                }
                try {
                    return MessageConverter.toBallerinaMessage(message, bTypedesc);
                } catch (BallerinaSolaceDatabindingException e) {
                    return CommonUtils.createError(e.getMessage());
                } catch (Exception e) {
                    return CommonUtils.createError("Failed to receive message", e);
                }
            });

            if (result instanceof BError bError) {
                SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_RECEIVE);
                return bError;
            }
            if (result != null) {
                int size = getPayloadSize((BMap<BString, Object>) result);
                SolaceMetricsUtil.reportConsume(consumer, size);
            }
            return result;
        } catch (Exception e) {
            SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_RECEIVE);
            return CommonUtils.createError("Failed to receive message", e);
        }
    }

    /**
     * Receive a message without waiting.
     *
     * @param env       the Ballerina environment (injected for tracing)
     * @param consumer  the Ballerina consumer object
     * @param bTypedesc the caller-declared expected message type
     * @return the received message, null if none available, or BError on failure
     */
    public static Object receiveNoWait(Environment env, BObject consumer, BTypedesc bTypedesc) {
        SolaceTracingUtil.traceResourceInvocation(env, consumer);
        Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
        if (closed != null && closed) {
            return CommonUtils.createError("Consumer is closed");
        }
        String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);
        try {
            Object result = CommonUtils.executeBlocking(() -> {
                BytesXMLMessage message = null;
                if (SUBSCRIPTION_TYPE_QUEUE.equals(subscriptionType) ||
                        SUBSCRIPTION_TYPE_DURABLE_TOPIC.equals(subscriptionType)) {
                    FlowReceiver flowReceiver = (FlowReceiver) consumer.getNativeData(NATIVE_FLOW);
                    if (flowReceiver == null) {
                        return CommonUtils.createError("Consumer flow not initialized");
                    }
                    message = flowReceiver.receiveNoWait();
                } else if (SUBSCRIPTION_TYPE_DIRECT_TOPIC.equals(subscriptionType)) {
                    XMLMessageConsumer xmlConsumer = (XMLMessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);
                    if (xmlConsumer == null) {
                        return CommonUtils.createError("Consumer not initialized");
                    }
                    message = xmlConsumer.receiveNoWait();
                }
                if (message == null) {
                    return null;
                }
                try {
                    return MessageConverter.toBallerinaMessage(message, bTypedesc);
                } catch (BallerinaSolaceDatabindingException e) {
                    return CommonUtils.createError(e.getMessage());
                } catch (Exception e) {
                    return CommonUtils.createError("Failed to receive message", e);
                }
            });

            if (result instanceof BError bError) {
                SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_RECEIVE);
                return bError;
            }
            if (result != null) {
                int size = getPayloadSize((BMap<BString, Object>) result);
                SolaceMetricsUtil.reportConsume(consumer, size);
            }
            return result;
        } catch (Exception e) {
            SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_RECEIVE);
            return CommonUtils.createError("Failed to receive message", e);
        }
    }

    /**
     * Acknowledge a message.
     *
     * @param consumer the Ballerina consumer object
     * @param message  the Ballerina message to acknowledge
     * @return null on success, BError on failure
     */
    public static BError acknowledge(BObject consumer, BMap<BString, Object> message) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            XMLMessage nativeMessage = MessageConverter.extractNativeMessage(message);
            if (nativeMessage == null) {
                return CommonUtils.createError("Cannot acknowledge: native message not found");
            }

            Object result = CommonUtils.executeBlocking(nativeMessage::ackMessage);
            if (result instanceof BError) {
                return (BError) result;
            }
            return null;
        } catch (Exception e) {
            SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_ACKNOWLEDGE);
            return CommonUtils.createError("Failed to acknowledge message", e);
        }
    }

    /**
     * Negatively acknowledge a message (NACK).
     *
     * @param consumer the Ballerina consumer object
     * @param message  the Ballerina message to NACK
     * @param requeue  if true, use FAILED outcome (requeue); if false, use REJECTED outcome (DMQ)
     * @return null on success, BError on failure
     */
    public static BError nack(BObject consumer, BMap<BString, Object> message, boolean requeue) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            XMLMessage nativeMessage = MessageConverter.extractNativeMessage(message);
            if (nativeMessage == null) {
                return CommonUtils.createError("Cannot NACK: native message not found");
            }

            // Use settle() with appropriate outcome
            Object result = CommonUtils.executeBlocking(() -> {
                XMLMessage.Outcome outcome = requeue ? XMLMessage.Outcome.FAILED : XMLMessage.Outcome.REJECTED;
                nativeMessage.settle(outcome);
                return null;
            });
            if (result instanceof BError) {
                return (BError) result;
            }
            return null;
        } catch (Exception e) {
            SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_NACK);
            return CommonUtils.createError("Failed to NACK message", e);
        }
    }

    /**
     * Commit the current transaction. Only valid for transacted consumers (when connectionConfig.transacted = true).
     *
     * @param consumer the Ballerina consumer object
     * @return null on success, BError on failure
     */
    public static BError commit(BObject consumer) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            Boolean transacted = (Boolean) consumer.getNativeData(NATIVE_TRANSACTED);
            if (transacted == null || !transacted) {
                return CommonUtils.createError("commit() can only be called on transacted consumers. " +
                        "Set connectionConfig.transacted = true to enable transactions.");
            }

            TransactedSession txSession = (TransactedSession) consumer.getNativeData(NATIVE_TX_SESSION);
            if (txSession == null) {
                return CommonUtils.createError("TransactedSession not initialized");
            }

            // Commit transaction on TransactedSession (blocking operation)
            Object result = CommonUtils.executeBlocking(txSession::commit);

            if (result instanceof BError) {
                SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_COMMIT);
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
            SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_COMMIT);
            return CommonUtils.createError("Failed to commit transaction", e);
        }
    }

    /**
     * Rollback the current transaction. Only valid for transacted consumers (when connectionConfig.transacted = true).
     *
     * @param consumer the Ballerina consumer object
     * @return null on success, BError on failure
     */
    public static BError rollback(BObject consumer) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            Boolean transacted = (Boolean) consumer.getNativeData(NATIVE_TRANSACTED);
            if (transacted == null || !transacted) {
                return CommonUtils.createError("rollback() can only be called on transacted consumers. " +
                        "Set connectionConfig.transacted = true to enable transactions.");
            }

            TransactedSession txSession = (TransactedSession) consumer.getNativeData(NATIVE_TX_SESSION);
            if (txSession == null) {
                return CommonUtils.createError("TransactedSession not initialized");
            }

            // Rollback transaction on TransactedSession (blocking operation)
            Object result = CommonUtils.executeBlocking(txSession::rollback);

            if (result instanceof BError) {
                SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_ROLLBACK);
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
            SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_ROLLBACK);
            return CommonUtils.createError("Failed to rollback transaction", e);
        }
    }

    /**
     * Close the consumer and release resources. Closes flow receiver/consumer, transacted session (if any), and base
     * session in that order.
     *
     * @param env      the Ballerina environment (injected for tracing)
     * @param consumer the Ballerina consumer object
     * @return null on success, BError on failure
     */
    public static BError close(Environment env, BObject consumer) {
        SolaceTracingUtil.traceResourceInvocation(env, consumer);
        String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);
        FlowReceiver flowReceiver = (FlowReceiver) consumer.getNativeData(NATIVE_FLOW);
        XMLMessageConsumer xmlConsumer = (XMLMessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);
        TransactedSession txSession = (TransactedSession) consumer.getNativeData(NATIVE_TX_SESSION);
        JCSMPSession session = (JCSMPSession) consumer.getNativeData(NATIVE_SESSION);

        // Attempt to close every resource independently so one failure doesn't block the rest.
        Exception firstError = null;
        if (SUBSCRIPTION_TYPE_QUEUE.equals(subscriptionType) ||
                SUBSCRIPTION_TYPE_DURABLE_TOPIC.equals(subscriptionType)) {
            if (flowReceiver != null) {
                firstError = CommonUtils.attemptClose(() -> {
                    flowReceiver.stop();
                    flowReceiver.close();
                });
            }
        } else if (SUBSCRIPTION_TYPE_DIRECT_TOPIC.equals(subscriptionType)) {
            if (xmlConsumer != null) {
                firstError = CommonUtils.attemptClose(() -> {
                    xmlConsumer.stop();
                    xmlConsumer.close();
                });
            }
        }

        if (txSession != null) {
            Exception e = CommonUtils.attemptClose(txSession::close);
            firstError = firstError == null ? e : firstError;
        }

        if (session != null) {
            Exception e = CommonUtils.attemptClose(session::closeSession);
            firstError = firstError == null ? e : firstError;
        }

        // Mark as closed and clear native data regardless of partial failures above.
        consumer.addNativeData(NATIVE_CLOSED, true);
        consumer.addNativeData(NATIVE_FLOW, null);
        consumer.addNativeData(NATIVE_CONSUMER, null);
        consumer.addNativeData(NATIVE_TX_SESSION, null);
        consumer.addNativeData(NATIVE_TRANSACTED, null);
        consumer.addNativeData(NATIVE_SESSION, null);

        if (firstError != null) {
            SolaceMetricsUtil.reportConsumerError(consumer, ERROR_TYPE_CLOSE);
            return CommonUtils.createError("Failed to close consumer", firstError);
        }

        SolaceMetricsUtil.reportConsumerClose(consumer);
        return null;
    }

    /**
     * Returns the resolved name of the destination (queue or topic) this consumer is bound to. For a TEMPORARY
     * queue created without a name hint, this is the broker-generated name.
     *
     * @param consumer the Ballerina consumer object
     * @return the destination name
     */
    public static BString destinationName(BObject consumer) {
        String destinationName = (String) consumer.getNativeData(NATIVE_DESTINATION);
        return StringUtils.fromString(destinationName);
    }

    @SuppressWarnings("unchecked")
    private static int getPayloadSize(BMap<BString, Object> message) {
        if (message == null) {
            return 0;
        }
        Object payload = message.get(PAYLOAD_KEY);
        if (payload instanceof BArray arr) {
            return arr.size();
        }
        if (payload instanceof BString str) {
            return str.getValue().getBytes(StandardCharsets.UTF_8).length;
        }
        // Best-effort only: exact byte-accounting for other payload shapes (record/map/etc.) is not attempted.
        return 0;
    }
}
