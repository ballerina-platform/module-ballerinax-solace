/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.solace.config;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.utils.StringUtils;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for TopicConsumerConfig.validate() that run without a broker.
 * Covers validation of messageSelector on TEMPORARY topics and endpointName on DURABLE topics.
 */
public class TopicConsumerConfigTest {

    private static final BString TOPIC_NAME = StringUtils.fromString("test/topic");
    private static final BString ACK_MODE_AUTO = StringUtils.fromString("AUTO_ACK");
    private static final BString ACK_MODE_CLIENT = StringUtils.fromString("CLIENT_ACK");
    private static final Object RECONNECT_INTERVAL = ValueCreator.createDecimalValue(new BigDecimal("3.0"));

    /**
     * Helper to create a valid TopicConsumerConfig map with required fields.
     * All other fields are optional and can be added as needed.
     */
    private static BMap<BString, Object> baseConfig() {
        BMap<BString, Object> config = ValueCreator.createMapValue();
        config.put(TOPIC_NAME_KEY(), TOPIC_NAME);
        config.put(ACK_MODE_KEY(), ACK_MODE_AUTO);
        config.put(RECONNECT_RETRY_INTERVAL_KEY(), RECONNECT_INTERVAL);
        return config;
    }

    private static BString TOPIC_NAME_KEY() {
        return StringUtils.fromString("topicName");
    }

    private static BString ACK_MODE_KEY() {
        return StringUtils.fromString("ackMode");
    }

    private static BString RECONNECT_RETRY_INTERVAL_KEY() {
        return StringUtils.fromString("reconnectRetryInterval");
    }

    private static BString MESSAGE_SELECTOR_KEY() {
        return StringUtils.fromString("messageSelector");
    }

    private static BString DURABILITY_KEY() {
        return StringUtils.fromString("durability");
    }

    private static BString ENDPOINT_NAME_KEY() {
        return StringUtils.fromString("endpointName");
    }

    @Test
    public void testTemporaryTopicWithNonEmptySelectorThrows() {
        BMap<BString, Object> config = baseConfig();
        config.put(MESSAGE_SELECTOR_KEY(), StringUtils.fromString("header > 10"));
        // durability defaults to TEMPORARY

        try {
            new TopicConsumerConfig(config).validate();
            org.testng.Assert.fail("Expected IllegalArgumentException for non-empty selector on TEMPORARY topic");
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "messageSelector is not supported for direct topic subscriptions");
        }
    }

    @Test
    public void testTemporaryTopicWithEmptySelectorPasses() {
        BMap<BString, Object> config = baseConfig();
        config.put(MESSAGE_SELECTOR_KEY(), StringUtils.fromString(""));
        // durability defaults to TEMPORARY

        // Should not throw
        new TopicConsumerConfig(config).validate();
    }

    @Test
    public void testTemporaryTopicWithAbsentSelectorPasses() {
        BMap<BString, Object> config = baseConfig();
        // selector key not present
        // durability defaults to TEMPORARY

        // Should not throw
        new TopicConsumerConfig(config).validate();
    }

    @Test
    public void testDurableTopicWithSelectorAndEndpointNamePasses() {
        BMap<BString, Object> config = baseConfig();
        config.put(MESSAGE_SELECTOR_KEY(), StringUtils.fromString("header > 10"));
        config.put(DURABILITY_KEY(), StringUtils.fromString("DURABLE"));
        config.put(ENDPOINT_NAME_KEY(), StringUtils.fromString("test-endpoint"));

        // Should not throw
        new TopicConsumerConfig(config).validate();
    }

    @Test
    public void testDurableTopicWithSelectorMissingEndpointNameThrows() {
        BMap<BString, Object> config = baseConfig();
        config.put(MESSAGE_SELECTOR_KEY(), StringUtils.fromString("header > 10"));
        config.put(DURABILITY_KEY(), StringUtils.fromString("DURABLE"));
        // endpointName missing

        try {
            new TopicConsumerConfig(config).validate();
            org.testng.Assert.fail("Expected IllegalArgumentException for missing endpointName on DURABLE topic");
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "endpointName is required when the topic is DURABLE");
        }
    }
}