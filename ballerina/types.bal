// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

# The Solace service type.
public type Service distinct service object {
    // remote function onMessage(solace:Message message, solace:Caller caller) returns error?;
};

# Defines the JMS session acknowledgement modes.
public enum AcknowledgementMode {
    # Indicates that the session will use a local transaction which may subsequently 
    # be committed or rolled back by calling the session's `commit` or `rollback` methods. 
    SESSION_TRANSACTED = "SESSION_TRANSACTED",
    # Indicates that the session automatically acknowledges a client's receipt of a message 
    # either when the session has successfully returned from a call to `receive` or when 
    # the message listener the session has called to process the message successfully returns.
    AUTO_ACKNOWLEDGE = "AUTO_ACKNOWLEDGE",
    # Indicates that the client acknowledges a consumed message by calling the 
    # MessageConsumer's or Caller's `acknowledge` method. Acknowledging a consumed message 
    # acknowledges all messages that the session has consumed.
    CLIENT_ACKNOWLEDGE = "CLIENT_ACKNOWLEDGE",
    # Indicates that the session to lazily acknowledge the delivery of messages. 
    # This is likely to result in the delivery of some duplicate messages if the JMS provider fails, 
    # so it should only be used by consumers that can tolerate duplicate messages. 
    # Use of this mode can reduce session overhead by minimizing the work the session does to prevent duplicates.
    DUPS_OK_ACKNOWLEDGE = "DUPS_OK_ACKNOWLEDGE"
}

# Defines the supported JMS message consumer types.
public enum ConsumerType {
    # Represents JMS durable subscriber
    DURABLE = "DURABLE",
    # Represents JMS shared consumer
    SHARED = "SHARED",
    # Represents JMS shared durable subscriber
    SHARED_DURABLE = "SHARED_DURABLE",
    # Represents JMS default consumer
    DEFAULT = "DEFAULT"
}

# Common configurations related to the Solace queue or topic subscription.
#
# + sessionAckMode - Configuration indicating how messages received by the session will be acknowledged
# + messageSelector - Only messages with properties matching the message selector expression are delivered. 
# If this value is not set that indicates that there is no message selector for the message consumer
# For example, to only receive messages with a property `priority` set to `'high'`, use:
# `"priority = 'high'"`. If this value is not set, all messages in the queue will be delivered.
type CommonSubscriptionConfig record {|
    AcknowledgementMode sessionAckMode = AUTO_ACKNOWLEDGE;
    string messageSelector?;
|};

# Represents configurations for a Solace queue subscription.
#
# + queueName - The name of the queue to consume messages from
public type QueueConfig record {|
    *CommonSubscriptionConfig;
    string queueName;
|};

# Represents configurations for Solace topic subscription.
#
# + topicName - The name of the topic to subscribe to
# + consumerType - The message consumer type
# + subscriberName - the name used to identify the subscription
# If this value is not set that indicates that there is no message selector for the message consumer
# For example, to only receive messages with a property `priority` set to `'high'`, use:
# `"priority = 'high'"`. If this value is not set, all messages in the queue will be delivered.
# + noLocal - If true then any messages published to the topic using this session's connection, or any other connection
# with the same client identifier, will not be added to the durable subscription.
public type TopicConfig record {|
    *CommonSubscriptionConfig;
    string topicName;
    ConsumerType consumerType = DEFAULT;
    string subscriberName?;
    boolean noLocal = false;
|};

# Common configurations related to the Solace service configuration related to queue or topic subscription.
# 
# + pollingInterval - The polling interval in seconds
# + receiveTimeout - The timeout to wait till a `receive` action finishes when there are no messages
type CommonServiceConfig record {|
    *CommonSubscriptionConfig;
    decimal pollingInterval = 10;
    decimal receiveTimeout = 10.0;
|};

# Represents configurations for a service configurations related to solace queue subscription.
#
# + queueName - The name of the queue to consume messages from
public type QueueServiceConfig record {|
    *CommonServiceConfig;
    string queueName;
|};

