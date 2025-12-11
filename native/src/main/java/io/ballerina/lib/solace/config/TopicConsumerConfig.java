package io.ballerina.lib.solace.config;

import io.ballerina.lib.solace.consumer.AcknowledgementMode;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Topic consumer configuration for synchronous (pull-based) consumption. Represents the subscription to a topic for
 * receiving messages. Maps to TopicSubscription in Ballerina types.bal.
 *
 * @param topicName                     the name of the topic to subscribe to
 * @param ackMode                       the JCSMP acknowledgement mode (SUPPORTED_MESSAGE_ACK_AUTO or
 *                                      SUPPORTED_MESSAGE_ACK_CLIENT)
 * @param selector                      optional SQL-92 message selector expression for filtering
 * @param endpointType                  the endpoint type (DEFAULT for ephemeral or DURABLE for persisted)
 * @param endpointName                  the endpoint name (required if endpointType is DURABLE)
 * @param transportWindowSize           JCSMP transport window size for flow control (1-255, default 255) - DURABLE
 *                                      only
 * @param ackThreshold                  ACK threshold as percentage of window size (1-75, default 0) - DURABLE only
 * @param ackTimerInMsecs               ACK timer in milliseconds (20-1500, default 0) - DURABLE only
 * @param startState                    auto-start the flow upon creation (default false) - DURABLE only
 * @param noLocal                       prevent receiving messages published on same session (default false)
 * @param activeFlowIndication          enable active/inactive flow indication (default false) - DURABLE only
 * @param reconnectTries                number of reconnection attempts after flow goes down (-1 = infinite) - DURABLE
 *                                      only
 * @param reconnectRetryIntervalInMsecs wait time between reconnection attempts in ms (min 50, default 3000) - DURABLE
 *                                      only
 */
public record TopicConsumerConfig(
        String topicName,
        AcknowledgementMode ackMode,
        String selector,
        String endpointType,
        String endpointName,
        Integer transportWindowSize,
        Integer ackThreshold,
        Integer ackTimerInMsecs,
        Boolean startState,
        Boolean noLocal,
        Boolean activeFlowIndication,
        Integer reconnectTries,
        Integer reconnectRetryIntervalInMsecs
) implements ConsumerSubscriptionConfig {

    private static final BString TOPIC_NAME_KEY = StringUtils.fromString("topicName");
    private static final BString ACK_MODE_KEY = StringUtils.fromString("ackMode");
    private static final BString SELECTOR_KEY = StringUtils.fromString("selector");
    private static final BString ENDPOINT_TYPE_KEY = StringUtils.fromString("endpointType");
    private static final BString ENDPOINT_NAME_KEY = StringUtils.fromString("endpointName");
    private static final BString TRANSPORT_WINDOW_SIZE_KEY = StringUtils.fromString("transportWindowSize");
    private static final BString ACK_THRESHOLD_KEY = StringUtils.fromString("ackThreshold");
    private static final BString ACK_TIMER_IN_MSECS_KEY = StringUtils.fromString("ackTimerInMsecs");
    private static final BString START_STATE_KEY = StringUtils.fromString("startState");
    private static final BString NO_LOCAL_KEY = StringUtils.fromString("noLocal");
    private static final BString ACTIVE_FLOW_INDICATION_KEY = StringUtils.fromString("activeFlowIndication");
    private static final BString RECONNECT_TRIES_KEY = StringUtils.fromString("reconnectTries");
    private static final BString RECONNECT_RETRY_INTERVAL_KEY = StringUtils.fromString("reconnectRetryIntervalInMsecs");

    private static final String DEFAULT_ENDPOINT_TYPE = "DEFAULT";

    /**
     * Creates a TopicConsumerConfig from a Ballerina map record.
     *
     * @param config the configuration map
     */
    public TopicConsumerConfig(BMap<BString, Object> config) {
        this(
                extractTopicName(config),
                AcknowledgementMode.valueOf(config.getStringValue(ACK_MODE_KEY).getValue()),
                extractSelector(config),
                extractEndpointType(config),
                extractEndpointName(config),
                extractOptionalInteger(config, TRANSPORT_WINDOW_SIZE_KEY),
                extractOptionalInteger(config, ACK_THRESHOLD_KEY),
                extractOptionalInteger(config, ACK_TIMER_IN_MSECS_KEY),
                config.containsKey(START_STATE_KEY) ? config.getBooleanValue(START_STATE_KEY) : null,
                config.containsKey(NO_LOCAL_KEY) ? config.getBooleanValue(NO_LOCAL_KEY) : null,
                config.containsKey(ACTIVE_FLOW_INDICATION_KEY) ? config.getBooleanValue(ACTIVE_FLOW_INDICATION_KEY) :
                        null,
                extractOptionalInteger(config, RECONNECT_TRIES_KEY),
                extractOptionalInteger(config, RECONNECT_RETRY_INTERVAL_KEY)
        );
    }

    private static String extractTopicName(BMap<BString, Object> config) {
        Object value = config.get(TOPIC_NAME_KEY);
        if (value == null) {
            throw new IllegalArgumentException("topicName is required for TopicConsumerConfig");
        }
        return value.toString();
    }

    private static String extractSelector(BMap<BString, Object> config) {
        Object value = config.get(SELECTOR_KEY);
        return value != null ? value.toString() : null;
    }

    private static String extractEndpointType(BMap<BString, Object> config) {
        Object value = config.get(ENDPOINT_TYPE_KEY);
        return value != null ? value.toString() : DEFAULT_ENDPOINT_TYPE;
    }

    private static String extractEndpointName(BMap<BString, Object> config) {
        Object value = config.get(ENDPOINT_NAME_KEY);
        return value != null ? value.toString() : null;
    }

    private static Integer extractOptionalInteger(BMap<BString, Object> config, BString key) {
        Object value = config.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * Check if this is a durable topic subscription.
     *
     * @return true if endpoint type is DURABLE
     */
    public boolean isDurable() {
        return "DURABLE".equalsIgnoreCase(endpointType);
    }

    /**
     * Validates that endpointName is provided for DURABLE endpoints.
     *
     * @throws IllegalArgumentException if endpointName is missing for DURABLE endpoints
     */
    public void validate() {
        if (isDurable() && (endpointName == null || endpointName.isEmpty())) {
            throw new IllegalArgumentException(
                    "endpointName is required for DURABLE topic endpoints"
            );
        }
    }
}
