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
 * Key store configuration containing client's private key and certificate for client certificate authentication.
 *
 * @param location    URL or file path of key store
 * @param password    password for key store
 * @param keyPassword password for private key within key store, or {@code null} to use key store password
 * @param keyAlias    alias of private key to use from key store, or {@code null} to use first key found
 * @param format      format of key store file (e.g., "jks" or "pkcs12")
 */
public record KeyStoreConfig(String location, String password, String keyPassword, String keyAlias, String format) {
    private static final BString LOCATION = StringUtils.fromString("location");
    private static final BString PASSWORD = StringUtils.fromString("password");
    private static final BString KEY_PASSWORD = StringUtils.fromString("keyPassword");
    private static final BString KEY_ALIAS = StringUtils.fromString("keyAlias");
    private static final BString FORMAT = StringUtils.fromString("format");

    /**
     * Creates a KeyStoreConfig from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public KeyStoreConfig(BMap<BString, Object> config) {
        this(
                config.getStringValue(LOCATION).getValue(),
                config.getStringValue(PASSWORD).getValue(),
                config.containsKey(KEY_PASSWORD) ? config.getStringValue(KEY_PASSWORD).getValue() : null,
                config.containsKey(KEY_ALIAS) ? config.getStringValue(KEY_ALIAS).getValue() : null,
                config.getStringValue(FORMAT).getValue()
        );
    }
}
