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
 * Trust store configuration containing trusted CA certificates for validating broker's certificate.
 *
 * @param location URL or file path of trust store
 * @param password password for trust store
 * @param format   format of trust store file (e.g., "jks" or "pkcs12")
 */
public record TrustStoreConfig(String location, String password, String format) {
    private static final BString LOCATION = StringUtils.fromString("location");
    private static final BString PASSWORD = StringUtils.fromString("password");
    private static final BString FORMAT = StringUtils.fromString("format");

    /**
     * Creates a TrustStoreConfig from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public TrustStoreConfig(BMap<BString, Object> config) {
        this(
                config.getStringValue(LOCATION).getValue(),
                config.getStringValue(PASSWORD).getValue(),
                config.getStringValue(FORMAT).getValue()
        );
    }
}
