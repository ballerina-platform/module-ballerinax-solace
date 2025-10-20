#!/bin/bash
# Script to generate self-signed certificates for SSL/TLS testing
# This creates a CA certificate, server certificate, and client certificate for mutual TLS testing

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CERTS_DIR="${SCRIPT_DIR}/certs"

# Clean up existing certificates
rm -rf "${CERTS_DIR}"
mkdir -p "${CERTS_DIR}"

cd "${CERTS_DIR}"

echo "================================================"
echo "Generating SSL/TLS certificates for testing..."
echo "================================================"

# Generate CA private key and certificate
echo ""
echo "1. Generating CA certificate..."
openssl genrsa -out ca-key.pem 4096

openssl req -new -x509 -days 3650 -key ca-key.pem -out ca-cert.pem \
    -subj "/C=US/ST=California/L=San Francisco/O=Solace Test CA/OU=Testing/CN=Solace Test CA"

# Generate server private key and CSR
echo ""
echo "2. Generating server certificate..."
openssl genrsa -out server-key.pem 4096

openssl req -new -key server-key.pem -out server.csr \
    -subj "/C=US/ST=California/L=San Francisco/O=Solace/OU=Broker/CN=localhost"

# Create server certificate extensions file
cat > server-ext.cnf << EOF
subjectAltName = DNS:localhost,DNS:solace-test-server,IP:127.0.0.1
EOF

# Sign server certificate with CA
openssl x509 -req -days 3650 -in server.csr \
    -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
    -out server-cert.pem -extfile server-ext.cnf

# Generate client private key and CSR
echo ""
echo "3. Generating client certificate..."
openssl genrsa -out client-key.pem 4096

openssl req -new -key client-key.pem -out client.csr \
    -subj "/C=US/ST=California/L=San Francisco/O=Solace Client/OU=Testing/CN=test-client"

# Sign client certificate with CA
openssl x509 -req -days 3650 -in client.csr \
    -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
    -out client-cert.pem

# Create PKCS12 keystore for client (used by Java)
echo ""
echo "4. Creating PKCS12 keystores..."
openssl pkcs12 -export -out client-keystore.p12 \
    -inkey client-key.pem -in client-cert.pem \
    -certfile ca-cert.pem \
    -password pass:changeit -name client

# Create server keystore (for Solace broker)
openssl pkcs12 -export -out server-keystore.p12 \
    -inkey server-key.pem -in server-cert.pem \
    -certfile ca-cert.pem \
    -password pass:changeit -name server

# Create truststore with CA certificate
echo ""
echo "5. Creating truststores..."
keytool -import -trustcacerts -noprompt \
    -alias ca-cert -file ca-cert.pem \
    -keystore truststore.p12 -storetype PKCS12 \
    -storepass changeit 2>/dev/null || true

# Create Java keystore format (JKS) for compatibility
keytool -importkeystore -noprompt \
    -srckeystore client-keystore.p12 -srcstoretype PKCS12 -srcstorepass changeit \
    -destkeystore client-keystore.jks -deststoretype JKS -deststorepass changeit 2>/dev/null || true

keytool -importkeystore -noprompt \
    -srckeystore truststore.p12 -srcstoretype PKCS12 -srcstorepass changeit \
    -destkeystore truststore.jks -deststoretype JKS -deststorepass changeit 2>/dev/null || true

# Clean up temporary files
rm -f *.csr *.srl server-ext.cnf

# Set appropriate permissions
chmod 644 *.pem *.p12 *.jks

echo ""
echo "================================================"
echo "Certificate generation completed!"
echo "================================================"
echo ""
echo "Generated files:"
echo "  - ca-cert.pem: CA certificate (for trust store)"
echo "  - ca-key.pem: CA private key"
echo "  - server-cert.pem: Server certificate"
echo "  - server-key.pem: Server private key"
echo "  - server-keystore.p12: Server keystore (PKCS12)"
echo "  - client-cert.pem: Client certificate"
echo "  - client-key.pem: Client private key"
echo "  - client-keystore.p12: Client keystore (PKCS12)"
echo "  - client-keystore.jks: Client keystore (JKS)"
echo "  - truststore.p12: CA trust store (PKCS12)"
echo "  - truststore.jks: CA trust store (JKS)"
echo ""
echo "Default password for all keystores: changeit"
echo ""
