package io.ballerina.lib.solace.smf.config.auth;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Basic authentication configuration using username and password.
 *
 * @param username username for authentication
 * @param password password for authentication, or null
 */
public record BasicAuthConfig(String username, String password) implements AuthConfig {
    private static final BString USERNAME_KEY = StringUtils.fromString("username");
    private static final BString PASSWORD_KEY = StringUtils.fromString("password");

    /**
     * Creates a BasicAuthConfig from a Ballerina map record.
     */
    public BasicAuthConfig(BMap<BString, Object> config) {
        this(
            config.getStringValue(USERNAME_KEY).getValue(),
            config.containsKey(PASSWORD_KEY) ? config.getStringValue(PASSWORD_KEY).getValue() : null
        );
    }
}