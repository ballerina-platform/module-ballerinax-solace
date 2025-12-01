package io.ballerina.lib.solace.smf.config.ssl;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Trust store configuration for server certificate validation.
 *
 * @param location URL or file path of trust store
 * @param password password for trust store
 * @param format format of trust store file (e.g., "jks" or "pkcs12")
 */
public record TrustStoreConfig(String location, String password, String format) {
    private static final BString LOCATION_KEY = StringUtils.fromString("location");
    private static final BString PASSWORD_KEY = StringUtils.fromString("password");
    private static final BString FORMAT_KEY = StringUtils.fromString("format");

    /**
     * Creates a TrustStoreConfig from a Ballerina map record.
     */
    public TrustStoreConfig(BMap<BString, Object> config) {
        this(
            config.getStringValue(LOCATION_KEY).getValue(),
            config.getStringValue(PASSWORD_KEY).getValue(),
            config.getStringValue(FORMAT_KEY).getValue()
        );
    }
}