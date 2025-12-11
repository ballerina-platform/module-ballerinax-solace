package io.ballerina.lib.solace.config.retry;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.math.BigDecimal;

/**
 * Retry configuration for connection attempts.
 *
 * @param connectRetries        number of times to retry connecting during initial connection
 * @param connectRetriesPerHost number of connection retries per host
 * @param reconnectRetries      number of times to retry reconnecting after connection loss
 * @param reconnectRetryWait    time to wait between reconnection attempts in milliseconds
 */
public record RetryConfig(int connectRetries, int connectRetriesPerHost, int reconnectRetries,
                          long reconnectRetryWait) {

    private static final BString CONNECT_RETRIES_KEY = StringUtils.fromString("connectRetries");
    private static final BString CONNECT_RETRIES_PER_HOST_KEY = StringUtils.fromString("connectRetriesPerHost");
    private static final BString RECONNECT_RETRIES_KEY = StringUtils.fromString("reconnectRetries");
    private static final BString RECONNECT_RETRY_WAIT_KEY = StringUtils.fromString("reconnectRetryWait");

    /**
     * Creates a RetryConfig from a Ballerina map record.
     */
    public RetryConfig(BMap<BString, Object> config) {
        this(
                Math.toIntExact(config.getIntValue(CONNECT_RETRIES_KEY)),
                Math.toIntExact(config.getIntValue(CONNECT_RETRIES_PER_HOST_KEY)),
                Math.toIntExact(config.getIntValue(RECONNECT_RETRIES_KEY)),
                decimalToMillis(((BDecimal) config.get(RECONNECT_RETRY_WAIT_KEY)).decimalValue())
        );
    }

    /**
     * Converts decimal seconds to milliseconds.
     */
    private static long decimalToMillis(BigDecimal seconds) {
        return seconds.multiply(BigDecimal.valueOf(1000)).longValue();
    }
}
