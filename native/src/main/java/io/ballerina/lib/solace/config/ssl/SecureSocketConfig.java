package io.ballerina.lib.solace.config.ssl;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.List;

import static io.ballerina.lib.solace.common.CommonUtils.convertToStringArray;

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
