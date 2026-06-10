# Smart Institute ERP

Multi-tenant school and institute management platform with role-based dashboards for super admins, institute admins, principals, teachers, and students.

## Repository layout

```
erp/
├── src/                 # Spring Boot backend
├── erp-frontend/        # React + Vite frontend
├── BACKEND.md           # Backend setup, APIs, architecture
└── erp-frontend/FRONTEND.md   # Frontend setup, routes, UI modules
```

## Quick start

### 1. Backend (port 8081)

```bash
# Create MySQL database: smartinstitute
# Configure src/main/resources/application.properties

.\mvnw.cmd spring-boot:run
```

### 2. Frontend (port 5173)

```bash
cd erp-frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173)

## Documentation

| Doc | Description |
|-----|-------------|
| [BACKEND.md](./BACKEND.md) | Java/Spring API, auth, multi-tenancy, fees module, configuration |
| [FRONTEND.md](./erp-frontend/FRONTEND.md) | React app, routes, roles, API client, feature modules |

## Roles

| Role | Dashboard |
|------|-----------|
| Super Admin | `/super-admin` |
| Institute Admin | `/institute-admin` |
| Principal | `/principal` |
| Teacher | `/teacher` |
| Student | `/student` |

## Main features

- JWT authentication with refresh tokens and forgot-password flow
- Multi-tenant institutes (tenant-scoped data)
- Academic setup: courses, classes, subjects, teachers, schedule
- Attendance tracking and reports
- Quizzes / exams
- Fees: assignment, partial payments, PDF receipts, CSV export, online payment skeleton
- In-app notifications
- Profile page for all roles

## Tech stack

| Layer | Stack |
|-------|-------|
| Backend | Java 17, Spring Boot, Spring Security, JPA, MySQL |
| Frontend | React 19, Vite, Tailwind CSS, React Router |
