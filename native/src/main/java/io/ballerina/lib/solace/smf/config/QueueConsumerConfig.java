package io.ballerina.lib.solace.smf.config;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Queue consumer configuration for synchronous (pull-based) consumption.
 * Represents the subscription to a queue endpoint for receiving guaranteed messages.
 * Maps to QueueSubscription in Ballerina types.bal.
 *
 * @param queueName the name of the queue to consume from
 * @param temporary whether this is a temporary queue (auto-deleted when session disconnects)
 * @param ackMode the JCSMP acknowledgement mode (SUPPORTED_MESSAGE_ACK_AUTO or SUPPORTED_MESSAGE_ACK_CLIENT)
 * @param selector optional SQL-92 message selector expression for filtering
 * @param transportWindowSize JCSMP transport window size for flow control (1-255, default 255)
 * @param ackThreshold ACK threshold as percentage of window size (1-75, default 0)
 * @param ackTimerInMsecs ACK timer in milliseconds (20-1500, default 0)
 * @param startState auto-start the flow upon creation (default false)
 * @param noLocal prevent receiving messages published on same session (default false)
 * @param activeFlowIndication enable active/inactive flow indication (default false)
 * @param reconnectTries number of reconnection attempts after flow goes down (-1 = infinite)
 * @param reconnectRetryIntervalInMsecs wait time between reconnection attempts in ms (min 50, default 3000)
 */
public record QueueConsumerConfig(
        String queueName,
        boolean temporary,
        String ackMode,
        String selector,
        Integer transportWindowSize,
        Integer ackThreshold,
        Integer ackTimerInMsecs,
        Boolean startState,
        Boolean noLocal,
        Boolean activeFlowIndication,
        Integer reconnectTries,
        Integer reconnectRetryIntervalInMsecs
) implements ConsumerSubscriptionConfig {

    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");
    private static final BString TEMPORARY_KEY = StringUtils.fromString("temporary");
    private static final BString ACK_MODE_KEY = StringUtils.fromString("ackMode");
    private static final BString SELECTOR_KEY = StringUtils.fromString("selector");
    private static final BString TRANSPORT_WINDOW_SIZE_KEY = StringUtils.fromString("transportWindowSize");
    private static final BString ACK_THRESHOLD_KEY = StringUtils.fromString("ackThreshold");
    private static final BString ACK_TIMER_IN_MSECS_KEY = StringUtils.fromString("ackTimerInMsecs");
    private static final BString START_STATE_KEY = StringUtils.fromString("startState");
    private static final BString NO_LOCAL_KEY = StringUtils.fromString("noLocal");
    private static final BString ACTIVE_FLOW_INDICATION_KEY = StringUtils.fromString("activeFlowIndication");
    private static final BString RECONNECT_TRIES_KEY = StringUtils.fromString("reconnectTries");
    private static final BString RECONNECT_RETRY_INTERVAL_KEY = StringUtils.fromString("reconnectRetryIntervalInMsecs");

    private static final String DEFAULT_ACK_MODE = "SUPPORTED_MESSAGE_ACK_AUTO";
    private static final int DEFAULT_WINDOW_SIZE = 255;

    /**
     * Creates a QueueConsumerConfig from a Ballerina map record.
     *
     * @param config the configuration map
     */
    public QueueConsumerConfig(BMap<BString, Object> config) {
        this(
            extractQueueName(config),
            extractTemporary(config),
            extractAckMode(config),
            extractSelector(config),
            extractOptionalInteger(config, TRANSPORT_WINDOW_SIZE_KEY),
            extractOptionalInteger(config, ACK_THRESHOLD_KEY),
            extractOptionalInteger(config, ACK_TIMER_IN_MSECS_KEY),
            extractOptionalBoolean(config, START_STATE_KEY),
            extractOptionalBoolean(config, NO_LOCAL_KEY),
            extractOptionalBoolean(config, ACTIVE_FLOW_INDICATION_KEY),
            extractOptionalInteger(config, RECONNECT_TRIES_KEY),
            extractOptionalInteger(config, RECONNECT_RETRY_INTERVAL_KEY)
        );
    }

    private static String extractQueueName(BMap<BString, Object> config) {
        Object value = config.get(QUEUE_NAME_KEY);
        if (value == null) {
            throw new IllegalArgumentException("queueName is required for QueueConsumerConfig");
        }
        return value.toString();
    }

    private static boolean extractTemporary(BMap<BString, Object> config) {
        Object value = config.get(TEMPORARY_KEY);
        if (value == null) {
            return false; // Default to false (non-temporary)
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static String extractAckMode(BMap<BString, Object> config) {
        Object value = config.get(ACK_MODE_KEY);
        return value != null ? value.toString() : DEFAULT_ACK_MODE;
    }

    private static String extractSelector(BMap<BString, Object> config) {
        Object value = config.get(SELECTOR_KEY);
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

    private static Boolean extractOptionalBoolean(BMap<BString, Object> config, BString key) {
        Object value = config.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Get the effective transport window size, using the default if not specified.
     *
     * @return the transport window size (or default 255 if not specified)
     */
    public int getEffectiveTransportWindowSize() {
        return transportWindowSize != null ? transportWindowSize : DEFAULT_WINDOW_SIZE;
    }
}
