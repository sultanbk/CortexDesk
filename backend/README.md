# Backend - Network Ticketing API (Spring Boot)

RESTful API server for the CortexDesk ticketing system. Built with Spring Boot 3.5.9, provides ticket management, user authentication, SLA tracking, and AI-powered diagnostics.

## 🎯 Overview

This backend service provides:
- Ticket lifecycle management (create, assign, resolve, close, reopen)
- Role-based access control (RBAC)
- JWT authentication
- Email notifications
- SLA tracking and alerts
- AI-powered resolution suggestions
- File attachment handling

## 📋 Prerequisites

### System Requirements
- **Java 17 or higher**
- **Maven 3.6+** (or use bundled `mvnw`)
- **MySQL 5.7 or higher**
- **Git** (for cloning)

### Verify Installation
```bash
java -version          # Should show Java 17+
mvn -version          # Or ./mvnw -version
mysql --version       # Should show MySQL 5.7+
```

## 🚀 Quick Start

### 1. Setup Database

**Create MySQL Database:**
```bash
# Login to MySQL
mysql -u root -p

# Create database
CREATE DATABASE networkticketingdb;
CREATE USER 'cortex_user'@'localhost' IDENTIFIED BY 'cortex_password';
GRANT ALL PRIVILEGES ON networkticketingdb.* TO 'cortex_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

Or use default (quick dev setup):
```sql
CREATE DATABASE networkticketingdb;
-- Uses root/root by default
```

### 2. Configure Application

**Copy and Edit Configuration:**
```bash
cd backend
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

**Edit `src/main/resources/application.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/networkticketingdb
    username: root                    # Change to your MySQL user
    password: root                    # Change to your MySQL password
  
  mail:
    username: your-email@gmail.com
    password: your-app-password       # Generate at myaccount.google.com/apppasswords
```

