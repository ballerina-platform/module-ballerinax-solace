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

import io.ballerina.lib.solace.CommonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.List;

/**
 * SSL/TLS configuration for secure connections to a Solace broker.
 *
 * @param validation          certificate validation settings
 * @param trustStore          trust store configuration containing trusted CA certificates, or {@code null}
 * @param keyStore            key store configuration containing client's private key and certificate, or {@code null}
 * @param protocols           unmodifiable list of SSL/TLS protocol versions to enable (e.g., TLSv1.2), or {@code null}
 * @param cipherSuites        unmodifiable list of cipher suites to enable for connection, or {@code null}
 * @param trustedCommonNames  unmodifiable list of acceptable common names for broker certificate validation
 *                            (max 16 entries), or {@code null}
 */
public record SecureSocketConfig(
        ValidationConfig validation,
        TrustStoreConfig trustStore,
        KeyStoreConfig keyStore,
        List<String> protocols,
        List<String> cipherSuites,
        List<String> trustedCommonNames) {

    private static final BString VALIDATION = StringUtils.fromString("validation");
    private static final BString TRUST_STORE = StringUtils.fromString("trustStore");
    private static final BString KEY_STORE = StringUtils.fromString("keyStore");
    private static final BString PROTOCOLS = StringUtils.fromString("protocols");
    private static final BString CIPHER_SUITES = StringUtils.fromString("cipherSuites");
    private static final BString TRUSTED_COMMON_NAMES = StringUtils.fromString("trustedCommonNames");

    /**
     * Creates a SecureSocketConfig from Ballerina configuration map.
     * Creates unmodifiable defensive copies of all list parameters.
     *
     * @param config Ballerina configuration map
     */
    public SecureSocketConfig(BMap<BString, Object> config) {
        this(
                new ValidationConfig((BMap<BString, Object>) config.getMapValue(VALIDATION)),
                config.containsKey(TRUST_STORE) ?
                        new TrustStoreConfig((BMap<BString, Object>) config.getMapValue(TRUST_STORE)) : null,
                config.containsKey(KEY_STORE) ?
                        new KeyStoreConfig((BMap<BString, Object>) config.getMapValue(KEY_STORE)) : null,
                config.containsKey(PROTOCOLS) ?
                        List.of(CommonUtils.mapProtocols(CommonUtils.convertToStringArray(
                                config.getArrayValue(PROTOCOLS).getValues()))) : null,
                config.containsKey(CIPHER_SUITES) ?
                        List.of(CommonUtils.convertToStringArray(
                                config.getArrayValue(CIPHER_SUITES).getValues())) : null,
                config.containsKey(TRUSTED_COMMON_NAMES) ?
                        List.of(CommonUtils.convertToStringArray(
                                config.getArrayValue(TRUSTED_COMMON_NAMES).getValues())) : null
        );
    }
}
