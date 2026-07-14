/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.lib.solace.config;

import com.solacesystems.jcsmp.JCSMPProperties;
import io.ballerina.lib.solace.consumer.AcknowledgementMode;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.math.BigDecimal;

/**
 * Queue consumer configuration. Represents the subscription to a queue endpoint for receiving guaranteed messages,
 * shared by both the synchronous (pull-based) MessageConsumer and the asynchronous (push-based) Listener. Maps to
 * QueueConfiguration/QueueServiceConfiguration in Ballerina types.bal.
 *
 * @param queueName                     the name of the queue to consume from - required unless durability is
 *                                      TEMPORARY (optional broker-generated name hint when TEMPORARY)
 * @param durability                    DURABLE (pre-provisioned, named queue) or TEMPORARY (auto-deleted when
 *                                      session disconnects)
 * @param ackMode                       the JCSMP acknowledgement mode (SUPPORTED_MESSAGE_ACK_AUTO or
 *                                      SUPPORTED_MESSAGE_ACK_CLIENT)
 * @param selector                      optional SQL-92 message selector expression for filtering
 * @param transportWindowSize           JCSMP transport window size for flow control (1-255, default 255)
 * @param ackThreshold                  ACK threshold as percentage of window size (1-75, default 60)
 * @param ackTimerInMsecs               ACK timer in milliseconds (20-1500). Disabled (null) by default
 * @param reconnectTries                number of reconnection attempts after flow goes down (-1 = infinite)
 * @param reconnectRetryIntervalInMsecs wait time between reconnection attempts in ms (min 50, default 3000)
 */
public record QueueConsumerConfig(
        String queueName,
        String durability,
        AcknowledgementMode ackMode,
        String selector,
        Integer transportWindowSize,
        Integer ackThreshold,
        Integer ackTimerInMsecs,
        Integer reconnectTries,
        int reconnectRetryIntervalInMsecs
) implements ConsumerSubscriptionConfig {

    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");
    private static final BString DURABILITY_KEY = StringUtils.fromString("durability");
    private static final BString ACK_MODE_KEY = StringUtils.fromString("ackMode");
    private static final BString MESSAGE_SELECTOR_KEY = StringUtils.fromString("messageSelector");
    private static final BString TRANSPORT_WINDOW_SIZE_KEY = StringUtils.fromString("transportWindowSize");
    private static final BString ACK_THRESHOLD_KEY = StringUtils.fromString("ackThreshold");
    private static final BString ACK_TIMER_KEY = StringUtils.fromString("ackTimer");
    private static final BString RECONNECT_TRIES_KEY = StringUtils.fromString("reconnectTries");
    private static final BString RECONNECT_RETRY_INTERVAL_KEY = StringUtils.fromString("reconnectRetryInterval");

    private static final String DEFAULT_ACK_MODE = JCSMPProperties.SUPPORTED_MESSAGE_ACK_AUTO;
    private static final int DEFAULT_WINDOW_SIZE = 255;
    private static final String DEFAULT_DURABILITY = "DURABLE";

    /**
     * Creates a QueueConsumerConfig from a Ballerina map record.
     *
     * @param config the configuration map
     */
    public QueueConsumerConfig(BMap<BString, Object> config) {
        this(
                extractQueueName(config),
                extractDurability(config),
                AcknowledgementMode.valueOf(config.getStringValue(ACK_MODE_KEY).getValue()),
                extractSelector(config),
                extractOptionalInteger(config, TRANSPORT_WINDOW_SIZE_KEY),
                extractOptionalInteger(config, ACK_THRESHOLD_KEY),
                extractOptionalDecimalMillis(config, ACK_TIMER_KEY),
                extractOptionalInteger(config, RECONNECT_TRIES_KEY),
                decimalToMillis(((BDecimal) config.get(RECONNECT_RETRY_INTERVAL_KEY)).decimalValue())
        );
    }

    private static String extractQueueName(BMap<BString, Object> config) {
        Object value = config.get(QUEUE_NAME_KEY);
        return value != null ? value.toString() : null;
    }

    private static String extractDurability(BMap<BString, Object> config) {
        Object value = config.get(DURABILITY_KEY);
        return value != null ? value.toString() : DEFAULT_DURABILITY;
    }

    private static String extractSelector(BMap<BString, Object> config) {
        Object value = config.get(MESSAGE_SELECTOR_KEY);
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

    private static int decimalToMillis(BigDecimal seconds) {
        return seconds.multiply(BigDecimal.valueOf(1000)).intValue();
    }

    private static Integer extractOptionalDecimalMillis(BMap<BString, Object> config, BString key) {
        Object value = config.get(key);
        if (value instanceof BDecimal decimal) {
            return decimalToMillis(decimal.decimalValue());
        }
        return null;
    }

    /**
     * Check if this is a temporary queue.
     *
     * @return true if durability is TEMPORARY
     */
    public boolean isTemporary() {
        return "TEMPORARY".equalsIgnoreCase(durability);
    }

    /**
     * Validates the shared flow-control bounds, then that queueName is provided when durability is DURABLE.
     *
     * @throws IllegalArgumentException if a flow-control bound is violated, or queueName is missing while durable
     */
    @Override
    public void validate() {
        ConsumerSubscriptionConfig.super.validate();
        if (!isTemporary() && (queueName == null || queueName.isEmpty())) {
            throw new IllegalArgumentException("queueName is required when durability is not TEMPORARY");
        }
    }
}
