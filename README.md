# DisputeHub - Transaction Dispute Management System

A production-grade full-stack transaction dispute portal for banking customers and administrators.

## 📋 Overview

DisputeHub allows bank customers to view their transactions and dispute them when fraudulent or incorrect charges occur. Administrators can review disputes, update statuses, and maintain a complete audit trail.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Frontend                               │
│  React 18 + TypeScript + Vite + TailwindCSS                │
│  React Router + React Query                                  │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/REST (JWT Bearer Token)
┌──────────────────────▼──────────────────────────────────────┐
│                       Backend                                │
│  Spring Boot 3 + Java 17                                    │
│  Spring Security (JWT) + Spring Data JPA                    │
└──────────────────────┬──────────────────────────────────────┘
                       │ JDBC
┌──────────────────────▼──────────────────────────────────────┐
│                      PostgreSQL 15                           │
│  Transactional Database with ACID guarantees                │
└──────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Docker Desktop installed and running
- Git

### Run with Docker (Recommended)

```bash
# Clone or navigate to project directory
cd DisputeHub

# Start all services
docker-compose up --build

# Wait for services to start (about 2 minutes)
# Backend will seed database with sample data automatically
```

**Access the application:**
- Frontend: http://localhost
- Backend API: http://localhost:8080

### Test Accounts

| Role     | Username                  | Password      |
|----------|---------------------------|---------------|
| Customer | john.doe@example.com      | Password@123  |
| Customer | jane.smith@example.com    | Password@123  |
| Admin    | admin@disputehub.com      | Admin@123     |

## 🧪 Testing the Application

### As a Customer

1. Login as `john.doe@example.com` / `Password@123`
2. View your transactions on the dashboard
3. Click "Dispute" on a transaction
4. Fill out dispute form and submit
5. Track dispute status in "My Disputes" section

### As an Admin

1. Login as `admin@disputehub.com` / `Admin@123`
2. View dashboard with dispute statistics
3. See all disputes from all customers
4. Click "Update Status" on a dispute
5. Change status and add resolution notes
6. View audit log (who changed what and when)

## 📁 Project Structure

```
DisputeHub/
├── backend/                    # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/disputehub/api/
│   │   │   │   ├── entity/    # JPA entities (User, Transaction, Dispute)
│   │   │   │   ├── repository/ # Spring Data JPA repositories
│   │   │   │   ├── service/   # Business logic layer
│   │   │   │   ├── controller/ # REST API endpoints
│   │   │   │   ├── mapper/    # Entity to DTO mappers
│   │   │   │   ├── security/  # JWT + Spring Security config
│   │   │   │   ├── dto/       # Data Transfer Objects (Request/Response)
│   │   │   │   └── exception/ # Exception handlers
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/              # Unit and integration tests
│   ├── pom.xml                # Maven dependencies
│   └── Dockerfile             # Multi-stage Docker build
│
├── frontend/                   # React + TypeScript application
│   ├── src/
│   │   ├── components/        # React components
│   │   │   ├── Login.tsx
│   │   │   ├── Register.tsx
│   │   │   ├── CustomerDashboard.tsx
│   │   │   └── AdminDashboard.tsx
│   │   ├── contexts/          # React Context (Auth)
│   │   ├── services/          # API service (Axios)
│   │   ├── types/             # TypeScript types
│   │   ├── App.tsx            # Main app with routing
│   │   └── main.tsx           # Entry point
│   ├── package.json
│   ├── Dockerfile             # Multi-stage Docker build
│   └── nginx.conf             # Nginx configuration
│
├── docker-compose.yml         # Orchestrates all services
├── README.md                  # This file
└── PROJECT_CONTEXT.md         # Detailed technical documentation
```

## 🔑 Key Features

### Authentication & Security
- ✅ JWT-based authentication
- ✅ Role-based access control (Customer vs Admin)
- ✅ BCrypt password hashing
- ✅ Secure HTTP-only communication
- ✅ CORS configuration
- ✅ SQL injection prevention (parameterized queries)

