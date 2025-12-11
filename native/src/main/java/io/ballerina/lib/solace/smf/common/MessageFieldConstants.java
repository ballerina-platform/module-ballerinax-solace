package io.ballerina.lib.solace.smf.common;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

/**
 * Constants for Ballerina Message record field keys. Shared between consumer and producer MessageConverter classes.
 */
public class MessageFieldConstants {

    // Message field keys
    public static final BString PAYLOAD_KEY = StringUtils.fromString("payload");
    public static final BString DELIVERY_MODE_KEY = StringUtils.fromString("deliveryMode");
    public static final BString PRIORITY_KEY = StringUtils.fromString("priority");
    public static final BString TIME_TO_LIVE_KEY = StringUtils.fromString("timeToLive");
    public static final BString APPLICATION_MESSAGE_ID_KEY = StringUtils.fromString("applicationMessageId");
    public static final BString APPLICATION_MESSAGE_TYPE_KEY = StringUtils.fromString("applicationMessageType");
    public static final BString CORRELATION_ID_KEY = StringUtils.fromString("correlationId");
    public static final BString REPLY_TO_KEY = StringUtils.fromString("replyTo");
    public static final BString SENDER_ID_KEY = StringUtils.fromString("senderId");
    public static final BString SENDER_TIMESTAMP_KEY = StringUtils.fromString("senderTimestamp");
    public static final BString RECEIVE_TIMESTAMP_KEY = StringUtils.fromString("receiveTimestamp");
    public static final BString SEQUENCE_NUMBER_KEY = StringUtils.fromString("sequenceNumber");
    public static final BString REDELIVERED_KEY = StringUtils.fromString("redelivered");
    public static final BString DELIVERY_COUNT_KEY = StringUtils.fromString("deliveryCount");
    public static final BString PROPERTIES_KEY = StringUtils.fromString("properties");
    public static final BString USER_DATA_KEY = StringUtils.fromString("userData");

    // Destination field keys
    public static final BString TOPIC_NAME_KEY = StringUtils.fromString("topicName");
    public static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");

}
