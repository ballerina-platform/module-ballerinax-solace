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

package io.xlibb.solace.config;

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
     * <p>
     * Queue is the default when neither {@code queueName} nor {@code topicName} is present: a topic subscription
     * always requires a {@code topicName}, so the only legitimate reason to omit both is an anonymous temporary
     * queue (a {@code durability: TEMPORARY} queue with no name hint).
     *
     * @param config the configuration map containing either queueName or topicName
     * @return a QueueConsumerConfig or TopicConsumerConfig instance
     */
    static ConsumerSubscriptionConfig fromBMap(BMap<BString, Object> config) {
        BString queueNameKey = StringUtils.fromString("queueName");
        BString topicNameKey = StringUtils.fromString("topicName");

        if (config.containsKey(topicNameKey) && !config.containsKey(queueNameKey)) {
            return new TopicConsumerConfig(config);
        }
        return new QueueConsumerConfig(config);
    }

    AcknowledgementMode ackMode();

    String selector();

    Integer transportWindowSize();

    Integer ackThreshold();

    Integer ackTimerInMsecs();

    Boolean noLocal();

    Boolean activeFlowIndication();

    Integer reconnectTries();

    int reconnectRetryIntervalInMsecs();

    /**
     * Validates the flow-control bounds shared by queue and topic subscriptions: {@code transportWindowSize}
     * (1-255), {@code ackThreshold} (1-75), and {@code ackTimer} (20-1500ms / 0.02-1.5s). {@code ackTimerInMsecs}
     * is {@code null} when the Ballerina-side {@code ackTimer} is left unset (disabled by default), so it is
     * not bounds-checked in that case.
     * <p>
     * Used by both the pull-based {@code MessageConsumer} and the push-based {@code Listener} paths, since both
     * construct these records from a {@code CommonConsumerConfig}/{@code CommonServiceConfig}-shaped map via
     * {@link #fromBMap(BMap)}.
     *
     * @throws IllegalArgumentException if any bound is violated
     */
    default void validate() {
        Integer windowSize = transportWindowSize();
        if (windowSize != null && (windowSize < 1 || windowSize > 255)) {
            throw new IllegalArgumentException("transportWindowSize must be between 1 and 255");
        }
        Integer threshold = ackThreshold();
        if (threshold != null && (threshold < 1 || threshold > 75)) {
            throw new IllegalArgumentException("ackThreshold must be between 1 and 75");
        }
        Integer timerMs = ackTimerInMsecs();
        if (timerMs != null && (timerMs < 20 || timerMs > 1500)) {
            throw new IllegalArgumentException("ackTimer must be between 0.02 and 1.5 seconds");
        }
    }
}
