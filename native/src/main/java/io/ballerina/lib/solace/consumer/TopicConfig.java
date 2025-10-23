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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Topic subscription configuration.
 *
 * @param sessionAckMode Session acknowledgement mode
 * @param topicName Topic name
 * @param messageSelector Message selector (optional)
 * @param noLocal No local flag for topic subscribers
 * @param consumerType Consumer type (DEFAULT, DURABLE, SHARED, SHARED_DURABLE)
 * @param subscriberName Subscriber name for durable/shared subscriptions
 */
public record TopicConfig(
        AcknowledgementMode sessionAckMode,
        String topicName,
        String messageSelector,
        boolean noLocal,
        ConsumerType consumerType,
        String subscriberName) implements SubscriptionConfig {

    private static final BString SESSION_ACK_MODE_KEY = StringUtils.fromString("sessionAckMode");
    private static final BString TOPIC_NAME_KEY = StringUtils.fromString("topicName");
    private static final BString MESSAGE_SELECTOR_KEY = StringUtils.fromString("messageSelector");
    private static final BString NO_LOCAL_KEY = StringUtils.fromString("noLocal");
    private static final BString CONSUMER_TYPE_KEY = StringUtils.fromString("consumerType");
    private static final BString SUBSCRIBER_NAME_KEY = StringUtils.fromString("subscriberName");

    public TopicConfig(BMap<BString, Object> config) {
        this(
                AcknowledgementMode.valueOf(
                        config.getStringValue(SESSION_ACK_MODE_KEY).getValue()),
                config.getStringValue(TOPIC_NAME_KEY).getValue(),
                config.containsKey(MESSAGE_SELECTOR_KEY)
                        ? config.getStringValue(MESSAGE_SELECTOR_KEY).getValue()
                        : null,
                config.containsKey(NO_LOCAL_KEY)
                        ? config.getBooleanValue(NO_LOCAL_KEY)
                        : false,
                ConsumerType.valueOf(
                        config.getStringValue(CONSUMER_TYPE_KEY).getValue()),
                config.containsKey(SUBSCRIBER_NAME_KEY)
                        ? config.getStringValue(SUBSCRIBER_NAME_KEY).getValue()
                        : null
        );
    }
}
