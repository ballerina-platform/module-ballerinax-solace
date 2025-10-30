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

package io.ballerina.lib.solace;

import com.solacesystems.jms.SupportedProperty;
import io.ballerina.lib.solace.config.ConnectionConfiguration;
import io.ballerina.lib.solace.config.auth.BasicAuthConfig;
import io.ballerina.lib.solace.config.auth.KerberosConfig;
import io.ballerina.lib.solace.config.auth.OAuth2Config;
import io.ballerina.lib.solace.config.retry.RetryConfig;
import io.ballerina.lib.solace.config.ssl.SecureSocketConfig;
import io.ballerina.lib.solace.consumer.ConsumerType;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Objects;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;

/**
 * Common utility methods for Solace connector.
 */
public final class CommonUtils {

    private static final String SOLACE_ERROR = "Error";

    private CommonUtils() {}

    /**
     * Builds Solace JNDI connection properties from connection configuration.
     *
     * @param url    Solace broker URL
     * @param config Connection configuration
     * @return Hashtable of connection properties
     */
    public static Hashtable<String, Object> buildConnectionProperties(String url, ConnectionConfiguration config) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Context.PROVIDER_URL, url);
        props.put(SupportedProperty.SOLACE_JMS_VPN, config.messageVpn());
        props.put(SupportedProperty.SOLACE_JMS_DYNAMIC_DURABLES, config.enableDynamicDurables());

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

    /**
     * Creates a Ballerina error with given message.
     *
     * @param message error message
     * @return Ballerina error
     */
    public static BError createError(String message) {
        return ErrorCreator.createError(ModuleUtils.getModule(), SOLACE_ERROR,
                StringUtils.fromString(message), null, null);
    }

    /**
     * Creates a Ballerina error with given message and cause.
     *
     * @param message error message
     * @param cause   exception cause
     * @return Ballerina error
     */
    public static BError createError(String message, Throwable cause) {
        return ErrorCreator.createError(ModuleUtils.getModule(), SOLACE_ERROR,
                StringUtils.fromString(message), ErrorCreator.createError(cause), null);
    }

    /**
     * Creates a JMS MessageConsumer for a queue.
     *
     * @param session         JMS session
     * @param queueName       Queue name
     * @param messageSelector Optional message selector
     * @return JMS MessageConsumer
     * @throws JMSException if consumer creation fails
     */
    public static MessageConsumer createQueueConsumer(Session session, String queueName, String messageSelector)
            throws JMSException {
        Queue queue = session.createQueue(queueName);

        if (messageSelector != null && !messageSelector.isEmpty()) {
            return session.createConsumer(queue, messageSelector);
        } else {
            return session.createConsumer(queue);
        }
    }

    /**
     * Creates a JMS MessageConsumer for a topic.
     *
     * @param session         JMS session
     * @param topicName       Topic name
     * @param messageSelector Optional message selector
     * @param noLocal         No local flag
     * @param consumerType    Consumer type (DEFAULT or DURABLE)
     * @param subscriberName  Subscriber name (required for DURABLE)
     * @return JMS MessageConsumer
     * @throws JMSException if consumer creation fails
     */
    public static MessageConsumer createTopicConsumer(Session session, String topicName, String messageSelector,
                                                      boolean noLocal, ConsumerType consumerType,
                                                      String subscriberName) throws JMSException {
        Topic topic = session.createTopic(topicName);

        return switch (consumerType) {
            case DEFAULT -> {
                if (messageSelector != null && !messageSelector.isEmpty()) {
                    yield session.createConsumer(topic, messageSelector, noLocal);
                } else {
                    yield session.createConsumer(topic);
                }
            }
            case DURABLE -> {
                if (subscriberName == null || subscriberName.isEmpty()) {
                    throw new IllegalArgumentException("Subscriber name is required for DURABLE consumer type");
                }
                if (messageSelector != null && !messageSelector.isEmpty()) {
                    yield session.createDurableSubscriber(topic, subscriberName, messageSelector, noLocal);
                } else {
                    yield session.createDurableSubscriber(topic, subscriberName);
                }
            }
        };
    }

    /**
     * Converts an array of Objects to an array of Strings.
     *
     * @param objectArray array of Objects
     * @return array of Strings
     */
    public static String[] convertToStringArray(Object[] objectArray) {
        if (Objects.isNull(objectArray)) {
            return new String[]{};
        }
        return Arrays.stream(objectArray)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toArray(String[]::new);
    }

    /**
     * Maps protocol names from Ballerina constants to Solace JMS expected values.
     *
     * @param protocols array of protocol names
     * @return mapped array of protocol names
     */
    public static String[] mapProtocols(String[] protocols) {
        if (protocols == null || protocols.length == 0) {
            return new String[]{};
        }
        return Arrays.stream(protocols).map(protocol -> {
            return switch (protocol) {
                case "sslv3" -> "SSLv3";
                case "tlsv1" -> "TLSv1";
                case "tlsv11" -> "TLSv1.1";
                case "tlsv12" -> "TLSv1.2";
                default -> throw new IllegalArgumentException("Unsupported protocol: " + protocol);
            };
        }).toArray(String[]::new);
    }
}
