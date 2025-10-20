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

import io.ballerina.lib.solace.ModuleUtils;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.Enumeration;

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
    private static final BString CONTENT = StringUtils.fromString("content");

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

    private MessageConverter() {}

    /**
     * Converts JMS message to Ballerina message.
     *
     * @param jmsMessage JMS message
     * @return Ballerina message map
     * @throws JMSException if message conversion fails
     */
    public static BMap<BString, Object> toBallerinaMessage(Message jmsMessage) throws JMSException {
        BMap<BString, Object> ballerinaMessage = ValueCreator.createRecordValue(ModuleUtils.getModule(),
                MESSAGE_RECORD_NAME);

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

        // Set message content based on message type
        Object content = extractContent(jmsMessage);
        ballerinaMessage.put(CONTENT, content);

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

    private static BMap<BString, Object> extractProperties(Message jmsMessage) throws JMSException {
        BMap<BString, Object> properties = ValueCreator.createMapValue(BALLERINA_MSG_PROPERTY_TYPE);
        Enumeration<?> propertyNames = jmsMessage.getPropertyNames();

        while (propertyNames.hasMoreElements()) {
            String propertyName = (String) propertyNames.nextElement();
            BString key = StringUtils.fromString(propertyName);
            Object value = jmsMessage.getObjectProperty(propertyName);

            if (value instanceof String s) {
                properties.put(key, StringUtils.fromString(s));
            } else if (value instanceof Integer i) {
                properties.put(key, i.longValue());
            } else if (value instanceof Long l) {
                properties.put(key, l);
            } else if (value instanceof Double d) {
                properties.put(key, d);
            } else if (value instanceof Float f) {
                properties.put(key, f.doubleValue());
            } else if (value instanceof Boolean b) {
                properties.put(key, b);
            } else if (value instanceof Byte b) {
                properties.put(key, b.longValue());
            }
        }

        return properties;
    }

    private static Object extractContent(Message jmsMessage) throws JMSException {
        if (jmsMessage instanceof TextMessage textMessage) {
            String text = textMessage.getText();
            return text != null ? StringUtils.fromString(text) : StringUtils.fromString("");
        } else if (jmsMessage instanceof BytesMessage bytesMessage) {
            long bodyLength = bytesMessage.getBodyLength();
            byte[] bytes = new byte[(int) bodyLength];
            bytesMessage.readBytes(bytes);
            return ValueCreator.createArrayValue(bytes);
        } else if (jmsMessage instanceof MapMessage mapMessage) {
            BMap<BString, Object> map = ValueCreator.createMapValue(BALLERINA_MAP_MSG_TYPE);
            Enumeration<?> mapNames = mapMessage.getMapNames();

            while (mapNames.hasMoreElements()) {
                String name = (String) mapNames.nextElement();
                BString key = StringUtils.fromString(name);
                Object value = mapMessage.getObject(name);

                if (value instanceof String s) {
                    map.put(key, StringUtils.fromString(s));
                } else if (value instanceof Integer i) {
                    map.put(key, i.longValue());
                } else if (value instanceof Long l) {
                    map.put(key, l);
                } else if (value instanceof Double d) {
                    map.put(key, d);
                } else if (value instanceof Float f) {
                    map.put(key, f.doubleValue());
                } else if (value instanceof Boolean b) {
                    map.put(key, b);
                } else if (value instanceof Byte b) {
                    map.put(key, b.longValue());
                } else if (value instanceof byte[] byteArray) {
                    map.put(key, ValueCreator.createArrayValue(byteArray));
                }
            }
            return map;
        }

        // For other message types, return empty string as fallback
        return StringUtils.fromString("");
    }
}
