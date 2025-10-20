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
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Utility methods for consumer operations.
 */
public final class ConsumerUtils {

    private ConsumerUtils() {}

    /**
     * Creates a Ballerina error with the given message.
     *
     * @param message error message
     * @return Ballerina error
     */
    public static BError createError(String message) {
        return ErrorCreator.createError(ModuleUtils.getModule(), "Error",
                StringUtils.fromString(message), null, null);
    }

    /**
     * Creates a Ballerina error with the given message and cause.
     *
     * @param message error message
     * @param cause   throwable cause
     * @return Ballerina error
     */
    public static BError createError(String message, Throwable cause) {
        return ErrorCreator.createError(ModuleUtils.getModule(), "Error",
                StringUtils.fromString(message), ErrorCreator.createError(cause), null);
    }

    /**
     * Creates a JMS MessageConsumer based on subscription configuration.
     *
     * @param session            JMS session
     * @param subscriptionConfig subscription configuration
     * @return JMS MessageConsumer
     * @throws JMSException if consumer creation fails
     */
    public static MessageConsumer createConsumer(Session session, SubscriptionConfig subscriptionConfig)
            throws JMSException {
        return switch (subscriptionConfig) {
            case QueueConfig queueConfig -> createQueueConsumer(session, queueConfig);
            case TopicConfig topicConfig -> createTopicConsumer(session, topicConfig);
        };
    }

    private static MessageConsumer createQueueConsumer(Session session, QueueConfig config) throws JMSException {
        Queue queue = session.createQueue(config.queueName());
        String messageSelector = config.messageSelector();

        if (messageSelector != null && !messageSelector.isEmpty()) {
            return session.createConsumer(queue, messageSelector);
        } else {
            return session.createConsumer(queue);
        }
    }

    private static MessageConsumer createTopicConsumer(Session session, TopicConfig config) throws JMSException {
        Topic topic = session.createTopic(config.topicName());
        String messageSelector = config.messageSelector();
        String subscriberName = config.subscriberName();

        return switch (config.consumerType()) {
            case DEFAULT -> {
                if (messageSelector != null && !messageSelector.isEmpty()) {
                    yield session.createConsumer(topic, messageSelector, config.noLocal());
                } else {
                    yield session.createConsumer(topic);
                }
            }
            case DURABLE -> {
                if (subscriberName == null || subscriberName.isEmpty()) {
                    throw new IllegalArgumentException("Subscriber name is required for DURABLE consumer type");
                }
                if (messageSelector != null && !messageSelector.isEmpty()) {
                    yield session.createDurableSubscriber(topic, subscriberName, messageSelector, config.noLocal());
                } else {
                    yield session.createDurableSubscriber(topic, subscriberName);
                }
            }
            case SHARED -> {
                if (subscriberName == null || subscriberName.isEmpty()) {
                    throw new IllegalArgumentException("Subscriber name is required for SHARED consumer type");
                }
                if (messageSelector != null && !messageSelector.isEmpty()) {
                    yield session.createSharedConsumer(topic, subscriberName, messageSelector);
                } else {
                    yield session.createSharedConsumer(topic, subscriberName);
                }
            }
            case SHARED_DURABLE -> {
                if (subscriberName == null || subscriberName.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Subscriber name is required for SHARED_DURABLE consumer type");
                }
                if (messageSelector != null && !messageSelector.isEmpty()) {
                    yield session.createSharedDurableConsumer(topic, subscriberName, messageSelector);
                } else {
                    yield session.createSharedDurableConsumer(topic, subscriberName);
                }
            }
        };
    }
}
