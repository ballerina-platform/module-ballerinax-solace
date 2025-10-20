# SSL/TLS Testing Guide

This guide provides instructions for setting up and running SSL/TLS tests for the Ballerina Solace connector.

## Overview

The SSL/TLS test suite covers two key scenarios:
1. **Basic SSL/TLS**: Producer and consumer using SSL/TLS with username/password authentication
2. **Client Certificate Authentication**: Producer and consumer using SSL/TLS with mutual TLS (client certificates)

## Prerequisites

- Docker and Docker Compose
- OpenSSL (for certificate generation)
- Java keytool (usually comes with JDK)
- Solace PubSub+ broker with SSL/TLS support

## Setup Instructions

### Automated Setup (Recommended)

Run the automated setup script that handles everything:

```bash
cd ballerina/tests/resources
./setup-ssl-tests.sh
```

This script will:
1. Generate all SSL/TLS certificates
2. Start the Solace broker container
3. Create test queues
4. Apply basic SSL configuration

After running this script, you'll need to complete the SSL certificate configuration using the Solace CLI (see Step 2 below).

### Manual Setup

If you prefer manual setup, follow these steps:

#### Step 1: Generate Test Certificates

Run the certificate generation script to create all necessary certificates and keystores:

```bash
cd ballerina/tests/resources
./generate-certs.sh
```

This script creates:
- **CA certificate and key**: Self-signed Certificate Authority
- **Server certificate and key**: For the Solace broker
- **Client certificate and key**: For client authentication
- **Keystores and truststores**: In both JKS and PKCS12 formats

All certificates are valid for 10 years and use a default password of `changeit`.

**Generated files (in `certs/` directory):**
```
certs/
├── ca-cert.pem              # CA certificate
├── ca-key.pem               # CA private key
├── server-cert.pem          # Solace broker certificate
├── server-key.pem           # Solace broker private key
├── server-keystore.p12      # Server keystore (PKCS12)
├── client-cert.pem          # Client certificate
├── client-key.pem           # Client private key
├── client-keystore.p12      # Client keystore (PKCS12)
├── client-keystore.jks      # Client keystore (JKS)
├── truststore.p12           # CA trust store (PKCS12)
└── truststore.jks           # CA trust store (JKS)
```

#### Step 2: Start Solace Broker

```bash
cd ballerina/tests/resources
docker compose up -d
```

Wait for the broker to be healthy:
```bash
docker compose ps
# Wait until health status shows "healthy"
```

### Step 2: Configure Solace Broker for SSL/TLS

The Solace broker needs to be configured with SSL/TLS certificates. Due to limitations in the Standard Edition, this requires CLI configuration.

#### Option A: Using Solace CLI (Required for Standard Edition)

1. Access the Solace CLI:
```bash
docker exec -it solace-test-server cli
```

2. Configure SSL/TLS in the CLI:
```
enable
configure

# Configure server certificate
ssl server-certificate server
shutdown
certificate-file /etc/solace/certs/server-cert.pem
private-key-file /etc/solace/certs/server-key.pem
no shutdown
exit

# Enable SSL on the message VPN
message-vpn default
ssl
shutdown
no shutdown
exit
exit

# Configure secure SMF service
service smf
shutdown
ssl
port 55443
shutdown
no shutdown
exit
no shutdown
exit

end
exit
```

#### Option B: Using Docker Entrypoint (Experimental)

The repository includes a custom Docker entrypoint script that can automate SSL configuration. To use it:

1. Uncomment the entrypoint line in `docker-compose.yaml`:
```yaml
# Change this line:
# entrypoint: ["/docker-entrypoint.sh"]
# To:
entrypoint: ["/docker-entrypoint.sh"]
```

2. Restart the container:
```bash
docker compose down
docker compose up -d
```

**Note**: This approach may not work reliably with all versions of the Solace container.

#### Option C: Using Solace PubSub+ Manager GUI

1. Access PubSub+ Manager:
   - URL: http://localhost:8080
   - Username: admin
   - Password: admin

3. Configure SSL:
   - Navigate to: Message VPN > default > Services
   - Enable SMF over SSL/TLS on port 55443
   - Upload server certificate and private key
   - Apply changes and restart services

