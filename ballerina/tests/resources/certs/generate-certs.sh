#!/bin/bash
# Generates a throwaway CA + Solace broker TLS cert + client truststore for local/CI test runs.
# Nothing produced here is checked into git - it is regenerated on every test run
# (see the "generated" entry in .gitignore).
set -euo pipefail

CERT_DIR="$(cd "$(dirname "$0")" && pwd)/generated"
rm -rf "$CERT_DIR"
mkdir -p "$CERT_DIR"
cd "$CERT_DIR"

# 1. Self-signed CA
openssl genrsa -out ca-key.pem 4096
openssl req -x509 -new -nodes -key ca-key.pem -sha256 -days 3650 \
    -subj "/CN=solace-test-ca" -out ca-cert.pem

# 2. Broker leaf key + CSR, with SANs for localhost (host-facing SMF-TLS connections)
cat > san.cnf <<'EOF'
[req]
distinguished_name = dn
req_extensions = v3_req
[dn]
[v3_req]
subjectAltName = DNS:localhost,IP:127.0.0.1
EOF
openssl genrsa -out broker-key.pem 2048
openssl req -new -key broker-key.pem -subj "/CN=localhost" \
    -config san.cnf -out broker.csr

# 3. Sign the broker cert with the CA, carrying the SAN extension through
openssl x509 -req -in broker.csr -CA ca-cert.pem -CAkey ca-key.pem \
    -CAcreateserial -days 825 -sha256 \
    -extfile san.cnf -extensions v3_req -out broker-cert.pem

# 4. The Solace docker image expects the server cert + key concatenated in one PEM
cat broker-cert.pem broker-key.pem ca-cert.pem > broker-combined.pem

# 5. Client-side truststore (PKCS12) containing only the CA cert - this connector's
#    TrustStore/KeyStore config only supports JKS/PKCS12, not raw PEM.
keytool -importcert -noprompt -alias solace-test-ca \
    -file ca-cert.pem -keystore client-truststore.p12 \
    -storetype PKCS12 -storepass changeit

chmod 644 broker-combined.pem client-truststore.p12
echo "Test certs generated in $CERT_DIR"
