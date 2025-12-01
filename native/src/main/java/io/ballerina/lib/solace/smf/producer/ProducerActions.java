package io.ballerina.lib.solace.smf.producer;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;
import io.ballerina.lib.solace.smf.common.CommonUtils;
import io.ballerina.lib.solace.smf.config.ConfigurationUtils;
import io.ballerina.lib.solace.smf.config.ProducerConfiguration;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

/**
 * Producer actions - main entry point for Ballerina interop.
 */
public class ProducerActions {

    private static final String NATIVE_PRODUCER = "native.producer";
    private static final String NATIVE_SESSION = "native.session";

    /**
     * Initialize the producer with connection and create session and producer.
     */
    public static BError init(BObject producer, BMap<BString, Object> config) {
        try {
            // Create configuration objects from Ballerina map
            ProducerConfiguration producerConfig = new ProducerConfiguration(config);

            // Build JCSMP properties from configuration
            JCSMPProperties jcsmpProps = ConfigurationUtils.buildJCSMPProperties(
                    producerConfig.connectionConfig());

            // Create and connect JCSMP session
            JCSMPSession session = JCSMPFactory.onlyInstance().createSession(jcsmpProps);
            session.connect();

            // Create message producer from session with publish event handler
            XMLMessageProducer xmlProducer = session.getMessageProducer(new PublishEventHandler());

            // Store in native data for later use
            producer.addNativeData(NATIVE_SESSION, session);
            producer.addNativeData(NATIVE_PRODUCER, xmlProducer);

            return null; // Success
        } catch (Exception e) {
            return CommonUtils.createError("Failed to initialize producer", e);
        }
    }

    /**
     * Send a message to the specified destination.
     */
    public static BError send(BObject producer, BMap<BString, Object> message, BMap<BString, Object> destinationMap) {
        try {
            XMLMessageProducer xmlProducer = (XMLMessageProducer) producer.getNativeData(NATIVE_PRODUCER);
            if (xmlProducer == null) {
                return CommonUtils.createError("Producer not initialized");
            }

            // Convert Ballerina message to JCSMP message
            XMLMessage jcsmpMessage = MessageConverter.toJCSMPMessage(xmlProducer, message);

            // Get destination - convert map to sealed interface then to JCSMP destination
            com.solacesystems.jcsmp.Destination jcsmpDestination = null;
            if (destinationMap != null && !destinationMap.isEmpty()) {
                // Convert BMap to sealed Destination interface using factory method
                io.ballerina.lib.solace.smf.producer.Destination destination = createDestinationFromMap(destinationMap);
                jcsmpDestination = MessageConverter.fromDestinationInterface(destination);
            }

            if (jcsmpDestination == null) {
                return CommonUtils.createError("Destination must be specified");
            }

            // Execute send on virtual thread (blocking operation)
            final XMLMessage finalMessage = jcsmpMessage;
            final com.solacesystems.jcsmp.Destination finalDestination = jcsmpDestination;
            Object result = CommonUtils.executeBlocking(() -> {
                xmlProducer.send(finalMessage, finalDestination);
            });

            if (result instanceof BError) {
                return (BError) result;
            }

            return null; // Success
        } catch (Exception e) {
            return CommonUtils.createError("Failed to send message", e);
        }
    }

    /**
     * Factory method to create Destination sealed interface from BMap.
     */
    private static io.ballerina.lib.solace.smf.producer.Destination createDestinationFromMap(
            BMap<BString, Object> destinationMap) {
        // For now, assume it's always a Topic (can be enhanced to detect based on fields)
        return new Topic(destinationMap);
    }

    /**
     * Close the producer and release resources.
     */
    public static BError close(BObject producer) {
        try {
            XMLMessageProducer xmlProducer = (XMLMessageProducer) producer.getNativeData(NATIVE_PRODUCER);
            JCSMPSession session = (JCSMPSession) producer.getNativeData(NATIVE_SESSION);

            // Close in reverse order: producer, then session
            if (xmlProducer != null) {
                try {
                    xmlProducer.close();
                } catch (Exception e) {
                    // Log but continue with session close
                    // Error is ignored to ensure session cleanup continues
                }
            }

            if (session != null) {
                try {
                    session.closeSession();
                } catch (Exception e) {
                    // Log but continue
                    // Error is ignored to ensure cleanup completes
                }
            }

            // Clear native data
            producer.addNativeData(NATIVE_PRODUCER, null);
            producer.addNativeData(NATIVE_SESSION, null);

            return null; // Success
        } catch (Exception e) {
            return CommonUtils.createError("Failed to close producer", e);
        }
    }
}
