# Warehouse Product Verification System
### Flipkart Supply Chain Digital Automation — Technical Assignment

---

## Problem Statement
A robust system for bulk import of product data, on-the-spot warehouse verification of individual products, and comprehensive reporting on all verification activities — built for Flipkart's logistics and warehouse operations.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.14 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| Bulk Processing | JDBC Template (batch inserts) |
| Async Processing | Spring @Async + ThreadPoolTaskExecutor |
| Cloud Storage | AWS S3 SDK 2.x (Proof of Delivery photos) |
| Security | Spring Security 6.x + HTTP Basic Auth + RBAC |
| Password Hashing | BCrypt |
| Build Tool | Maven 3.x |
| CSV Parsing | Apache Commons CSV |
| OCR Validation | Regex-based + AWS Rekognition ready |

---

## Features Implemented

### ✅ 1. Bulk Product Data Ingestion
- Upload CSV file with millions of product records
- Async processing — returns `202 Accepted` immediately, processes in background
- JDBC batch inserts (batch size 5000) — handles millions of rows without OOM
- Idempotent upserts — re-uploading same file never corrupts data (`ON CONFLICT DO UPDATE`)
- CSV columns: `WID`, `EAN`, `Manufacturing_Date`, `Expiry_Date`
- WID uniqueness strictly enforced at DB + application level

### ✅ 2. On-the-Floor Product Validation
- POST WID + operator ID + product photo → instantly fetch and verify product details
- Returns verification result with OCR date cross-validation
- Logs every verification event (which operator checked which WID and when)
- Supports photo capture for physical product inspection
- OCR regex extracts dates from product label image and cross-validates against DB

### ✅ 3. Verification Reporting
- Generate reports by custom date range (start date → end date)
- Returns all verification activities within the range
- Ordered by most recent first
- Fast queries via indexed `verified_at` column

### ✅ 4. Proof of Delivery (POD)
- Driver submits AWB number + GPS coordinates + photo
- Uploads media to AWS S3 with structured path: `pod-records/{year}/{awb}_{timestamp}.jpg`
- Returns cloud URI for the uploaded media

### ✅ 5. User Administration
- Admin creates users with specific roles
- Two roles: `ROLE_ADMIN` and `ROLE_OPERATOR`
- BCrypt password hashing — never plain text

### ✅ 6. Role-Based Access Control
- Spring Security 6.x native RBAC enforcement with HTTP Basic Authentication
- ROLE_ADMIN: bulk upload, reports, user management
- ROLE_OPERATOR: verify products and submit POD only

### ✅ 7. OCR Image Date Validation
- `ImageOcrValidationService` extracts dates from product label photos
- Regex pattern matches ISO Standard dates (YYYY-MM-DD format)
- Cross-validates extracted dates against DB record automatically
- Architecture ready for AWS Rekognition or Tesseract OCR integration

---

## Project Structure

```
src/main/java/com/warehouse/verification/
├── config/
│   ├── AsyncConfig.java                   # Thread pool for bulk upload
│   ├── SecurityConfig.java                # HTTP Basic Auth + RBAC rules
│   └── S3Config.java                      # AWS S3 client bean
├── controller/
│   ├── WarehouseController.java           # CSV upload, verify, reports
│   ├── ProofOfDeliveryController.java     # POD photo upload to S3
│   └── UserManagementController.java      # Admin user creation
├── model/
│   ├── Product.java                       # products table entity
│   ├── VerificationLog.java               # verification_logs table entity
│   └── UserAccount.java                   # user_accounts table entity
├── repository/
│   ├── ProductRepository.java
│   ├── VerificationLogRepository.java
│   └── UserAccountRepository.java
└── service/
    ├── InventoryService.java              # Bulk CSV async processing
    ├── CustomUserDetailsService.java      # Spring Security user loader
    └── ImageOcrValidationService.java     # OCR date extraction + validation
```

---

## Database Schema

```sql
-- Core product inventory
CREATE TABLE products (
    wid                VARCHAR(100) NOT NULL,
    ean                VARCHAR(13)  NOT NULL,
    manufacturing_date DATE         NOT NULL,
    expiry_date        DATE         NOT NULL,
    CONSTRAINT pk_products_wid PRIMARY KEY (wid)
);
CREATE INDEX idx_products_ean ON products(ean);

-- Verification audit log
CREATE TABLE verification_logs (
    id                  BIGSERIAL    NOT NULL,
    wid                 VARCHAR(100) NOT NULL,
    operator_id         INT          NOT NULL,
    verified_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    captured_image_url  VARCHAR(500),
    CONSTRAINT pk_verification_logs_id PRIMARY KEY (id),
    CONSTRAINT fk_logs_product_wid FOREIGN KEY (wid)
        REFERENCES products(wid) ON DELETE CASCADE
);
CREATE INDEX idx_logs_timestamp ON verification_logs(verified_at);

-- User accounts
CREATE TABLE user_accounts (
    id       BIGSERIAL   NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    CONSTRAINT pk_user_accounts PRIMARY KEY (id)
);

-- User roles
CREATE TABLE user_roles (
    user_id   BIGINT      NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_roles FOREIGN KEY (user_id)
        REFERENCES user_accounts(id) ON DELETE CASCADE
);
```

