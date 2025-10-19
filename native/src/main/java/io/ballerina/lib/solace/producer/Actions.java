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

package io.ballerina.lib.solace.producer;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;
import com.solacesystems.jms.SupportedProperty;
import io.ballerina.lib.solace.config.ConnectionConfiguration;
import io.ballerina.lib.solace.config.auth.BasicAuthConfig;
import io.ballerina.lib.solace.config.auth.KerberosConfig;
import io.ballerina.lib.solace.config.auth.OAuth2Config;
import io.ballerina.lib.solace.config.retry.RetryConfig;
import io.ballerina.lib.solace.config.ssl.SecureSocketConfig;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;

/**
 * Actions class for {@link javax.jms.MessageProducer} with utility methods to invoke as inter-op functions.
 */
public final class Actions {

    // Native object storage keys
    private static final String NATIVE_PRODUCER = "native.producer";
    private static final String NATIVE_SESSION = "native.session";
    private static final String NATIVE_CONNECTION = "native.connection";

    private Actions() {
        // Prevent instantiation
    }

    /**
     * Creates a {@link javax.jms.MessageProducer} using the broker URL and producer configurations.
     *
     * @param producer Ballerina producer object
     * @param url      Solace broker URL
     * @param config   Ballerina producer configurations
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object init(BObject producer, BString url, BMap<BString, Object> config) {
        try {
            ProducerConfiguration producerConfig = new ProducerConfiguration(config);
            ConnectionConfiguration connConfig = producerConfig.connectionConfig();
            Hashtable<String, Object> connectionProps = buildConnectionProperties(url.getValue(), connConfig);
            SolConnectionFactory connectionFactory = SolJmsUtility.createConnectionFactory(connectionProps);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(producerConfig.transacted(), Session.AUTO_ACKNOWLEDGE);
            Destination destination = SolaceUtils.createDestination(session, producerConfig.destination());
            MessageProducer jmsProducer = session.createProducer(destination);

            producer.addNativeData(NATIVE_PRODUCER, jmsProducer);
            producer.addNativeData(NATIVE_SESSION, session);
            producer.addNativeData(NATIVE_CONNECTION, connection);

            return null;
        } catch (JMSException exception) {
            return SolaceUtils.createError(
                    String.format("Error occurred while initializing the Solace MessageProducer: %s",
                            exception.getMessage()), exception);
        } catch (Exception exception) {
            return SolaceUtils.createError(
                    String.format("Unexpected error occurred during producer initialization: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Sends a message to a destination in the Solace message broker.
     *
     * @param producer Ballerina producer object
     * @param bMessage Ballerina Solace JMS message representation
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object send(BObject producer, BMap<BString, Object> bMessage) {
        MessageProducer nativeProducer = (MessageProducer) producer.getNativeData(NATIVE_PRODUCER);
        Session nativeSession = (Session) producer.getNativeData(NATIVE_SESSION);

        CompletableFuture<Object> future = new CompletableFuture<>();
        Thread.startVirtualThread(() -> {
            try {
                Message message = MessageConverter.toJmsMessage(nativeSession, bMessage);
                nativeProducer.send(message);
                future.complete(null);
            } catch (JMSException exception) {
                future.complete(SolaceUtils.createError(
                        String.format("Error occurred while sending message to Solace broker: %s",
                                exception.getMessage()), exception));
            } catch (Exception exception) {
                future.complete(SolaceUtils.createError(
                        String.format("Unexpected error occurred while sending message: %s",
                                exception.getMessage()), exception));
            }
        });

        try {
            return future.get();
        } catch (Exception exception) {
            return SolaceUtils.createError(
                    String.format("Error occurred while waiting for operation to complete: %s",
                            exception.getMessage()), exception);
        }
    }

    /**
     * Closes the message producer and the underlying connection.
     *
     * @param producer Ballerina producer object
     * @return {@code null} on success, or Ballerina {@code solace:Error} on failure
     */
    public static Object close(BObject producer) {
        try {
            MessageProducer nativeProducer = (MessageProducer) producer.getNativeData(NATIVE_PRODUCER);
            Session nativeSession = (Session) producer.getNativeData(NATIVE_SESSION);
            Connection nativeConnection = (Connection) producer.getNativeData(NATIVE_CONNECTION);

            if (nativeProducer != null) {
                nativeProducer.close();
            }
            if (nativeSession != null) {
                nativeSession.close();
            }
            if (nativeConnection != null) {
                nativeConnection.close();
            }

            return null;
        } catch (JMSException exception) {
            return SolaceUtils.createError(
                    String.format("Error occurred while closing the message producer: %s",
                            exception.getMessage()), exception);
        }
    }

