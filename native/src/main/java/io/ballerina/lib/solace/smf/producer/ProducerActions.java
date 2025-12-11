package io.ballerina.lib.solace.smf.producer;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.ProducerFlowProperties;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;
import com.solacesystems.jcsmp.transaction.TransactedSession;
import io.ballerina.lib.solace.smf.common.CommonUtils;
import io.ballerina.lib.solace.smf.config.ConfigurationUtils;
import io.ballerina.lib.solace.smf.config.ProducerConfiguration;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_CLOSED;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_PRODUCER;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_SESSION;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_TRANSACTED;
import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_TX_SESSION;

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
        try {
            // Create configuration objects from Ballerina map
            ProducerConfiguration producerConfig = new ProducerConfiguration(config);

            // Build JCSMP properties from configuration (URL passed separately)
            JCSMPProperties jcsmpProps = ConfigurationUtils.buildJCSMPProperties(
                    url.getValue(),
                    producerConfig.connectionConfig());

            // Create and connect base JCSMP session
            JCSMPSession session = JCSMPFactory.onlyInstance().createSession(jcsmpProps);
            session.connect();

            boolean isTransacted = producerConfig.connectionConfig().transacted();
            XMLMessageProducer xmlProducer;
            TransactedSession txSession = null;

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

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to initialize producer", e);
        }
    }

    /**
     * Send a message to the specified destination.
     *
     * @param producer       the Ballerina producer object
     * @param destinationMap the destination (Topic or Queue)
     * @param message        the message to send
     * @return null on success, BError on failure
     */
    public static BError send(BObject producer, BMap<BString, Object> destinationMap, BMap<BString, Object> message) {
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
                    MessageConverter.fromDestinationInterface(destination);

            final XMLMessage finalMessage = jcsmpMessage;
            final com.solacesystems.jcsmp.Destination finalDestination = jcsmpDestination;
            Object result = CommonUtils.executeBlocking(() -> {
                xmlProducer.send(finalMessage, finalDestination);
            });

            if (result instanceof BError bError) {
                return CommonUtils.createError(bError.getMessage());
            }

            return null; // Success
        } catch (Exception e) {
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
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
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
                return (BError) result;
            }

            return null;
        } catch (Exception e) {
            return CommonUtils.createError("Failed to rollback transaction", e);
        }
    }

    /**
     * Check if the producer is closed.
     *
     * @param producer the Ballerina producer object
     * @return true if closed, false otherwise
     */
    public static boolean isClosed(BObject producer) {
        Boolean closed = (Boolean) producer.getNativeData(NATIVE_CLOSED);
        return closed != null && closed;
    }

    /**
     * Close the producer and release resources.
     *
     * @param producer the Ballerina producer object
     * @return null on success, BError on failure
     */
    public static BError close(BObject producer) {
        try {
            XMLMessageProducer xmlProducer = (XMLMessageProducer) producer.getNativeData(NATIVE_PRODUCER);
            JCSMPSession session = (JCSMPSession) producer.getNativeData(NATIVE_SESSION);

            // Close in reverse order: producer, then session
            if (xmlProducer != null) {
                xmlProducer.close();
            }

            if (session != null) {
                session.closeSession();
            }

            // Mark as closed and clear native data
            producer.addNativeData(NATIVE_CLOSED, true);
            producer.addNativeData(NATIVE_PRODUCER, null);
            producer.addNativeData(NATIVE_SESSION, null);

            return null; // Success
        } catch (Exception e) {
            return CommonUtils.createError("Failed to close producer", e);
        }
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
}
