package io.ballerina.lib.solace.smf.consumer;

import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessage;
import io.ballerina.lib.solace.smf.ModuleUtils;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.nio.charset.StandardCharsets;

import static io.ballerina.lib.solace.smf.common.Constants.NATIVE_MESSAGE;

/**
 * Converter for translating JCSMP XMLMessage to Ballerina Message record.
 */
public class MessageConverter {

    // Define union types for map values
    private static final UnionType MSG_PROPERTY_TYPE = TypeCreator.createUnionType(
            PredefinedTypes.TYPE_BOOLEAN, PredefinedTypes.TYPE_INT, PredefinedTypes.TYPE_BYTE,
            PredefinedTypes.TYPE_FLOAT, PredefinedTypes.TYPE_STRING);
    private static final ArrayType BYTE_ARR_TYPE = TypeCreator.createArrayType(PredefinedTypes.TYPE_BYTE);
    private static final UnionType MSG_VALUE_TYPE = TypeCreator.createUnionType(MSG_PROPERTY_TYPE, BYTE_ARR_TYPE);

    // Create typed MapType instances matching Ballerina types
    private static final MapType BALLERINA_MSG_PROPERTY_TYPE = TypeCreator.createMapType(
            "Property", MSG_PROPERTY_TYPE, ModuleUtils.getModule());

    // Ballerina Message record field keys
    private static final BString PAYLOAD_KEY = StringUtils.fromString("payload");
    private static final BString DELIVERY_MODE_KEY = StringUtils.fromString("deliveryMode");
    private static final BString PRIORITY_KEY = StringUtils.fromString("priority");
    private static final BString TIME_TO_LIVE_KEY = StringUtils.fromString("timeToLive");
    private static final BString APPLICATION_MESSAGE_ID_KEY = StringUtils.fromString("applicationMessageId");
    private static final BString APPLICATION_MESSAGE_TYPE_KEY = StringUtils.fromString("applicationMessageType");
    private static final BString CORRELATION_ID_KEY = StringUtils.fromString("correlationId");
    private static final BString REPLY_TO_KEY = StringUtils.fromString("replyTo");
    private static final BString SENDER_ID_KEY = StringUtils.fromString("senderId");
    private static final BString SENDER_TIMESTAMP_KEY = StringUtils.fromString("senderTimestamp");
    private static final BString RECEIVE_TIMESTAMP_KEY = StringUtils.fromString("receiveTimestamp");
    private static final BString SEQUENCE_NUMBER_KEY = StringUtils.fromString("sequenceNumber");
    private static final BString REDELIVERED_KEY = StringUtils.fromString("redelivered");
    private static final BString DELIVERY_COUNT_KEY = StringUtils.fromString("deliveryCount");
    private static final BString PROPERTIES_KEY = StringUtils.fromString("properties");
    private static final BString USER_DATA_KEY = StringUtils.fromString("userData");

    // Destination field keys
    private static final BString TOPIC_NAME_KEY = StringUtils.fromString("topicName");
    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");

    /**
     * Converts a JCSMP XMLMessage to a Ballerina Message record.
     *
     * @param xmlMessage the JCSMP message to convert
     * @return the Ballerina Message record
     * @throws Exception if conversion fails
     */
    public static BMap<BString, Object> toBallerinaMessage(XMLMessage xmlMessage) throws Exception {
        // Create the Message record
        RecordType messageType = getMessageRecordType();
        BMap<BString, Object> message = ValueCreator.createRecordValue(messageType);

        // Extract and set payload
        byte[] payload = extractPayload(xmlMessage);
        message.put(PAYLOAD_KEY, ValueCreator.createArrayValue(payload));

        // Set delivery mode
        message.put(DELIVERY_MODE_KEY, StringUtils.fromString(xmlMessage.getDeliveryMode().toString()));

        // Set priority if present (getPriority returns -1 if not set)
        int priority = xmlMessage.getPriority();
        if (priority >= 0) {
            message.put(PRIORITY_KEY, (byte) priority);
        }

        // Set time to live
        long ttl = xmlMessage.getTimeToLive();
        if (ttl > 0) {
            message.put(TIME_TO_LIVE_KEY, (int) ttl);
        }

        // Set application message ID if present
        String appMsgId = xmlMessage.getApplicationMessageId();
        if (appMsgId != null) {
            message.put(APPLICATION_MESSAGE_ID_KEY, StringUtils.fromString(appMsgId));
        }

        // Set application message type if present
        String appMsgType = xmlMessage.getApplicationMessageType();
        if (appMsgType != null) {
            message.put(APPLICATION_MESSAGE_TYPE_KEY, StringUtils.fromString(appMsgType));
        }

        // Set correlation ID if present
        String correlationId = xmlMessage.getCorrelationId();
        if (correlationId != null) {
            message.put(CORRELATION_ID_KEY, StringUtils.fromString(correlationId));
        }

        // Set reply-to destination if present
        Destination replyTo = xmlMessage.getReplyTo();
        if (replyTo != null) {
            BMap<BString, Object> replyToMap = convertDestination(replyTo);
            if (replyToMap != null) {
                message.put(REPLY_TO_KEY, replyToMap);
            }
        }

        // Set sender ID if present
        String senderId = xmlMessage.getSenderId();
        if (senderId != null) {
            message.put(SENDER_ID_KEY, StringUtils.fromString(senderId));
        }

        // Set sender timestamp if present
        Long senderTimestamp = xmlMessage.getSenderTimestamp();
        if (senderTimestamp != null) {
            message.put(SENDER_TIMESTAMP_KEY, senderTimestamp.intValue());
        }

        // Set receive timestamp if present (0 means not set)
        long receiveTimestamp = xmlMessage.getReceiveTimestamp();
        if (receiveTimestamp > 0) {
            message.put(RECEIVE_TIMESTAMP_KEY, (int) receiveTimestamp);
        }

        // Set sequence number if present
        Long sequenceNumber = xmlMessage.getSequenceNumber();
        if (sequenceNumber != null) {
            message.put(SEQUENCE_NUMBER_KEY, sequenceNumber.intValue());
        }

        // Set redelivered flag
        message.put(REDELIVERED_KEY, xmlMessage.getRedelivered());

        try {
            int deliveryCount = xmlMessage.getDeliveryCount();
            if (deliveryCount > 0) {
                message.put(DELIVERY_COUNT_KEY, deliveryCount);
            }
        } catch (UnsupportedOperationException ignored) {
        }

        // Set properties if present
        SDTMap sdtProperties = xmlMessage.getProperties();
        if (sdtProperties != null) {
            BMap<BString, Object> properties = convertSDTMapToBallerina(sdtProperties);
            if (!properties.isEmpty()) {
                message.put(PROPERTIES_KEY, properties);
            }
        }

        // Set user data if present
        if (xmlMessage.hasUserData()) {
            byte[] userData = xmlMessage.getUserData();
            if (userData != null && userData.length > 0) {
                message.put(USER_DATA_KEY, ValueCreator.createArrayValue(userData));
            }
        }

        // Store native message for acknowledgement operations
        message.addNativeData(NATIVE_MESSAGE, xmlMessage);

        return message;
    }

