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

package io.ballerina.lib.solace.producer;

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
import io.ballerina.lib.solace.observability.SolaceSessionEventHandler;
import io.ballerina.lib.solace.observability.SolaceTracingUtil;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.ballerina.lib.solace.common.Constants.NATIVE_CLOSED;
import static io.ballerina.lib.solace.common.Constants.NATIVE_EVENT_HANDLER;
import static io.ballerina.lib.solace.common.Constants.NATIVE_PUBLISH_ACK_TRACKER;
import static io.ballerina.lib.solace.common.Constants.NATIVE_PRODUCER;
import static io.ballerina.lib.solace.common.Constants.NATIVE_SESSION;
import static io.ballerina.lib.solace.common.Constants.NATIVE_TRANSACTED;
import static io.ballerina.lib.solace.common.Constants.NATIVE_TX_SESSION;
import static io.ballerina.lib.solace.common.Constants.NATIVE_URL;
import static io.ballerina.lib.solace.common.Constants.NATIVE_VPN;
import static io.ballerina.lib.solace.common.MessageFieldConstants.DELIVERY_MODE_KEY;
import static io.ballerina.lib.solace.common.MessageFieldConstants.PAYLOAD_KEY;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.CONTEXT_PRODUCER;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.DESTINATION_KIND_QUEUE;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.DESTINATION_KIND_TOPIC;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_CLOSE;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_COMMIT;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_PUBLISH;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.ERROR_TYPE_ROLLBACK;
import static io.ballerina.lib.solace.observability.SolaceObservabilityConstants.UNKNOWN;

/**
 * Producer actions - main entry point for Ballerina MessageProducer interop.
 */
public class ProducerActions {

    private static final Logger LOGGER = Logger.getLogger(ProducerActions.class.getName());

    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");
    private static final BString TOPIC_NAME_KEY = StringUtils.fromString("topicName");
    private static final Duration PUBLISH_ACK_DRAIN_TIMEOUT = Duration.ofSeconds(30);

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
        String messageVpn = UNKNOWN;
        try {
            // Create configuration objects from Ballerina map
            ProducerConfiguration producerConfig = new ProducerConfiguration(config);
            messageVpn = producerConfig.connectionConfig().messageVpn();

            // Build JCSMP properties from configuration (URL passed separately)
            JCSMPProperties jcsmpProps = ConfigurationUtils.buildJCSMPProperties(
                    url.getValue(),
                    producerConfig.connectionConfig());
            ConfigurationUtils.applyProducerTimestampProperties(
                    jcsmpProps,
                    producerConfig.generateSendTimestamps(),
                    producerConfig.generateSequenceNumbers());

            // Create and connect base JCSMP session.
            SolaceSessionEventHandler eventHandler =
                    new SolaceSessionEventHandler(CONTEXT_PRODUCER, url.getValue(), messageVpn);
            session = JCSMPFactory.onlyInstance().createSession(jcsmpProps, null, eventHandler);
            session.connect();
            eventHandler.markConnected();

            boolean isTransacted = producerConfig.connectionConfig().transacted();
            PublishAcknowledgementTracker acknowledgementTracker =
                    isTransacted ? null : new PublishAcknowledgementTracker();
            XMLMessageProducer xmlProducer;

            if (isTransacted) {
                // Transacted mode: Create TransactedSession and producer within it
                txSession = session.createTransactedSession();
                PublishEventHandler publishEventHandler =
                        new PublishEventHandler(url.getValue(), messageVpn);

                // IMPORTANT: Must first call getMessageProducer on base session before creating transacted producer
                session.getMessageProducer(publishEventHandler);

                // Create producer within transacted session with streaming callback
                ProducerFlowProperties flowProps = new ProducerFlowProperties();
                xmlProducer = txSession.createProducer(flowProps, publishEventHandler);
            } else {
                // Non-transacted mode: Use regular session producer
                xmlProducer = session.getMessageProducer(
                        new PublishEventHandler(url.getValue(), messageVpn, acknowledgementTracker));
            }

            // Store session references in native data
            producer.addNativeData(NATIVE_SESSION, session);
            producer.addNativeData(NATIVE_TX_SESSION, txSession);
            producer.addNativeData(NATIVE_TRANSACTED, isTransacted);
            producer.addNativeData(NATIVE_PRODUCER, xmlProducer);
            producer.addNativeData(NATIVE_CLOSED, false);
            producer.addNativeData(NATIVE_URL, url.getValue());
            producer.addNativeData(NATIVE_VPN, messageVpn);
            producer.addNativeData(NATIVE_EVENT_HANDLER, eventHandler);
            producer.addNativeData(NATIVE_PUBLISH_ACK_TRACKER, acknowledgementTracker);
        } catch (Exception e) {
            if (txSession != null) {
                CommonUtils.closeQuietly(txSession::close);
            }
            if (session != null) {
                CommonUtils.closeQuietly(session::closeSession);
            }
            SolaceMetricsUtil.reportConnectionError(CONTEXT_PRODUCER, url.getValue(), messageVpn);
            return CommonUtils.createError("Failed to initialize producer", e);
        }

