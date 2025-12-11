package io.ballerina.lib.solace.config.auth;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * OAuth 2.0/OIDC authentication configuration.
 *
 * @param issuer OAuth 2.0 issuer identifier URI
 * @param accessToken OAuth 2.0 access token for authentication, or null
 * @param oidcToken OpenID Connect (OIDC) ID token for authentication, or null
 */
public record OAuth2Config(String issuer, String accessToken, String oidcToken) implements AuthConfig {
    private static final BString ISSUER_KEY = StringUtils.fromString("issuer");
    private static final BString ACCESS_TOKEN_KEY = StringUtils.fromString("accessToken");
    private static final BString OIDC_TOKEN_KEY = StringUtils.fromString("oidcToken");

    /**
     * Creates an OAuth2Config from a Ballerina map record.
     */
    public OAuth2Config(BMap<BString, Object> config) {
        this(
            config.getStringValue(ISSUER_KEY).getValue(),
            config.containsKey(ACCESS_TOKEN_KEY) ? config.getStringValue(ACCESS_TOKEN_KEY).getValue() : null,
            config.containsKey(OIDC_TOKEN_KEY) ? config.getStringValue(OIDC_TOKEN_KEY).getValue() : null
        );
    }
}
