package io.ballerina.lib.solace.smf.config;

import io.ballerina.lib.solace.smf.config.auth.AuthConfig;
import io.ballerina.lib.solace.smf.config.auth.BasicAuthConfig;
import io.ballerina.lib.solace.smf.config.auth.KerberosConfig;
import io.ballerina.lib.solace.smf.config.auth.OAuth2Config;
import io.ballerina.lib.solace.smf.config.retry.RetryConfig;
import io.ballerina.lib.solace.smf.config.ssl.SecureSocketConfig;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.math.BigDecimal;

/**
 * Connection configuration for Solace JCSMP connections.
 *
 * @param host broker host URL (supports protocol prefixes and port)
 * @param vpnName message VPN to connect to
 * @param clientId client identifier, or null for auto-generated
 * @param clientDescription description for application client
 * @param localAddress local interface IP address to bind, or null
 * @param connectionTimeout maximum time in milliseconds for connection attempt
 * @param readTimeout maximum time in milliseconds for reading replies
 * @param compressionLevel ZLIB compression level (0-9, 0 = disabled)
 * @param transacted true to enable transacted messaging
 * @param auth authentication configuration, or null
 * @param retryConfig retry configuration, or null
 * @param secureSocket SSL/TLS configuration, or null
 */
public record ConnectionConfiguration(
        String host,
        String vpnName,
        String clientId,
        String clientDescription,
        String localAddress,
        long connectionTimeout,
        long readTimeout,
        int compressionLevel,
        boolean transacted,
        AuthConfig auth,
        RetryConfig retryConfig,
        SecureSocketConfig secureSocket) {

    private static final BString HOST_KEY = StringUtils.fromString("host");
    private static final BString VPN_NAME_KEY = StringUtils.fromString("vpnName");
    private static final BString CLIENT_ID_KEY = StringUtils.fromString("clientId");
    private static final BString CLIENT_DESCRIPTION_KEY = StringUtils.fromString("clientDescription");
    private static final BString LOCAL_ADDRESS_KEY = StringUtils.fromString("localAddress");
    private static final BString CONNECTION_TIMEOUT_KEY = StringUtils.fromString("connectionTimeout");
    private static final BString READ_TIMEOUT_KEY = StringUtils.fromString("readTimeout");
    private static final BString COMPRESSION_LEVEL_KEY = StringUtils.fromString("compressionLevel");
    private static final BString TRANSACTED_KEY = StringUtils.fromString("transacted");
    private static final BString AUTH_KEY = StringUtils.fromString("auth");
    private static final BString RETRY_CONFIG_KEY = StringUtils.fromString("retryConfig");
    private static final BString SECURE_SOCKET_KEY = StringUtils.fromString("secureSocket");

    /**
     * Creates a ConnectionConfiguration from a Ballerina map record.
     */
    public ConnectionConfiguration(BMap<BString, Object> config) {
        this(
            config.getStringValue(HOST_KEY).getValue(),
            config.getStringValue(VPN_NAME_KEY).getValue(),
            getOptionalString(config, CLIENT_ID_KEY),
            config.getStringValue(CLIENT_DESCRIPTION_KEY).getValue(),
            getOptionalString(config, LOCAL_ADDRESS_KEY),
            decimalToMillis(((BDecimal) config.get(CONNECTION_TIMEOUT_KEY)).decimalValue()),
            decimalToMillis(((BDecimal) config.get(READ_TIMEOUT_KEY)).decimalValue()),
            Math.toIntExact(config.getIntValue(COMPRESSION_LEVEL_KEY)),
            config.getBooleanValue(TRANSACTED_KEY),
            getAuthConfig(config),
            getRetryConfig(config),
            getSecureSocketConfig(config)
        );
    }

    /**
     * Extracts optional string value from map.
     */
    private static String getOptionalString(BMap<BString, Object> map, BString key) {
        if (map.containsKey(key)) {
            return map.getStringValue(key).getValue();
        }
        return null;
    }

    /**
     * Extracts and converts authentication configuration from map.
     */
    private static AuthConfig getAuthConfig(BMap<BString, Object> config) {
        if (!config.containsKey(AUTH_KEY)) {
            return null;
        }
        BMap<BString, Object> authMap = (BMap<BString, Object>) config.getMapValue(AUTH_KEY);
        return createAuthConfig(authMap);
    }

    /**
     * Factory method to create appropriate AuthConfig based on fields present.
     */
    private static AuthConfig createAuthConfig(BMap<BString, Object> authMap) {
        BString usernameKey = StringUtils.fromString("username");
        BString issuerKey = StringUtils.fromString("issuer");
        BString serviceNameKey = StringUtils.fromString("serviceName");

        if (authMap.containsKey(usernameKey)) {
            return new BasicAuthConfig(authMap);
        } else if (authMap.containsKey(issuerKey)) {
            return new OAuth2Config(authMap);
        } else if (authMap.containsKey(serviceNameKey)) {
            return new KerberosConfig(authMap);
        }
        return null;
    }

    /**
     * Extracts retry configuration from map.
     */
    private static RetryConfig getRetryConfig(BMap<BString, Object> config) {
        if (config.containsKey(RETRY_CONFIG_KEY)) {
            BMap<BString, Object> retryMap = (BMap<BString, Object>) config.getMapValue(RETRY_CONFIG_KEY);
            return new RetryConfig(retryMap);
        }
        return null;
    }

    /**
     * Extracts secure socket configuration from map.
     */
    private static SecureSocketConfig getSecureSocketConfig(BMap<BString, Object> config) {
        if (config.containsKey(SECURE_SOCKET_KEY)) {
            BMap<BString, Object> secureSocketMap = (BMap<BString, Object>) config.getMapValue(SECURE_SOCKET_KEY);
            return new SecureSocketConfig(secureSocketMap);
        }
        return null;
    }

    /**
     * Converts decimal seconds to milliseconds.
     */
    private static long decimalToMillis(BigDecimal seconds) {
        return seconds.multiply(BigDecimal.valueOf(1000)).longValue();
    }
}