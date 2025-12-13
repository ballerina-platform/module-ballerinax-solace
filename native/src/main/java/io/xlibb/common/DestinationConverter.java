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

package io.xlibb.common;

import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import static io.xlibb.common.MessageFieldConstants.QUEUE_NAME_KEY;
import static io.xlibb.common.MessageFieldConstants.TOPIC_NAME_KEY;

/**
 * Utility for bidirectional conversion between JCSMP Destination and Ballerina Topic/Queue map.
 */
public class DestinationConverter {

    /**
     * Converts a JCSMP Destination to a Ballerina Topic or Queue map.
     *
     * @param destination the JCSMP destination
     * @return Ballerina map with topicName or queueName field, or null if destination is null
     */
    public static BMap<BString, Object> fromJCSMPDestination(Destination destination) {
        if (destination == null) {
            return null;
        }

        BMap<BString, Object> destMap = ValueCreator.createMapValue();
        if (destination instanceof Topic topic) {
            destMap.put(TOPIC_NAME_KEY, StringUtils.fromString(topic.getName()));
        } else if (destination instanceof Queue queue) {
            destMap.put(QUEUE_NAME_KEY, StringUtils.fromString(queue.getName()));
        }
        return destMap;
    }

    /**
     * Converts a Ballerina Topic or Queue map to a JCSMP Destination.
     *
     * @param destinationMap the Ballerina destination map
     * @return JCSMP Destination (Topic or Queue), or null if map is null or invalid
     */
    public static Destination toJCSMPDestination(BMap<BString, Object> destinationMap) {
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
     * Converts a sealed Destination interface to a JCSMP Destination.
     *
     * @param destination the Ballerina Destination (Topic or Queue record)
     * @return the JCSMP Destination
     * @throws Exception if destination is null or invalid type
     */
    public static Destination fromDestinationInterface(
            io.xlibb.producer.Destination destination)
            throws Exception {
        return switch (destination) {
            case null -> throw new Exception("Destination cannot be null");
            case io.xlibb.producer.Topic(String topicName) ->
                    JCSMPFactory.onlyInstance().createTopic(topicName);
            case io.xlibb.producer.Queue(String queueName) ->
                    JCSMPFactory.onlyInstance().createQueue(queueName);
        };

    }

}