    private static Hashtable<String, Object> buildConnectionProperties(String url, ConnectionConfiguration config) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Context.PROVIDER_URL, url);
        props.put(SupportedProperty.SOLACE_JMS_VPN, config.messageVpn());

        if (config.clientId() != null) {
            props.put(SupportedProperty.SOLACE_JMS_JNDI_CLIENT_ID, config.clientId());
        }
        if (config.clientDescription() != null && !config.clientDescription().isEmpty()) {
            props.put(SupportedProperty.SOLACE_JMS_JNDI_CLIENT_DESCRIPTION, config.clientDescription());
        }

        props.put(SupportedProperty.SOLACE_JMS_JNDI_CONNECT_TIMEOUT, Math.toIntExact(config.connectTimeout()));
        props.put(SupportedProperty.SOLACE_JMS_JNDI_READ_TIMEOUT, Math.toIntExact(config.readTimeout()));
        props.put(SupportedProperty.SOLACE_JMS_COMPRESSION_LEVEL, config.compressionLevel());

        if (config.localhost() != null) {
            props.put(SupportedProperty.SOLACE_JMS_LOCALHOST, config.localhost());
        }

        if (config.retryConfig() != null) {
            RetryConfig retryConfig = config.retryConfig();
            props.put(SupportedProperty.SOLACE_JMS_JNDI_CONNECT_RETRIES, retryConfig.connectRetries());
            props.put(SupportedProperty.SOLACE_JMS_JNDI_CONNECT_RETRIES_PER_HOST,
                    retryConfig.connectRetriesPerHost());
            props.put(SupportedProperty.SOLACE_JMS_JNDI_RECONNECT_RETRIES, retryConfig.reconnectRetries());
            props.put(SupportedProperty.SOLACE_JMS_JNDI_RECONNECT_RETRY_WAIT,
                    Math.toIntExact(retryConfig.reconnectRetryWait()));
        }

        // Authentication priority: explicit auth config > client certificate (keyStore) > basic auth (default)
        if (config.auth() != null) {
            switch (config.auth()) {
                case BasicAuthConfig basic -> {
                    props.put(SupportedProperty.SOLACE_JMS_AUTHENTICATION_SCHEME,
                            SupportedProperty.AUTHENTICATION_SCHEME_BASIC);
                    props.put(Context.SECURITY_PRINCIPAL, basic.username());
                    if (basic.password() != null) {
                        props.put(Context.SECURITY_CREDENTIALS, basic.password());
                    }
                }
                case KerberosConfig kerberos -> {
                    props.put(SupportedProperty.SOLACE_JMS_AUTHENTICATION_SCHEME,
                            SupportedProperty.AUTHENTICATION_SCHEME_GSS_KRB);
                    props.put(SupportedProperty.SOLACE_JMS_KRB_MUTUAL_AUTHENTICATION,
                            kerberos.mutualAuthentication());
                    props.put(SupportedProperty.SOLACE_JMS_KRB_SERVICE_NAME, kerberos.serviceName());
                    if (kerberos.jaasLoginContext() != null) {
                        props.put(SupportedProperty.SOLACE_JMS_JAAS_LOGIN_CONTEXT, kerberos.jaasLoginContext());
                    }
                }
                case OAuth2Config oauth -> {
                    props.put(SupportedProperty.SOLACE_JMS_AUTHENTICATION_SCHEME,
                            SupportedProperty.AUTHENTICATION_SCHEME_OAUTH2);
                    props.put(SupportedProperty.SOLACE_JMS_OAUTH2_ISSUER_IDENTIFIER, oauth.issuer());
                    if (oauth.accessToken() != null) {
                        props.put(SupportedProperty.SOLACE_JMS_OAUTH2_ACCESS_TOKEN, oauth.accessToken());
                    }
                    if (oauth.oidcToken() != null) {
                        props.put(SupportedProperty.SOLACE_JMS_OIDC_ID_TOKEN, oauth.oidcToken());
                    }
                }
            }
        } else {
            props.put(SupportedProperty.SOLACE_JMS_AUTHENTICATION_SCHEME,
                    SupportedProperty.AUTHENTICATION_SCHEME_BASIC);
        }

        if (config.secureSocket() != null) {
            SecureSocketConfig sslConfig = config.secureSocket();

            if (sslConfig.trustStore() != null) {
                var trustStore = sslConfig.trustStore();
                props.put(SupportedProperty.SOLACE_JMS_SSL_TRUST_STORE, trustStore.location());
                props.put(SupportedProperty.SOLACE_JMS_SSL_TRUST_STORE_PASSWORD, trustStore.password());
                props.put(SupportedProperty.SOLACE_JMS_SSL_TRUST_STORE_FORMAT, trustStore.format());
            }

            if (sslConfig.keyStore() != null) {
                // Override to client certificate auth when keyStore is present without explicit auth config
                if (config.auth() == null) {
                    props.put(SupportedProperty.SOLACE_JMS_AUTHENTICATION_SCHEME,
                            SupportedProperty.AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE);
                }
                var keyStore = sslConfig.keyStore();
                props.put(SupportedProperty.SOLACE_JMS_SSL_KEY_STORE, keyStore.location());
                props.put(SupportedProperty.SOLACE_JMS_SSL_KEY_STORE_PASSWORD, keyStore.password());
                props.put(SupportedProperty.SOLACE_JMS_SSL_KEY_STORE_FORMAT, keyStore.format());
                if (keyStore.keyPassword() != null) {
                    props.put(SupportedProperty.SOLACE_JMS_SSL_PRIVATE_KEY_PASSWORD, keyStore.keyPassword());
                }
                if (keyStore.keyAlias() != null) {
                    props.put(SupportedProperty.SOLACE_JMS_SSL_PRIVATE_KEY_ALIAS, keyStore.keyAlias());
                }
            }

            props.put(SupportedProperty.SOLACE_JMS_SSL_VALIDATE_CERTIFICATE, sslConfig.validation().enabled());
            props.put(SupportedProperty.SOLACE_JMS_SSL_VALIDATE_CERTIFICATE_DATE,
                    sslConfig.validation().validateDate());
            props.put(SupportedProperty.SOLACE_JMS_SSL_VALIDATE_CERTIFICATE_HOST,
                    sslConfig.validation().validateHost());

            if (sslConfig.protocols() != null && !sslConfig.protocols().isEmpty()) {
                props.put(SupportedProperty.SOLACE_JMS_SSL_PROTOCOL, String.join(",", sslConfig.protocols()));
            }

            if (sslConfig.cipherSuites() != null && !sslConfig.cipherSuites().isEmpty()) {
                props.put(SupportedProperty.SOLACE_JMS_SSL_CIPHER_SUITES,
                        String.join(",", sslConfig.cipherSuites()));
            }

            if (sslConfig.trustedCommonNames() != null && !sslConfig.trustedCommonNames().isEmpty()) {
                props.put(SupportedProperty.SOLACE_JMS_SSL_TRUSTED_COMMON_NAME_LIST,
                        String.join(",", sslConfig.trustedCommonNames()));
            }
        }

        return props;
    }
}
