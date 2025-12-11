package io.ballerina.lib.solace.smf.config;

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

    // Common flow control properties (from CommonConsumerConfig in types.bal)
    String ackMode();

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
