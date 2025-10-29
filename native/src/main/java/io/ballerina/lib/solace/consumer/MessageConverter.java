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

package io.ballerina.lib.solace.consumer;

import com.solacesystems.jms.SupportedProperty;
import io.ballerina.lib.solace.BallerinaSolaceDatabindingException;
import io.ballerina.lib.solace.BallerinaSolaceException;
import io.ballerina.lib.solace.ModuleUtils;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.ValueUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.Topic;

import static io.ballerina.runtime.api.creators.ValueCreator.createMapValue;

/**
 * Converter for JMS messages to Ballerina messages.
 */
public final class MessageConverter {

    private static final String MESSAGE_RECORD_NAME = "Message";
    private static final BString MESSAGE_ID = StringUtils.fromString("messageId");
    private static final BString TIMESTAMP = StringUtils.fromString("timestamp");
    private static final BString CORRELATION_ID = StringUtils.fromString("correlationId");
    private static final BString REPLY_TO = StringUtils.fromString("replyTo");
    private static final BString DESTINATION = StringUtils.fromString("destination");
    private static final BString DELIVERY_MODE = StringUtils.fromString("deliveryMode");
    private static final BString REDELIVERED = StringUtils.fromString("redelivered");
    private static final BString JMS_TYPE = StringUtils.fromString("jmsType");
    private static final BString EXPIRATION = StringUtils.fromString("expiration");
    private static final BString PRIORITY = StringUtils.fromString("priority");
    private static final BString PROPERTIES = StringUtils.fromString("properties");
    private static final BString PAYLOAD = StringUtils.fromString("payload");

    private static final BString QUEUE_NAME = StringUtils.fromString("queueName");
    private static final BString TOPIC_NAME = StringUtils.fromString("topicName");

    private static final UnionType MSG_PROPERTY_TYPE = TypeCreator.createUnionType(
            PredefinedTypes.TYPE_BOOLEAN, PredefinedTypes.TYPE_INT, PredefinedTypes.TYPE_BYTE,
            PredefinedTypes.TYPE_FLOAT, PredefinedTypes.TYPE_STRING);
    private static final ArrayType BYTE_ARR_TYPE = TypeCreator.createArrayType(PredefinedTypes.TYPE_BYTE);
    private static final UnionType MSG_VALUE_TYPE = TypeCreator.createUnionType(MSG_PROPERTY_TYPE, BYTE_ARR_TYPE);
    private static final MapType BALLERINA_MSG_PROPERTY_TYPE = TypeCreator.createMapType(
            "Property", MSG_PROPERTY_TYPE, ModuleUtils.getModule());
    private static final MapType BALLERINA_MAP_MSG_TYPE = TypeCreator.createMapType(
            "Value", MSG_VALUE_TYPE, ModuleUtils.getModule());

    public static final String NATIVE_MESSAGE = "native.message";

    private MessageConverter() {
    }

    public static BMap<BString, Object> toBallerinaMessage(Message jmsMessage, BTypedesc expectedType)
            throws JMSException, BallerinaSolaceException {
        RecordType messageType = getRecordType(expectedType);
        RecordType recordType = getRecordType(messageType);
        BMap<BString, Object> ballerinaMessage = ValueCreator.createRecordValue(recordType);

        // Store the native JMS message for later acknowledgement
        ballerinaMessage.addNativeData(NATIVE_MESSAGE, jmsMessage);

        // Set provider-set fields
        String messageId = jmsMessage.getJMSMessageID();
        if (messageId != null) {
            ballerinaMessage.put(MESSAGE_ID, StringUtils.fromString(messageId));
        }

        long timestamp = jmsMessage.getJMSTimestamp();
        if (timestamp > 0) {
            ballerinaMessage.put(TIMESTAMP, timestamp);
        }

        String correlationId = jmsMessage.getJMSCorrelationID();
        if (correlationId != null) {
            ballerinaMessage.put(CORRELATION_ID, StringUtils.fromString(correlationId));
        }

        Destination replyTo = jmsMessage.getJMSReplyTo();
        if (replyTo != null) {
            ballerinaMessage.put(REPLY_TO, convertDestination(replyTo));
        }

        Destination destination = jmsMessage.getJMSDestination();
        if (destination != null) {
            ballerinaMessage.put(DESTINATION, convertDestination(destination));
        }

        int deliveryMode = jmsMessage.getJMSDeliveryMode();
        ballerinaMessage.put(DELIVERY_MODE, (long) deliveryMode);

        boolean redelivered = jmsMessage.getJMSRedelivered();
        ballerinaMessage.put(REDELIVERED, redelivered);

        String jmsType = jmsMessage.getJMSType();
        if (jmsType != null) {
            ballerinaMessage.put(JMS_TYPE, StringUtils.fromString(jmsType));
        }

        long expiration = jmsMessage.getJMSExpiration();
        if (expiration > 0) {
            ballerinaMessage.put(EXPIRATION, expiration);
        }

        int priority = jmsMessage.getJMSPriority();
        ballerinaMessage.put(PRIORITY, (long) priority);

        // Set custom properties
        BMap<BString, Object> properties = extractProperties(jmsMessage);
        if (!properties.isEmpty()) {
            ballerinaMessage.put(PROPERTIES, properties);
        }

        // Set message payload based on message type
        Object payload = getPayloadWithIntendedTypeForBMessage(jmsMessage, recordType);
        ballerinaMessage.put(PAYLOAD, payload);

        return ballerinaMessage;
    }

