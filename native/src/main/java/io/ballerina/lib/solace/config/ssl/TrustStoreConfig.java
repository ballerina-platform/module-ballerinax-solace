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

package io.xlibb.solace.config.ssl;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Trust store configuration for server certificate validation.
 *
 * @param location URL or file path of trust store
 * @param password password for trust store
 * @param format   format of trust store file (e.g., "jks" or "pkcs12")
 */
public record TrustStoreConfig(String location, String password, String format) {

    private static final BString LOCATION_KEY = StringUtils.fromString("location");
    private static final BString PASSWORD_KEY = StringUtils.fromString("password");
    private static final BString FORMAT_KEY = StringUtils.fromString("format");

    /**
     * Creates a TrustStoreConfig from a Ballerina map record.
     */
    public TrustStoreConfig(BMap<BString, Object> config) {
        this(
                config.getStringValue(LOCATION_KEY).getValue(),
                config.getStringValue(PASSWORD_KEY).getValue(),
                config.getStringValue(FORMAT_KEY).getValue()
        );
    }
}
