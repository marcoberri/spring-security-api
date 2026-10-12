#!/usr/bin/env bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# Root CA
openssl req -x509 -newkey rsa:4096 -days 365 -nodes \
  -keyout ca-key.pem -out ca-cert.pem \
  -subj "/CN=Internal-Root-CA/O=SpringApiSecurity"

# Generate Keystore for Gateway
openssl req -newkey rsa:2048 -nodes \
  -keyout gateway-key.pem -out gateway-req.pem \
  -subj "/CN=api-gateway/O=SpringApiSecurity"

openssl x509 -req -in gateway-req.pem -days 365 \
  -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out gateway-cert.pem \
  -extfile <(printf "subjectAltName=DNS:localhost,DNS:api-gateway,IP:127.0.0.1")

openssl pkcs12 -export -in gateway-cert.pem -inkey gateway-key.pem \
  -out gateway-keystore.p12 -name gateway -passout pass:changeit

# Generate Keystore for Sensitive Data Backend
openssl req -newkey rsa:2048 -nodes \
  -keyout backend-key.pem -out backend-req.pem \
  -subj "/CN=sensitive-data-service/O=SpringApiSecurity"

openssl x509 -req -in backend-req.pem -days 365 \
  -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out backend-cert.pem \
  -extfile <(printf "subjectAltName=DNS:localhost,DNS:sensitive-data-service,DNS:sensitivedata-service,IP:127.0.0.1")

openssl pkcs12 -export -in backend-cert.pem -inkey backend-key.pem \
  -out backend-keystore.p12 -name backend -passout pass:changeit

# Truststore containing CA
keytool -importcert -noprompt -alias internal-ca -file ca-cert.pem \
  -keystore truststore.p12 -storepass changeit -storetype PKCS12

# Copy keystores to respective service resources for local and container run
cp gateway-keystore.p12 ../api-gateway/src/main/resources/keystore.p12
cp truststore.p12 ../api-gateway/src/main/resources/truststore.p12
cp backend-keystore.p12 ../sensitive-data-service/src/main/resources/keystore.p12
cp truststore.p12 ../sensitive-data-service/src/main/resources/truststore.p12

echo "Certificates generated successfully!"
