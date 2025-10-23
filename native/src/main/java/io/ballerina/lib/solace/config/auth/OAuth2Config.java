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
 * OAuth 2.0 authentication configuration.
 *
 * @param issuer      OAuth 2.0 issuer identifier URI
 * @param accessToken OAuth 2.0 access token for authentication, or {@code null}
 * @param oidcToken   OpenID Connect (OIDC) ID token for authentication, or {@code null}
 */
public record OAuth2Config(String issuer, String accessToken, String oidcToken) implements AuthConfig {
    private static final BString ISSUER = StringUtils.fromString("issuer");
    private static final BString ACCESS_TOKEN = StringUtils.fromString("accessToken");
    private static final BString OIDC_TOKEN = StringUtils.fromString("oidcToken");

    /**
     * Creates an OAuth2Config from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public OAuth2Config(BMap<BString, Object> config) {
        this(
                config.getStringValue(ISSUER).getValue(),
                config.containsKey(ACCESS_TOKEN) ? config.getStringValue(ACCESS_TOKEN).getValue() : null,
                config.containsKey(OIDC_TOKEN) ? config.getStringValue(OIDC_TOKEN).getValue() : null
        );
    }
}
