package io.ballerina.lib.solace.smf.consumer;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.transaction.TransactedSession;
import io.ballerina.lib.solace.smf.common.CommonUtils;
import io.ballerina.lib.solace.smf.config.ConfigurationUtils;
import io.ballerina.lib.solace.smf.config.ConsumerConfiguration;
import io.ballerina.lib.solace.smf.config.ConsumerSubscriptionConfig;
import io.ballerina.lib.solace.smf.config.QueueConsumerConfig;
import io.ballerina.lib.solace.smf.config.TopicConsumerConfig;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.math.BigDecimal;

import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_CLOSED;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_CONSUMER;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_FLOW;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_SESSION;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_SUBSCRIPTION_TYPE;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_TRANSACTED;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_TX_SESSION;
import static io.ballerina.lib.solace.smf.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_DIRECT_TOPIC;
import static io.ballerina.lib.solace.smf.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_DURABLE_TOPIC;
import static io.ballerina.lib.solace.smf.consumer.ConsumerUtils.SUBSCRIPTION_TYPE_QUEUE;
import static io.ballerina.lib.solace.smf.consumer.ConsumerUtils.createDirectTopicConsumer;
import static io.ballerina.lib.solace.smf.consumer.ConsumerUtils.createDurableTopicConsumer;
import static io.ballerina.lib.solace.smf.consumer.ConsumerUtils.createQueueConsumer;

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
        try {
            // Parse configuration
            ConsumerConfiguration consumerConfig = new ConsumerConfiguration(config);
            ConsumerSubscriptionConfig subscriptionConfig = consumerConfig.subscriptionConfig();
            boolean isTransacted = consumerConfig.connectionConfig().transacted();

            // Build JCSMP properties from configuration
            JCSMPProperties jcsmpProps =
                    ConfigurationUtils.buildJCSMPProperties(url.getValue(), consumerConfig.connectionConfig());

            // Create and connect base JCSMP session
            final JCSMPSession session = JCSMPFactory.onlyInstance().createSession(jcsmpProps);
            session.connect();

            // Validate: Direct topic subscriptions cannot be transacted
            if (isTransacted && subscriptionConfig instanceof TopicConsumerConfig topicConfig &&
                    !topicConfig.isDurable()) {
                return CommonUtils.createError("Transacted mode is not supported for direct topic subscriptions. " +
                        "Use DURABLE endpoint type for guaranteed delivery with transactions.");
            }

            // Create TransactedSession if in transacted mode
            final TransactedSession txSession = isTransacted ? session.createTransactedSession() : null;

            // Store session references and transacted flag
            consumer.addNativeData(NATIVE_SESSION, session);
            consumer.addNativeData(NATIVE_TX_SESSION, txSession);
            consumer.addNativeData(NATIVE_TRANSACTED, isTransacted);
            consumer.addNativeData(NATIVE_CLOSED, false);

            // Create appropriate consumer based on subscription type
            if (subscriptionConfig instanceof QueueConsumerConfig queueConfig) {
                FlowReceiverFactory factory = isTransacted
                        ? props -> txSession.createFlow(null, props, null)
                        : props -> session.createFlow(null, props);
                createQueueConsumer(consumer, factory, queueConfig, isTransacted);
            } else if (subscriptionConfig instanceof TopicConsumerConfig topicConfig) {
                topicConfig.validate();
                if (topicConfig.isDurable()) {
                    FlowReceiverFactory factory = isTransacted
                            ? props -> txSession.createFlow(null, props, null)
                            : props -> session.createFlow(null, props);
                    createDurableTopicConsumer(consumer, factory, topicConfig, isTransacted);
                } else {
                    createDirectTopicConsumer(consumer, session, topicConfig);
                }
            } else {
                return CommonUtils.createError("Unknown subscription configuration type");
            }

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to initialize consumer", e);
        }
    }

    /**
     * Receive a message with timeout.
     *
     * @param consumer the Ballerina consumer object
     * @param timeout  the timeout in seconds
     * @return the received message, null if timeout, or BError on failure
     */
    public static Object receive(BObject consumer, BDecimal timeout) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            long timeoutMs = timeout.decimalValue().multiply(BigDecimal.valueOf(1000)).longValue();
            String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);

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

            // Convert to Ballerina Message
            return MessageConverter.toBallerinaMessage(message);
        } catch (Exception e) {
            return CommonUtils.createError("Failed to receive message", e);
        }
    }

    /**
     * Receive a message without waiting.
     *
     * @param consumer the Ballerina consumer object
     * @return the received message, null if none available, or BError on failure
     */
    public static Object receiveNoWait(BObject consumer) {
        try {
            Boolean closed = (Boolean) consumer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Consumer is closed");
            }

            String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);

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
                return null; // No message available
            }

            // Convert to Ballerina Message
            return MessageConverter.toBallerinaMessage(message);
        } catch (Exception e) {
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

            nativeMessage.ackMessage();
            return null;
        } catch (Exception e) {
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
            XMLMessage.Outcome outcome = requeue ? XMLMessage.Outcome.FAILED : XMLMessage.Outcome.REJECTED;
            nativeMessage.settle(outcome);
            return null;
        } catch (Exception e) {
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
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
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
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to rollback transaction", e);
        }
    }

    /**
     * Close the consumer and release resources. Closes flow receiver/consumer, transacted session (if any), and base
     * session in that order.
     *
     * @param consumer the Ballerina consumer object
     * @return null on success, BError on failure
     */
    public static BError close(BObject consumer) {
        try {
            String subscriptionType = (String) consumer.getNativeData(NATIVE_SUBSCRIPTION_TYPE);

            // Close flow receiver or XML consumer
            if (SUBSCRIPTION_TYPE_QUEUE.equals(subscriptionType) ||
                    SUBSCRIPTION_TYPE_DURABLE_TOPIC.equals(subscriptionType)) {
                FlowReceiver flowReceiver = (FlowReceiver) consumer.getNativeData(NATIVE_FLOW);
                if (flowReceiver != null) {
                    flowReceiver.stop();
                    flowReceiver.close();
                }
            } else if (SUBSCRIPTION_TYPE_DIRECT_TOPIC.equals(subscriptionType)) {
                XMLMessageConsumer xmlConsumer = (XMLMessageConsumer) consumer.getNativeData(NATIVE_CONSUMER);
                if (xmlConsumer != null) {
                    xmlConsumer.stop();
                    xmlConsumer.close();
                }
            }

            // Close transacted session if present
            TransactedSession txSession = (TransactedSession) consumer.getNativeData(NATIVE_TX_SESSION);
            if (txSession != null) {
                txSession.close();
            }

            // Close base session
            JCSMPSession session = (JCSMPSession) consumer.getNativeData(NATIVE_SESSION);
            if (session != null) {
                session.closeSession();
            }

            // Mark as closed and clear native data
            consumer.addNativeData(NATIVE_CLOSED, true);
            consumer.addNativeData(NATIVE_FLOW, null);
            consumer.addNativeData(NATIVE_CONSUMER, null);
            consumer.addNativeData(NATIVE_TX_SESSION, null);
            consumer.addNativeData(NATIVE_TRANSACTED, null);
            consumer.addNativeData(NATIVE_SESSION, null);

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to close consumer", e);
        }
    }
}