    private static BMap<BString, Object> convertDestination(Destination destination) throws JMSException {
        BMap<BString, Object> destMap = createMapValue();
        if (destination instanceof Queue queue) {
            destMap.put(QUEUE_NAME, StringUtils.fromString(queue.getQueueName()));
        } else if (destination instanceof Topic topic) {
            destMap.put(TOPIC_NAME, StringUtils.fromString(topic.getTopicName()));
        }
        return destMap;
    }

    private static BMap<BString, Object> extractProperties(Message message) throws JMSException {
        BMap<BString, Object> messageProperties = ValueCreator.createMapValue(BALLERINA_MSG_PROPERTY_TYPE);
        Enumeration<String> propertyNames = message.getPropertyNames();
        Iterator<String> iterator = propertyNames.asIterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = message.getObjectProperty(key);
            messageProperties.put(StringUtils.fromString(key), getMapValue(value));
        }
        return messageProperties;
    }

    private static Object getPayloadWithIntendedTypeForBMessage(Message jmsMessage, RecordType recordType)
            throws JMSException {
        Type intendedType = TypeUtils.getReferredType(recordType.getFields().get(PAYLOAD.getValue()).getFieldType());
        return getPayloadWithIntendedType(jmsMessage, intendedType);
    }

    private static Object getPayloadWithIntendedType(Message jmsMessage, Type payloadType)
            throws JMSException, BallerinaSolaceDatabindingException {
        int typeTag = payloadType.getTag();
        try {
            if (jmsMessage instanceof TextMessage textMessage) {
                return getPayloadFromTextMessage(textMessage, payloadType, typeTag);
            }
            if (jmsMessage instanceof MapMessage mapMessage) {
                return getPayloadFromMapMessage(mapMessage, payloadType, typeTag);
            }
            if (jmsMessage instanceof BytesMessage bytesMessage) {
                return getPayloadFromBytesMessage(bytesMessage, payloadType, typeTag);
            }
        } catch (BError bError) {
            StringBuilder errorMsg = new StringBuilder("Data binding failed: ").append(bError.getDetails());
            throw new BallerinaSolaceDatabindingException(errorMsg.toString());
        }
        throw new BallerinaSolaceDatabindingException(
                String.format("Data binding failed: Unsupported message type '%s'",
                        jmsMessage.getClass().getSimpleName()));
    }

    private static Object getPayloadFromTextMessage(TextMessage message, Type payloadType, int typeTag)
            throws JMSException, BallerinaSolaceDatabindingException {
        if (typeTag == TypeTags.ANYDATA_TAG) {
            if (message.propertyExists(SupportedProperty.SOLACE_JMS_PROP_ISXML) &&
                    message.getBooleanProperty(SupportedProperty.SOLACE_JMS_PROP_ISXML)) {
                return XmlUtils.parse(message.getText());
            }
            return StringUtils.fromString(message.getText());
        }

        if (typeTag != TypeTags.STRING_TAG && typeTag != TypeTags.XML_TAG) {
            throw new BallerinaSolaceDatabindingException(
                    String.format("Data binding failed: Cannot bind TextMessage to type '%s'. " +
                            "Expected 'string' or 'xml'", payloadType));
        }

        if (typeTag == TypeTags.XML_TAG) {
            if (!message.propertyExists(SupportedProperty.SOLACE_JMS_PROP_ISXML) ||
                    !message.getBooleanProperty(SupportedProperty.SOLACE_JMS_PROP_ISXML)) {
                throw new BallerinaSolaceDatabindingException(
                        "Data binding failed: Cannot bind TextMessage to 'xml' type. " +
                                "Message is missing XML marker property (JMS_Solace_isXML=true)");
            }
            return XmlUtils.parse(message.getText());
        }

        return StringUtils.fromString(message.getText());
    }

    private static Object getPayloadFromMapMessage(MapMessage message, Type payloadType, int typeTag)
            throws JMSException, BallerinaSolaceDatabindingException {
        if (!TypeUtils.isSameType(payloadType, BALLERINA_MAP_MSG_TYPE) && typeTag != TypeTags.ANYDATA_TAG
                && typeTag != TypeTags.RECORD_TYPE_TAG) {
            throw new BallerinaSolaceDatabindingException(
                    String.format("Data binding failed: Cannot bind MapMessage to type '%s'. " +
                            "Expected 'map<solace:Value>'", payloadType));
        }

        BMap<BString, Object> payload = ValueCreator.createMapValue(BALLERINA_MAP_MSG_TYPE);
        Enumeration<String> mapNames = message.getMapNames();
        Iterator<String> iterator = mapNames.asIterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = message.getObject(key);
            payload.put(StringUtils.fromString(key), getMapValue(value));
        }
        if (typeTag == TypeTags.RECORD_TYPE_TAG) {
            return ValueUtils.convert(payload, payloadType);
        }
        return payload;
    }

    private static Object getPayloadFromBytesMessage(BytesMessage message, Type payloadType, int typeTag)
            throws JMSException, BallerinaSolaceDatabindingException {
        // Validate that string/xml types are not used with BytesMessage
        if (typeTag == TypeTags.STRING_TAG || typeTag == TypeTags.XML_TAG) {
            throw new BallerinaSolaceDatabindingException(
                    String.format("Data binding failed: Cannot bind BytesMessage to type '%s'. " +
                            "Use TextMessage for string/xml payloads", payloadType));
        }

        // Validate that map<Value> type is not used with BytesMessage
        if (TypeUtils.isSameType(payloadType, BALLERINA_MAP_MSG_TYPE)) {
            throw new BallerinaSolaceDatabindingException(
                    String.format("Data binding failed: Cannot bind BytesMessage to type '%s'. " +
                            "Use MapMessage for map payloads", payloadType));
        }

        long bodyLength = message.getBodyLength();
        byte[] bytes = new byte[(int) bodyLength];
        message.readBytes(bytes);

        // Handle byte array types directly
        if (typeTag == TypeTags.ANYDATA_TAG) {
            return ValueCreator.createArrayValue(bytes);
        }

        if (typeTag == TypeTags.ARRAY_TAG) {
            Type elementType = TypeUtils.getReferredType(((ArrayType) payloadType).getElementType());
            if (elementType.getTag() == TypeTags.BYTE_TAG) {
                return ValueCreator.createArrayValue(bytes);
            }
        }

        // For other types, convert bytes to JSON string and parse
        String jsonString = new String(bytes, StandardCharsets.UTF_8);

        switch (typeTag) {
            case TypeTags.ARRAY_TAG:
            default:
                return getValueFromJson(payloadType, jsonString);
        }
    }

    public static RecordType getRecordType(BTypedesc bTypedesc) {
        RecordType recordType;
        if (bTypedesc.getDescribingType().isReadOnly()) {
            recordType = (RecordType) ((IntersectionType) (bTypedesc.getDescribingType())).getConstituentTypes().get(0);
        } else {
            recordType = (RecordType) bTypedesc.getDescribingType();
        }
        return recordType;
    }

    public static RecordType getRecordType(Type type) {
        if (type.getTag() == TypeTags.INTERSECTION_TAG) {
            return (RecordType) TypeUtils.getReferredType(((IntersectionType) (type)).getConstituentTypes().get(0));
        }
        return (RecordType) type;
    }

    private static Object getValueFromJson(Type type, String stringValue) {
        return ValueUtils.convert(JsonUtils.parse(stringValue), type);
    }

    private static Object getMapValue(Object value) throws BallerinaSolaceDatabindingException {
        if (isPrimitive(value)) {
            Type type = TypeUtils.getType(value);
            return ValueUtils.convert(value, type);
        }
        if (value instanceof String) {
            return StringUtils.fromString((String) value);
        }
        if (value instanceof byte[]) {
            return ValueCreator.createArrayValue((byte[]) value);
        }
        throw new BallerinaSolaceDatabindingException(
                String.format("Data binding failed: Unsupported map value type '%s'",
                        value.getClass().getSimpleName()));
    }

    private static boolean isPrimitive(Object value) {
        return value instanceof Boolean || value instanceof Byte || value instanceof Character ||
                value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double;
    }
}
