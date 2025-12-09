package io.ballerina.lib.solace.smf.config;

import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPProperties;
import io.ballerina.lib.solace.smf.config.auth.AuthConfig;
import io.ballerina.lib.solace.smf.config.auth.BasicAuthConfig;
import io.ballerina.lib.solace.smf.config.auth.KerberosConfig;
import io.ballerina.lib.solace.smf.config.auth.OAuth2Config;
import io.ballerina.lib.solace.smf.config.retry.RetryConfig;
import io.ballerina.lib.solace.smf.config.ssl.KeyStoreConfig;
import io.ballerina.lib.solace.smf.config.ssl.SecureSocketConfig;
import io.ballerina.lib.solace.smf.config.ssl.TrustStoreConfig;

/**
 * Utility class for building JCSMP properties from Ballerina configuration objects.
 */
public final class ConfigurationUtils {

    private ConfigurationUtils() {
        // Utility class - no instantiation
    }

    /**
     * Builds JCSMPProperties from a ConnectionConfiguration and host URL.
     *
     * @param host   the broker host URL (from Ballerina init's url parameter)
     * @param config the connection configuration
     * @return configured JCSMPProperties
     * @throws Exception if configuration fails
     */
    public static JCSMPProperties buildJCSMPProperties(String host, ConnectionConfiguration config) throws Exception {
        JCSMPProperties props = new JCSMPProperties();

        // Set host and VPN
        props.setProperty(JCSMPProperties.HOST, host);
        props.setProperty(JCSMPProperties.VPN_NAME, config.vpnName());

        // Set client identification
        if (config.clientName() != null) {
            props.setProperty(JCSMPProperties.CLIENT_NAME, config.clientName());
        }
        props.setProperty(JCSMPProperties.APPLICATION_DESCRIPTION, config.clientDescription());

        // Set local address if specified
        if (config.localAddress() != null) {
            props.setProperty(JCSMPProperties.LOCALHOST, config.localAddress());
        }

        // Set timestamp and sequence number generation
        props.setProperty(JCSMPProperties.GENERATE_RCV_TIMESTAMPS, config.generateReceiveTimestamps());
        props.setProperty(JCSMPProperties.GENERATE_SEND_TIMESTAMPS, config.generateSendTimestamps());
        props.setProperty(JCSMPProperties.GENERATE_SEQUENCE_NUMBERS, config.generateSequenceNumbers());
        props.setProperty(JCSMPProperties.CALCULATE_MESSAGE_EXPIRATION, config.calculateMessageExpiration());

        // Set channel properties (timeouts, retries, compression)
        setChannelProperties(props, config);

        // Set authentication
        setAuthentication(props, config.auth());

        // Set SSL/TLS if configured
        if (config.secureSocket() != null) {
            setSecureSocket(props, config.secureSocket());
        }

        return props;
    }

    /**
     * Sets channel properties like timeouts, retries, and compression.
     */
    private static void setChannelProperties(JCSMPProperties props, ConnectionConfiguration config) {
        JCSMPChannelProperties channelProps = (JCSMPChannelProperties) props.getProperty(
                JCSMPProperties.CLIENT_CHANNEL_PROPERTIES);

        // Set connection timeouts
        channelProps.setProperty(JCSMPChannelProperties.CONNECT_TIMEOUT_IN_MILLIS,
                (int) config.connectionTimeout());
        channelProps.setProperty(JCSMPChannelProperties.READ_TIMEOUT_IN_MILLIS,
                (int) config.readTimeout());

        // Set compression level
        if (config.compressionLevel() > 0) {
            channelProps.setProperty(JCSMPChannelProperties.COMPRESSION_LEVEL, config.compressionLevel());
        }

        // Set retry configuration if specified
        if (config.retryConfig() != null) {
            RetryConfig retry = config.retryConfig();
            channelProps.setProperty(JCSMPChannelProperties.CONNECT_RETRIES, retry.connectRetries());
            channelProps.setProperty(JCSMPChannelProperties.CONNECT_RETRIES_PER_HOST,
                    retry.connectRetriesPerHost());
            channelProps.setProperty(JCSMPChannelProperties.RECONNECT_RETRIES, retry.reconnectRetries());
            channelProps.setProperty(JCSMPChannelProperties.RECONNECT_RETRY_WAIT_IN_MILLIS,
                    (int) retry.reconnectRetryWait());
        }
    }

