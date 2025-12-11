package io.ballerina.lib.solace.smf.producer;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Queue destination for point-to-point messaging.
 *
 * @param queueName name of the queue to publish messages to
 */
public record Queue(String queueName) implements Destination {

    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");

    /**
     * Creates a Queue from a Ballerina map record.
     */
    public Queue(BMap<BString, Object> config) {
        this(config.getStringValue(QUEUE_NAME_KEY).getValue());
    }
}
