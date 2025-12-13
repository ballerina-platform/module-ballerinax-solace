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

package io.xlibb.config.ssl;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.List;

import static io.xlibb.common.CommonUtils.convertToStringArray;

/**
 * SSL/TLS configuration for secure connections.
 *
 * @param validation         certificate validation settings
 * @param trustStore         trust store configuration, or null
 * @param keyStore           key store configuration for client certificate authentication, or null
 * @param trustedCommonNames list of acceptable common names for broker certificate validation
 */
public record SecureSocketConfig(
        ValidationConfig validation,
        TrustStoreConfig trustStore,
        KeyStoreConfig keyStore,
        List<String> trustedCommonNames) {

    /**
     * Canonical constructor with defensive copying to prevent external modification.
     */
    public SecureSocketConfig {
        // Defensive copy to prevent external modification
        trustedCommonNames = trustedCommonNames == null ? null :
                List.copyOf(trustedCommonNames);
    }

    private static final BString VALIDATION_KEY = StringUtils.fromString("validation");
    private static final BString TRUST_STORE_KEY = StringUtils.fromString("trustStore");
    private static final BString KEY_STORE_KEY = StringUtils.fromString("keyStore");
    private static final BString TRUSTED_COMMON_NAMES_KEY = StringUtils.fromString("trustedCommonNames");

    /**
     * Creates a SecureSocketConfig from a Ballerina map record.
     */
    public SecureSocketConfig(BMap<BString, Object> config) {
        this(
                config.containsKey(VALIDATION_KEY) ?
                        new ValidationConfig((BMap<BString, Object>) config.getMapValue(VALIDATION_KEY)) : null,
                config.containsKey(TRUST_STORE_KEY) ?
                        new TrustStoreConfig((BMap<BString, Object>) config.getMapValue(TRUST_STORE_KEY)) : null,
                config.containsKey(KEY_STORE_KEY) ?
                        new KeyStoreConfig((BMap<BString, Object>) config.getMapValue(KEY_STORE_KEY)) : null,
                config.containsKey(TRUSTED_COMMON_NAMES_KEY) ?
                        List.of(convertToStringArray(config.getArrayValue(TRUSTED_COMMON_NAMES_KEY).getValues())) : null

        );
    }

}
