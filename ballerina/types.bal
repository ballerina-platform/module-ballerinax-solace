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
    # Specifies whether to enable Kerberos mutual authentication
    boolean mutualAuthentication = true;
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

# Trust store configuration for server certificate validation
public type TrustStore record {|
    # The URL or file path of the trust store
    string location;
    # The password for the trust store
    string password;
    # The format of the trust store file (JKS, PKCS12, etc.)
    string format = "JKS";
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
    string format = "JKS";
|};

# SSL/TLS configuration for secure connections
public type SecureSocket record {|
    # The trust store configuration for server certificate validation
    TrustStore trustStore?;
    # The key store configuration for client certificate authentication
    KeyStore keyStore?;
    # The list of trusted common names for certificate validation
    string[] trustedCommonNames?;
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
    # The broker host URL with format: [protocol:]host[:port]
    # Supports tcp:#, tcps:#, ws:#, wss:# protocols
    # Examples: "192.168.1.100", "tcps:#broker.example.com:55443"
    # Supports comma-separated host list for failover (max 4 hosts)
    string host;
    # The message VPN to connect to
    string vpnName = "default";
    # The authentication configuration (basic, Kerberos, or OAuth2)
    BasicAuthConfig|KerberosConfig|OAuth2Config auth?;
    # The SSL/TLS configuration for secure connections
    SecureSocket secureSocket?;
    # The client identifier (auto-generated if not specified)
    string clientId?;
    # A description for the application client
    string clientDescription = "Ballerina Solace SMF Connector";
    # The local interface IP address to bind for outbound connections
    string localAddress?;
    # The maximum time in seconds for a connection attempt
    decimal connectionTimeout = 30.0;
    # The maximum time in seconds for reading connection replies
    decimal readTimeout = 10.0;
    # ZLIB compression level (0 = disabled, 1-9 = compression)
    int compressionLevel = 0;
    # Enable transacted messaging
    boolean transacted = false;
    # Retry configuration for connection attempts
    RetryConfig retryConfig?;
|};

# Producer-specific configuration
public type ProducerConfiguration record {|
    *CommonConnectionConfiguration;
    # The destination (Topic or Queue) for message publishing
    Destination destination;
|};

# Message type for publishing
public type Message record {|
    # The message payload (text, binary, or structured data)
    string|byte[]|map<anydata> payload;
    # Optional message properties/headers
    map<anydata> properties?;
|};

# Delivery modes for message publishing
public enum DeliveryMode {
    DIRECT,
    PERSISTENT,
    NON_PERSISTENT
}