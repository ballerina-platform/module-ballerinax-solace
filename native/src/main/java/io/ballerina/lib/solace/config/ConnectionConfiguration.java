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

import io.ballerina.lib.solace.config.auth.AuthConfig;
import io.ballerina.lib.solace.config.auth.BasicAuthConfig;
import io.ballerina.lib.solace.config.auth.KerberosConfig;
import io.ballerina.lib.solace.config.auth.OAuth2Config;
import io.ballerina.lib.solace.config.retry.RetryConfig;
import io.ballerina.lib.solace.config.ssl.SecureSocketConfig;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import static io.ballerina.lib.solace.config.ConfigUtils.decimalToMillis;

/**
 * Configuration for establishing a connection to a Solace broker.
 * <p>
 * Contains connection settings, authentication, retry behavior, and SSL/TLS security options.
 *
 * @param messageVpn              name of message VPN to connect to
 * @param clientId                client identifier, or {@code null} for auto-generated ID
 * @param clientDescription       description for application client
 * @param allowDuplicateClientId  {@code true} to allow same client ID across multiple connections
 * @param localhost               local interface IP address to bind for outbound connections, or {@code null}
 * @param connectTimeout          maximum time in milliseconds permitted for connection attempt
 * @param readTimeout             maximum time in milliseconds permitted for reading replies
 * @param compressionLevel        ZLIB compression level (0-9, where 0 means no compression)
 * @param auth                    authentication configuration, or {@code null}
 * @param retryConfig             retry configuration for connection attempts, or {@code null}
 * @param secureSocket            SSL/TLS configuration for secure connections, or {@code null}
 */
public record ConnectionConfiguration(
        String messageVpn,
        String clientId,
        String clientDescription,
        boolean allowDuplicateClientId,
        String localhost,
        long connectTimeout,
        long readTimeout,
        int compressionLevel,
        AuthConfig auth,
        RetryConfig retryConfig,
        SecureSocketConfig secureSocket) {

    // Configuration field names
    private static final BString MESSAGE_VPN = StringUtils.fromString("messageVpn");
    private static final BString CLIENT_ID = StringUtils.fromString("clientId");
    private static final BString CLIENT_DESCRIPTION = StringUtils.fromString("clientDescription");
    private static final BString ALLOW_DUPLICATE_CLIENT_ID = StringUtils.fromString("allowDuplicateClientId");
    private static final BString LOCALHOST = StringUtils.fromString("localhost");
    private static final BString CONNECT_TIMEOUT = StringUtils.fromString("connectTimeout");
    private static final BString READ_TIMEOUT = StringUtils.fromString("readTimeout");
    private static final BString COMPRESSION_LEVEL = StringUtils.fromString("compressionLevel");
    private static final BString AUTH = StringUtils.fromString("auth");
    private static final BString RETRY_CONFIG = StringUtils.fromString("retryConfig");
    private static final BString SECURE_SOCKET = StringUtils.fromString("secureSocket");

    /**
     * Creates a ConnectionConfiguration from Ballerina configuration map.
     *
     * @param config Ballerina configuration map
     */
    public ConnectionConfiguration(BMap<BString, Object> config) {
        this(
                config.getStringValue(MESSAGE_VPN).getValue(),
                config.containsKey(CLIENT_ID) ? config.getStringValue(CLIENT_ID).getValue() : null,
                config.getStringValue(CLIENT_DESCRIPTION).getValue(),
                config.getBooleanValue(ALLOW_DUPLICATE_CLIENT_ID),
                config.containsKey(LOCALHOST) ? config.getStringValue(LOCALHOST).getValue() : null,
                decimalToMillis(((BDecimal) config.get(CONNECT_TIMEOUT)).decimalValue()),
                decimalToMillis(((BDecimal) config.get(READ_TIMEOUT)).decimalValue()),
                config.getIntValue(COMPRESSION_LEVEL).intValue(),
                config.containsKey(AUTH) ? getAuthConfig((BMap<BString, Object>) config.getMapValue(AUTH)) : null,
                config.containsKey(RETRY_CONFIG) ?
                        new RetryConfig((BMap<BString, Object>) config.getMapValue(RETRY_CONFIG)) : null,
                config.containsKey(SECURE_SOCKET) ?
                        new SecureSocketConfig((BMap<BString, Object>) config.getMapValue(SECURE_SOCKET)) : null
        );
    }

    /**
     * Determines and creates appropriate AuthConfig based on configuration.
     *
     * @param authMap authentication configuration map
     * @return AuthConfig instance (BasicAuthConfig, KerberosConfig, or OAuth2Config), or {@code null}
     */
    private static AuthConfig getAuthConfig(BMap<BString, Object> authMap) {
        // Check which type of auth config by inspecting fields
        if (authMap.containsKey(StringUtils.fromString("username"))) {
            return new BasicAuthConfig(authMap);
        } else if (authMap.containsKey(StringUtils.fromString("mutualAuthentication"))) {
            return new KerberosConfig(authMap);
        } else if (authMap.containsKey(StringUtils.fromString("issuer"))) {
            return new OAuth2Config(authMap);
        }
        return null;
    }
}
