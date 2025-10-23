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

package io.ballerina.lib.solace.config.auth;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Basic authentication configuration using username and password.
 *
 * @param username username for authentication (max 32 characters)
 * @param password password for authentication (max 128 characters), or {@code null}
 */
public record BasicAuthConfig(String username, String password) implements AuthConfig {
    private static final BString USERNAME = StringUtils.fromString("username");
    private static final BString PASSWORD = StringUtils.fromString("password");

    /**
     * Creates a BasicAuthConfig from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public BasicAuthConfig(BMap<BString, Object> config) {
        this(
                config.getStringValue(USERNAME).getValue(),
                config.containsKey(PASSWORD) ? config.getStringValue(PASSWORD).getValue() : null
        );
    }
}