# Represents configurations for a service configurations related to solace topic subscription.
# 
# + topicName - The name of the topic to subscribe to
# + consumerType - The message consumer type
# + subscriberName - the name used to identify the subscription
# If this value is not set that indicates that there is no message selector for the message consumer
# For example, to only receive messages with a property `priority` set to `'high'`, use:
# `"priority = 'high'"`. If this value is not set, all messages in the queue will be delivered.
# + noLocal - If true then any messages published to the topic using this session's connection, or any other connection
# with the same client identifier, will not be added to the durable subscription.
public type TopicServiceConfig record {|
    *CommonServiceConfig;
    string topicName;
    ConsumerType consumerType = DEFAULT;
    string subscriberName?;
    boolean noLocal = false;
|};

# The service configuration type for the `solace:Service`.
public type ServiceConfiguration QueueServiceConfig|TopicServiceConfig;

# Annotation to configure the `solace:Service`.
public annotation ServiceConfiguration ServiceConfig on service;

# Represents a message destination in Solace.
# Can be either a Topic for publish/subscribe messaging or a Queue for point-to-point messaging.
public type Destination Topic|Queue;

# Represents a topic destination for publish/subscribe messaging.
public type Topic record {|
    # The name of the topic. Topics support wildcard subscriptions and multi-level hierarchies 
    # using '/' as a delimiter (e.g., "orders/retail/usa")
    string topicName;
|};

# Represents a queue destination for point-to-point messaging.
public type Queue record {|
    # The name of the queue
    string queueName;
|};

type CommonConnectionConfiguration record {
    # The name of the message VPN to connect to
    string messageVpn = "default";
    # The authentication configuration. Supports basic authentication, Kerberos, and OAuth2.
    # For client certificate authentication, configure the `secureSocket.keyStore` field
    BasicAuthConfig|KerberosConfig|OAuth2Config auth?;
    # The SSL/TLS configuration for secure connections
    SecureSocket secureSocket?;
    # Enables transacted messaging when set to `true`. In transacted mode, messages are sent and received
    # within a transaction context, requiring explicit commit or rollback
    boolean transacted = false;
    # The client identifier. If not specified, a unique client ID is auto-generated
    string clientId?;
    # A description for the application client
    string clientDescription = "JNDI";
    # Specifies whether to allow the same client ID to be used across multiple connections
    boolean allowDuplicateClientId = false;
    # Enables automatic creation of durable queues and topic endpoints on the broker
    boolean enableDynamicDurables = false;
    # Enables direct transport mode for message delivery. When `true`, uses direct (at-most-once) delivery.
    # When `false`, uses guaranteed (persistent) delivery mode. Direct transport must be disabled for
    # transacted sessions and XA transactions.
    boolean directTransport = true;
    # Enables direct message optimization. When `true`, optimizes message delivery in direct transport mode
    # by reducing protocol overhead. Only applicable when `directTransport` is `true`.
    boolean directOptimized = true;
    # The local interface IP address to bind for outbound connections
    string localhost?;
    # The the maximum amount of time (in seconds) permitted for a JNDI connection attempt.
    # A value of 0 means wait indefinitely
    decimal connectTimeout = 30.0;
    # the maximum amount of time (in seconds) permitted for reading a JNDI lookup reply from the host
    decimal readTimeout = 10.0;
    # The configuration to enable and specify the ZLIB compression level.
    # Valid range is 0-9, where 0 means no compression. Higher values provide better compression at the slower throughput
    int compressionLevel = 0;
    # The retry configuration for connection and reconnection attempts
    RetryConfig retryConfig?;
};

# Represents the configuration for a Solace message producer.
public type ProducerConfiguration record {|
    *CommonConnectionConfiguration;
    # The destination (Topic or Queue) where messages will be published
    Destination destination;
|};

