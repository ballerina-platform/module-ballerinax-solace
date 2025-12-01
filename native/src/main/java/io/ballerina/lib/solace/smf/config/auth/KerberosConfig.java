package io.ballerina.lib.solace.smf.config.auth;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

/**
 * Kerberos (GSS-KRB) authentication configuration.
 *
 * @param serviceName Kerberos service name used during authentication
 * @param jaasLoginContext JAAS login context name to use for authentication
 * @param mutualAuthentication true to enable Kerberos mutual authentication
 * @param jaasConfigFileReloadEnabled true to enable automatic reload of JAAS configuration file
 */
public record KerberosConfig(
        String serviceName,
        String jaasLoginContext,
        boolean mutualAuthentication,
        boolean jaasConfigFileReloadEnabled) implements AuthConfig {
    private static final BString SERVICE_NAME_KEY = StringUtils.fromString("serviceName");
    private static final BString JAAS_LOGIN_CONTEXT_KEY = StringUtils.fromString("jaasLoginContext");
    private static final BString MUTUAL_AUTH_KEY = StringUtils.fromString("mutualAuthentication");
    private static final BString JAAS_RELOAD_KEY = StringUtils.fromString("jaasConfigFileReloadEnabled");

    /**
     * Creates a KerberosConfig from a Ballerina map record.
     */
    public KerberosConfig(BMap<BString, Object> config) {
        this(
            config.getStringValue(SERVICE_NAME_KEY).getValue(),
            config.getStringValue(JAAS_LOGIN_CONTEXT_KEY).getValue(),
            config.getBooleanValue(MUTUAL_AUTH_KEY),
            config.getBooleanValue(JAAS_RELOAD_KEY)
        );
    }
}
