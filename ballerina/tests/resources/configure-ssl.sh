#!/bin/bash
# Script to configure SSL/TLS on Solace broker after container startup
# This runs inside the Solace container

set -e

echo "Waiting for Solace broker to be ready..."
sleep 20

# Check if Solace CLI is available
if ! command -v /usr/sw/loads/currentload/bin/cli &> /dev/null; then
    echo "Solace CLI not found, using SEMP API for configuration..."
    exit 0
fi

echo "Configuring SSL/TLS on Solace broker..."

# Execute the configuration commands
/usr/sw/loads/currentload/bin/cli -A -s solace-config.cli

echo "SSL/TLS configuration completed!"
