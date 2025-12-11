package io.ballerina.lib.solace.smf.producer;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Topic destination for publish/subscribe messaging.
 *
 * @param topicName name of topic to publish messages to
 */
public record Topic(String topicName) implements Destination {

    private static final BString TOPIC_NAME_KEY = StringUtils.fromString("topicName");

    /**
     * Creates a Topic from a Ballerina map record.
     */
    public Topic(BMap<BString, Object> config) {
        this(config.getStringValue(TOPIC_NAME_KEY).getValue());
    }
}
