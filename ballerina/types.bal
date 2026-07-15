// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org).
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

# The Solace service type attached to a `solace:Listener` for asynchronous (push-based) consumption.
#
# An attached service must declare a remote `onMessage` method and may optionally declare an
# `onError` method. The accepted signatures are:
# ```ballerina
# remote function onMessage(record {|*solace:Message; T payload;|} message) returns solace:Error?;
# remote function onMessage(record {|*solace:Message; T payload;|} message, solace:Caller caller) returns solace:Error?;
# remote function onMessage(solace:Message message) returns solace:Error?;
# remote function onError(solace:Error err) returns solace:Error?;
# ```
# Declaring a narrowed `payload` type (`T`) causes the message payload to be data-bound into that
# type; declaring the base `solace:Message` type yields the raw payload as `anydata`.
# The subscription (queue or topic) and flow options are supplied via the
# `@solace:ServiceConfig` annotation on the service.
public type Service distinct service object {
};

# Destination types - Topic and Queue
public type Topic record {|
    # The topic name
    string topicName;
|};

public type Queue record {|
    # The queue name
    string queueName;
|};

public type Destination Topic|Queue;

# Acknowledgement modes for message consumption
public enum AcknowledgementMode {
    AUTO_ACK,
    CLIENT_ACK
}

# Authentication configuration types
public type BasicAuthConfiguration record {|
    # The username for authentication
    string username;
    # The password for authentication (optional for some auth schemes)
    string password?;
|};

public type KerberosConfiguration record {|
    # The Kerberos service name used during authentication
    string serviceName = "solace";
    # The JAAS login context name to use for authentication
    string jaasLoginContext = "SolaceGSS";
    # Specifies whether to enable Kerberos mutual authentication
    boolean mutualAuthentication = false;
    # Specifies whether to enable automatic reload of the JAAS configuration file
    boolean jaasConfigFileReloadEnabled = false;
|};

# OAuth2 Access Token authentication configuration
public type OAuth2AccessTokenAuth record {|
    # Issuer identifier URI for token validation
    string issuer;
    # The OAuth 2.0 access token for authentication
    string accessToken;
|};

# OpenID Connect (OIDC) ID Token authentication configuration
public type OidcIdTokenAuth record {|
    # Issuer identifier URI for token validation
    string issuer;
    # The OpenID Connect (OIDC) ID token for authentication
    string oidcToken;
|};

# OAuth2 authentication configuration (mutually exclusive - use either access token or ID token)
# When using OAuth2 authentication scheme, exactly one of OAuth2AccessTokenAuth or OidcIdTokenAuth must be provided
public type OAuth2Configuration OAuth2AccessTokenAuth|OidcIdTokenAuth;

# Authentication configuration (basic, Kerberos, or OAuth2)
public type AuthConfiguration BasicAuthConfiguration|KerberosConfiguration|OAuth2Configuration;

# SSL/TLS certificate validation configuration
public type CertificateValidation record {|
    # Enable certificate validation
    boolean enabled = true;
    # Validate the certificate's expiration date
    boolean validateDate = true;
    # Validate that the certificate's common name matches the broker hostname/IP
    boolean validateHostname = true;
|};

# Java KeyStore format
public const JKS = "jks";
# PKCS12 format
public const PKCS12 = "pkcs12";

# Represents the supported SSL store formats.
public type SslStoreFormat JKS|PKCS12;

# Trust store configuration for server certificate validation
public type TrustStore record {|
    # The URL or file path of the trust store
    string location;
    # The password for the trust store
    string password;
    # The format of the trust store file (JKS, PKCS12, etc.)
    SslStoreFormat format = JKS;
|};

# Key store configuration for client certificate authentication
public type KeyStore record {|
    # The URL or file path of the key store
    string location;
    # The password for the key store
    string password;
    # The password for the private key (if different from key store password)
    string keyPassword?;
    # The alias of the private key to use from the key store
    string keyAlias?;
    # The format of the key store file (JKS, PKCS12, etc.)
    SslStoreFormat format = JKS;
|};

