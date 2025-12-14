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

package io.xlibb.solace.config;

import com.solacesystems.jcsmp.JCSMPProperties;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.xlibb.solace.consumer.AcknowledgementMode;

import java.math.BigDecimal;

/**
 * Queue consumer configuration for synchronous (pull-based) consumption. Represents the subscription to a queue
 * endpoint for receiving guaranteed messages. Maps to QueueSubscription in Ballerina types.bal.
 *
 * @param queueName                     the name of the queue to consume from
 * @param temporary                     whether this is a temporary queue (auto-deleted when session disconnects)
 * @param ackMode                       the JCSMP acknowledgement mode (SUPPORTED_MESSAGE_ACK_AUTO or
 *                                      SUPPORTED_MESSAGE_ACK_CLIENT)
 * @param selector                      optional SQL-92 message selector expression for filtering
 * @param transportWindowSize           JCSMP transport window size for flow control (1-255, default 255)
 * @param ackThreshold                  ACK threshold as percentage of window size (1-75, default 0)
 * @param ackTimerInMsecs               ACK timer in milliseconds (20-1500, default 0)
 * @param startState                    auto-start the flow upon creation (default false)
 * @param noLocal                       prevent receiving messages published on same session (default false)
 * @param activeFlowIndication          enable active/inactive flow indication (default false)
 * @param reconnectTries                number of reconnection attempts after flow goes down (-1 = infinite)
 * @param reconnectRetryIntervalInMsecs wait time between reconnection attempts in ms (min 50, default 3000)
 */
public record QueueConsumerConfig(
        String queueName,
        boolean temporary,
        AcknowledgementMode ackMode,
        String selector,
        Integer transportWindowSize,
        Integer ackThreshold,
        int ackTimerInMsecs,
        Boolean startState,
        Boolean noLocal,
        Boolean activeFlowIndication,
        Integer reconnectTries,
        int reconnectRetryIntervalInMsecs
) implements ConsumerSubscriptionConfig {

    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");
    private static final BString TEMPORARY_KEY = StringUtils.fromString("temporary");
    private static final BString ACK_MODE_KEY = StringUtils.fromString("ackMode");
    private static final BString SELECTOR_KEY = StringUtils.fromString("selector");
    private static final BString TRANSPORT_WINDOW_SIZE_KEY = StringUtils.fromString("transportWindowSize");
    private static final BString ACK_THRESHOLD_KEY = StringUtils.fromString("ackThreshold");
    private static final BString ACK_TIMER_KEY = StringUtils.fromString("ackTimer");
    private static final BString START_STATE_KEY = StringUtils.fromString("startState");
    private static final BString NO_LOCAL_KEY = StringUtils.fromString("noLocal");
    private static final BString ACTIVE_FLOW_INDICATION_KEY = StringUtils.fromString("activeFlowIndication");
    private static final BString RECONNECT_TRIES_KEY = StringUtils.fromString("reconnectTries");
    private static final BString RECONNECT_RETRY_INTERVAL_KEY = StringUtils.fromString("reconnectRetryInterval");

    private static final String DEFAULT_ACK_MODE = JCSMPProperties.SUPPORTED_MESSAGE_ACK_AUTO;
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
                AcknowledgementMode.valueOf(config.getStringValue(ACK_MODE_KEY).getValue()),
                extractSelector(config),
                extractOptionalInteger(config, TRANSPORT_WINDOW_SIZE_KEY),
                extractOptionalInteger(config, ACK_THRESHOLD_KEY),
                decimalToMillis(((BDecimal) config.get(ACK_TIMER_KEY)).decimalValue()),
                config.containsKey(START_STATE_KEY) ? config.getBooleanValue(START_STATE_KEY) : null,
                config.containsKey(NO_LOCAL_KEY) ? config.getBooleanValue(NO_LOCAL_KEY) : null,
                config.containsKey(ACTIVE_FLOW_INDICATION_KEY) ? config.getBooleanValue(ACTIVE_FLOW_INDICATION_KEY) :
                        null,
                extractOptionalInteger(config, RECONNECT_TRIES_KEY),
                decimalToMillis(((BDecimal) config.get(RECONNECT_RETRY_INTERVAL_KEY)).decimalValue())
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
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
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

    private static int decimalToMillis(BigDecimal seconds) {
        return seconds.multiply(BigDecimal.valueOf(1000)).intValue();
    }
}
