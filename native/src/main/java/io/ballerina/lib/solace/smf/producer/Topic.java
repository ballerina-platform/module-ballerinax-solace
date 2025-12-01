package io.ballerina.lib.solace.smf.producer;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Topic destination implementation for publish/subscribe messaging.
 */
public record Topic(String name) implements Destination {
    private static final BString NAME_KEY = StringUtils.fromString("name");

    /**
     * Creates a Topic from a Ballerina map record.
     */
    public Topic(BMap<BString, Object> config) {
        this(config.getStringValue(NAME_KEY).getValue());
    }
}
