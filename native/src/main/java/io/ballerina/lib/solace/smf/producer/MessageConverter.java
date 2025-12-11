package io.ballerina.lib.solace.smf.producer;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
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
    private static final BString SEQUENCE_NUMBER_KEY = StringUtils.fromString("sequenceNumber");
    private static final BString PROPERTIES_KEY = StringUtils.fromString("properties");
    private static final BString USER_DATA_KEY = StringUtils.fromString("userData");

    // Destination field keys
    private static final BString TOPIC_NAME_KEY = StringUtils.fromString("topicName");
    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");

    // DeliveryMode values (matching Ballerina types.bal)
    private static final String DELIVERY_MODE_DIRECT = "DIRECT";
    private static final String DELIVERY_MODE_PERSISTENT = "PERSISTENT";

    /**
     * Converts a Ballerina Message to a JCSMP XMLMessage.
     *
     * @param producer the JCSMP message producer (used for message creation)
     * @param message  the Ballerina Message record
     * @return the JCSMP XMLMessage with all fields set
     * @throws Exception if conversion fails
     */
    public static XMLMessage toJCSMPMessage(XMLMessageProducer producer, BMap<BString, Object> message)
            throws Exception {
        Object payload = message.get(PAYLOAD_KEY);

        XMLMessage jcsmpMessage;
        if (payload instanceof BArray) {
            jcsmpMessage = toByteMessage(producer, ((BArray) payload).getBytes());
        } else {
            throw new Exception(
                    "Unsupported payload type: " + (payload != null ? payload.getClass().getName() : "null"));
        }

        // Set all message fields from Ballerina Message record
        setMessageFields(jcsmpMessage, message);

        return jcsmpMessage;
    }

    /**
     * Sets all message fields on a JCSMP XMLMessage from a Ballerina Message record.
     *
     * @param jcsmpMessage the JCSMP message to configure
     * @param message      the Ballerina Message record containing field values
     * @throws SDTException if SDTMap conversion fails
     */
    private static void setMessageFields(XMLMessage jcsmpMessage, BMap<BString, Object> message) throws SDTException {
        // Delivery mode
        String deliveryMode = message.getStringValue(DELIVERY_MODE_KEY).getValue();
        if (deliveryMode != null) {
            jcsmpMessage.setDeliveryMode(DeliveryMode.valueOf(deliveryMode));
        }

        // Priority (0-255)
        Object priority = message.get(PRIORITY_KEY);
        if (priority != null) {
            int priorityValue = ((Number) priority).intValue();
            jcsmpMessage.setPriority(priorityValue);
        }

        // Time to live in milliseconds
        Object timeToLive = message.get(TIME_TO_LIVE_KEY);
        if (timeToLive != null) {
            long ttl = ((Number) timeToLive).longValue();
            jcsmpMessage.setTimeToLive(ttl);
        }

        // Application message ID
        Object appMsgId = message.get(APPLICATION_MESSAGE_ID_KEY);
        if (appMsgId != null) {
            jcsmpMessage.setApplicationMessageId(appMsgId.toString());
        }

        // Application message type
        Object appMsgType = message.get(APPLICATION_MESSAGE_TYPE_KEY);
        if (appMsgType != null) {
            jcsmpMessage.setApplicationMessageType(appMsgType.toString());
        }

        // Correlation ID
        Object correlationId = message.get(CORRELATION_ID_KEY);
        if (correlationId != null) {
            jcsmpMessage.setCorrelationId(correlationId.toString());
        }

        // Reply-to destination
        Object replyTo = message.get(REPLY_TO_KEY);
        if (replyTo instanceof BMap) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> replyToMap = (BMap<BString, Object>) replyTo;
            Destination replyToDestination = convertBallerinaDestination(replyToMap);
            if (replyToDestination != null) {
                jcsmpMessage.setReplyTo(replyToDestination);
            }
        }

        // Sender ID
        Object senderId = message.get(SENDER_ID_KEY);
        if (senderId != null) {
            jcsmpMessage.setSenderId(senderId.toString());
        }

        // Sender timestamp (milliseconds from epoch)
        Object senderTimestamp = message.get(SENDER_TIMESTAMP_KEY);
        if (senderTimestamp != null) {
            long timestamp = ((Number) senderTimestamp).longValue();
            jcsmpMessage.setSenderTimestamp(timestamp);
        }

        // Sequence number
        Object sequenceNumber = message.get(SEQUENCE_NUMBER_KEY);
        if (sequenceNumber != null) {
            long seqNum = ((Number) sequenceNumber).longValue();
            jcsmpMessage.setSequenceNumber(seqNum);
        }

        // User data (max 36 bytes)
        Object userData = message.get(USER_DATA_KEY);
        if (userData instanceof BArray) {
            byte[] userDataBytes = ((BArray) userData).getBytes();
            if (userDataBytes.length > 36) {
                throw new IllegalArgumentException("userData cannot exceed 36 bytes, got " + userDataBytes.length);
            }
            jcsmpMessage.setUserData(userDataBytes);
        }

        // Custom properties (SDTMap)
        Object properties = message.get(PROPERTIES_KEY);
        if (properties instanceof BMap) {
            BMap<BString, Object> propsMap = (BMap<BString, Object>) properties;
            SDTMap sdtMap = convertToSDTMap(propsMap);
            if (sdtMap != null && !sdtMap.isEmpty()) {
                jcsmpMessage.setProperties(sdtMap);
            }
        }
    }

    /**
     * Converts a Ballerina Destination (Topic or Queue) to a JCSMP Destination.
     *
     * @param destinationMap the Ballerina destination map
     * @return the JCSMP Destination, or null if invalid
     */
    private static Destination convertBallerinaDestination(BMap<BString, Object> destinationMap) {
        if (destinationMap == null) {
            return null;
        }

        // Check for topic
        Object topicName = destinationMap.get(TOPIC_NAME_KEY);
        if (topicName != null) {
            return JCSMPFactory.onlyInstance().createTopic(topicName.toString());
        }

        // Check for queue
        Object queueName = destinationMap.get(QUEUE_NAME_KEY);
        if (queueName != null) {
            return JCSMPFactory.onlyInstance().createQueue(queueName.toString());
        }

        return null;
    }

    /**
     * Converts a Ballerina map to an SDTMap for message properties.
     *
     * @param propsMap the Ballerina properties map
     * @return the SDTMap
     * @throws SDTException if conversion fails
     */
    private static SDTMap convertToSDTMap(BMap<BString, Object> propsMap) throws SDTException {
        if (propsMap == null || propsMap.isEmpty()) {
            return null;
        }

        SDTMap sdtMap = JCSMPFactory.onlyInstance().createMap();

        for (BString key : propsMap.getKeys()) {
            Object value = propsMap.get(key);
            String keyStr = key.getValue();

            switch (value) {
                case null -> {
                    continue;
                }

                // Convert Ballerina value to SDT-compatible value
                case BString bString -> sdtMap.putString(keyStr, value.toString());
                case Boolean b -> sdtMap.putBoolean(keyStr, b);
                case Long l -> sdtMap.putLong(keyStr, l);
                case Integer i -> sdtMap.putInteger(keyStr, i);
                case Double v -> sdtMap.putDouble(keyStr, v);
                case Float v -> sdtMap.putFloat(keyStr, v);
                case BArray bArray -> sdtMap.putBytes(keyStr, bArray.getBytes());
                default ->
                    // For other types, convert to string
                        sdtMap.putString(keyStr, value.toString());
            }

        }

        return sdtMap;
    }

    /**
     * Creates a BytesMessage from byte array payload. Uses attachment part instead of content part for the payload.
     */
    public static BytesMessage toByteMessage(XMLMessageProducer producer, byte[] content)
            throws Exception {
        if (producer == null) {
            throw new Exception("XMLMessageProducer cannot be null");
        }
        if (content == null) {
            throw new Exception("Message content cannot be null");
        }

        BytesMessage message = producer.createBytesMessage();
        if (message == null) {
            throw new Exception("Failed to create BytesXMLMessage");
        }

        // Use attachment instead of content
        message.setData(content);
        return message;
    }

    /**
     * Converts a sealed Destination interface to a JCSMP Destination.
     *
     * @param destination the Ballerina Destination (Topic or Queue)
     * @return the JCSMP Destination
     * @throws Exception if destination is null or invalid type
     */
    public static Destination fromDestinationInterface(
            io.ballerina.lib.solace.smf.producer.Destination destination)
            throws Exception {
        if (destination == null) {
            throw new Exception("Destination cannot be null");
        }

        if (destination instanceof Topic(String topicName)) {
            return JCSMPFactory.onlyInstance().createTopic(topicName);
        } else if (destination instanceof Queue(String queueName)) {
            return JCSMPFactory.onlyInstance().createQueue(queueName);
        } else {
            throw new Exception("Unknown destination type: " + destination.getClass().getName());
        }
    }
}
