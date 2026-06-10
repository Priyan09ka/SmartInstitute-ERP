# Smart Institute ERP — Frontend

React single-page application for the Smart Institute ERP platform.

## Tech stack

| Layer | Technology |
|-------|------------|
| UI | React 19 |
| Routing | React Router DOM 7 |
| Build | Vite 8 |
| Styling | Tailwind CSS 3 |
| API | Fetch (`services/Api.js`) |

Default dev URL: `http://localhost:5173`  
Backend API: `http://localhost:8081`

## Prerequisites

- Node.js 18+
- npm

## Quick start

```bash
cd erp-frontend
npm install
npm run dev
```

Production build:

```bash
npm run build
npm run preview
```

## Project structure

```
erp-frontend/
├── src/
│   ├── App.jsx                    # Routes
│   ├── main.jsx                   # Entry point
│   ├── components/
│   │   ├── ProtectedRoute.jsx     # Auth + role guard
│   │   ├── InstitutePortalLayout.jsx
│   │   ├── SuperAdminLayout.jsx
│   │   ├── Sidebar.jsx
│   │   └── Navbar.jsx
│   ├── pages/
│   │   ├── Login.jsx
│   │   ├── ForgotPassword.jsx
│   │   ├── Profile.jsx
│   │   ├── InstituteRequestForm.jsx
│   │   ├── superAdmin/            # Super admin dashboards
│   │   ├── instituteAdmin/        # Institute admin portal
│   │   ├── principal/             # Principal academic setup + fees
│   │   ├── teacher/               # Teacher dashboard
│   │   └── student/               # Student dashboard
│   ├── services/
│   │   └── Api.js                 # apiRequest, apiUploadMultipart
│   └── utils/
│       └── device.js              # Device ID for auth
├── index.html
├── vite.config.js
├── tailwind.config.js
└── package.json
```

## Routes

| Path | Access | Page |
|------|--------|------|
| `/` | Public | Login |
| `/forgot-password` | Public | Forgot / reset password |
| `/institute-request` | Public | Institute onboarding request |
| `/profile` | All logged-in roles | User profile |
| `/super-admin` | SUPER_ADMIN | Super admin home |
| `/institutes` | SUPER_ADMIN | Institute list |
| `/requests` | SUPER_ADMIN | Onboarding requests |
| `/institute-admin` | INSTITUTE_ADMIN | Institute admin portal |
| `/principal` | PRINCIPAL | Principal academic setup |
| `/teacher` | TEACHER | Teacher dashboard |
| `/student` | STUDENT | Student dashboard |
| `/dashboard` | Logged-in | Redirects to role home |

## Roles & navigation

After login, users are redirected to their role dashboard:

| Role | Dashboard path |
|------|----------------|
| SUPER_ADMIN | `/super-admin` |
| INSTITUTE_ADMIN | `/institute-admin` |
| PRINCIPAL | `/principal` |
| TEACHER | `/teacher` |
| STUDENT | `/student` |

`ProtectedRoute` checks `localStorage.token` and `localStorage.role`.

Profile link is available in sidebar for all institute roles and super admin.

## API client

### `apiRequest(url, method, body)`

```javascript
import { apiRequest } from "../services/Api";

const data = await apiRequest("/api/fees/my");
await apiRequest("/api/students/me/name", "PATCH", { name: "New Name" });
```

- Sends `Authorization: Bearer <token>` when token exists
- Uses `credentials: "include"` for refresh cookie
- On 401, clears storage and redirects to `/`
- Parses JSON error `message` from backend

### `apiUploadMultipart(url, formData)`

For file uploads (e.g. student bulk import, profile images).

### Binary downloads

Receipt/CSV downloads use `fetch` + `res.blob()` (see `downloadFromApi` in dashboards).

## Feature modules

### Authentication

- **Login** (`Login.jsx`) — email/password, device ID, role-based redirect
- **Forgot password** (`ForgotPassword.jsx`) — two-step: request code → reset password

### Profile (`Profile.jsx`)

- View role, email, institute, class
- Students: edit name, view roll number, upload passport-style photo (client validation)
- Profile photo stored in `localStorage` (preview only)

### Super admin

- Manage institutes
- Approve/reject institute onboarding requests

### Institute admin

- Institute portal dashboard
- Student/teacher management

### Principal (`PrincipalManagementDashboard.jsx`)

Sidebar sections:

- Overview, Classes, Class-wise view
- **Fees** — assign fees, partial payments, mark paid, CSV export, PDF receipts, payment history
- Courses, Subjects, Teachers
- Teacher ↔ Subject, Class teacher, Schedule

### Teacher (`TeacherDashboard.jsx`)

- Attendance marking
- Quiz/exam management
- Notifications

### Student (`StudentDashboard.jsx`)

Tabs:

- **Overview** — summary cards (attendance, fees)
- **Notifications**
- **Attendance** — table + pie chart
- **Fees** — fee list, pay online (mock), payment history, CSV export, PDF receipt
- **Exams** — take quizzes, view results

## Fees UI flow

### Principal

1. Assign fee to student (title, amount, due date)
2. **Add payment** — partial amount + method (CASH/UPI/CARD/…)
3. **Mark full paid** — pays remaining balance
4. **View payments** — expandable installment timeline
5. **Export CSV** / **Download receipt** (PDF)

### Student

1. View pending/paid summary
2. **Pay online** — mock gateway (`/online/init` → `/online/confirm`)
3. **View payments** — installment history
4. **Download receipt** (PDF) when fully paid
5. **Export CSV**

## Styling

- Tailwind utility classes
- Layout components: `InstitutePortalLayout`, `SuperAdminLayout`
- Responsive tables with `overflow-x-auto`

## Local storage keys

| Key | Purpose |
|-----|---------|
| `token` | JWT access token |
| `role` | User role string |
| `profilePhoto_<email>` | Student profile image (base64) |

## Environment

API base URL is hardcoded in `Api.js`:

```javascript
fetch(`http://localhost:8081${url}`, ...)
```

For other environments, update this base URL or introduce a `VITE_API_URL` env variable.

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start Vite dev server |
| `npm run build` | Production build to `dist/` |
| `npm run preview` | Preview production build |
| `npm run lint` | ESLint |

## Development workflow

1. Start MySQL and backend (`.\mvnw.cmd spring-boot:run` from repo root)
2. Start frontend (`npm run dev` in `erp-frontend`)
3. Open `http://localhost:5173`
4. Log in with a seeded or created user

## Related docs

See [BACKEND.md](../BACKEND.md) for API details, roles, and server setup.
