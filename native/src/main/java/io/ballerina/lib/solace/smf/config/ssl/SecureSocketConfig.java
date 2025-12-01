package io.ballerina.lib.solace.smf.config.ssl;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SSL/TLS configuration for secure connections.
 *
 * @param validation certificate validation settings
 * @param trustStore trust store configuration, or null
 * @param keyStore key store configuration for client certificate authentication, or null
 * @param trustedCommonNames list of acceptable common names for broker certificate validation
 */
public record SecureSocketConfig(
        ValidationConfig validation,
        TrustStoreConfig trustStore,
        KeyStoreConfig keyStore,
        List<String> trustedCommonNames) {
    private static final BString VALIDATION_KEY = StringUtils.fromString("validation");
    private static final BString TRUST_STORE_KEY = StringUtils.fromString("trustStore");
    private static final BString KEY_STORE_KEY = StringUtils.fromString("keyStore");
    private static final BString TRUSTED_COMMON_NAMES_KEY = StringUtils.fromString("trustedCommonNames");

    /**
     * Creates a SecureSocketConfig from a Ballerina map record.
     */
    public SecureSocketConfig(BMap<BString, Object> config) {
        this(
            getValidationConfig(config),
            getTrustStoreConfig(config),
            getKeyStoreConfig(config),
            getTrustedCommonNames(config)
        );
    }

    private static ValidationConfig getValidationConfig(BMap<BString, Object> config) {
        if (config.containsKey(VALIDATION_KEY)) {
            BMap<BString, Object> validationMap = (BMap<BString, Object>) config.getMapValue(VALIDATION_KEY);
            return new ValidationConfig(validationMap);
        }
        // Default validation config
        return new ValidationConfig(true, true, true);
    }

    private static TrustStoreConfig getTrustStoreConfig(BMap<BString, Object> config) {
        if (config.containsKey(TRUST_STORE_KEY)) {
            BMap<BString, Object> trustStoreMap = (BMap<BString, Object>) config.getMapValue(TRUST_STORE_KEY);
            return new TrustStoreConfig(trustStoreMap);
        }
        return null;
    }

    private static KeyStoreConfig getKeyStoreConfig(BMap<BString, Object> config) {
        if (config.containsKey(KEY_STORE_KEY)) {
            BMap<BString, Object> keyStoreMap = (BMap<BString, Object>) config.getMapValue(KEY_STORE_KEY);
            return new KeyStoreConfig(keyStoreMap);
        }
        return null;
    }

    private static List<String> getTrustedCommonNames(BMap<BString, Object> config) {
        if (config.containsKey(TRUSTED_COMMON_NAMES_KEY)) {
            BArray array = (BArray) config.getArrayValue(TRUSTED_COMMON_NAMES_KEY);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                names.add(((BString) array.get(i)).getValue());
            }
            return Collections.unmodifiableList(names);
        }
        return Collections.emptyList();
    }
}