# Represents the supported SSL/TLS protocol versions.
public enum Protocol {
    TLSV1_1,
    TLSV1_2,
    TLSV1_3
}

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

# SSL/TLS configuration for secure connections
public type SecureSocket record {|
    # The trust store configuration for server certificate validation
    TrustStore trustStore?;
    # The key store configuration for client certificate authentication
    KeyStore keyStore?;
    # The list of trusted common names for certificate validation
    string[] trustedCommonNames?;
    # The SSL/TLS protocols NOT to use. None are excluded by default
    Protocol[] excludedProtocols = [];
    # The list of cipher suites to enable for the connection.
    # If not specified, the default cipher suites for the JVM are used
    SslCipherSuite[] cipherSuites?;
    # Certificate validation settings
    CertificateValidation validation = {};
|};

# Retry configuration for connection attempts
public type RetryConfiguration record {|
    # Number of times to retry connecting during initial connection (0 = no retries, -1 = infinite)
    int connectRetries = 0;
    # Number of connection retries per host when multiple hosts are specified
    int connectRetriesPerHost = 0;
    # Number of times to retry reconnecting after connection loss (-1 = infinite)
    int reconnectRetries = 3;
    # Time to wait between reconnection attempts in seconds
    decimal reconnectRetryWait = 3.0;
|};

# Common connection configuration shared between producer and consumer
type CommonConnectionConfiguration record {
    # The message VPN to connect to
    string messageVpn = "default";
    # The authentication configuration (basic, Kerberos, or OAuth2)
    AuthConfiguration auth?;
    # The SSL/TLS configuration for secure connections
    SecureSocket secureSocket?;
    # A unique client name to use to register to the appliance (auto-generated if not specified)
    string clientName?;
    # A description for the application client
    string clientDescription = "Ballerina Solace Connector";
    # Enable transacted messaging
    boolean transacted = false;
    # ZLIB compression level (0 = disabled, 1-9 = compression)
    int compressionLevel = 0;
    # The local interface IP address to bind for outbound connections
    string localhost?;
    # The maximum time in seconds for a connection attempt
    decimal connectTimeout = 30.0;
    # The maximum time in seconds for reading connection replies
    decimal readTimeout = 10.0;
    # Retry configuration for connection attempts
    RetryConfiguration retryConfig?;
};

# Producer-specific configuration
# Note: Destination is passed at send-time, not specified in configuration
public type ProducerConfiguration record {|
    *CommonConnectionConfiguration;
    # Whether to generate a send timestamp in outgoing messages
    # When enabled, outgoing messages will have senderTimestamp automatically set if not already provided
    boolean generateSendTimestamps = false;
    # Whether to generate a sequence number in outgoing messages
    # When enabled, outgoing messages will have sequenceNumber automatically generated if not already set
    boolean generateSequenceNumbers = false;
|};

# Common connection configuration for consumer and listener sessions, which additionally receive messages
type CommonConsumerConnectionConfiguration record {|
    *CommonConnectionConfiguration;
    # Whether to generate a receive timestamp on incoming messages
    # When enabled, incoming messages will have receiveTimestamp automatically set by the client on receipt
    boolean generateReceiveTimestamps = false;
    # Whether to calculate message expiration time on incoming messages
    # When enabled, incoming messages will have expiration populated from the timeToLive field on receipt
    boolean calculateMessageExpiration = false;
|};

# Listener configuration for asynchronous (push-based) message consumption
# Note: Subscription (queue or topic) is specified per-service via the ServiceConfig annotation
public type ListenerConfiguration record {|
    *CommonConsumerConnectionConfiguration;
|};