    /**
     * Sets authentication based on the AuthConfig type using sealed interface pattern.
     */
    private static void setAuthentication(JCSMPProperties props, AuthConfig auth) throws Exception {
        if (auth == null) {
            // Default to basic authentication with empty credentials
            props.setProperty(JCSMPProperties.AUTHENTICATION_SCHEME, JCSMPProperties.AUTHENTICATION_SCHEME_BASIC);
            return;
        }

        if (auth instanceof BasicAuthConfig basicAuth) {
            props.setProperty(JCSMPProperties.AUTHENTICATION_SCHEME, JCSMPProperties.AUTHENTICATION_SCHEME_BASIC);
            props.setProperty(JCSMPProperties.USERNAME, basicAuth.username());
            if (basicAuth.password() != null) {
                props.setProperty(JCSMPProperties.PASSWORD, basicAuth.password());
            }
        } else if (auth instanceof KerberosConfig kerberosAuth) {
            props.setProperty(JCSMPProperties.AUTHENTICATION_SCHEME, JCSMPProperties.AUTHENTICATION_SCHEME_GSS_KRB);
            props.setProperty(JCSMPProperties.KRB_SERVICE_NAME, kerberosAuth.serviceName());
            props.setProperty(JCSMPProperties.JAAS_LOGIN_CONTEXT, kerberosAuth.jaasLoginContext());
            props.setProperty(JCSMPProperties.JAAS_CONFIG_FILE_RELOAD_ENABLED,
                    kerberosAuth.jaasConfigFileReloadEnabled());
        } else if (auth instanceof OAuth2Config oauth2Auth) {
            props.setProperty(JCSMPProperties.AUTHENTICATION_SCHEME, JCSMPProperties.AUTHENTICATION_SCHEME_OAUTH2);
            if (oauth2Auth.issuer() != null) {
                props.setProperty(JCSMPProperties.OAUTH2_ISSUER_IDENTIFIER, oauth2Auth.issuer());
            }
            if (oauth2Auth.accessToken() != null) {
                props.setProperty(JCSMPProperties.OAUTH2_ACCESS_TOKEN, oauth2Auth.accessToken());
            }
            if (oauth2Auth.oidcToken() != null) {
                props.setProperty(JCSMPProperties.OIDC_ID_TOKEN, oauth2Auth.oidcToken());
            }
        }
    }

    /**
     * Sets SSL/TLS configuration.
     */
    private static void setSecureSocket(JCSMPProperties props, SecureSocketConfig secureSocket) throws Exception {
        // Set trust store for server certificate validation
        if (secureSocket.trustStore() != null) {
            TrustStoreConfig trustStore = secureSocket.trustStore();
            props.setProperty(JCSMPProperties.SSL_TRUST_STORE, trustStore.location());
            props.setProperty(JCSMPProperties.SSL_TRUST_STORE_PASSWORD, trustStore.password());
            props.setProperty(JCSMPProperties.SSL_TRUST_STORE_FORMAT, trustStore.format());
        }

        // Set key store for client certificate authentication
        if (secureSocket.keyStore() != null) {
            KeyStoreConfig keyStore = secureSocket.keyStore();
            props.setProperty(JCSMPProperties.SSL_KEY_STORE, keyStore.location());
            props.setProperty(JCSMPProperties.SSL_KEY_STORE_PASSWORD, keyStore.password());
            if (keyStore.keyPassword() != null) {
                props.setProperty(JCSMPProperties.SSL_PRIVATE_KEY_PASSWORD, keyStore.keyPassword());
            }
            if (keyStore.keyAlias() != null) {
                props.setProperty(JCSMPProperties.SSL_PRIVATE_KEY_ALIAS, keyStore.keyAlias());
            }
            props.setProperty(JCSMPProperties.SSL_KEY_STORE_FORMAT, keyStore.format());
        }

        // Set certificate validation
        props.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, secureSocket.validation().enabled());
        props.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, secureSocket.validation().validateDate());
        props.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_HOST,
                secureSocket.validation().validateHostname());

        // Set trusted common names
        if (!secureSocket.trustedCommonNames().isEmpty()) {
            StringBuilder cnList = new StringBuilder();
            for (String cn : secureSocket.trustedCommonNames()) {
                if (cnList.length() > 0) {
                    cnList.append(",");
                }
                cnList.append(cn);
            }
            props.setProperty(JCSMPProperties.SSL_TRUSTED_COMMON_NAME_LIST, cnList.toString());
        }
    }
}
