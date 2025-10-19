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

package io.ballerina.lib.solace.config.retry;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.math.BigDecimal;

import static io.ballerina.lib.solace.config.ConfigUtils.decimalToMillis;

/**
 * Retry configuration for connection and reconnection attempts to a Solace broker.
 *
 * @param connectRetries         number of times to retry connecting during initial connection
 *                               (-1 for unlimited retries, 0 for no retries)
 * @param connectRetriesPerHost  number of connection retries per host when multiple hosts are specified
 * @param reconnectRetries       number of times to retry reconnecting after connection loss
 *                               (-1 for unlimited retries)
 * @param reconnectRetryWait     time to wait between reconnection attempts in milliseconds
 */
public record RetryConfig(
        int connectRetries,
        int connectRetriesPerHost,
        int reconnectRetries,
        long reconnectRetryWait) {

    private static final BString CONNECT_RETRIES = StringUtils.fromString("connectRetries");
    private static final BString CONNECT_RETRIES_PER_HOST = StringUtils.fromString("connectRetriesPerHost");
    private static final BString RECONNECT_RETRIES = StringUtils.fromString("reconnectRetries");
    private static final BString RECONNECT_RETRY_WAIT = StringUtils.fromString("reconnectRetryWait");

    /**
     * Creates a RetryConfig from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public RetryConfig(BMap<BString, Object> config) {
        this(
                config.getIntValue(CONNECT_RETRIES).intValue(),
                config.getIntValue(CONNECT_RETRIES_PER_HOST).intValue(),
                config.getIntValue(RECONNECT_RETRIES).intValue(),
                decimalToMillis((BigDecimal) config.get(RECONNECT_RETRY_WAIT))
        );
    }
}
