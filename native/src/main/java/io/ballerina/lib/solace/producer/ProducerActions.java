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

package io.xlibb.solace.producer;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.ProducerFlowProperties;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;
import com.solacesystems.jcsmp.transaction.TransactedSession;
import io.ballerina.lib.solace.common.CommonUtils;
import io.ballerina.lib.solace.common.DestinationConverter;
import io.ballerina.lib.solace.config.ConfigurationUtils;
import io.ballerina.lib.solace.config.ProducerConfiguration;
import io.ballerina.lib.solace.observability.SolaceMetricsUtil;
import io.ballerina.lib.solace.observability.SolaceTracingUtil;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import static io.xlibb.solace.common.Constants.NATIVE_CLOSED;
import static io.xlibb.solace.common.Constants.NATIVE_PRODUCER;
import static io.xlibb.solace.common.Constants.NATIVE_SESSION;
import static io.xlibb.solace.common.Constants.NATIVE_TRANSACTED;
import static io.xlibb.solace.common.Constants.NATIVE_TX_SESSION;
import static io.xlibb.solace.common.Constants.NATIVE_URL;
import static io.xlibb.solace.common.MessageFieldConstants.PAYLOAD_KEY;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.CONTEXT_PRODUCER;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_CLOSE;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_COMMIT;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_PUBLISH;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_ROLLBACK;
import static io.xlibb.solace.observability.SolaceObservabilityConstants.UNKNOWN;

/**
 * Producer actions - main entry point for Ballerina MessageProducer interop.
 */
public class ProducerActions {

    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");
    private static final BString TOPIC_NAME_KEY = StringUtils.fromString("topicName");

    /**
     * Initialize the producer with connection URL and configuration. Creates either a transacted or non-transacted
     * producer based on configuration.
     *
     * @param producer the Ballerina producer object
     * @param url      the broker URL
     * @param config   the producer configuration
     * @return null on success, BError on failure
     */
    public static BError init(BObject producer, BString url, BMap<BString, Object> config) {
        JCSMPSession session = null;
        TransactedSession txSession = null;
        try {
            // Create configuration objects from Ballerina map
            ProducerConfiguration producerConfig = new ProducerConfiguration(config);

            // Build JCSMP properties from configuration (URL passed separately)
            JCSMPProperties jcsmpProps = ConfigurationUtils.buildJCSMPProperties(
                    url.getValue(),
                    producerConfig.connectionConfig());

            // Create and connect base JCSMP session
            session = JCSMPFactory.onlyInstance().createSession(jcsmpProps);
            session.connect();

            boolean isTransacted = producerConfig.connectionConfig().transacted();
            XMLMessageProducer xmlProducer;

            if (isTransacted) {
                // Transacted mode: Create TransactedSession and producer within it
                txSession = session.createTransactedSession();

                // IMPORTANT: Must first call getMessageProducer on base session before creating transacted producer
                session.getMessageProducer(new PublishEventHandler());

                // Create producer within transacted session with streaming callback
                ProducerFlowProperties flowProps = new ProducerFlowProperties();
                xmlProducer = txSession.createProducer(flowProps, new PublishEventHandler());
            } else {
                // Non-transacted mode: Use regular session producer
                xmlProducer = session.getMessageProducer(new PublishEventHandler());
            }

            // Store session references in native data
            producer.addNativeData(NATIVE_SESSION, session);
            producer.addNativeData(NATIVE_TX_SESSION, txSession);
            producer.addNativeData(NATIVE_TRANSACTED, isTransacted);
            producer.addNativeData(NATIVE_PRODUCER, xmlProducer);
            producer.addNativeData(NATIVE_CLOSED, false);
            producer.addNativeData(NATIVE_URL, url.getValue());

            SolaceMetricsUtil.reportNewProducer(producer);
            return null;
        } catch (Exception e) {
            if (txSession != null) {
                CommonUtils.closeQuietly(txSession::close);
            }
            if (session != null) {
                CommonUtils.closeQuietly(session::closeSession);
            }
            SolaceMetricsUtil.reportConnectionError(CONTEXT_PRODUCER);
            return CommonUtils.createError("Failed to initialize producer", e);
        }
    }