# Common consumer subscription fields
type CommonConsumerConfiguration record {|
    # JCSMP message acknowledgement mode
    AcknowledgementMode ackMode = AUTO_ACK;
    # Optional SQL-92 message selector for filtering messages on the broker before delivery
    # Applies to both queue consumers and durable topic endpoint subscriptions (flows only).
    # Not supported for direct topic subscriptions. Filters messages based on their properties and headers.
    # Example: "OrderType = 'URGENT' AND Priority > 5" - only messages matching this condition will be delivered.
    string messageSelector?;
    # JCSMP flow control transport window size (1-255)
    int transportWindowSize = 255;
    # Acknowledgement threshold as percentage of window size (1-75).
    int ackThreshold = 60;
    # Acknowledgement timer in seconds (0.02 - 1.5). Disabled by default
    decimal ackTimer?;
    # Number of reconnection attempts after flow goes down (-1 = infinite)
    int reconnectTries = -1;
    # Wait time between reconnection attempts in seconds (min 0.05 seconds)
    decimal reconnectRetryInterval = 3.0;
|};

# Queue consumer configuration for synchronous (pull-based) consumption
public type QueueConfiguration record {|
    *CommonConsumerConfiguration;
    # The queue name to consume messages from - REQUIRED unless durability is TEMPORARY (optional broker-generated
    # name hint when TEMPORARY)
    string queueName?;
    # Whether this is a durable (pre-provisioned, named) queue or a temporary (auto-deleted when session
    # disconnects) one. Temporary queues are useful for short-lived, session-specific messaging patterns like
    # request-reply.
    Durability durability = DURABLE;
|};

# Topic consumer configuration for synchronous (pull-based) consumption
public type TopicConfiguration record {|
    *CommonConsumerConfiguration;
    # The topic name to subscribe to
    string topicName;
    # Durability: TEMPORARY (ephemeral/direct) or DURABLE (persisted on broker)
    Durability durability = TEMPORARY;
    # Endpoint name - REQUIRED when durability is DURABLE (optional for TEMPORARY)
    # Used to identify the durable topic endpoint on the broker. Must be unique for durable endpoints.
    string endpointName?;
|};

# Consumer subscription configuration: QueueConfiguration or TopicConfiguration
public type SubscriptionConfiguration QueueConfiguration|TopicConfiguration;

# Consumer configuration for synchronous (pull-based) message consumption via MessageConsumer
public type ConsumerConfiguration record {|
    *CommonConsumerConnectionConfiguration;
    # The subscription configuration (queue or topic)
    SubscriptionConfiguration subscriptionConfig;
|};

# Delivery modes for messages
public enum DeliveryMode {
    # At-most-once delivery mode. Direct messages are not retained for disconnected clients and can be discarded
    # during congestion or failures. They can be reordered during network topology changes.
    # Most appropriate for high-rate, low-latency messaging applications.
    DIRECT,
    # Once-and-only-once delivery mode for Guaranteed Messaging. Persistent messages cannot be lost once acknowledged,
    # cannot be reordered during topology changes, and cannot be delivered more than once (unless redelivered flag is set).
    # Retained on durable endpoints for disconnected clients. Recommended for applications requiring persistent storage
    # and reliable message delivery.
    PERSISTENT
}

# Durability of a queue or topic subscription: DURABLE (persisted on broker) or TEMPORARY (ephemeral, auto-deleted)
public enum Durability {
    # Subscription is persisted on the broker and survives client disconnects.
    DURABLE,
    # Subscription is ephemeral and auto-deleted when the client disconnects.
    TEMPORARY
}

# Queue service configuration for asynchronous (push-based) consumption via Listener
public type QueueServiceConfiguration record {|
    *CommonConsumerConfiguration;
    # The queue name to consume messages from
    string queueName;
|};

# Topic service configuration for asynchronous (push-based) consumption via Listener
public type TopicServiceConfiguration record {|
    *CommonConsumerConfiguration;
    # The topic name to subscribe to
    string topicName;
    # Durability: TEMPORARY (ephemeral/direct) or DURABLE (persisted on broker)
    Durability durability = TEMPORARY;
    # Endpoint name - REQUIRED when durability is DURABLE (optional for TEMPORARY)
    # Used to identify the durable topic endpoint on the broker. Must be unique for durable endpoints.
    string endpointName?;
|};