### Customer Features
- ✅ View personal transactions
- ✅ Create disputes on transactions
- ✅ Track dispute status (Pending → Under Review → Merchant Contacted → Resolved/Rejected)
- ✅ View dispute history
- ✅ Provide evidence/description for disputes

### Admin Features
- ✅ Dashboard with statistics
- ✅ View all disputes across all customers
- ✅ Update dispute statuses
- ✅ Add resolution notes
- ✅ Complete audit trail (who did what, when)

### Data Management
- ✅ Complete audit logging
- ✅ Transaction history
- ✅ Dispute lifecycle management
- ✅ Automatic database seeding with sample data

### Architecture & Code Quality
- ✅ DTO pattern (prevents circular references, decouples API from database)
- ✅ Entity-to-DTO mappers (centralized transformation logic)
- ✅ Service layer works with entities, controllers return DTOs
- ✅ Clean separation of concerns (Controller → Service → Repository)
- ✅ Comprehensive exception handling
- ✅ OpenAPI/Swagger documentation

## 🛠️ Development Setup

### Backend (Spring Boot)

```bash
cd backend

# Run without Docker (requires PostgreSQL installed)
# Update application.properties with your database credentials
mvn spring-boot:run

# Run tests
mvn test

# Build JAR
mvn clean package
```

### Frontend (React)

```bash
cd frontend

# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Database (PostgreSQL)

```bash
# Run PostgreSQL in Docker
docker run -d \
  --name disputehub-postgres \
  -e POSTGRES_DB=disputehub \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

## 📡 API Documentation

### Authentication Endpoints

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john.doe@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "email": "john@example.com"
}

Response: { "token": "eyJhbG...", "id": 1, "username": "john.doe@example.com", "role": "CUSTOMER" }
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john.doe@example.com",
  "password": "password123"
}

Response: { "token": "eyJhbG...", "id": 1, "username": "john.doe@example.com", "role": "CUSTOMER" }
```

### Transaction Endpoints (Customer Only)

#### Get My Transactions
```http
GET /api/transactions
Authorization: Bearer <JWT_TOKEN>

Response: [
  {
    "id": 1,
    "merchantName": "Amazon",
    "amount": 499.99,
    "transactionDate": "2024-04-10T14:30:00",
    "category": "Online Shopping",
    "description": "Laptop purchase"
  }
]
```

### Dispute Endpoints

#### Create Dispute (Customer)
```http
POST /api/disputes
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "transactionId": 1,
  "reason": "UNAUTHORIZED",
  "description": "I did not authorize this transaction",
  "evidenceUrl": "https://example.com/receipt.jpg"
}

Response: { "id": 1, "status": "PENDING", ... }
```

#### Get My Disputes (Customer)
```http
GET /api/disputes/my-disputes
Authorization: Bearer <JWT_TOKEN>

Response: [ { "id": 1, "status": "UNDER_REVIEW", ... } ]
```

#### Get All Disputes (Admin)
```http
GET /api/disputes
Authorization: Bearer <JWT_TOKEN>

Response: [ { "id": 1, "user": {...}, "transaction": {...}, ... } ]
```

#### Update Dispute Status (Admin)
```http
PUT /api/disputes/1/status
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "status": "RESOLVED",
  "resolutionNotes": "Refund approved after merchant confirmation"
}

Response: { "id": 1, "status": "RESOLVED", ... }
```

## 🗄️ Database Schema

```sql
-- Users table
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,           -- BCrypt hashed
  full_name VARCHAR(255),
  email VARCHAR(255),
  role VARCHAR(255) NOT NULL,               -- CUSTOMER or ADMIN (enum)
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transactions table
CREATE TABLE transactions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  merchant_name VARCHAR(255) NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  transaction_date TIMESTAMP NOT NULL,
  category VARCHAR(255),
  description VARCHAR(255),
  reference_number VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) 
    REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date DESC);

