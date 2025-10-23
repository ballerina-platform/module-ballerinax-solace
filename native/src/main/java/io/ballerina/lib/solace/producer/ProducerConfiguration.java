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

package io.ballerina.lib.solace.producer;

import io.ballerina.lib.solace.config.ConnectionConfiguration;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Configuration for Solace message producer.
 * <p>
 * Extends connection configuration with producer-specific settings.
 *
 * @param connectionConfig connection configuration for broker connection
 * @param transacted       {@code true} to enable transacted messaging
 * @param destination      destination (Topic or Queue) for messages
 */
public record ProducerConfiguration(
        ConnectionConfiguration connectionConfig,
        boolean transacted,
        Destination destination) {

    private static final BString TRANSACTED = StringUtils.fromString("transacted");
    private static final BString DESTINATION = StringUtils.fromString("destination");

    /**
     * Creates a ProducerConfiguration from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public ProducerConfiguration(BMap<BString, Object> config) {
        this(
                new ConnectionConfiguration(config),
                config.getBooleanValue(TRANSACTED),
                getDestination((BMap<BString, Object>) config.getMapValue(DESTINATION))
        );
    }

    /**
     * Determines and creates appropriate Destination based on configuration.
     *
     * @param destinationMap destination configuration map
     * @return Destination instance (Topic or Queue)
     */
    private static Destination getDestination(BMap<BString, Object> destinationMap) {
        if (destinationMap.containsKey(StringUtils.fromString("topicName"))) {
            return new Topic(destinationMap);
        } else if (destinationMap.containsKey(StringUtils.fromString("queueName"))) {
            return new Queue(destinationMap);
        }
        throw new IllegalArgumentException(
                "Invalid destination configuration: must contain either topicName or queueName");
    }
}
