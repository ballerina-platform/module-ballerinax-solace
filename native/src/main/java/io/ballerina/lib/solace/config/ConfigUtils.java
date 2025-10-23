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

import java.math.BigDecimal;

/**
 * Utility class for configuration-related helper methods.
 */
public final class ConfigUtils {

    private ConfigUtils() {}

    /**
     * Converts decimal seconds to milliseconds for timeouts.
     * Visible to config package and sub-packages only.
     *
     * @param seconds timeout in seconds as BigDecimal
     * @return timeout in milliseconds
     */
    public static long decimalToMillis(BigDecimal seconds) {
        return seconds.multiply(BigDecimal.valueOf(1000)).longValue();
    }
}
