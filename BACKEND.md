# Smart Institute ERP — Backend

Spring Boot REST API for a multi-tenant school/institute management system.

## Tech stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 17 |
| Framework | Spring Boot 4.0.2 |
| Security | Spring Security + JWT |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL |
| Mail | Spring Mail (SMTP / logging fallback) |
| PDF | Apache PDFBox |
| Build | Maven (`mvnw`) |

Default API base URL: `http://localhost:8081`

## Prerequisites

- JDK 17+
- MySQL 8+
- Maven (or use included `mvnw.cmd` / `mvnw`)

## Quick start

1. Create database:

```sql
CREATE DATABASE smartinstitute CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Configure `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartinstitute
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
server.port=8081
erp.app.login-url=http://localhost:5173
```

3. Run:

```bash
.\mvnw.cmd spring-boot:run
```

Schema is auto-created/updated via `spring.jpa.hibernate.ddl-auto=update`.

## Project structure

```
src/main/java/com/smartinstitute/erp/
├── auth/                 # Login, JWT, refresh tokens, forgot password
├── academic/
│   ├── attendance/
│   ├── classroom/
│   ├── classteacher/
│   ├── course/
│   ├── fee/              # Fees, payments, receipts, online payment skeleton
│   ├── principal/
│   ├── quiz/
│   ├── schedule/
│   ├── student/
│   ├── subject/
│   ├── teacher/
│   └── teachersubject/
├── notification/
│   ├── inapp/            # In-app notifications
│   └── mail/             # SMTP / logging mail service
├── platform/
│   ├── admin/            # Super admin institute management
│   └── institute/        # Institute admin + public onboarding requests
├── user/                 # User entities and repositories
└── exception/            # Global exception handling
```

## Roles

| Role | Description |
|------|-------------|
| `SUPER_ADMIN` | Platform owner; manages institutes and onboarding requests |
| `INSTITUTE_ADMIN` | Institute-level admin |
| `PRINCIPAL` | Academic setup, fees, schedule, teachers |
| `TEACHER` | Attendance, quizzes, class activities |
| `STUDENT` | Own profile, attendance, fees, exams |

Authorization uses `@PreAuthorize` on controllers plus URL rules in `SecurityConfig`.

## Authentication

### Login

`POST /auth/login`

```json
{
  "email": "student@school.com",
  "password": "secret",
  "deviceId": "browser-uuid",
  "userAgent": "Mozilla/5.0 ..."
}
```

Returns JWT access token in body; refresh token in HTTP-only cookie.

### Other auth endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/refresh` | Refresh access token (cookie + deviceId) |
| POST | `/auth/logout` | Revoke refresh token |
| POST | `/auth/change-password` | Change password (authenticated) |
| POST | `/auth/forgot-password` | Send reset code email |
| POST | `/auth/reset-password` | Reset with email + code + new password |

### Request headers

Protected endpoints require:

```
Authorization: Bearer <access_token>
```

## Multi-tenancy

- Each institute is a tenant (`institute_id`).
- `JwtAuthFilter` resolves tenant from JWT and sets `TenantContext`.
- Hibernate `@Filter(name = "tenantFilter")` scopes tenant-bound entities.
- `/auth/**` and `/public/**` bypass JWT filter.

## API overview

### Platform

| Base path | Role | Purpose |
|-----------|------|---------|
| `/platform/institutes` | SUPER_ADMIN | List/toggle institutes, approve/reject requests |
| `/platform/institute-admin` | INSTITUTE_ADMIN | Institute profile |
| `/public/institute-request` | Public | Submit onboarding request |

### Academic

| Base path | Purpose |
|-----------|---------|
| `/api/courses` | Courses CRUD |
| `/api/subjects` | Subjects CRUD |
| `/institute/classes` | Classrooms CRUD |
| `/api/teachers` | Teachers CRUD + `/me` |
| `/api/principals` | Principals |
| `/institute/students` | Students CRUD + bulk upload |
| `/api/students/me` | Student profile (`GET`, `PATCH /me/name`) |
| `/api/teacher-subjects` | Teacher ↔ subject links |
| `/api/class-teachers` | Class teacher assignments |
| `/api/schedule` | Weekly timetable slots |
| `/api/attendance` | Attendance mark/list/report |
| `/api/quizzes` | Exams + submit + results |
| `/api/questions` | Quiz questions + CSV/PDF import |
| `/api/notifications` | In-app notifications |

### Fees module

Base path: `/api/fees`

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/my` | STUDENT | List own fees |
| GET | `/` | ADMIN, PRINCIPAL | List institute fees (`?studentId`, `?status`) |
| POST | `/` | ADMIN, PRINCIPAL | Create fee record |
| PATCH | `/{id}/pay` | ADMIN, PRINCIPAL | Record partial/full payment |
| PATCH | `/{id}/mark-paid` | ADMIN, PRINCIPAL | Mark remaining balance paid |
| GET | `/{id}/payments` | ADMIN, PRINCIPAL | Payment history for a fee |
| GET | `/my/{id}/payments` | STUDENT | Own payment history |
| GET | `/export` | ADMIN, PRINCIPAL | CSV export |
| GET | `/my/export` | STUDENT | CSV export |
| GET | `/{id}/receipt` | ADMIN, PRINCIPAL | PDF receipt |
| GET | `/my/{id}/receipt` | STUDENT | PDF receipt |
| POST | `/my/{id}/online/init` | STUDENT | Start online payment order |
| POST | `/my/{id}/online/confirm` | STUDENT | Confirm mock gateway payment |

#### Fee statuses

- `UNPAID`
- `PARTIALLY_PAID`
- `PAID`

#### Payment methods

`ONLINE`, `CASH`, `UPI`, `CARD`, `NET_BANKING`, `OTHER`

#### Fee entities

- `FeeRecord` — assigned fee (total, paid, balance, status, receipt)
- `FeePayment` — each installment/payment entry
- `FeeGatewayTransaction` — online gateway order lifecycle (mock-ready for Razorpay/Stripe)

## Email

Configured in `application.properties`:

```properties
erp.mail.enabled=true
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your@email.com
spring.mail.password=APP_PASSWORD
```

Set `erp.mail.enabled=false` to use logging mail service (no SMTP).

Used for:

- Student credential emails
- Password reset codes

## Error handling

`GlobalExceptionHandler` returns JSON errors with `message` field. Frontend `Api.js` parses these for display.

Common HTTP codes:

| Code | Meaning |
|------|---------|
| 400 | Validation / bad request |
| 401 | Missing or invalid token |
| 403 | Wrong role or tenant |
| 404 | Resource not found |
| 500 | Server error |

## Build & test

```bash
# Compile
.\mvnw.cmd -DskipTests compile

# Run
.\mvnw.cmd spring-boot:run

# Clean build
.\mvnw.cmd clean package -DskipTests
```

## Configuration reference

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8081` | API port |
| `erp.app.login-url` | — | Frontend URL in emails |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto schema sync |
| `erp.mail.enabled` | `true` | Enable SMTP |

## Security notes

- Do not commit real SMTP passwords or DB credentials.
- JWT access tokens are short-lived; refresh tokens are HTTP-only cookies.
- Password reset revokes all refresh tokens for the user.
- CORS allows `localhost` origins for development.

## Related docs

See [FRONTEND.md](./erp-frontend/FRONTEND.md) for the React client.
