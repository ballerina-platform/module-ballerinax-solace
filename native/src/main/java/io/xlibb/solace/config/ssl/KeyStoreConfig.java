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
 * Key store configuration for client certificate authentication.
 *
 * @param location    URL or file path of key store
 * @param password    password for key store
 * @param keyPassword password for private key within key store, or null
 * @param keyAlias    alias of private key to use from key store, or null
 * @param format      format of key store file (e.g., "jks" or "pkcs12")
 */
public record KeyStoreConfig(String location, String password, String keyPassword, String keyAlias, String format) {

    private static final BString LOCATION_KEY = StringUtils.fromString("location");
    private static final BString PASSWORD_KEY = StringUtils.fromString("password");
    private static final BString KEY_PASSWORD_KEY = StringUtils.fromString("keyPassword");
    private static final BString KEY_ALIAS_KEY = StringUtils.fromString("keyAlias");
    private static final BString FORMAT_KEY = StringUtils.fromString("format");

    /**
     * Creates a KeyStoreConfig from a Ballerina map record.
     */
    public KeyStoreConfig(BMap<BString, Object> config) {
        this(
                config.getStringValue(LOCATION_KEY).getValue(),
                config.getStringValue(PASSWORD_KEY).getValue(),
                config.containsKey(KEY_PASSWORD_KEY) ? config.getStringValue(KEY_PASSWORD_KEY).getValue() : null,
                config.containsKey(KEY_ALIAS_KEY) ? config.getStringValue(KEY_ALIAS_KEY).getValue() : null,
                config.getStringValue(FORMAT_KEY).getValue()
        );
    }
}
