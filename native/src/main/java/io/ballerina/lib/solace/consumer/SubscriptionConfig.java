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
 * Sealed interface for subscription configurations (QueueConfig or TopicConfig).
 */
public sealed interface SubscriptionConfig permits QueueConfig, TopicConfig {

    /**
     * Returns the session acknowledgement mode.
     *
     * @return acknowledgement mode
     */
    AcknowledgementMode sessionAckMode();

    /**
     * Returns the message selector (optional).
     *
     * @return message selector or null
     */
    String messageSelector();

    /**
     * Factory method to create SubscriptionConfig from Ballerina map.
     *
     * @param config Ballerina subscription config map
     * @return QueueConfig or TopicConfig
     */
    static SubscriptionConfig fromBMap(BMap<BString, Object> config) {
        BString queueNameKey = StringUtils.fromString("queueName");
        BString topicNameKey = StringUtils.fromString("topicName");

        if (config.containsKey(queueNameKey)) {
            return new QueueConfig(config);
        } else if (config.containsKey(topicNameKey)) {
            return new TopicConfig(config);
        } else {
            throw new IllegalArgumentException(
                    "Subscription config must contain either 'queueName' or 'topicName'");
        }
    }
}
