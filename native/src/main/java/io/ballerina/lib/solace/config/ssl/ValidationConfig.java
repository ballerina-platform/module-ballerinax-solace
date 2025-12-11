package io.ballerina.lib.solace.config.ssl;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Certificate validation configuration.
 *
 * @param enabled          true if certificate validation is enabled
 * @param validateDate     true to validate certificate's expiration date
 * @param validateHostname true to validate that certificate's common name matches broker hostname/IP
 */
public record ValidationConfig(boolean enabled, boolean validateDate, boolean validateHostname) {

    private static final BString ENABLED_KEY = StringUtils.fromString("enabled");
    private static final BString VALIDATE_DATE_KEY = StringUtils.fromString("validateDate");
    private static final BString VALIDATE_HOSTNAME_KEY = StringUtils.fromString("validateHostname");

    /**
     * Creates a ValidationConfig from a Ballerina map record.
     */
    public ValidationConfig(BMap<BString, Object> config) {
        this(
                config.getBooleanValue(ENABLED_KEY),
                config.getBooleanValue(VALIDATE_DATE_KEY),
                config.getBooleanValue(VALIDATE_HOSTNAME_KEY)
        );
    }
}
