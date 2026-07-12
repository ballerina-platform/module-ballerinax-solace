/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org).
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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Listener configuration for asynchronous (push-based) message consumption. Maps to ListenerConfiguration in
 * Ballerina types.bal.
 *
 * @param connectionConfig          the common connection configuration
 * @param generateReceiveTimestamps whether to generate receive timestamps on incoming messages
 */
public record ListenerConfiguration(
        ConnectionConfiguration connectionConfig,
        boolean generateReceiveTimestamps) {

    private static final BString GENERATE_RECEIVE_TIMESTAMPS_KEY = StringUtils.fromString("generateReceiveTimestamps");

    /**
     * Creates a ListenerConfiguration from a Ballerina map record. The map contains connection configuration fields.
     *
     * @param config the Ballerina configuration map
     */
    public ListenerConfiguration(BMap<BString, Object> config) {
        this(
                new ConnectionConfiguration(config),
                config.getBooleanValue(GENERATE_RECEIVE_TIMESTAMPS_KEY)
        );
    }
}
