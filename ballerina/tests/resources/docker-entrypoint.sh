#!/bin/bash
# Custom entrypoint script for Solace container to configure SSL automatically
# This script runs when the container starts

set -e

echo "Starting Solace PubSub+ with SSL/TLS configuration..."

# Start Solace in background
/usr/sbin/boot.sh &
SOLACE_PID=$!

# Function to wait for Solace to be ready
wait_for_solace() {
    echo "Waiting for Solace to be ready..."
    local max_attempts=60
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if /usr/sw/loads/currentload/bin/cli -A <<EOF 2>/dev/null
show version
exit
EOF
        then
            echo "Solace is ready!"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
        echo -n "."
    done

    echo "Timeout waiting for Solace"
    return 1
}

# Wait for Solace to start
wait_for_solace

# Apply SSL configuration if certificates exist
if [ -f "/etc/solace/certs/server-cert.pem" ] && [ -f "/etc/solace/certs/server-key.pem" ]; then
    echo "Configuring SSL/TLS..."

    /usr/sw/loads/currentload/bin/cli -A <<'EOF'
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
server-certificate-name server
no shutdown
exit

# Enable client certificate authentication
authentication
client-certificate
shutdown
allow-api-provided-username
validate-certificate-date
no shutdown
exit
exit

# Configure SMF service with SSL
service smf
shutdown
ssl
port 55443
server-certificate-name server
shutdown
no shutdown
exit
no shutdown
exit

end
exit
EOF

    echo "SSL/TLS configuration completed!"
else
    echo "SSL certificates not found, skipping SSL configuration"
fi

# Keep container running
wait $SOLACE_PID
