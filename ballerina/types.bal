# The Solace service type.
public type Service distinct service object {
    // remote function onMessage(solace:Message message, solace:Caller caller) returns error?;
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
    AUTO_ACK = "SUPPORTED_MESSAGE_ACK_AUTO",
    CLIENT_ACK = "SUPPORTED_MESSAGE_ACK_CLIENT"
}

# Message settlement outcomes for explicit acknowledgement control
public enum SettlementOutcome {
    # Acknowledge the message - positive ACK indicating successful processing
    ACCEPTED = "ACCEPTED",
    # Negative ACK with redelivery - message will be redelivered by broker, delivery count incremented
    FAILED = "FAILED",
    # Negative ACK without redelivery - message moves to DMQ immediately (or deleted if no DMQ)
    REJECTED = "REJECTED"
}

# Authentication configuration types
public type BasicAuthConfig record {|
    # The username for authentication
    string username;
    # The password for authentication (optional for some auth schemes)
    string password?;
|};

public type KerberosConfig record {|
    # The Kerberos service name used during authentication
    string serviceName = "solace";
    # The JAAS login context name to use for authentication
    string jaasLoginContext = "SolaceGSS";
    # Specifies whether to enable automatic reload of the JAAS configuration file
    boolean jaasConfigFileReloadEnabled = false;
|};

public type OAuth2Config record {|
    # The OAuth 2.0 issuer identifier URI
    string issuer;
    # The OAuth 2.0 access token for authentication
    string accessToken?;
    # The OpenID Connect (OIDC) ID token for authentication
    string oidcToken?;
|};

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

# SSL protocol version 3.0
public const SSLv30 = "sslv3";
# TLS protocol version 1.0
public const TLSv10 = "tlsv1";
# TLS protocol version 1.1
public const TLSv11 = "tlsv11";
# TLS protocol version 1.2
public const TLSv12 = "tlsv12";
# SSL protocol version SSLv2Hello
public const SSLv2Hello = "SSLv2Hello";

# Represents the supported SSL/TLS protocol versions.
public type Protocol SSLv30|TLSv10|TLSv11|TLSv12|SSLv2Hello;

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
    # The SSL protocols NOT to use
    Protocol[] excludedProtocols = [SSLv2Hello];
    # The SSL cipher suites to use
    SslCipherSuite[] cipherSuites?;
    # Certificate validation settings
    CertificateValidation validation = {};
|};

# Retry configuration for connection attempts
public type RetryConfig record {|
    # Number of times to retry connecting during initial connection (0 = no retries, -1 = infinite)
    int connectRetries = 0;
    # Number of connection retries per host when multiple hosts are specified
    int connectRetriesPerHost = 0;
    # Number of times to retry reconnecting after connection loss (-1 = infinite)
    int reconnectRetries = 20;
    # Time to wait between reconnection attempts in seconds
    decimal reconnectRetryWait = 3.0;
|};

# Common connection configuration shared between producer and consumer
public type CommonConnectionConfiguration record {|
    # The message VPN to connect to
    string vpnName = "default";
    # The authentication configuration (basic, Kerberos, or OAuth2)
    BasicAuthConfig|KerberosConfig|OAuth2Config auth?;
    # The SSL/TLS configuration for secure connections
    SecureSocket secureSocket?;
    # A unique client name to use to register to the appliance (auto-generated if not specified)
    string clientName?;
    # A description for the application client
    string clientDescription = "Ballerina Solace Connector";
    # The local interface IP address to bind for outbound connections
    string localAddress?;
    # The maximum time in seconds for a connection attempt
    decimal connectionTimeout = 30.0;
    # The maximum time in seconds for reading connection replies
    decimal readTimeout = 10.0;
    # JCSMP message acknowledgement mode
    AcknowledgementMode ackMode = SUPPORTED_MESSAGE_ACK_AUTO;
    # ZLIB compression level (0 = disabled, 1-9 = compression)
    int compressionLevel = 0;
    # Enable transacted messaging
    boolean transacted = false;
    # Retry configuration for connection attempts
    RetryConfig retryConfig?;
|};

# Producer-specific configuration
# Note: Destination is passed at send-time, not specified in configuration
public type ProducerConfiguration record {|
    *CommonConnectionConfiguration;
|};

# Common consumer subscription fields
# Note: Flow control properties below only apply to FlowReceiver usage (queues and durable topic endpoints)
# They are ignored for direct topic subscriptions which use XMLMessageConsumer
public type CommonConsumerConfig record {|
    # Optional SQL-92 message selector for filtering messages (only for queue consumers)
    string selector?;
    # JCSMP flow control transport window size (1-255, default 255) - FlowReceiver only
    int transportWindowSize?;
    # Acknowledgement threshold as percentage of window size (1-75, default 0) - FlowReceiver only
    int ackThreshold?;
    # Acknowledgement timer in milliseconds (20-1500, default 0) - FlowReceiver only
    int ackTimerInMsecs?;
    # Auto-start the flow upon creation (default false) - FlowReceiver only
    boolean startState?;
    # Prevent receiving messages published on same session (default false) - FlowReceiver only
    boolean noLocal?;
    # Enable active/inactive flow indication (default false) - FlowReceiver only
    boolean activeFlowIndication?;
    # Number of reconnection attempts after flow goes down (-1 = infinite, default -1) - FlowReceiver only
    int reconnectTries?;
    # Wait time between reconnection attempts in milliseconds (min 50, default 3000) - FlowReceiver only
    int reconnectRetryIntervalInMsecs?;
|};

