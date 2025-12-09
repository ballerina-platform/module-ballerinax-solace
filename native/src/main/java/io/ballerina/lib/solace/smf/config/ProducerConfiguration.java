package io.ballerina.lib.solace.smf.config;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Producer-specific configuration containing connection configuration.
 * Maps to ProducerConfiguration in Ballerina types.bal.
 *
 * Note: Destination is NOT part of producer configuration - it's specified at send-time.
 *
 * @param connectionConfig connection configuration for broker connection
 */
public record ProducerConfiguration(ConnectionConfiguration connectionConfig) {

    /**
     * Creates a ProducerConfiguration from a Ballerina map record.
     * The map contains connection configuration fields.
     *
     * @param config the Ballerina configuration map
     */
    public ProducerConfiguration(BMap<BString, Object> config) {
        this(new ConnectionConfiguration(config));
    }
}
