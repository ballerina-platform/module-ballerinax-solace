package io.ballerina.lib.solace.smf.config;

import io.ballerina.lib.solace.smf.producer.Destination;
import io.ballerina.lib.solace.smf.producer.Queue;
import io.ballerina.lib.solace.smf.producer.Topic;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Producer-specific configuration containing connection configuration and destination.
 *
 * @param connectionConfig connection configuration for broker connection
 * @param destination destination (Topic or Queue) for message publishing
 */
public record ProducerConfiguration(
        ConnectionConfiguration connectionConfig,
        Destination destination) {

    private static final BString DESTINATION_KEY = StringUtils.fromString("destination");

    /**
     * Creates a ProducerConfiguration from a Ballerina map record.
     * The map contains both connection configuration fields and destination.
     */
    public ProducerConfiguration(BMap<BString, Object> config) {
        this(
            new ConnectionConfiguration(config),
            getDestination((BMap<BString, Object>) config.getMapValue(DESTINATION_KEY))
        );
    }

    /**
     * Factory method to create the appropriate Destination type (Topic or Queue).
     * Determines type by checking for topicName or queueName field.
     */
    private static Destination getDestination(BMap<BString, Object> destinationMap) {
        if (destinationMap == null) {
            throw new IllegalArgumentException("Destination configuration is required");
        }

        BString topicNameKey = StringUtils.fromString("topicName");
        BString queueNameKey = StringUtils.fromString("queueName");

        if (destinationMap.containsKey(topicNameKey)) {
            return new Topic(destinationMap);
        } else if (destinationMap.containsKey(queueNameKey)) {
            return new Queue(destinationMap);
        } else {
            throw new IllegalArgumentException("Destination must have either 'topicName' or 'queueName' field");
        }
    }
}