    /**
     * Extracts the native XMLMessage from a Ballerina Message record.
     *
     * @param message the Ballerina Message record
     * @return the native XMLMessage, or null if not found
     */
    public static XMLMessage extractNativeMessage(BMap<BString, Object> message) {
        Object nativeMsg = message.getNativeData(NATIVE_MESSAGE);
        if (nativeMsg instanceof XMLMessage) {
            return (XMLMessage) nativeMsg;
        }
        return null;
    }

    /**
     * Extracts the binary payload from an XMLMessage. Reads from attachment part instead of content part.
     */
    private static byte[] extractPayload(XMLMessage xmlMessage) {
        // Check if message has attachment (primary payload location)
        if (xmlMessage.hasAttachment()) {
            int attachmentLength = xmlMessage.getAttachmentContentLength();
            if (attachmentLength > 0) {
                byte[] attachment = new byte[attachmentLength];
                xmlMessage.readAttachmentBytes(attachment);
                return attachment;
            }
        }

        // Fallback: If no attachment, try reading from content (for backward compatibility)
        if (xmlMessage instanceof TextMessage textMessage) {
            String text = textMessage.getText();
            if (text != null) {
                return text.getBytes(StandardCharsets.UTF_8);
            }
        } else if (xmlMessage.hasContent()) {
            int contentLength = xmlMessage.getContentLength();
            if (contentLength > 0) {
                byte[] content = new byte[contentLength];
                xmlMessage.readContentBytes(content);
                return content;
            }
        }

        return new byte[0];
    }

    /**
     * Converts a JCSMP Destination to a Ballerina Topic or Queue record.
     */
    private static BMap<BString, Object> convertDestination(Destination destination) {
        BMap<BString, Object> destMap = ValueCreator.createMapValue();
        if (destination instanceof Topic topic) {
            destMap.put(TOPIC_NAME_KEY, StringUtils.fromString(topic.getName()));
        } else if (destination instanceof Queue queue) {
            destMap.put(QUEUE_NAME_KEY, StringUtils.fromString(queue.getName()));
        }
        return null;
    }

    /**
     * Converts an SDTMap to a Ballerina map<anydata>.
     */
    private static BMap<BString, Object> convertSDTMapToBallerina(SDTMap sdtMap) throws SDTException {
        // Create typed map with BALLERINA_MSG_PROPERTY_TYPE
        BMap<BString, Object> messageProperties = ValueCreator.createMapValue(BALLERINA_MSG_PROPERTY_TYPE);

        for (String key : sdtMap.keySet()) {
            Object value = sdtMap.get(key);

            if (value == null) {
                continue;
            }

            // Convert SDT value to Ballerina value
            Object ballerinaValue = convertSDTValue(value);
            if (ballerinaValue != null) {
                messageProperties.put(StringUtils.fromString(key), ballerinaValue);
            }
        }

        return messageProperties;
    }

    /**
     * Converts an SDT value to a Ballerina-compatible value.
     */
    private static Object convertSDTValue(Object value) throws SDTException {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return StringUtils.fromString((String) value);
        } else if (value instanceof Boolean) {
            return value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return value;
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            return value;
        } else if (value instanceof Byte) {
            return ((Byte) value).longValue();
        } else if (value instanceof Short) {
            return ((Short) value).longValue();
        } else if (value instanceof byte[]) {
            return ValueCreator.createArrayValue((byte[]) value);
        } else if (value instanceof SDTMap nestedMap) {
            return convertSDTMapToBallerina(nestedMap);
        }

        // For unsupported types, convert to string
        return StringUtils.fromString(value.toString());
    }

    /**
     * Gets the Message record type from the Ballerina module.
     */
    private static RecordType getMessageRecordType() {
        return (RecordType) ValueCreator.createRecordValue(
                ModuleUtils.getModule(),
                "Message"
        ).getType();
    }
}
