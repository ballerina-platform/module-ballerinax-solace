package io.ballerina.lib.solace.smf.producer;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.TextXMLMessage;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Converter for translating Ballerina messages to JCSMP message types.
 */
public class MessageConverter {

    private static final String PAYLOAD = "payload";
    private static final String PROPERTIES = "properties";
    private static final String NAME = "name";

    /**
     * Converts a Ballerina Message to a JCSMP XMLMessage.
     */
    public static XMLMessage toJCSMPMessage(XMLMessageProducer producer, BMap<BString, Object> message)
            throws Exception {
        Object payload = message.get(StringUtils.fromString(PAYLOAD));

        XMLMessage jcsmpMessage;
        if (payload instanceof BString) {
            jcsmpMessage = toTextMessage(producer, (BString) payload);
        } else if (payload instanceof BArray) {
            jcsmpMessage = toByteMessage(producer, ((BArray) payload).getBytes());
        } else {
            throw new Exception("Unsupported payload type: " + (payload != null ? payload.getClass().getName() : "null"));
        }

        // Add custom properties if provided
        @SuppressWarnings("unchecked")
        BMap<BString, Object> properties =
                (BMap<BString, Object>) message.get(StringUtils.fromString(PROPERTIES));
        if (properties != null) {
            addPropertiesToMessage(jcsmpMessage, properties);
        }

        return jcsmpMessage;
    }

    /**
     * Creates a TextXMLMessage from a string payload.
     */
    public static TextXMLMessage toTextMessage(XMLMessageProducer producer, BString content)
            throws Exception {
        TextXMLMessage message = producer.createTextXMLMessage();
        message.setText(content.getValue());
        return message;
    }

    /**
     * Creates a BytesXMLMessage from byte array payload.
     */
    public static BytesXMLMessage toByteMessage(XMLMessageProducer producer, byte[] content)
            throws Exception {
        BytesXMLMessage message = producer.createBytesXMLMessage();
        message.writeBytes(content);
        return message;
    }

    /**
     * Converts a Ballerina Destination record to a JCSMP Destination.
     */
    public static com.solacesystems.jcsmp.Destination toJCSMPDestination(
            BMap<BString, Object> destinationMap)
            throws Exception {
        if (destinationMap == null) {
            throw new Exception("Destination cannot be null");
        }

        // Determine which destination type based on field presence
        BString nameKey = StringUtils.fromString(NAME);
        Object nameObj = destinationMap.get(nameKey);
        if (nameObj == null) {
            throw new Exception("Destination must have a 'name' field");
        }

        String destinationName = nameObj.toString();

        // For now, default to Topic (can be enhanced to detect queue type)
        return JCSMPFactory.onlyInstance().createTopic(destinationName);
    }

    /**
     * Converts a sealed Destination interface to a JCSMP Destination.
     */
    public static com.solacesystems.jcsmp.Destination fromDestinationInterface(
            io.ballerina.lib.solace.smf.producer.Destination destination)
            throws Exception {
        if (destination == null) {
            throw new Exception("Destination cannot be null");
        }

        if (destination instanceof Topic topic) {
            return JCSMPFactory.onlyInstance().createTopic(topic.name());
        } else if (destination instanceof Queue queue) {
            return JCSMPFactory.onlyInstance().createQueue(queue.name());
        } else {
            throw new Exception("Unknown destination type: " + destination.getClass().getName());
        }
    }

    /**
     * Adds custom properties to a JCSMP message.
     */
    @SuppressWarnings("unused")
    private static void addPropertiesToMessage(XMLMessage message, BMap<BString, Object> properties) {
        // JCSMP doesn't support the same property setting as JMS
        // Properties would need to be set differently depending on message type
        // For now, we'll skip this - can be enhanced later
    }
}
