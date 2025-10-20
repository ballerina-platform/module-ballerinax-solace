# Solace Connector Test Resources

This directory contains resources required for testing the Ballerina Solace connector.

## Files

- **docker-compose.yaml**: Docker Compose configuration for Solace PubSub+ broker
- **init-solace.sh**: Script to initialize queues and SSL configuration via SEMP API
- **generate-certs.sh**: Script to generate SSL/TLS certificates for testing
- **SSL_TESTING_GUIDE.md**: Comprehensive guide for SSL/TLS testing setup

## Quick Start

### Basic Testing (No SSL)

1. Start Solace broker:
```bash
docker-compose up -d
```

2. Wait for broker to initialize and run the init script (automatically done by Gradle build)

3. Run tests:
```bash
cd ../../..  # Navigate to project root
./gradlew clean test
```

### SSL/TLS Testing

#### Quick Start (Automated)

Run the automated setup script:
```bash
./setup-ssl-tests.sh
```

This will generate certificates, start the broker, and create queues. Then:

1. Configure SSL certificates via Solace CLI:
```bash
docker exec -it solace-test-server cli
# Follow the commands in SSL_TESTING_GUIDE.md (Section: Option A)
```

2. Enable SSL tests in `ssl_tests.bal` (change `enable: false` to `enable: true`)

3. Run SSL tests:
```bash
cd ../../..  # Navigate to project root
./gradlew clean test -Pgroups=ssl
```

#### Manual Setup

1. Generate certificates:
```bash
./generate-certs.sh
```

2. Start Solace broker:
```bash
docker-compose up -d
```

3. Configure SSL on the broker (see SSL_TESTING_GUIDE.md for detailed instructions)

4. Enable SSL tests in `ssl_tests.bal` (change `enable: false` to `enable: true`)

5. Run SSL tests:
```bash
cd ../../..  # Navigate to project root
./gradlew clean test -Pgroups=ssl
```

## Ports

| Port | Service | Description |
|------|---------|-------------|
| 55554 | SMF | Plain text messaging (mapped from internal 55555) |
| 55003 | SMF Compressed | Compressed messaging |
| 55443 | SMFS | SSL/TLS secured messaging |
| 8080 | Web/SEMP | Management interface and SEMP API |
| 943 | SEMP over TLS | Secure management interface |

## Credentials

- **Username**: admin
- **Password**: admin
- **Message VPN**: default

## Certificate Passwords

All keystores and truststores use the password: `changeit`

## Cleaning Up

Stop and remove the Solace broker container:
```bash
docker-compose down
```

Remove generated certificates:
```bash
rm -rf certs/
```

## Additional Information

- For detailed SSL/TLS setup instructions, see [SSL_TESTING_GUIDE.md](SSL_TESTING_GUIDE.md)
- For Solace PubSub+ documentation, visit: https://docs.solace.com/