# Represents the configuration for a Solace message consumer.
public type ConsumerConfiguration record {|
    *CommonConnectionConfiguration;
    # The subscription configuration specifying either a queue or topic to consume messages from
    QueueConfig|TopicConfig subscriptionConfig;
|};

# Represents the listener configuration for Ballerina Solace listener.
public type ListenerConfiguration record {|
    *CommonConnectionConfiguration;
|};

# Represents the basic authentication credentials for connecting to a Solace broker.
public type BasicAuthConfig record {|
    # The username for authentication
    string username;
    # The password for authentication
    string password?;
|};

# Represents the Kerberos (GSS-KRB) authentication configuration for connecting to a Solace broker
public type KerberosConfig record {|
    # The Kerberos service name used during authentication
    string serviceName = "solace";
    # The JAAS login context name to use for authentication
    string jaasLoginContext = "SolaceGSS";
    # Specifies whether to enable Kerberos mutual authentication
    boolean mutualAuthentication = true;
    # Specifies whether to enable automatic reload of the JAAS configuration file
    boolean jaasConfigReloadEnabled = false;
|};

# Represents the OAuth 2.0 authentication configuration for connecting to a Solace broker
public type OAuth2Config record {|
    # The OAuth 2.0 issuer identifier URI
    string issuer;
    # The OAuth 2.0 access token for authentication
    string accessToken?;
    # The OpenID Connect (OIDC) ID token for authentication
    string oidcToken?;
|};

# Represents the retry configuration for connection and reconnection attempts to a Solace broker
public type RetryConfig record {|
    # The number of times to retry connecting to the broker during initial connection.
    # A value of -1 means retry forever, 0 means no retries (fail immediately on first failure)
    int connectRetries = 0;
    # The number of connection retries per host when multiple hosts are specified in the URL.
    # This applies to each host in a comma-separated host list
    int connectRetriesPerHost = 0;
    # The number of times to retry reconnecting after an established connection is lost.
    # A value of -1 means retry forever
    int reconnectRetries = 20;
    # The time to wait between reconnection attempts, in seconds
    decimal reconnectRetryWait = 3.0;
|};

# SSL protocol version 3.0
public const SSLv30 = "sslv3";
# TLS protocol version 1.0
public const TLSv10 = "tlsv1";
# TLS protocol version 1.1
public const TLSv11 = "tlsv11";
# TLS protocol version 1.2
public const TLSv12 = "tlsv12";

# Represents the supported SSL/TLS protocol versions.
public type Protocol SSLv30|TLSv10|TLSv11|TLSv12;

# Cipher suite: TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384
public const ECDHE_RSA_AES256_CBC_SHA384 = "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384";
# Cipher suite: TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA
public const ECDHE_RSA_AES256_CBC_SHA = "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA";
# Cipher suite: TLS_RSA_WITH_AES_256_CBC_SHA256
public const RSA_AES256_CBC_SHA256 = "TLS_RSA_WITH_AES_256_CBC_SHA256";
# Cipher suite: TLS_RSA_WITH_AES_256_CBC_SHA
public const RSA_AES256_CBC_SHA = "TLS_RSA_WITH_AES_256_CBC_SHA";
# Cipher suite: TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA
public const ECDHE_RSA_3DES_EDE_CBC_SHA = "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA";
# Cipher suite: SSL_RSA_WITH_3DES_EDE_CBC_SHA
public const RSA_3DES_EDE_CBC_SHA = "SSL_RSA_WITH_3DES_EDE_CBC_SHA";
# Cipher suite: TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA
public const ECDHE_RSA_AES128_CBC_SHA = "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA";
# Cipher suite: TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256
public const ECDHE_RSA_AES128_CBC_SHA256 = "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256";
# Cipher suite: TLS_RSA_WITH_AES_128_CBC_SHA256
public const RSA_AES128_CBC_SHA256 = "TLS_RSA_WITH_AES_128_CBC_SHA256";
# Cipher suite: TLS_RSA_WITH_AES_128_CBC_SHA
public const RSA_AES128_CBC_SHA = "TLS_RSA_WITH_AES_128_CBC_SHA";

