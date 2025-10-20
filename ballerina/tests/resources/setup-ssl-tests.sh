#!/bin/bash
# Complete automated setup for SSL/TLS testing
# This script generates certificates, configures the broker, and prepares for testing

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "${SCRIPT_DIR}"

echo "========================================"
echo "Solace SSL/TLS Test Setup"
echo "========================================"

# Step 1: Generate certificates
echo ""
echo "Step 1: Generating SSL/TLS certificates..."
if [ -d "certs" ]; then
    echo "Certificates already exist. Skipping generation."
    echo "To regenerate, run: rm -rf certs && ./setup-ssl-tests.sh"
else
    ./generate-certs.sh
fi

# Step 2: Start Docker container
echo ""
echo "Step 2: Starting Solace broker container..."
docker compose down 2>/dev/null || true
docker compose up -d

# Step 3: Wait for broker to be ready
echo ""
echo "Step 3: Waiting for broker to initialize..."
echo "This may take 30-60 seconds..."

# Wait for broker to be ready (check SEMP API)
for i in {1..30}; do
    if curl -s -u admin:admin http://localhost:8080/SEMP/v2/config/msgVpns/default >/dev/null 2>&1; then
        echo "Broker is ready!"
        break
    fi
    echo -n "."
    sleep 2
done
echo ""

# Step 4: Initialize queues
echo ""
echo "Step 4: Creating test queues..."
./init-solace.sh

# Step 5: Configure SSL via SEMP API
echo ""
echo "Step 5: Configuring SSL/TLS on broker..."

SEMP_URL="http://localhost:8080/SEMP/v2/config"
AUTH="admin:admin"

# Enable SSL on message VPN
echo "Enabling SSL on message VPN..."
curl -X PATCH "${SEMP_URL}/msgVpns/default" \
    -u "${AUTH}" \
    -H "Content-Type: application/json" \
    -d '{
        "serviceSmfPlainTextEnabled": true,
        "serviceSmfCompressionEnabled": true,
        "serviceSmfTlsEnabled": true,
        "authenticationClientCertificateAllowApiProvidedUsernameEnabled": true,
        "authenticationClientCertificateValidateDateEnabled": true
    }' \
    -s -o /dev/null -w "Response: %{http_code}\n"

# Note: For full SSL configuration with certificates, we need to use CLI
# which requires container restart with mounted config
echo ""
echo "⚠️  IMPORTANT: For full SSL/TLS support with certificates:"
echo "    The Solace PubSub+ Standard Edition requires additional configuration"
echo "    that cannot be done via SEMP API alone."
echo ""
echo "    For complete SSL testing, you have two options:"
echo ""
echo "    Option 1 - Use Solace PubSub+ CLI (via docker exec):"
echo "      docker exec -it solace-test-server cli"
echo "      Then follow the commands in SSL_TESTING_GUIDE.md"
echo ""
echo "    Option 2 - Use Solace PubSub+ Cloud or Enterprise Edition:"
echo "      These versions support full SEMP-based SSL configuration"
echo ""

# Step 6: Verify setup
echo ""
echo "Step 6: Verifying setup..."
echo ""
echo "Checking services..."
echo "  - SMF (plain):      smf://localhost:55554"
echo "  - SMF (compressed): smf://localhost:55003"
echo "  - SMFS (SSL/TLS):   smfs://localhost:55443 (requires manual SSL setup)"
echo "  - SEMP API:         http://localhost:8080"
echo ""

# Check if SSL port is listening
if nc -z localhost 55443 2>/dev/null; then
    echo "✓ SSL port 55443 is open"
else
    echo "⚠ SSL port 55443 is not responding yet (this is normal, SSL needs CLI config)"
fi

echo ""
echo "========================================"
echo "Setup Summary"
echo "========================================"
echo "✓ Certificates generated in: ${SCRIPT_DIR}/certs/"
echo "✓ Solace broker running"
echo "✓ Test queues created"
echo "✓ Basic configuration applied"
echo ""
echo "Next Steps:"
echo "1. Configure SSL certificates using Solace CLI (see SSL_TESTING_GUIDE.md)"
echo "2. Enable SSL tests in ssl_tests.bal (change enable: false to enable: true)"
echo "3. Run tests: cd ../../../.. && ./gradlew clean test -Pgroups=ssl"
echo ""
echo "To stop the broker: docker compose down"
echo "========================================"