        // Observability only, deliberately outside the block above: the producer is fully created by this point, so a
        // failure here must not run the cleanup and report an init failure for an init that succeeded.
        SolaceMetricsUtil.reportNewProducer(producer);
        return null;
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
        String destinationKind = getDestinationKind(destinationMap);
        SolaceTracingUtil.traceResourceInvocation(env, producer, destinationName);
        PublishAcknowledgementTracker tracker = null;
        Long correlationKey = null;
        try {
            XMLMessageProducer xmlProducer = (XMLMessageProducer) producer.getNativeData(NATIVE_PRODUCER);
            if (xmlProducer == null) {
                return reportPublishFailure(producer, destinationName, destinationKind, "Producer not initialized");
            }

            Boolean closed = (Boolean) producer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return reportPublishFailure(producer, destinationName, destinationKind, "Producer is closed");
            }

            XMLMessage jcsmpMessage = MessageConverter.toJCSMPMessage(xmlProducer, message);
            injectTraceContext(env, jcsmpMessage);

            if (destinationMap == null || destinationMap.isEmpty()) {
                return reportPublishFailure(producer, destinationName, destinationKind,
                        "Destination must be specified");
            }

            Destination destination = createDestinationFromMap(destinationMap);
            com.solacesystems.jcsmp.Destination jcsmpDestination =
                    DestinationConverter.fromDestinationInterface(destination);

            tracker = (PublishAcknowledgementTracker) producer.getNativeData(NATIVE_PUBLISH_ACK_TRACKER);
            if (tracker != null && jcsmpMessage.getDeliveryMode() != com.solacesystems.jcsmp.DeliveryMode.DIRECT) {
                try {
                    correlationKey = tracker.register();
                    jcsmpMessage.setCorrelationKey(correlationKey);
                } catch (IllegalStateException e) {
                    return reportPublishFailure(producer, destinationName, destinationKind, "Producer is closing");
                }
            }

            final XMLMessage finalMessage = jcsmpMessage;
            final com.solacesystems.jcsmp.Destination finalDestination = jcsmpDestination;
            final Long finalCorrelationKey = correlationKey;
            long startNanos = System.nanoTime();
            Object result = CommonUtils.executeBlocking(() -> {
                xmlProducer.send(finalMessage, finalDestination);
            });
            long elapsedNanos = System.nanoTime() - startNanos;

            if (result instanceof BError bError) {
                if (tracker != null && finalCorrelationKey != null) {
                    tracker.cancel(finalCorrelationKey);
                }
                SolaceMetricsUtil.reportProducerError(producer, destinationName, destinationKind, ERROR_TYPE_PUBLISH);
                return CommonUtils.createError(bError.getMessage());
            }

