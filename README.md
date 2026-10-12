# Spring Boot API Security Architecture (Java 25, DDD, TLS, Keycloak, Shared File DB)

Enterprise-grade microservice architecture for secure management of sensitive data built with **Domain-Driven Design (DDD)**, **Spring Cloud Gateway (Spring Cloud 2025.1.3 / Spring Boot 4.0.8)**, **Keycloak OIDC**, **AES-256-GCM** encryption at rest, persistent file-based H2 database shared across concurrent backend replicas, and Round Robin load balancing.

---

## 🏗️ System Architecture

```
[ Client / Browser ]
        │
        ├── (1) POST /realms/spring-security-realm/protocol/openid-connect/token ──► [ Keycloak IDP (port 8080) ]
        │                                                                                     │
        │   ◄── RSA Signed JWT Token (Lifespan: 1 minute, aud: sensitive-data-service) ───────┘
        │
        ▼ (2) Protected API requests over TLS (Port 443)
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        Spring Cloud Gateway (api-gateway)                              │
│   • PKCS12 TLS Termination (External port 443)                                         │
│   • OIDC JWT validation and token expiration check (60-second lifespan)                │
│   • Request payload size enforcement (Max 1MB) & JSON Content-Type validation          │
│   • Spring Cloud LoadBalancer (Round Robin) with Actuator Health Checks                │
│   • Zero-Trust internal TLS connection to backend nodes                                │
└────────────────────────────────────┬───────────────────────────────────────────────────┘
                                     │ (3) Internal TLS Zero-Trust Connection (Port 8443)
                    ┌────────────────┴────────────────┐
                    ▼                                 ▼
   ┌─────────────────────────────────┐ ┌─────────────────────────────────┐
   │ Sensitive Data Service (Clone 1)│ │ Sensitive Data Service (Clone 2)│
   │  • Internal port 8443 (TLS)     │ │  • Internal port 8443 (TLS)     │
   │  • No exposed host ports        │ │  • No exposed host ports        │
   │  • JPA Encryption: AES-256-GCM  │ │  • JPA Encryption: AES-256-GCM  │
   │  • Security Audit Tracking Log  │ │  • Security Audit Tracking Log  │
   │  • Docker Secret: db_aes_key    │ │  • Docker Secret: db_aes_key    │
   └────────────────┬────────────────┘ └────────────────┬────────────────┘
                    │                                   │
                    └─────────────────┬─────────────────┘
                                      ▼
                        ┌───────────────────────────┐
                        │  H2 Shared File Database  │
                        │   (./data/sensitivedb)    │
                        │    AUTO_SERVER=TRUE       │
                        │    Encrypted data at rest │
                        └───────────────────────────┘
```

---

## 🔒 Key Security Features

1. **Java 25 & Modern Spring Ecosystem**:
   - Built on Java 25 (`release: 25`, `-parameters` enabled for AOT and Spring Data).
   - Multi-module Maven setup with Spring Boot `4.0.8` and Spring Cloud `2025.1.3`.