# Service subscription configuration (sealed: QueueServiceConfiguration | TopicServiceConfiguration)
public type ServiceConfiguration QueueServiceConfiguration|TopicServiceConfiguration;

# Message type for publishing/consuming
public type Message record {|
    # The payload of the message. When consuming, declare a narrowed subtype
    # (e.g. `record {|*Message; string payload;|}`) to have the payload data-bound
    # into the declared type
    anydata payload;
    # Delivery mode for the message (DIRECT, PERSISTENT, or NON_PERSISTENT)
    DeliveryMode deliveryMode = DIRECT;
    # Message priority (0-9, where 9 is the highest)
    int priority?;
    # Time-to-live in seconds (0 = never expires, only for PERSISTENT/NON_PERSISTENT modes)
    decimal timeToLive?;
    # Application-defined message ID for correlation
    string messageId?;
    # Application-defined message type
    string messageType?;
    # Correlation ID for request-reply patterns
    string correlationId?;
    # Reply-to destination for request-reply patterns
    Destination replyTo?;
    # Sender ID (set by client or broker)
    string senderId?;
    # Sender timestamp in UTC milliseconds from epoch
    int senderTimestamp?;
    # Sequence number for message ordering (application-managed)
    # Set by the application for message ordering and duplicate detection.
    int sequenceNumber?;
    # Properties map for custom key-value pairs
    map<Property> properties?;
    # Application-specific user data attachment (max 36 bytes)
    byte[] userData?;
    # Receive timestamp in UTC milliseconds from epoch (set by broker)
    int receiveTimestamp?;
    # Whether message was previously delivered
    boolean redelivered?;
    # Destination this message was published to (Only set on receive)
    Destination destination?;
    # Number of times this message has been delivered (Only set on receive)
    int deliveryCount?;
    # Message expiration time in UTC milliseconds from epoch (populated by the client on receipt only when
    # `calculateMessageExpiration` is enabled on the consumer/listener configuration; `0`/unset otherwise)
    int expiration?;
|};

# Represents the allowed value types for a Solace message property.
public type Property boolean|int|byte|float|string|byte[]|map<Property>;

# Represents the allowed value types for entries in a Solace MapMessage payload.
public type Value boolean|int|byte|float|string|byte[]|map<Value>;

# A property key used internally to mark that a message's text payload is XML.
public const SOLACE_ISXML_PROP = "solace_isXML";

# Internal representation of a Solace message crossing into native code for `send`. The payload is
# narrowed to the concrete wire shapes native code understands; everything else mirrors `Message`.
type InternalMessage record {|
    # The payload encoded in one of the wire-level shapes expected by native code.
    string|map<Value>|byte[] payload;
    # Delivery mode for the message (DIRECT or PERSISTENT).
    DeliveryMode deliveryMode = DIRECT;
    # Message priority (0-9, where 9 is the highest).
    int priority?;
    # Time-to-live in seconds (0 = never expires).
    decimal timeToLive?;
    # Application-defined message ID for correlation.
    string messageId?;
    # Application-defined message type.
    string messageType?;
    # Correlation ID for request-reply patterns.
    string correlationId?;
    # Reply-to destination for request-reply patterns.
    Destination replyTo?;
    # Sender ID (set by client or broker).
    string senderId?;
    # Sender timestamp in UTC milliseconds from epoch.
    int senderTimestamp?;
    # Sequence number for message ordering.
    int sequenceNumber?;
    # Properties map for custom key-value pairs.
    map<Property> properties?;
    # Application-specific user data attachment.
    byte[] userData?;
    # Receive timestamp in UTC milliseconds from epoch (set by broker).
    int receiveTimestamp?;
    # Destination this message was published to (Only set on receive).
    Destination destination?;
    # Whether message was previously delivered.
    boolean redelivered?;
    # Number of times this message has been delivered (Only set on receive).
    int deliveryCount?;
    # Message expiration time in UTC milliseconds from epoch (populated by the client on receipt only when
    # `calculateMessageExpiration` is enabled on the consumer/listener configuration; `0`/unset otherwise).
    int expiration?;
|};
