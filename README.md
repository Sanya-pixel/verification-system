# Warehouse Product Verification System
### Flipkart Supply Chain Digital Automation — Technical Assignment

---

## Problem Statement
A robust system for bulk import of product data, on-the-spot warehouse verification of individual products, and comprehensive reporting on all verification activities.

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
| Cloud Storage | AWS S3 (Proof of Delivery photos) |
| Security | Spring Security + JWT (RBAC) |
| Build Tool | Maven 3.x |
| CSV Parsing | Apache Commons CSV |

---

## Features Implemented

### ✅ 1. Bulk Product Data Ingestion (Mandatory)
- Upload CSV file with millions of product records
- Async processing — returns `202 Accepted` immediately, processes in background
- JDBC batch inserts (batch size 5000) — handles millions of rows without OOM
- Idempotent upserts — re-uploading same file never corrupts data (`ON CONFLICT DO UPDATE`)
- CSV columns: `WID`, `EAN`, `Manufacturing_Date`, `Expiry_Date`

### ✅ 2. On-the-Floor Product Validation (Mandatory)
- Scan WID barcode → instantly fetch product details
- Returns EAN, Manufacturing Date, Expiry Date
- Logs every verification event (who checked which WID and when)
- Supports photo capture URL for physical product inspection

### ✅ 3. Verification Reporting (Mandatory)
- Generate reports by custom date range (start date → end date)
- Returns all verification activities within the range
- Ordered by most recent first

### ✅ 4. Proof of Delivery (Mandatory)
- Driver scans AWB number
- Captures photo/video as proof of delivery
- Uploads media to AWS S3 with structured path
- Returns cloud URI for the uploaded media

### ✅ 5. RBAC — Role Based Access Control (Optional)
- Two roles: `ADMIN` and `OPERATOR`
- JWT-based stateless authentication
- Admin: can upload CSV, view reports, register users
- Operator: can only verify products and submit POD
- BCrypt password hashing

---

## Project Structure

```
src/main/java/com/warehouse/verification/
├── config/
│   ├── AsyncConfig.java              # Thread pool for bulk upload
│   ├── SecurityConfig.java           # JWT + RBAC rules
│   └── S3Config.java                 # AWS S3 client bean
├── controller/
│   ├── WarehouseController.java      # CSV upload, verify, reports
│   ├── ProofOfDeliveryController.java# POD photo upload
│   └── AuthController.java           # Login, register
├── model/
│   ├── Product.java                  # products table entity
│   ├── VerificationLog.java          # verification_logs table entity
│   ├── User.java                     # users table entity
│   └── Role.java                     # ADMIN / OPERATOR enum
├── repository/
│   ├── ProductRepository.java
│   ├── VerificationLogRepository.java
│   └── UserRepository.java
├── security/
│   ├── JwtUtil.java                  # Token generate + validate
│   ├── JwtAuthFilter.java            # Intercepts every request
│   └── UserDetailsServiceImpl.java
├── service/
│   ├── InventoryService.java         # Bulk CSV async processing
│   └── AuthService.java              # Login + register logic
└── dto/
    ├── LoginRequest.java
    └── RegisterRequest.java
```

---

## Database Schema

```sql
CREATE TABLE products (
    wid                VARCHAR(100) NOT NULL,
    ean                VARCHAR(13)  NOT NULL,
    manufacturing_date DATE         NOT NULL,
    expiry_date        DATE         NOT NULL,
    CONSTRAINT pk_products_wid PRIMARY KEY (wid)
);

CREATE INDEX idx_products_ean ON products(ean);

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

CREATE TABLE users (
    id       BIGSERIAL    NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_users_id       PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
);
```

---

## Setup & Installation

