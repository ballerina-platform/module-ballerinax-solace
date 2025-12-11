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

package io.ballerina.lib.solace.config;

import io.ballerina.lib.solace.consumer.AcknowledgementMode;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Interface for consumer subscription configuration.
 * <p>
 * Can be either {@link QueueConsumerConfig} or {@link TopicConsumerConfig}.
 * <p>
 * Contains common flow control properties shared by both queue and topic consumers.
 */
public sealed interface ConsumerSubscriptionConfig permits QueueConsumerConfig, TopicConsumerConfig {

    /**
     * Factory method to create the appropriate ConsumerSubscriptionConfig type based on the configuration map.
     *
     * @param config the configuration map containing either queueName or topicName
     * @return a QueueConsumerConfig or TopicConsumerConfig instance
     * @throws IllegalArgumentException if neither queueName nor topicName is present
     */
    static ConsumerSubscriptionConfig fromBMap(BMap<BString, Object> config) {
        BString queueNameKey = StringUtils.fromString("queueName");
        BString topicNameKey = StringUtils.fromString("topicName");

        if (config.containsKey(queueNameKey)) {
            return new QueueConsumerConfig(config);
        } else if (config.containsKey(topicNameKey)) {
            return new TopicConsumerConfig(config);
        } else {
            throw new IllegalArgumentException(
                    "Consumer subscription config must have either 'queueName' or 'topicName' field"
            );
        }
    }

    AcknowledgementMode ackMode();

    String selector();

    Integer transportWindowSize();

    Integer ackThreshold();

    Integer ackTimerInMsecs();

    Boolean startState();

    Boolean noLocal();

    Boolean activeFlowIndication();

    Integer reconnectTries();

    Integer reconnectRetryIntervalInMsecs();
}