    /**
     * Send a message to the specified destination.
     *
     * @param env            the Ballerina environment (injected for tracing)
     * @param producer       the Ballerina producer object
     * @param message        the message to send
     * @param destinationMap the destination (Topic or Queue)
     * @return null on success, BError on failure
     */
    public static BError send(Environment env, BObject producer, BMap<BString, Object> message,
                              BMap<BString, Object> destinationMap) {
        String destinationName = getDestinationName(destinationMap);
        SolaceTracingUtil.traceResourceInvocation(env, producer, destinationName);
        try {
            XMLMessageProducer xmlProducer = (XMLMessageProducer) producer.getNativeData(NATIVE_PRODUCER);
            if (xmlProducer == null) {
                return CommonUtils.createError("Producer not initialized");
            }

            Boolean closed = (Boolean) producer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Producer is closed");
            }

            XMLMessage jcsmpMessage = MessageConverter.toJCSMPMessage(xmlProducer, message);

            if (destinationMap == null || destinationMap.isEmpty()) {
                return CommonUtils.createError("Destination must be specified");
            }

            Destination destination = createDestinationFromMap(destinationMap);
            com.solacesystems.jcsmp.Destination jcsmpDestination =
                    DestinationConverter.fromDestinationInterface(destination);

            final XMLMessage finalMessage = jcsmpMessage;
            final com.solacesystems.jcsmp.Destination finalDestination = jcsmpDestination;
            Object result = CommonUtils.executeBlocking(() -> {
                xmlProducer.send(finalMessage, finalDestination);
            });

            if (result instanceof BError bError) {
                SolaceMetricsUtil.reportProducerError(producer, destinationName, ERROR_TYPE_PUBLISH);
                return CommonUtils.createError(bError.getMessage());
            }

            int size = getPayloadSize(message);
            SolaceMetricsUtil.reportPublish(producer, destinationName, size);
            return null;
        } catch (Exception e) {
            SolaceMetricsUtil.reportProducerError(producer, destinationName, ERROR_TYPE_PUBLISH);
            return CommonUtils.createError("Failed to send message", e);
        }
    }

    /**
     * Commit the current transaction. Only valid for transacted producers (when connectionConfig.transacted = true).
     *
     * @param producer the Ballerina producer object
     * @return null on success, BError on failure
     */
    public static BError commit(BObject producer) {
        try {
            Boolean transacted = (Boolean) producer.getNativeData(NATIVE_TRANSACTED);
            if (transacted == null || !transacted) {
                return CommonUtils.createError(
                        "commit() can only be called on transacted producers. " +
                                "Set connectionConfig.transacted = true to enable transactions."
                );
            }

            TransactedSession txSession = (TransactedSession) producer.getNativeData(NATIVE_TX_SESSION);
            if (txSession == null) {
                return CommonUtils.createError("TransactedSession not initialized");
            }

            Boolean closed = (Boolean) producer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Producer is closed");
            }

            // Commit transaction on TransactedSession (blocking operation)
            Object result = CommonUtils.executeBlocking(txSession::commit);

            if (result instanceof BError) {
                SolaceMetricsUtil.reportProducerError(producer, ERROR_TYPE_COMMIT);
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
            SolaceMetricsUtil.reportProducerError(producer, ERROR_TYPE_COMMIT);
            return CommonUtils.createError("Failed to commit transaction", e);
        }
    }

    /**
     * Rollback the current transaction. Only valid for transacted producers (when connectionConfig.transacted = true).
     *
     * @param producer the Ballerina producer object
     * @return null on success, BError on failure
     */
    public static BError rollback(BObject producer) {
        try {
            Boolean transacted = (Boolean) producer.getNativeData(NATIVE_TRANSACTED);
            if (transacted == null || !transacted) {
                return CommonUtils.createError(
                        "rollback() can only be called on transacted producers. " +
                                "Set connectionConfig.transacted = true to enable transactions."
                );
            }

            TransactedSession txSession = (TransactedSession) producer.getNativeData(NATIVE_TX_SESSION);
            if (txSession == null) {
                return CommonUtils.createError("TransactedSession not initialized");
            }

            Boolean closed = (Boolean) producer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return CommonUtils.createError("Producer is closed");
            }

            // Rollback transaction on TransactedSession (blocking operation)
            Object result = CommonUtils.executeBlocking(txSession::rollback);

            if (result instanceof BError) {
                SolaceMetricsUtil.reportProducerError(producer, ERROR_TYPE_ROLLBACK);
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
            SolaceMetricsUtil.reportProducerError(producer, ERROR_TYPE_ROLLBACK);
            return CommonUtils.createError("Failed to rollback transaction", e);
        }
    }

    /**
     * Close the producer and release resources.
     *
     * @param env      the Ballerina environment (injected for tracing)
     * @param producer the Ballerina producer object
     * @return null on success, BError on failure
     */
    public static BError close(Environment env, BObject producer) {
        SolaceTracingUtil.traceResourceInvocation(env, producer);
        XMLMessageProducer xmlProducer = (XMLMessageProducer) producer.getNativeData(NATIVE_PRODUCER);
        TransactedSession txSession = (TransactedSession) producer.getNativeData(NATIVE_TX_SESSION);
        JCSMPSession session = (JCSMPSession) producer.getNativeData(NATIVE_SESSION);

        // Attempt to close every resource independently so one failure doesn't block the rest.
        Exception firstError = null;
        if (xmlProducer != null) {
            firstError = CommonUtils.attemptClose(xmlProducer::close);
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
        producer.addNativeData(NATIVE_CLOSED, true);
        producer.addNativeData(NATIVE_PRODUCER, null);
        producer.addNativeData(NATIVE_TX_SESSION, null);
        producer.addNativeData(NATIVE_SESSION, null);

        if (firstError != null) {
            SolaceMetricsUtil.reportProducerError(producer, ERROR_TYPE_CLOSE);
            return CommonUtils.createError("Failed to close producer", firstError);
        }

        SolaceMetricsUtil.reportProducerClose(producer);
        return null;
    }

    /**
     * Factory method to create Destination sealed interface from BMap. Detects Queue vs Topic based on which field is
     * present.
     *
     * @param destinationMap the Ballerina destination map
     * @return Topic or Queue destination
     */
    private static Destination createDestinationFromMap(BMap<BString, Object> destinationMap) {
        if (destinationMap.containsKey(QUEUE_NAME_KEY)) {
            return new Queue(destinationMap);
        } else if (destinationMap.containsKey(TOPIC_NAME_KEY)) {
            return new Topic(destinationMap);
        }
        throw new IllegalArgumentException("Destination must have 'queueName' or 'topicName' field");
    }

    private static String getDestinationName(BMap<BString, Object> destinationMap) {
        if (destinationMap == null || destinationMap.isEmpty()) {
            return UNKNOWN;
        }
        Object queueName = destinationMap.get(QUEUE_NAME_KEY);
        if (queueName instanceof BString bStr) {
            return bStr.getValue();
        }
        Object topicName = destinationMap.get(TOPIC_NAME_KEY);
        if (topicName instanceof BString bStr) {
            return bStr.getValue();
        }
        return UNKNOWN;
    }

    private static int getPayloadSize(BMap<BString, Object> message) {
        if (message == null) {
            return 0;
        }
        Object payload = message.get(PAYLOAD_KEY);
        if (payload instanceof BArray arr) {
            return arr.size();
        }
        return 0;
    }
}