2. **Perimeter Security & Gateway**:
   - Single entry point: Only the API Gateway exposes port `443` to external networks over TLS.
   - [RequestPayloadAndValidationFilter](file:///mnt/c/dev/git/spring-api-security/api-gateway/src/main/java/com/security/gateway/filter/RequestPayloadAndValidationFilter.java): Enforces a strict 1MB maximum payload limit and validates `application/json` Content-Type before forwarding to backend services.
   - [GatewayHttpClientConfig](file:///mnt/c/dev/git/spring-api-security/api-gateway/src/main/java/com/security/gateway/config/GatewayHttpClientConfig.java): Configures trusted internal TLS communication between Gateway and backend replicas.

3. **Identity & Access Management (Keycloak OIDC)**:
   - Keycloak 26.1 container with pre-configured realm (`spring-security-realm`).
   - Ephemeral JWT access tokens with a **60-second expiration time**.
   - Audience enforcement (`sensitive-data-service`).
   - Pre-configured test client (`spring-api-gateway`) and test user (`testuser`).

4. **Domain-Driven Design (DDD) & Encrypted Persistence**:
   - **Domain Layer**:
     - [SensitiveRecord](file:///mnt/c/dev/git/spring-api-security/sensitive-data-service/src/main/java/com/security/sensitivedata/domain/model/SensitiveRecord.java): Aggregate root with transparent field-level encryption.
     - [AuditTrackingLog](file:///mnt/c/dev/git/spring-api-security/sensitive-data-service/src/main/java/com/security/sensitivedata/domain/model/AuditTrackingLog.java): Audit trail tracking user identity (`sub`), operation type, resource, status, and timestamps.
   - **Infrastructure Layer**:
     - [AesGcmCryptoConverter](file:///mnt/c/dev/git/spring-api-security/sensitive-data-service/src/main/java/com/security/sensitivedata/infrastructure/crypto/AesGcmCryptoConverter.java): Authenticated AES-256-GCM encryption at rest with randomized 12-byte IV per record.
     - Docker Secrets: Encryption key (`db_aes_key`) is securely mounted into `/run/secrets/` via memory/tmpfs.
   - **Database Sharing**:
     - H2 file-based persistence configured with `AUTO_SERVER=TRUE`, allowing concurrent connections from both `sensitive-data-service-1` and `sensitive-data-service-2` to the shared volume `./data`.
   - **Interfaces Layer**:
     - [SensitiveDataController](file:///mnt/c/dev/git/spring-api-security/sensitive-data-service/src/main/java/com/security/sensitivedata/interfaces/rest/SensitiveDataController.java): REST endpoints for parameter-validated search (`GET /search`) and schema-validated creation (`POST /`).

---

## 📁 Project Structure

```
spring-api-security/
├── pom.xml                                      # Parent POM (Java 25, Spring Boot 4.0.8, Spring Cloud 2025.1.3)
├── docker-compose.yml                           # Full environment orchestration (JVM runtime)
├── docker-compose.native.yml                    # GraalVM Native Image orchestration
├── README.md                                    # Project documentation
├── certs/
│   ├── generate-certs.sh                        # PKCS12 and X.509 certificate generator
│   ├── ca-cert.pem / ca-key.pem                 # Internal Root CA
│   ├── gateway-keystore.p12                     # Gateway TLS Keystore (port 443)
│   ├── backend-keystore.p12                     # Backend TLS Keystore (port 8443)
│   └── truststore.p12                           # Internal PKCS12 Truststore
├── secrets/
│   └── db_aes_key.txt                           # 256-bit symmetric AES key (Docker Secrets)
├── keycloak/
│   └── realm-export.json                        # Keycloak realm configuration with 1-min token expiry
├── data/                                        # Shared volume for persistent H2 database
├── api-gateway/
│   ├── Dockerfile                               # Multi-stage Dockerfile for API Gateway
│   ├── Dockerfile.native                        # GraalVM Native Image Dockerfile
│   ├── pom.xml                                  # Gateway Maven configuration
│   └── src/main/java/com/security/gateway/
│       ├── ApiGatewayApplication.java           # Gateway entrypoint
│       ├── config/
│       │   ├── GatewayHttpClientConfig.java     # Netty SSL client customizer
│       │   ├── GatewayRoutesConfig.java         # Load-balanced route definition
│       │   └── GatewaySecurityConfig.java       # Reactive Spring Security (JWT filter chain)
│       └── filter/
│           └── RequestPayloadAndValidationFilter.java # 1MB limit & Content-Type validation filter
└── sensitive-data-service/
    ├── Dockerfile                               # Multi-stage Dockerfile for Backend Service
    ├── Dockerfile.native                        # GraalVM Native Image Dockerfile
    ├── pom.xml                                  # Backend Service Maven configuration
    └── src/
        ├── main/java/com/security/sensitivedata/
        │   ├── SensitiveDataServiceApplication.java
        │   ├── domain/                          # DDD Domain Model & Repositories
        │   │   ├── model/
        │   │   │   ├── SensitiveRecord.java     # AES-256-GCM encrypted entity
        │   │   │   └── AuditTrackingLog.java    # Security audit tracking entity
        │   │   └── repository/
        │   │       ├── SensitiveRecordRepository.java
        │   │       └── AuditTrackingLogRepository.java
        │   ├── application/                     # Application Services & DTOs
        │   │   ├── dto/
        │   │   │   ├── CreateSensitiveDataCommand.java
        │   │   │   ├── SearchSensitiveDataQuery.java
        │   │   │   └── SensitiveDataResponse.java
        │   │   └── service/
        │   │       └── SensitiveDataApplicationService.java
        │   ├── infrastructure/                  # Infrastructure & Security
        │   │   ├── crypto/
        │   │   │   └── AesGcmCryptoConverter.java # JPA AES-256-GCM AttributeConverter
        │   │   └── config/
        │   │       ├── SecurityConfig.java      # Servlet Security (OAuth2 Resource Server)
        │   │       └── TestDataInitializer.java # Test data seeder
        │   └── interfaces/rest/                 # Web / REST Controllers
        │       ├── SensitiveDataController.java # API 1 (GET search) & API 2 (POST save)
        │       └── GlobalExceptionHandler.java  # RFC-7807 Problem Details error handler
        └── test/java/com/security/sensitivedata/infrastructure/crypto/
            └── AesGcmCryptoConverterTest.java   # Unit tests for encryption and random IVs
```

---

## 🚀 Quick Start Guide

### 1. Start the Environment
From the project root directory:
```bash
docker compose up -d --build
```

Verify that all four containers are running and report healthy status:
```bash
docker compose ps
```

Expected output:
```
NAME                       IMAGE                                   COMMAND                  SERVICE                    CREATED          STATUS                    PORTS
api-gateway                spring-api-security-api-gateway         "java -jar app.jar"      api-gateway                10 minutes ago   Up 10 minutes (healthy)   0.0.0.0:443->8443/tcp
keycloak-idp               quay.io/keycloak/keycloak:26.1          "/opt/keycloak/bin/k…"   keycloak                   10 minutes ago   Up 10 minutes (healthy)   0.0.0.0:8080->8080/tcp
sensitive-data-service-1   spring-api-security-sensitive-data-se…  "java -jar app.jar"      sensitive-data-service-1   10 minutes ago   Up 10 minutes (healthy)   
sensitive-data-service-2   spring-api-security-sensitive-data-se…  "java -jar app.jar"      sensitive-data-service-2   10 minutes ago   Up 10 minutes (healthy)   
```

---

### 2. Obtain JWT Access Token from Keycloak
Request a token using the resource owner password credentials grant (valid for 60 seconds):
```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/spring-security-realm/protocol/openid-connect/token" \
  -H "Host: keycloak:8080" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=spring-api-gateway" \
  -d "client_secret=gateway-client-secret-123456" \
  -d "username=testuser" \
  -d "password=password123" | jq -r .access_token)

echo "Acquired JWT Token: $TOKEN"
```

---

### 3. Test API 1: Query Sensitive Data (GET over TLS 443)
Query the protected endpoint through the API Gateway on port 443 (requests are load-balanced in Round Robin across backend clones):
```bash
curl -k -H "Authorization: Bearer $TOKEN" \
  "https://localhost:443/api/v1/sensitive-data/search?category=financial" | jq .
```

**Response:**
```json
[
  {
    "id": "e5e3c882-dede-4add-9a1d-95f22f0439b1",
    "category": "financial",
    "payload": "{\"accountNumber\": \"IT99A1234567890\", \"balance\": 450000.50, \"currency\": \"EUR\"}",
    "secretNote": "VIP Private Banking Account",
    "createdBy": "system-init"
  }
]
```

---

### 4. Test API 2: Store Encrypted Sensitive Data (POST with Validation & 1MB Limit)
Save a new sensitive record into the shared database:
```bash
curl -k -X POST "https://localhost:443/api/v1/sensitive-data" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "personal",
    "payload": "{\"taxId\": \"RSSMRA85M01H501Z\", \"iban\": \"IT60X0542811101000000123456\"}",
    "secretNote": "Confidential banking and personal data"
  }' | jq .
```

**Response (HTTP 201 Created):**
```json
{
  "id": "787bb7b6-0814-41d9-83d7-2f342f0a14ad",
  "category": "personal",
  "payload": "{\"taxId\": \"RSSMRA85M01H501Z\", \"iban\": \"IT60X0542811101000000123456\"}",
  "secretNote": "Confidential banking and personal data",
  "createdBy": "testuser"
}
```

---

### 5. Verify Database Sharing Across Clones & Disk Persistence
Query for the newly inserted record to confirm that both clones read the same shared file-based database:
```bash
curl -k -H "Authorization: Bearer $TOKEN" \
  "https://localhost:443/api/v1/sensitive-data/search?category=personal" | jq .
```

Verify that persistent database files exist on the host filesystem:
```bash
ls -lh ./data/
# Output includes:
# sensitivedb.mv.db
# sensitivedb.lock.db
```

---

### 6. Verify Security Controls

#### A. Token Expiration Enforcement
Wait 60 seconds after generating the token and repeat any request:
```bash
sleep 65
curl -k -i -H "Authorization: Bearer $TOKEN" \
  "https://localhost:443/api/v1/sensitive-data/search?category=financial"
```
**Expected Result:** HTTP 401 Unauthorized (`Bearer error="invalid_token", error_description="Jwt expired"`).

#### B. Request Payload Size Limit (Max 1MB)
Attempt to send a payload exceeding 1MB:
```bash
curl -k -i -X POST "https://localhost:443/api/v1/sensitive-data" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Content-Length: 1500000" \
  -d '{"category":"large"}'
```
**Expected Result:** HTTP 413 Payload Too Large (`{"error": "PAYLOAD_TOO_LARGE", "message": "Payload exceeds 1MB limit..."}`).

---

### 7. Logs & Service Management
```bash
# Follow logs across all services
docker compose logs -f

# Follow logs for specific backend clones
docker compose logs -f sensitive-data-service-1 sensitive-data-service-2

# Stop and remove containers
docker compose down
```
