package io.ballerina.lib.solace.config;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Consumer configuration for synchronous (pull-based) message consumption via MessageConsumer. Composes
 * CommonConnectionConfiguration and a ConsumerSubscriptionConfig (queue or topic).
 *
 * @param connectionConfig   the common connection configuration
 * @param subscriptionConfig the consumer subscription configuration (queue or topic)
 */
public record ConsumerConfiguration(
        ConnectionConfiguration connectionConfig,
        ConsumerSubscriptionConfig subscriptionConfig) {

    private static final BString SUBSCRIPTION_CONFIG_KEY = StringUtils.fromString("subscriptionConfig");

    /**
     * Creates a ConsumerConfiguration from a Ballerina map record. The map contains both connection configuration
     * fields and subscription configuration.
     *
     * @param config the configuration map
     */
    public ConsumerConfiguration(BMap<BString, Object> config) {
        this(
                new ConnectionConfiguration(config),
                getSubscriptionConfig((BMap<BString, Object>) config.getMapValue(SUBSCRIPTION_CONFIG_KEY))
        );
    }

    /**
     * Factory method to create the appropriate ConsumerSubscriptionConfig type based on the configuration map.
     *
     * @param subscriptionConfigMap the subscription configuration map
     * @return a ConsumerSubscriptionConfig (QueueConsumerConfig or TopicConsumerConfig)
     */
    private static ConsumerSubscriptionConfig getSubscriptionConfig(BMap<BString, Object> subscriptionConfigMap) {
        if (subscriptionConfigMap == null) {
            throw new IllegalArgumentException("Subscription configuration is required");
        }

        ConsumerSubscriptionConfig config = ConsumerSubscriptionConfig.fromBMap(subscriptionConfigMap);

        if (config instanceof TopicConsumerConfig topicConfig) {
            topicConfig.validate();
        }

        return config;
    }
}