### Step 3: Configure Client Certificate Authentication (Optional)

For mutual TLS (client certificate authentication), additional broker configuration is required:

```
enable
configure

# Configure client certificate verification
authentication
client-certificate
shutdown
validate-certificate-date
no shutdown
exit
exit

# Configure message VPN to require client certificates
message-vpn default
authentication
client-certificate
shutdown
allow-api-provided-username
validate-certificate-date
no shutdown
exit
exit

end
exit
```

### Step 4: Run SSL/TLS Tests

Once the broker is configured with SSL/TLS:

1. Enable the SSL tests by editing `ballerina/tests/ssl_tests.bal` and changing:
```ballerina
enable: false  // Change to true
```

2. Run the SSL test group:
```bash
./gradlew clean test -Pgroups=ssl
```

Or run specific SSL tests:
```bash
# Basic SSL/TLS tests
./gradlew clean test -Pgroups=ssl

# Client certificate tests only
./gradlew clean test -Pgroups=ssl-client-cert
```

## Test Scenarios

### 1. SSL/TLS with Basic Authentication

**Test**: `testSSLProducerWithBasicAuth` and `testSSLConsumerWithBasicAuth`

**Description**: Tests secure communication over SSL/TLS using username/password authentication.

**Configuration**:
- Protocol: `smfs://` (secure SMF)
- Port: 55443
- Authentication: Username/password
- TLS: Server certificate verification via truststore

**Expected behavior**:
- Producer connects securely and sends message
- Consumer connects securely and receives message
- All communication is encrypted

### 2. SSL/TLS with Client Certificate Authentication

**Test**: `testSSLProducerWithClientCertificate` and `testSSLConsumerWithClientCertificate`

**Description**: Tests mutual TLS authentication where both server and client present certificates.

**Configuration**:
- Protocol: `smfs://` (secure SMF)
- Port: 55443
- Authentication: Username/password + client certificate
- TLS: Mutual authentication (client and server certificates)

**Expected behavior**:
- Producer presents client certificate during TLS handshake
- Consumer presents client certificate during TLS handshake
- Server verifies client certificates
- All communication is encrypted

## Troubleshooting

### Certificate Issues

**Problem**: "Unable to find valid certification path"
**Solution**: Ensure the truststore contains the CA certificate that signed the server certificate.

**Problem**: "Invalid keystore format"
**Solution**: Verify you're using the correct keystore format (JKS or PKCS12) and password.

### Connection Issues

**Problem**: "Connection refused on port 55443"
**Solution**:
- Verify SSL is enabled on the broker
- Check that port 55443 is properly mapped in docker-compose.yaml
- Ensure the broker service has restarted after SSL configuration

**Problem**: "Hostname verification failed"
**Solution**:
- The server certificate CN must match the hostname (localhost)
- Disable hostname verification for testing (not recommended for production):
```ballerina
secureSocket: {
    validation: {
        validateHost: false
    },
    trustStore: { ... }
}
```

### Broker Configuration Issues

**Problem**: "SSL service not available"
**Solution**:
- Verify SSL is enabled in the message VPN
- Check that the server certificate is properly configured
- Restart the SSL service on the broker

**Problem**: "Client certificate validation failed"
**Solution**:
- Ensure client certificate is signed by the same CA as configured on broker
- Verify client certificate has not expired
- Check that client certificate validation is properly configured on broker

## Security Notes

⚠️ **Important**: These certificates are for TESTING ONLY!

- Do not use these certificates in production
- The default password `changeit` should be changed for any real deployment
- Certificates are self-signed and not trusted by default certificate authorities
- For production, use certificates from a trusted CA

## Certificate Validity

All generated certificates are valid for 10 years from the date of generation. To regenerate certificates:

```bash
cd ballerina/tests/resources
rm -rf certs
./generate-certs.sh
```

## Additional Resources

- [Solace SSL/TLS Configuration Guide](https://docs.solace.com/Security/Encrypting-Msgs/Configuring-TLS-SSL.htm)
- [Solace Client Certificate Authentication](https://docs.solace.com/Security/Client-Authentication/Client-Certificate-Authentication.htm)
- [Ballerina Crypto Module Documentation](https://lib.ballerina.io/ballerina/crypto/latest)
