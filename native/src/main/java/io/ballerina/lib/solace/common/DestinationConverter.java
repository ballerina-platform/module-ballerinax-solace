package io.ballerina.lib.solace.common;

import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import static io.ballerina.lib.solace.common.MessageFieldConstants.QUEUE_NAME_KEY;
import static io.ballerina.lib.solace.common.MessageFieldConstants.TOPIC_NAME_KEY;

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
            io.ballerina.lib.solace.producer.Destination destination)
            throws Exception {
        return switch (destination) {
            case null -> throw new Exception("Destination cannot be null");
            case io.ballerina.lib.solace.producer.Topic(String topicName) ->
                    JCSMPFactory.onlyInstance().createTopic(topicName);
            case io.ballerina.lib.solace.producer.Queue(String queueName) ->
                    JCSMPFactory.onlyInstance().createQueue(queueName);
        };

    }

}