### Prerequisites
- Java 17 ([Download](https://adoptium.net/))
- Maven 3.x
- PostgreSQL 16 ([Download](https://www.postgresql.org/download/))
- AWS Account (for S3 — optional, can mock for local testing)

### Step 1 — Clone the repository
```bash
git clone https://github.com/Sanya-pixel/verification-system.git
cd verification-system
```

### Step 2 — Set up PostgreSQL
```sql
psql -U postgres

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

CREATE TABLE users (
    id BIGSERIAL NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT pk_users_id PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
);

INSERT INTO users (username, password, role) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
('operator1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'OPERATOR');
-- admin password: admin123
-- operator1 password: operator123

\q
```

### Step 3 — Configure application.properties
Open `src/main/resources/application.properties` and update:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/warehouse_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD

aws.s3.bucket-name=your-bucket-name
aws.region=ap-south-1
```

### Step 4 — Build
```bash
mvn clean package -DskipTests
```

### Step 5 — Run
```bash
java -jar target/verification-system-1.0.0-SNAPSHOT.jar
```

App starts on: **http://localhost:8080**

---

## API Reference

### Authentication

#### Login
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

#### Register new user (Admin only)
```
POST /api/v1/auth/register
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "operator2",
  "password": "pass123",
  "role": "OPERATOR"
}
```

---

### Warehouse Operations

#### 1. Bulk CSV Upload
```
POST /api/v1/warehouse/inventory/bulk-upload
Authorization: Bearer <admin-token>
Content-Type: multipart/form-data

Param: file (CSV file)

Response: 202 Accepted
"Upload started in background."
```

Sample CSV format:
```csv
WID,EAN,Manufacturing_Date,Expiry_Date
WID-001,1234567890123,2024-01-01,2026-12-31
WID-002,9876543210987,2023-06-15,2025-06-15
```

#### 2. Verify Product by WID
```
GET /api/v1/warehouse/verify/{wid}?operatorId=101
Authorization: Bearer <token>

Response: 200 OK
{
  "wid": "WID-001",
  "ean": "1234567890123",
  "manufacturingDate": "2024-01-01",
  "expiryDate": "2026-12-31"
}

Response: 404 Not Found (if WID doesn't exist)
```

#### 3. QA Date-Range Report
```
GET /api/v1/warehouse/reports?start=2024-01-01&end=2026-12-31
Authorization: Bearer <admin-token>

Response: 200 OK
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

---

### Proof of Delivery

#### Upload POD Photo
```
POST /api/v1/pod/synchronize
Authorization: Bearer <token>
Content-Type: multipart/form-data

Params:
  awbNumber   - Air Waybill number
  driverId    - Driver ID
  latitude    - GPS latitude
  longitude   - GPS longitude
  mediaFile   - Photo/video file

Response: 200 OK
{
  "air_waybill_number": "AWB123456",
  "status": "SYNCHRONIZED",
  "cloud_uri": "s3://bucket/pod-records/2025/AWB123456_timestamp.jpg",
  "timestamp": "2025-06-07T10:30:00"
}
```

---

## Testing the APIs

### Quick test with curl

```bash
# 1. Create sample CSV
echo "WID,EAN,Manufacturing_Date,Expiry_Date
WID-001,1234567890123,2024-01-01,2026-12-31
WID-002,9876543210987,2023-06-15,2025-06-15" > test.csv

# 2. Upload CSV
curl -X POST http://localhost:8080/api/v1/warehouse/inventory/bulk-upload \
  -F "file=@test.csv"

# 3. Verify product
curl "http://localhost:8080/api/v1/warehouse/verify/WID-001?operatorId=101"

# 4. Get report
curl "http://localhost:8080/api/v1/warehouse/reports?start=2024-01-01&end=2026-12-31"
```

---

## Architecture Decisions

| Decision | Reason |
|---|---|
| **Async CSV processing** | Large files with millions of rows would block HTTP threads. `@Async` with dedicated thread pool returns 202 immediately and processes in background |
| **JDBC batch over JPA** | JPA loads entities into heap — at millions of rows causes OutOfMemoryError. Raw `JdbcTemplate.batchUpdate` bypasses Hibernate's session cache entirely |
| **ON CONFLICT DO UPDATE** | Idempotent upserts mean re-uploading the same file never corrupts data — critical for WID uniqueness enforcement |
| **PostgreSQL indexes** | WID is PK (instant lookup). EAN has B-Tree index for product family queries. `verified_at` is indexed for fast date-range reports |
| **JWT stateless auth** | No server-side session storage — scales horizontally across multiple pods without shared session store |
| **BCrypt passwords** | Industry standard — salted hash, resistant to rainbow table attacks |
| **HikariCP pool size 40** | Handles concurrent warehouse operator requests efficiently |
| **Thread pool 10-25 threads** | Bulk upload workers scale up to 25 threads for parallel CSV processing |

---

## RBAC Access Control

| Endpoint | ADMIN | OPERATOR |
|---|---|---|
| POST /auth/login | ✅ | ✅ |
| POST /auth/register | ✅ | ❌ |
| POST /inventory/bulk-upload | ✅ | ❌ |
| GET /warehouse/reports | ✅ | ❌ |
| GET /warehouse/verify/{wid} | ✅ | ✅ |
| POST /pod/synchronize | ✅ | ✅ |

---

## Author
**Sanya Mittal**
- Email: mittalsanya19@gmail.com
- GitHub: github.com/Sanya-pixel
- LinkedIn: linkedin.com/in/sanya-mittal-67bb031b3
