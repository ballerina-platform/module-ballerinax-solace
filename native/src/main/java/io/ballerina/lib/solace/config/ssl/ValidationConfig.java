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

package io.ballerina.lib.solace.config.ssl;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Certificate validation settings for SSL/TLS connections.
 *
 * @param enabled      {@code true} if certificate validation is enabled
 * @param validateDate {@code true} to validate certificate's expiration date
 * @param validateHost {@code true} to validate that certificate's common name matches broker hostname
 */
public record ValidationConfig(boolean enabled, boolean validateDate, boolean validateHost) {
    private static final BString ENABLED = StringUtils.fromString("enabled");
    private static final BString VALIDATE_DATE = StringUtils.fromString("validateDate");
    private static final BString VALIDATE_HOST = StringUtils.fromString("validateHost");

    /**
     * Creates a ValidationConfig from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public ValidationConfig(BMap<BString, Object> config) {
        this(
                config.getBooleanValue(ENABLED),
                config.getBooleanValue(VALIDATE_DATE),
                config.getBooleanValue(VALIDATE_HOST)
        );
    }
}