            int size = getPayloadSize(message);
            SolaceMetricsUtil.reportPublish(producer, destinationName, destinationKind, getDeliveryMode(message), size,
                    elapsedNanos);
            return null;
        } catch (Exception e) {
            if (tracker != null && correlationKey != null) {
                tracker.cancel(correlationKey);
            }
            SolaceMetricsUtil.reportProducerError(producer, destinationName, destinationKind, ERROR_TYPE_PUBLISH);
            return CommonUtils.createError("Failed to send message", e);
        }
    }

    /**
     * Counts a publish that failed before reaching the broker and returns the error.
     */
    private static BError reportPublishFailure(BObject producer, String destinationName, String destinationKind,
                                         String errorMessage) {
        SolaceMetricsUtil.reportProducerError(producer, destinationName, destinationKind, ERROR_TYPE_PUBLISH);
        return CommonUtils.createError(errorMessage);
    }

    /**
     * Puts the publishing span's trace context on the outbound message, both in the user properties and in the
     * message's native trace-context field.
     */
    private static void injectTraceContext(Environment env, XMLMessage jcsmpMessage) {
        try {
            SolaceTracingUtil.applyTraceContext(env, jcsmpMessage);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                    "Failed to attach the trace context to the outbound message; publishing it untraced", t);
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
                return reportProducerFailure(producer, ERROR_TYPE_COMMIT,
                        "commit() can only be called on transacted producers. " +
                                "Set connectionConfig.transacted = true to enable transactions."
                );
            }

            TransactedSession txSession = (TransactedSession) producer.getNativeData(NATIVE_TX_SESSION);
            if (txSession == null) {
                return reportProducerFailure(producer, ERROR_TYPE_COMMIT, "TransactedSession not initialized");
            }

            Boolean closed = (Boolean) producer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return reportProducerFailure(producer, ERROR_TYPE_COMMIT, "Producer is closed");
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
                return reportProducerFailure(producer, ERROR_TYPE_ROLLBACK,
                        "rollback() can only be called on transacted producers. " +
                                "Set connectionConfig.transacted = true to enable transactions."
                );
            }

            TransactedSession txSession = (TransactedSession) producer.getNativeData(NATIVE_TX_SESSION);
            if (txSession == null) {
                return reportProducerFailure(producer, ERROR_TYPE_ROLLBACK, "TransactedSession not initialized");
            }

            Boolean closed = (Boolean) producer.getNativeData(NATIVE_CLOSED);
            if (closed != null && closed) {
                return reportProducerFailure(producer, ERROR_TYPE_ROLLBACK, "Producer is closed");
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
        PublishAcknowledgementTracker tracker =
                (PublishAcknowledgementTracker) producer.getNativeData(NATIVE_PUBLISH_ACK_TRACKER);

        Exception firstError = null;
        PublishAcknowledgementTracker.DrainResult drainResult = null;
        if (tracker != null) {
            tracker.beginClose();
            try {
                drainResult = tracker.awaitSettlement(PUBLISH_ACK_DRAIN_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                firstError = e;
            }
        }

        // Attempt to close every resource independently so one failure doesn't block the rest.
        if (xmlProducer != null) {
            Exception e = CommonUtils.attemptClose(xmlProducer::close);
            firstError = firstError == null ? e : firstError;
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
        producer.addNativeData(NATIVE_PUBLISH_ACK_TRACKER, null);

        if (tracker != null) {
            tracker.markClosed();
        }

        SolaceSessionEventHandler.markDisconnected(producer);

        if (firstError != null) {
            SolaceMetricsUtil.reportProducerError(producer, ERROR_TYPE_CLOSE);
            return CommonUtils.createError("Failed to close producer", firstError);
        }
        if (drainResult != null && drainResult.unconfirmedCount() > 0) {
            SolaceMetricsUtil.reportProducerError(producer, ERROR_TYPE_CLOSE);
            return CommonUtils.createError(String.format(
                    "Timed out waiting for publish acknowledgements; %d message(s) remain unconfirmed",
                    drainResult.unconfirmedCount()));
        }
        if (drainResult != null && drainResult.rejectedCount() > 0) {
            SolaceMetricsUtil.reportProducerError(producer, ERROR_TYPE_CLOSE);
            return CommonUtils.createError(String.format(
                    "%d guaranteed publish(es) were rejected before close: %s",
                    drainResult.rejectedCount(), drainResult.firstRejection()));
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

    /**
     * Counts a producer-level failure and returns the error.
     */
    private static BError reportProducerFailure(BObject producer, String errorType, String errorMessage) {
        SolaceMetricsUtil.reportProducerError(producer, errorType);
        return CommonUtils.createError(errorMessage);
    }

    private static String getDestinationName(BMap<BString, Object> destinationMap) {
        if (destinationMap == null || destinationMap.isEmpty()) {
            return UNKNOWN;
        }
        Object queueName = destinationMap.get(QUEUE_NAME_KEY);
        if (queueName instanceof BString bStr) {
            return nameOrUnknown(bStr);
        }
        Object topicName = destinationMap.get(TOPIC_NAME_KEY);
        if (topicName instanceof BString bStr) {
            return nameOrUnknown(bStr);
        }
        return UNKNOWN;
    }

    private static String nameOrUnknown(BString name) {
        String value = name.getValue();
        return value.isBlank() ? UNKNOWN : value;
    }

    private static String getDestinationKind(BMap<BString, Object> destinationMap) {
        if (destinationMap == null || destinationMap.isEmpty()) {
            return UNKNOWN;
        }
        if (destinationMap.containsKey(QUEUE_NAME_KEY)) {
            return DESTINATION_KIND_QUEUE;
        }
        if (destinationMap.containsKey(TOPIC_NAME_KEY)) {
            return DESTINATION_KIND_TOPIC;
        }
        return UNKNOWN;
    }

    /**
     * Reads the message's delivery mode for tagging.
     */
    private static String getDeliveryMode(BMap<BString, Object> message) {
        if (message == null) {
            return UNKNOWN;
        }
        Object mode = message.get(DELIVERY_MODE_KEY);
        return mode instanceof BString bStr ? nameOrUnknown(bStr) : UNKNOWN;
    }

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
        return 0;
    }
}