# The SSL Cipher Suite to be used for secure communication with the Solace broker.
public type SslCipherSuite ECDHE_RSA_AES256_CBC_SHA384|ECDHE_RSA_AES256_CBC_SHA|RSA_AES256_CBC_SHA256|RSA_AES256_CBC_SHA|
    ECDHE_RSA_3DES_EDE_CBC_SHA|RSA_3DES_EDE_CBC_SHA|ECDHE_RSA_AES128_CBC_SHA|ECDHE_RSA_AES128_CBC_SHA256|RSA_AES128_CBC_SHA256|
    RSA_AES128_CBC_SHA;

# Represents the SSL/TLS configuration for secure connections to a Solace broker
public type SecureSocket record {|
    # The trust store configuration containing trusted CA certificates
    TrustStore trustStore?;
    # The key store configuration containing the client's private key and certificate.
    # When configured, enables client certificate authentication
    KeyStore keyStore?;
    # The list of SSL/TLS protocol versions to enable for the connection.
    # It is recommended to use only TLSv12 or higher for security
    Protocol[] protocols = [SSLv30, TLSv10, TLSv11, TLSv12];
    # The list of cipher suites to enable for the connection.
    # If not specified, the default cipher suites for the JVM are used
    SslCipherSuite[] cipherSuites?;
    # The list of acceptable common names for broker certificate validation.
    # If specified, the broker certificate's common name must match one of these values
    string[] trustedCommonNames?;
    # The certificate validation settings
    record {|
        # Enable certificate validation
        boolean enabled = true;
        # Specifies whether to validate the certificate's expiration date
        boolean validateDate = true;
        # Specifies whether to validate that the certificate's common name matches the broker hostname
        boolean validateHost = true;
    |} validation = {};
|};

# Java KeyStore format
public const JKS = "jks";
# PKCS12 format
public const PKCS12 = "pkcs12";

# Represents the supported SSL store formats.
public type SslStoreFormat JKS|PKCS12;

# Represents a trust store containing trusted CA certificates.
public type TrustStore record {|
    # The URL or path of the truststore file
    string location;
    # The password for the trust store
    string password;
    # The format of the trust store file
    SslStoreFormat format = JKS;
|};

# Represents a key store containing the client's private key and certificate.
public type KeyStore record {|
    # The URL or path of the keystore file
    string location;
    # The password for the key store
    string password;
    # The password for the private key within the key store.
    # If not specified, the key store password is used
    string keyPassword?;
    # The alias of the private key to use from the key store.
    # If not specified, the first private key found is used
    string keyAlias?;
    # The format of the key store file
    SslStoreFormat format = JKS;
|};

# Represent the valid value types allowed in JMS message properties.
public type Property boolean|int|byte|float|string;

# Represents the allowed value types for entries in the map content of a JMS MapMessage.
public type Value Property|byte[];

# Represent the Message used to send and receive content from the Solace broker.
public type Message record {|
    # Message payload (can be text, binary, or structured map data)
    string|map<Value>|byte[] payload;
    # Id which can be used to correlate multiple messages
    string correlationId?;
    # JMS destination to which a reply to this message should be sent
    Destination replyTo?;
    # Additional message properties
    map<Property> properties?;
    # Unique identifier for a JMS message (Only set by the JMS provider)
    string messageId?;
    # Time a message was handed off to a provider to be sent (Only set by the JMS provider)
    int timestamp?;
    # JMS destination of this message (Only set by the JMS provider)
    Destination destination?;
    # Delivery mode of this message (Only set by the JMS provider)
    int deliveryMode?;
    # Indication of whether this message is being redelivered (Only set by the JMS provider)
    boolean redelivered?;
    # Message type identifier supplied by the client when the message was sent
    string jmsType?;
    # Message expiration time (Only set by the JMS provider)
    int expiration?;
    # Message priority level (Only set by the JMS provider)
    int priority?;
|};

