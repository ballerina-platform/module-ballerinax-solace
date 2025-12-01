package io.ballerina.lib.solace.smf.producer;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Queue destination implementation for point-to-point messaging.
 */
public record Queue(String name) implements Destination {
    private static final BString NAME_KEY = StringUtils.fromString("name");

    /**
     * Creates a Queue from a Ballerina map record.
     */
    public Queue(BMap<BString, Object> config) {
        this(config.getStringValue(NAME_KEY).getValue());
    }
}
