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

package io.ballerina.lib.solace.consumer;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Queue subscription configuration.
 *
 * @param sessionAckMode Session acknowledgement mode
 * @param queueName Queue name
 * @param messageSelector Message selector (optional)
 */
public record QueueConfig(
        AcknowledgementMode sessionAckMode,
        String queueName,
        String messageSelector) implements SubscriptionConfig {

    private static final BString SESSION_ACK_MODE_KEY = StringUtils.fromString("sessionAckMode");
    private static final BString QUEUE_NAME_KEY = StringUtils.fromString("queueName");
    private static final BString MESSAGE_SELECTOR_KEY = StringUtils.fromString("messageSelector");

    public QueueConfig(BMap<BString, Object> config) {
        this(
                AcknowledgementMode.valueOf(
                        config.getStringValue(SESSION_ACK_MODE_KEY).getValue()),
                config.getStringValue(QUEUE_NAME_KEY).getValue(),
                config.containsKey(MESSAGE_SELECTOR_KEY)
                        ? config.getStringValue(MESSAGE_SELECTOR_KEY).getValue()
                        : null
        );
    }
}