---

## Setup & Installation

### Prerequisites
- Java 17 ([Download Temurin JDK 17](https://adoptium.net/))
- Maven 3.x
- PostgreSQL 16 ([Download](https://www.postgresql.org/download/))
- AWS Account with S3 bucket (optional — comment out POD controller for local testing)

### Step 1 — Clone the repository
```bash
git clone https://github.com/Sanya-pixel/verification-system.git
cd verification-system
```

### Step 2 — Set up PostgreSQL database
```bash
psql -U postgres
```

Run these SQL commands:
```sql
CREATE DATABASE warehouse_db;
\c warehouse_db

CREATE TABLE products (
    wid VARCHAR(100) NOT NULL,
    ean VARCHAR(13) NOT NULL,
    manufacturing_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    CONSTRAINT pk_products_wid PRIMARY KEY (wid)
);
CREATE INDEX idx_products_ean ON products(ean);

CREATE TABLE verification_logs (
    id BIGSERIAL NOT NULL,
    wid VARCHAR(100) NOT NULL,
    operator_id INT NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    captured_image_url VARCHAR(500),
    CONSTRAINT pk_verification_logs_id PRIMARY KEY (id),
    CONSTRAINT fk_logs_product_wid FOREIGN KEY (wid)
        REFERENCES products(wid) ON DELETE CASCADE
);
CREATE INDEX idx_logs_timestamp ON verification_logs(verified_at);

CREATE TABLE user_accounts (
    id BIGSERIAL NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    CONSTRAINT pk_user_accounts PRIMARY KEY (id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_roles FOREIGN KEY (user_id)
        REFERENCES user_accounts(id) ON DELETE CASCADE
);
\q
```

### Step 3 — Configure application.properties
Open `src/main/resources/application.properties` and update:
```properties
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/warehouse_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# Connection Pool
spring.datasource.hikari.maximum-pool-size=40
spring.datasource.hikari.minimum-idle=15

# JPA
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Batch performance
spring.jpa.properties.hibernate.jdbc.batch_size=5000
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# File upload limits
spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=250MB

# AWS S3 (only needed for POD feature)
aws.s3.bucket-name=your-bucket-name
aws.region=ap-south-1
```

### Step 4 — Create default admin user
Start the app first, then run:
```bash
curl -X POST http://localhost:8080/api/v1/admin/users/create \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","roles":["ROLE_ADMIN"]}'

curl -X POST http://localhost:8080/api/v1/admin/users/create \
  -H "Content-Type: application/json" \
  -d '{"username":"operator1","password":"operator123","roles":["ROLE_OPERATOR"]}'
```

### Step 5 — Build and Run
```bash
mvn clean package -DskipTests
java -jar target/verification-system-1.0.0-SNAPSHOT.jar
```

App starts on: **http://localhost:8080**

---

## API Reference

### Authentication
All endpoints (except user creation) use **HTTP Basic Authentication**.
Pass credentials as `-u username:password` in curl or set `Authorization: Basic base64(username:password)` header.

### User Administration

#### Create new user — public (no auth needed)
```
POST /api/v1/admin/users/create
Content-Type: application/json

{
  "username": "operator2",
  "password": "pass123",
  "roles": ["ROLE_OPERATOR"]
}

Response 200: "User registered successfully."
Response 400: "Conflict: Username already taken."
```

#### Get all users — ADMIN only
```
GET /api/v1/admin/users
Authorization: Basic admin:admin123

Response 200:
[
  { "id": 1, "username": "admin", "roles": ["ROLE_ADMIN"] },
  { "id": 2, "username": "operator1", "roles": ["ROLE_OPERATOR"] }
]
```

### Warehouse Operations

#### 1. Bulk CSV Upload — ADMIN only
```
POST /api/v1/warehouse/inventory/bulk-upload
Authorization: Basic admin:admin123
Content-Type: multipart/form-data
Param: file (CSV)

Response 202: "Upload started in background."
```

Sample CSV format:
```csv
WID,EAN,Manufacturing_Date,Expiry_Date
WID-001,1234567890123,2024-01-01,2026-12-31
WID-002,9876543210987,2023-06-15,2025-06-15
WID-003,1122334455667,2022-03-10,2024-03-10
```

#### 2. Verify Product by WID — ADMIN + OPERATOR
```
POST /api/v1/warehouse/verify/{wid}?operatorId=101
Authorization: Basic operator1:operator123
Content-Type: multipart/form-data
Param: image (product photo file)

Response 200: "Verification logged successfully. Physical label dates match database records completely."
Response 200: "Verification logged. WARNING: Physical label dates DO NOT match database records!"
Response 404: thrown when WID does not exist
```

#### 3. QA Date-Range Report — ADMIN only
```
GET /api/v1/warehouse/reports?start=2024-01-01&end=2026-12-31
Authorization: Basic admin:admin123

Response 200:
[
  {
    "logId": 1,
    "wid": "WID-001",
    "ean": "1234567890123",
    "verifiedAt": "2025-06-07T10:30:00",
    "operatorId": 101,
    "manufacturingDate": "2024-01-01",
    "expiryDate": "2026-12-31"
  }
]
```

### Proof of Delivery

#### Upload POD Photo — ADMIN + OPERATOR
```
POST /api/v1/pod/synchronize
Authorization: Basic operator1:operator123
Content-Type: multipart/form-data

Params:
  awbNumber   - Air Waybill number (e.g. AWB123456)
  driverId    - Driver ID (e.g. 201)
  latitude    - GPS latitude (e.g. 12.9716)
  longitude   - GPS longitude (e.g. 77.5946)
  mediaFile   - Photo or video file

Response 200:
{
  "air_waybill_number": "AWB123456",
  "status": "SYNCHRONIZED",
  "cloud_resource_uri": "s3://bucket/pod-records/2025/AWB123456_1234567890.jpg",
  "processed_timestamp": "2025-06-07T10:30:00"
}
```

---

## Quick Test with curl (Windows CMD)

```cmd
# 1. Create admin user
curl -X POST http://localhost:8080/api/v1/admin/users/create -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\",\"roles\":[\"ROLE_ADMIN\"]}"

# 2. Create operator user
curl -X POST http://localhost:8080/api/v1/admin/users/create -H "Content-Type: application/json" -d "{\"username\":\"operator1\",\"password\":\"operator123\",\"roles\":[\"ROLE_OPERATOR\"]}"

# 3. Upload CSV as admin
curl -u admin:admin123 -X POST http://localhost:8080/api/v1/warehouse/inventory/bulk-upload -F "file=@test.csv"

# 4. Get QA report as admin
curl -u admin:admin123 "http://localhost:8080/api/v1/warehouse/reports?start=2024-01-01&end=2026-12-31"

# 5. Try upload as operator — should get 403 FORBIDDEN
curl -u operator1:operator123 -X POST http://localhost:8080/api/v1/warehouse/inventory/bulk-upload -F "file=@test.csv"
```

---

## RBAC Access Control

| Endpoint | ROLE_ADMIN | ROLE_OPERATOR |
|---|---|---|
| POST /api/v1/admin/users/create | ✅ public | ✅ public |
| GET /api/v1/admin/users | ✅ | ❌ 403 |
| POST /inventory/bulk-upload | ✅ | ❌ 403 |
| GET /warehouse/reports | ✅ | ❌ 403 |
| POST /warehouse/verify/{wid} | ✅ | ✅ |
| POST /pod/synchronize | ✅ | ✅ |

---

## Architecture Decisions

| Decision | Reason |
|---|---|
| **Async CSV processing** | Large files block HTTP threads. @Async returns 202 immediately, processes in background thread pool |
| **JDBC batch over JPA** | JPA loads entities into heap — millions of rows causes OutOfMemoryError. JdbcTemplate.batchUpdate bypasses Hibernate session cache entirely |
| **ON CONFLICT DO UPDATE** | Idempotent upserts — re-uploading same file never corrupts data. Critical for WID uniqueness enforcement |
| **PostgreSQL indexes** | WID is PK (instant lookup). EAN B-Tree index for product queries. verified_at indexed for date-range reports |
| **HTTP Basic Auth + Spring Security RBAC** | Simple, stateless authentication. Spring Security 6.x handles role enforcement natively with CustomUserDetailsService |
| **BCrypt password hashing** | Industry standard — salted hash, resistant to rainbow table and brute force attacks |
| **HikariCP pool size 40** | Handles concurrent warehouse operator requests without connection exhaustion |
| **Thread pool 10-25 threads** | Bulk upload workers scale to 25 threads for high-throughput CSV processing |
| **@Lazy S3Client** | Allows app to start without AWS credentials for local testing |

---

## Default Credentials

| Username | Password | Role |
|---|---|---|
| admin | admin123 | ROLE_ADMIN |
| operator1 | operator123 | ROLE_OPERATOR |

> **Note:** Create users via `POST /api/v1/admin/users/create` on first run. Passwords are BCrypt hashed automatically.

---

## Author
**Sanya Mittal**
- Email: mittalsanya19@gmail.com
- GitHub: [github.com/Sanya-pixel](https://github.com/Sanya-pixel)
- LinkedIn: [linkedin.com/in/sanya-mittal-67bb031b3](https://linkedin.com/in/sanya-mittal-67bb031b3)