> **Gmail App Password**: Follow [Google's guide](https://support.google.com/accounts/answer/185833) to generate an app-specific password

### 3. Build Project

```bash
cd backend

# Clean and build
./mvnw clean install

# Or without tests (faster)
./mvnw clean install -DskipTests
```

### 4. Run Application

```bash
./mvnw spring-boot:run
```

**Expected Output:**
```
2026-01-17 10:30:45.123  INFO ... : Started NetworkTicketingAppApplication
2026-01-17 10:30:46.234  INFO ... : Server started on port 9091
```

✅ API is now running on `http://localhost:9091`

## 📐 Project Structure

```
backend/
├── src/main/java/com/prodapt/network_ticketing/
│   ├── NetworkTicketingAppApplication.java      # Spring Boot entry point
│   ├── controller/                              # REST API endpoints
│   │   ├── AuthController.java                  # Login/Authentication
│   │   ├── TicketController.java                # Ticket CRUD & lifecycle
│   │   ├── IssueCategoryController.java         # Issue categories
│   │   └── TicketAttachmentController.java      # File attachments
│   ├── service/                                 # Business logic layer
│   │   ├── TicketService.java                   # Ticket operations interface
│   │   ├── EmailService.java                    # Email notifications
│   │   ├── IssueCategoryService.java            # Category management
│   │   ├── SlaAlertService.java                 # SLA monitoring
│   │   ├── TicketAttachmentService.java         # File handling
│   │   └── impl/                                # Service implementations
│   ├── entity/                                  # JPA entities (Database models)
│   │   ├── User.java                            # User account
│   │   ├── Ticket.java                          # Support ticket
│   │   ├── IssueCategory.java                   # Ticket categories
│   │   ├── Role.java                            # User roles
│   │   ├── TicketAttachment.java                # File attachments
│   │   ├── TicketStatusHistory.java             # Ticket audit trail
│   │   └── enums/
│   │       ├── TicketStatus.java                # NEW, ASSIGNED, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED, REOPENED
│   │       ├── Priority.java                    # LOW, MEDIUM, HIGH
│   │       ├── RoleName.java                    # CUSTOMER, ENGINEER, MANAGER, ADMIN
│   │       └── SlaStatus.java                   # ON_TRACK, AT_RISK, BREACHED
│   ├── repository/                              # Data access layer (JPA)
│   ├── dto/                                     # Request/Response DTOs
│   │   ├── CreateTicketRequest.java
│   │   ├── AssignTicketRequest.java
│   │   ├── PickTicketRequest.java
│   │   ├── ResolveTicketRequest.java
│   │   ├── CloseTicketRequest.java
│   │   ├── ReopenTicketRequest.java
│   │   ├── UpdatePriorityRequest.java
│   │   ├── AddAiResolutionRequest.java
│   │   ├── LoginRequest.java
│   │   └── LoginResponse.java
│   ├── security/                                # JWT & authentication
│   │   ├── JwtUtil.java                         # Token generation/validation
│   │   └── JwtFilter.java                       # Request filter for JWT
│   ├── config/                                  # Spring Boot configuration
│   │   ├── SecurityConfig.java                  # Spring Security setup
│   │   ├── CorsConfig.java                      # CORS for frontend
│   │   ├── WebConfig.java                       # Web configuration
│   │   ├── UserSeeder.java                      # Initialize default users
│   │   └── IssueCategorySeeder.java             # Initialize categories
│   ├── scheduler/                               # Scheduled tasks
│   │   └── SlaMonitorScheduler.java             # Background SLA monitoring
│   ├── exception/                               # Custom exception handling
│   └── resources/
│       ├── application.yaml                     # Main configuration
│       └── application.yml.example              # Configuration template
├── src/test/java/                              # Unit & integration tests
├── pom.xml                                      # Maven dependencies
├── mvnw / mvnw.cmd                             # Maven wrapper
└── target/                                      # Build output (generated)
```

## 🔐 Default Credentials

### Database
| Field | Value |
|-------|-------|
| Host | localhost:3306 |
| Database | networkticketingdb |
| Username | root |
| Password | root |

### Pre-seeded Users
The application auto-seeds these users via `UserSeeder.java`:

| Role | Email | Password | Purpose |
|------|-------|----------|---------|
| ADMIN | admin@cortexdesk.com | password | System administration |
| CUSTOMER | customer@cortexdesk.com | password | Create tickets |
| ENGINEER | engineer@cortexdesk.com | password | Resolve tickets |
| MANAGER | manager@cortexdesk.com | password | Assign & track |

⚠️ **Change in production!** Modify `src/main/java/com/prodapt/network_ticketing/config/UserSeeder.java`

## 🔌 API Endpoints

All endpoints require JWT token in header (except login):
```
Authorization: Bearer <token>
```

### Authentication
```bash
POST /api/auth/login
Content-Type: application/json

Request:
{
  "email": "customer@cortexdesk.com",
  "password": "password"
}

Response (201):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "userId": 1,
    "email": "customer@cortexdesk.com",
    "firstName": "Customer",
    "lastName": "User",
    "role": "CUSTOMER"
  }
}
```

### Ticket Management

**Create Ticket** (Customer, Manager, Admin)
```bash
POST /api/tickets
Content-Type: application/json
Authorization: Bearer <token>

Request:
{
  "customerId": 1,
  "title": "No Internet Connection",
  "description": "Internet is down in zone A",
  "issueCategoryId": 1,
  "priority": "HIGH"
}

Response (200):
{
  "ticketId": 1,
  "customer": {...},
  "title": "No Internet Connection",
  "description": "Internet is down in zone A",
  "status": "NEW",
  "priority": "HIGH",
  "createdAt": "2026-01-18T10:30:45",
  "slaDeadline": "2026-01-18T14:30:45"
}
```

**List All Tickets** (Role-based filtering)
```bash
GET /api/tickets
Authorization: Bearer <token>

Response: Array of tickets filtered by user role
```

**Get Ticket Details**
```bash
GET /api/tickets/{ticketId}
Authorization: Bearer <token>
```

**Assign Ticket to Engineer** (Manager, Admin)
```bash
POST /api/tickets/assign
Authorization: Bearer <token>

Request:
{
  "ticketId": 1,
  "engineerId": 3
}

Response: Updated ticket with status ASSIGNED
```

**Engineer Picks Up Ticket**
```bash
POST /api/tickets/pick
Authorization: Bearer <token>

Request:
{
  "ticketId": 1,
  "engineerId": 3
}

Response: Updated ticket with status IN_PROGRESS
```

**Update Ticket Priority**
```bash
PUT /api/tickets/{ticketId}/priority
Authorization: Bearer <token>

Request:
{
  "priority": "CRITICAL"
}
```
datasource.username` | root | Database user |
| `spring.datasource.password` | root | Database password |
| `spring.jpa.hibernate.ddl-auto` | update | Auto-create/update schema |
| `spring.jpa.properties.hibernate.format_sql` | true | Format SQL output |
| `spring.mail.host` | smtp.gmail.com | Email SMTP server |
| `spring.mail.port` | 587 | Email SMTP port |
| `spring.mail.username` | your-email@gmail.com | Gmail account |
| `spring.mail.password` | app-password | Gmail app password |
| `spring.mail.properties.mail.smtp.auth` | true | Enable SMTP auth |
| `spring.mail.properties.mail.smtp.starttls.enable` | true | Enable TLS
POST /api/tickets/resolve
Authorization: Bearer <token>

Request:
{
  "ticketId": 1,
  "resolution": "Rebooted router, connection restored"
}

Response: Updated ticket with status RESOLVED
```

**Close Ticket** (Customer, Engineer, Manager)
```bash
POST /api/tickets/close
Authorization: Bearer <token>

Request:
{
  "ticketId": 1
}

Response: Updated ticket with status CLOSED
```

**Reopen Ticket**
```bash
POST /api/tickets/reopen
Authorization: Bearer <token>

Request:
{
  "ticketId": 1,
  "reason": "Issue not resolved"
}

Response: Updated ticket with status REOPENED
```

**Add AI Resolution Suggestion**
```bash
POST /api/tickets/{ticketId}/ai-resolution
Authorization: Bearer <token>

Request:
{
  "aiSuggestion": "Try rebooting the router",
  "confidence": 0.95
}
```

### Issue Categories

**List All Categories**
```bash
GET /api/issue-categories
Authorization: Bearer <token>

Response: Array of categories
[
  {
    "categoryId": 1,
    "name": "No Internet",
    "description": "Complete loss of internet connectivity"
  },
  ...
]
```

**Create Category** (Admin only)
```bash
POST /api/issue-categories
Authorization: Bearer <token>

Request:
{
  "name": "Router Issues",
  "description": "Problems with router configuration"
}

Response (201): Created category
```

**Update Category** (Admin)
```bash
PUT /api/issue-categories/{categoryId}
Authorization: Bearer <token>
```

**Delete Category** (Admin)
```bash
DELETE /api/issue-categories/{categoryId}
Authorization: Bearer <token>
```

### Ticket Attachments

**Upload File**
```bash
POST /api/tickets/{ticketId}/attachments
Authorization: Bearer <token>
Content-Type: multipart/form-data

Form Data:
file: <binary file>

Response: Created attachment with URL
```

**Download File**
```bash
GET /api/attachments/{attachmentId}
Authorization: Bearer <token>

Response: Binary file download
```

**Delete Attachment**
```bash
DELETE /api/attachments/{attachmentId}
Authorization: Bearer <token>
```

## 🧪 Testing

### Run All Tests
```bash
./mvnw test
```

### Run Specific Test Class
```bash
./mvnw test -Dtest=TicketServiceTest
```🎨 Ticket Status Flow

The application supports the following ticket lifecycle:

```
NEW
  ↓
ASSIGNED (by Manager)
  ↓
IN_PROGRESS (picked by Engineer)
  ↓ (optional)
ON_HOLD (paused work)
  ↓
RESOLVED (marked resolved by Engineer)
  ↓
CLOSED (closed by Customer/Engineer/Manager)
  
REOPENED (reopened from CLOSED state)
```

**Status Transitions**:
- NEW → ASSIGNED: Manager assigns to Engineer
- ASSIGNED → IN_PROGRESS: Engineer picks up ticket
- IN_PROGRESS → ON_HOLD: Pause work (optional)
- ON_HOLD → IN_PROGRESS: Resume work
- IN_PROGRESS → RESOLVED: Mark as resolved
- RESOLVED → CLOSED: Close the ticket
- CLOSED → REOPENED: Reopen if not satisfied

## 🎯 Priority Levels

- **LOW**: Non-urgent, can wait
- **MEDIUM**: Standard priority, handle in order
- **HIGH**: Urgent, needs quick attention

## ⏱️ SLA Management

The system includes automated SLA monitoring via `SlaMonitorScheduler`:

- **SLA Deadline**: Calculated based on ticket creation time
- **SLA Status**: 
  - ON_TRACK: Within SLA window
  - AT_RISK: Approaching deadline
  - BREACHED: Exceeded deadline

Scheduler runs periodically to:
1. Check all OPEN tickets
2. Calculate time remaining
3. Update SLA status
4. Send alerts for AT_RISK/BREACHED ticket

### Integration Tests
```bash
./mvnw test -Dtest=*IntegrationTest
```

## 📊 Key Configuration Properties

| Property | Default | Purpose |
|----------|---------|---------|
| `server.port` | 9091 | Server port |
| `spring.datasource.url` | jdbc:mysql://localhost:3306/networkticketingdb | Database URL |
| `spring.jpa.hibernate.ddl-auto` | update | Auto-create/update schema |
| `spring.mail.host` | smtp.gmail.com | Email provider |
| `spring.mail.port` | 587 | Email SMTP port |

## 🛠️ Common Tasks

### Rebuild Database Schema
```bash
# Method 1: Delete data and recreate
# Edit application.yml: ddl-auto: create-drop
./mvnw spring-boot:run

# Method 2: Update existing schema
# Edit application.yml: ddl-auto: update
./mvnw spring-boot:run
```

### View Database
```bash
mysql -u root -p networkticketingdb
SHOW TABLES;
DESCRIBE ticket;
```

### Clear Old Logs
```bash
# Logs are typically in target/
rm -rf backend/target
```

### Update Dependencies
```bash
./mvnw versions:display-dependency-updates
./mvnw dependency:tree
```

## 🔍 Logging & Debugging

### View Logs
```bash
# Logs appear in console during ./mvnw spring-boot:run
# For file logging, add to application.yml:
logging:
  level:
    root: INFO
    com.prodapt: DEBUG
  file: logs/application.log
```

### Debug Mode
```bash
./mvnw spring-boot:run -Ddebug
```

### Enable SQL Logging
Add to `application.yml`:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## 🐛 Troubleshooting

### Error: "Connection refused" (MySQL)
**Problem**: MySQL not running
**Solution**:
```bash
# Windows
mysql.server start

# macOS
brew services start mysql

# Linux
sudo systemctl start mysql
```

### Error: "Access denied for user 'root'@'localhost'"
**Problem**: Wrong MySQL credentials
**Solution**:
```bash
# Reset MySQL root password or update application.yml
spring:
  datasource:
    username: your_mysql_user
    password: your_mysql_password
```

### Error: "Port 9091 already in use"
**Problem**: Another process is using port 9091
**Solution** (Windows):
```bash
netstat -ano | findstr 9091
taskkill /PID <PID> /F

# Or change port in application.yml
server:
  port: 8080
```

### Error: "javax.mail.AuthenticationFailedException"
**Problem**: Gmail authentication failed
**Solution**:
1. Generate [App Password](https://myaccount.google.com/apppasswords)
2. Use app password (not account password) in `application.yml`
3. Enable 2FA on Google account

### Tables Not Created
**Problem**: Hibernate DDL not executing
**Solution**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create  # Or 'create-drop' for fresh start
```

## 📦 Dependencies

Key dependencies (see `pom.xml` for complete list):
- `spring-boot-starter-web` - REST endpoints
- `spring-boot-starter-data-jpa` - Database ORM
- `spring-boot-starter-security` - Authentication
- `spring-boot-starter-mail` - Email sending
- `mysql-connector-j` - MySQL driver
- `jjwt` - JWT token generation

## 🚢 Production Deployment

### Before Deploying:
1. ✅ Change default credentials in `UserSeeder.java`
2. ✅ Update MySQL credentials
3. ✅ Configure proper email service
4. ✅ Enable HTTPS
5. ✅ Set `ddl-auto: validate` (not update)
6. ✅ Configure proper logging
7. ✅ Set up database backups
8. ✅ Use environment variables for secrets

### Build for Production
```bash
./mvnw clean package -DskipTests
# JAR file: target/network-ticketing-0.0.1-SNAPSHOT.jar
```

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Guide](https://spring.io/projects/spring-data-jpa)
- [Spring Security Reference](https://spring.io/projects/spring-security)
- [MySQL Documentation](https://dev.mysql.com/doc/)

## ✅ Checklist for Running Locally

- [ ] Java 17+ installed
- [ ] Maven/mvnw available
- [ ] MySQL running
- [ ] Database `networkticketingdb` created
- [ ] `application.yml` configured
- [ ] Gmail app password generated
- [ ] `./mvnw clean install` successful
- [ ] `./mvnw spring-boot:run` starts without errors
- [ ] `http://localhost:9091/api/tickets` returns data

---

**Last Updated**: January 2026 | Backend Service | Spring Boot 3.5.9