# Queue consumer configuration for synchronous (pull-based) consumption
public type QueueConsumerConfig record {|
    *CommonConsumerConfig;
    # The queue name to consume messages from
    string queueName;
|};

# Topic consumer configuration for synchronous (pull-based) consumption
public type TopicConsumerConfig record {|
    *CommonConsumerConfig;
    # The topic name to subscribe to
    string topicName;
    # Endpoint type: DEFAULT (ephemeral/direct) or DURABLE (persisted on broker)
    EndpointType endpointType = DEFAULT;
    # Endpoint name (required if endpointType is DURABLE)
    string endpointName?;
|};

# Consumer subscription configuration (sealed: QueueConsumerConfig | TopicConsumerConfig)
public type ConsumerSubscription QueueConsumerConfig|TopicConsumerConfig;

# Consumer configuration for synchronous (pull-based) message consumption via MessageConsumer
public type ConsumerConfiguration record {|
    *CommonConnectionConfiguration;
    # The subscription configuration (queue or topic)
    ConsumerSubscription subscriptionConfig;
|};

# Delivery modes for messages
public enum DeliveryMode {
    # This mode provides at-most-once message delivery.
    DIRECT,
    # This mode provides once-and-only-once message delivery.
    PERSISTENT,
    # This mode is functionally the same as Persistent.
    NON_PERSISTENT
}

# Endpoint types for producer destinations
public enum EndpointType {
    DEFAULT,
    DURABLE
}

# Common service subscription fields (listener polling configuration)
# Note: Flow control properties below only apply to FlowReceiver usage (queues and durable topic endpoints)
# They are ignored for direct topic subscriptions which use XMLMessageConsumer
public type CommonServiceConfig record {|
    # JCSMP acknowledgement mode
    AcknowledgementMode ackMode = AUTO_ACK;
    # Optional SQL-92 message selector for filtering messages (only for queue consumers)
    string selector?;
    # Polling interval in seconds (how often to poll for messages)
    decimal pollingInterval = 10;
    # Receive timeout in seconds (how long to wait per polling cycle)
    decimal receiveTimeout = 10.0;
    # Whether to auto-start polling when listener starts
    boolean autoStart = true;
    # JCSMP flow control transport window size (1-255, default 255) - FlowReceiver only
    int transportWindowSize?;
    # Acknowledgement threshold as percentage of window size (1-75, default 0) - FlowReceiver only
    int ackThreshold?;
    # Acknowledgement timer in milliseconds (20-1500, default 0) - FlowReceiver only
    int ackTimerInMsecs?;
    # Prevent receiving messages published on same session (default false) - FlowReceiver only
    boolean noLocal?;
    # Enable active/inactive flow indication (default false) - FlowReceiver only
    boolean activeFlowIndication?;
    # Number of reconnection attempts after flow goes down (-1 = infinite, default -1) - FlowReceiver only
    int reconnectTries?;
    # Wait time between reconnection attempts in milliseconds (min 50, default 3000) - FlowReceiver only
    int reconnectRetryIntervalInMsecs?;
|};

# Queue service configuration for asynchronous (push-based) consumption via Listener
public type QueueServiceConfig record {|
    *CommonServiceConfig;
    # The queue name to consume messages from
    string queueName;
|};

# Topic service configuration for asynchronous (push-based) consumption via Listener
public type TopicServiceConfig record {|
    *CommonServiceConfig;
    # The topic name to subscribe to
    string topicName;
    # Endpoint type: DEFAULT (ephemeral/direct) or DURABLE (persisted on broker)
    EndpointType endpointType = DEFAULT;
    # Endpoint name (required if endpointType is DURABLE)
    string endpointName?;
|};

# Service subscription configuration (sealed: QueueServiceConfig | TopicServiceConfig)
public type ServiceConfiguration QueueServiceConfig|TopicServiceConfig;

# Message type for publishing/consuming
public type Message record {|
    # The binary payload of the message
    byte[] payload;
    # Delivery mode for the message (DIRECT, PERSISTENT, or NON_PERSISTENT)
    DeliveryMode deliveryMode = DIRECT;
    # Message priority (0-255, where 0 is lowest and 255 is highest)
    int priority?;
    # Time-to-live in milliseconds (0 = never expires, only for PERSISTENT/NON_PERSISTENT modes)
    int timeToLive?;
    # Application-defined message ID for correlation
    string applicationMessageId?;
    # Application-defined message type
    string applicationMessageType?;
    # Correlation ID for request-reply patterns
    string correlationId?;
    # Reply-to destination for request-reply patterns
    Destination replyTo?;
    # Sender ID (set by client or broker)
    string senderId?;
    # Sender timestamp in UTC milliseconds from epoch
    int senderTimestamp?;
    # Receive timestamp in UTC milliseconds from epoch (set by broker)
    int receiveTimestamp?;
    # Sequence number for message ordering
    int sequenceNumber?;
    # Whether message was previously delivered
    boolean redelivered?;
    # Number of times this message has been delivered
    int deliveryCount?;
    # User-defined properties map for custom key-value pairs
    record {|
        string...;
    |} properties?;
    # Application-specific user data attachment (max 36 bytes)
    byte[] userData?;
|};