-- Disputes table
CREATE TABLE disputes (
  id BIGSERIAL PRIMARY KEY,
  transaction_id BIGINT UNIQUE NOT NULL,    -- One dispute per transaction
  user_id BIGINT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  description VARCHAR(255) NOT NULL,
  status VARCHAR(255) NOT NULL,             -- PENDING, UNDER_REVIEW, MERCHANT_CONTACTED, RESOLVED, REJECTED
  resolution_notes VARCHAR(255),
  evidence_url VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP,
  CONSTRAINT fk_dispute_transaction FOREIGN KEY (transaction_id) 
    REFERENCES transactions(id) ON DELETE CASCADE,
  CONSTRAINT fk_dispute_user FOREIGN KEY (user_id) 
    REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_disputes_user_id ON disputes(user_id);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_created_at ON disputes(created_at DESC);

-- Audit logs table
CREATE TABLE audit_logs (
  id BIGSERIAL PRIMARY KEY,
  dispute_id BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  action VARCHAR(255) NOT NULL,
  old_value VARCHAR(255),
  new_value VARCHAR(255),
  notes VARCHAR(255),
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_auditlog_dispute FOREIGN KEY (dispute_id) 
    REFERENCES disputes(id) ON DELETE CASCADE,
  CONSTRAINT fk_auditlog_actor FOREIGN KEY (actor_id) 
    REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_logs_dispute_id ON audit_logs(dispute_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
```

**Key Constraints:**
- `UNIQUE` on `users.username` - no duplicate usernames
- `UNIQUE` on `disputes.transaction_id` - one dispute per transaction
- `NOT NULL` on all required fields
- `ON DELETE CASCADE` - delete related records when parent is deleted
- Indexes on foreign keys and frequently queried fields for performance

## 🧪 Testing

### Manual Testing with curl

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test_user","password":"password123","fullName":"Test User","email":"test@example.com"}'

# Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john.doe@example.com","password":"Password@123"}' \
  | jq -r '.token')

# Get transactions
curl http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $TOKEN"

# Create dispute
curl -X POST http://localhost:8080/api/disputes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"transactionId":1,"reason":"UNAUTHORIZED","description":"Did not authorize this"}'
```

### Unit Tests

```bash
cd backend
mvn test
```

## 🔒 Security Considerations

### Production Deployment Checklist

- [ ] Change JWT secret to strong random value (256+ bits)
- [ ] Use environment variables for all secrets
- [ ] Enable HTTPS/TLS
- [ ] Configure proper CORS origins (not `*`)
- [ ] Set up rate limiting
- [ ] Enable Spring Security CSRF for traditional forms
- [ ] Use connection pooling for database
- [ ] Set up monitoring and logging (ELK stack)
- [ ] Implement refresh tokens
- [ ] Add account lockout after failed logins
- [ ] Enable database encryption at rest
- [ ] Set up automated backups
- [ ] Configure proper network security groups
- [ ] Enable Web Application Firewall (WAF)

## 📊 Performance Considerations

- **Database Indexing**: Indexes on foreign keys, username, status fields
- **Lazy Loading**: JPA relationships use FETCH.LAZY to avoid N+1 queries
- **Connection Pooling**: HikariCP (default in Spring Boot)
- **Caching**: React Query caches API responses on frontend
- **Pagination**: Implement pagination for large transaction lists
- **CDN**: Serve frontend static assets from CDN in production

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check backend logs
docker logs disputehub-backend

# Common issue: Port 8080 already in use
lsof -i :8080  # Find process using port
kill -9 <PID>  # Kill the process
```

### Frontend can't connect to backend
```bash
# Check if backend is healthy
curl http://localhost:8080/api/auth/health

# Check CORS configuration in application.properties
# Ensure frontend URL is in cors.allowed-origins
```

### Database connection error
```bash
# Check PostgreSQL logs
docker logs disputehub-postgres

# Verify credentials in docker-compose.yml match application.properties
```

## 📝 Sample Data

The application automatically seeds the database with:
- 1 Admin user
- 2 Customer users
- 8 Transactions (4 per customer)
- 2 Disputes in different statuses
- Audit logs for dispute state changes

## 📄 License

This project is created for a technical assessment.

## 👤 Author

Created for technical assessment submission

---

**Built with:** Java 17, Spring Boot 3, PostgreSQL, React 18, TypeScript, Tailwind CSS, Docker
