# GestionSalles

Comprehensive technical and product reference for a Java desktop platform that manages university room operations, reservation workflows, organizational entities, and account/session security.

## 1. Product Identity

GestionSalles is an internal university operations system centered on room governance.  
It supports three roles:

- `Admin`
- `Chef_Departement`
- `Enseignant`

Core objective:

- deliver conflict-safe room planning,
- enforce role-scoped access and views,
- maintain secure authentication/session behavior,
- provide operational visibility through dashboards and activity tracking.

## 2. Business Scope

The system covers the following business domains:

- Reservation lifecycle management (create, update, delete, search, filter)
- Room planning and schedule visualization
- Organizational master data management:
  - faculties/departments
  - blocs
  - niveaux
  - rooms
  - users
- Security-aware access, password change, and password recovery workflows
- Audit/recency feedback for operational monitoring

## 3. User Experience Foundations

Interface behavior principles implemented across screens:

- Swing desktop UI with FlatLaf-based styling
- card-style navigation and role-specific dashboard composition
- confirmation gates for destructive actions
- asynchronous background work using `SwingWorker` to keep UI responsive
- validation feedback at form level (inline labels, contextual messages)
- interactive management tables:
  - search/filter
  - row selection state-driven actions
  - print/export support
  - explicit no-result states

## 4. Startup and Access Lifecycle

### 4.1 Cold Start

Entry path: `Main.main`

High-level runtime sequence:

1. initialize logging and theme context,
2. validate DB connectivity and schema prerequisites,
3. attempt remember-me token authentication,
4. open login screen if token auth fails.

Failure handling:

- unavailable DB or invalid schema -> startup abort with explicit dialog.

### 4.2 Remember-Me Authentication

Flow behavior:

- token is loaded from local storage,
- validated via `AuthService.authenticateWithToken(...)`,
- if valid, a fresh active session is created and role dashboard is opened,
- invalid/expired token is removed and login flow is shown.

### 4.3 Login Experience

Primary login UI (`LoginPanel`) includes:

- email input
- password input
- remember-me option
- forgot-password flow entry
- submit action with loading-state transition

Authentication pattern:

- client-side validation first,
- authentication off the EDT thread,
- success routes to role-specific dashboard,
- failure returns localized inline error feedback.

## 5. Role Journeys

### 5.1 Admin

Admin workflow includes:

- overview dashboard (KPIs + chart + recent activity)
- management modules for departments, rooms, niveaux, users, reservations
- schedule viewer with broader scope controls
- export/print tooling in management/scheduling contexts

Security behavior visible to Admin:

- periodic session revalidation (~60s),
- forced logout on invalid session,
- password policy enforcement (`mustChangePassword`) routed to account settings flow.

### 5.2 Chef_Departement

Chef role provides similar management and scheduling capabilities with scope restrictions:

- data and actions constrained by department/bloc context,
- role-filtered reservation and schedule operations,
- dedicated dashboard behavior when bloc assignment is missing.

### 5.3 Enseignant

Teacher workflow is intentionally simplified:

- personal schedule focus,
- reduced navigation complexity,
- account settings access from dashboard header,
- scoped print/PDF schedule outputs for own context.

## 6. Detailed Feature Flows

### 6.1 Password Recovery (Three-Step Flow)

Container: `PasswordRecoveryContainer`

Step 1 (email/code request):

- email validation,
- rate-limiting checks,
- user existence/active checks,
- email-service readiness checks,
- existing-valid-code protection.

Step 2 (code verification):

- six-digit entry UX with paste and focus controls,
- expiry countdown with state transitions,
- invalid/expired/success outcomes.

Step 3 (new password):

- strength and confirm checks,
- password update finalization,
- session invalidation and return to login path.

### 6.2 Account Settings Password Change

Direct change flow behavior:

- verify current password first,
- enforce new-password validity/strength rules,
- clear `mustChangePassword` on success,
- invalidate current session after successful change.

Incorrect current-password attempts are capped, with fallback toward recovery path.

### 6.3 Reservation Management

Shared Admin/Chef capabilities:

- debounce search and multidimensional filters,
- table-based selection actions,
- batch deletion with confirmation,
- print and spreadsheet export behavior,
- visual differentiation for past reservations.

Reservation dialog dynamics:

- adaptive form sections based on activity/recurrence/online mode,
- organizational and pedagogical selectors,
- recurrence-specific validation,
- Friday and invalid recurrence constraints,
- group-specific activity requirements.

Conflict control:

- pre-save conflict checks via conflict service + DB procedures,
- duplicate/single/multi-conflict dialog patterns,
- optional path to inspect conflicting reservation,
- save operation blocked until conflict-free.

### 6.4 Schedule Viewer

Admin schedule viewer supports broader filtering by:

- context type (`Salle`, `Enseignant`, `Niveau`)
- faculty/department scope
- specific entity and date
- full-week mode

Chef viewer applies equivalent behavior inside constrained scope.

Enseignant viewer (`MySchedulePanel`) is automatically identity-scoped and supports day/week, print, and PDF export.

## 7. Dashboard and Activity Model

Overview KPIs include:

- room count
- active user count
- reservations for today
- rooms currently in use

Recent activity panel behavior:

- icon/profile + textual summary + relative time formatting,
- Admin sees global activity slice,
- Chef sees bloc-scoped activity slice.

## 8. Architecture

### 8.1 Layered Structure

GestionSalles follows a layered architecture with strong package-level separation:

1. Entry/Bootstrap layer
2. Presentation layer
3. Application service layer
4. Data access layer
5. Domain model layer
6. Infrastructure/utilities layer
7. Schema/procedure layer

### 8.2 Layer Responsibilities

Entry and bootstrap:

- `Main`, `MainApp`
- startup orchestration, validation, and initial routing

Presentation (`views/*`):

- role dashboards, dialogs, management pages, shared UI components
- user interaction and UI-state orchestration

Services (`services/*`):

- authentication logic
- verification-code management
- email integration orchestration
- conflict-detection orchestration

Persistence (`dao/*`, `database/*`):

- SQL operations, mappings, and retrieval/update paths
- centralized database connection/config handling

Domain (`models/*`):

- entities and enums for core business concepts

Utilities (`utils/*`):

- session context and token lifecycle helpers
- security and secret support classes
- audit and reusable UI utilities

Schema/SQL (`db/*`):

- schema assets, conflict procedures, seed data

### 8.3 Runtime Flow

Standard execution flow:

1. UI event triggered in presentation layer,
2. service validates and coordinates the operation,
3. DAO executes DB operations,
4. model objects returned to service/UI,
5. UI updated on EDT, with long tasks offloaded to background workers.

## 9. Security Model

### 9.1 Authentication and Credential Handling

- password hashing/verification through dedicated utilities (`PasswordUtils`)
- remember-me token authentication support with DB session creation on success
- sensitive in-memory handling patterns in auth/recovery code paths

### 9.2 Session Security

- active session persistence in `active_sessions`
- single active session per user email
- session token comparisons use constant-time techniques
- periodic heartbeat/revalidation in dashboard runtime
- forced logout on invalidated or superseded sessions

### 9.3 Recovery Code Security

- six-digit verification code lifecycle
- hashed persistence (not plaintext storage)
- expiry enforcement with both server-side logic and user-visible countdown
- request/attempt controls to reduce abuse patterns

### 9.4 Secret Management

Verification secret resolution order:

1. JVM property `app.verification.secret`
2. env var `GESTION_SALLES_SECRET`
3. secure file `~/.gestion-salles/app.secret`

Token/secret files use hardened local storage behavior where supported.

### 9.5 Configuration Hardening

Database and email configuration are environment-first with strict checks:

- required values must be present and non-placeholder,
- insecure DB default combinations are rejected,
- invalid runtime config fails fast.

### 9.6 Auditing

Security-significant events are logged through audit facilities (`AuditLogger`) to improve traceability of sensitive operations.

## 10. Data Model and Schema

### 10.1 Main Domain Models

- `User`
- `Reservation`
- `Room`
- `Departement`
- `Bloc`
- `Niveau`
- `ScheduleEntry`
- `Conflict`
- `ActivityLog`
- `ActivityType`
- `DashboardScope`
- `ActivityItemData`

### 10.2 Key Tables

- `utilisateurs`
- `reservations`
- `salles`
- `niveaux`
- `blocs`
- `departements`
- `activity_log`
- `audit_log`
- `active_sessions`
- `verification_codes`

### 10.3 Conflict Procedures

Runtime conflict checks rely on stored procedures:

- `verifier_conflit_salle`
- `verifier_conflit_enseignant`
- `verifier_conflit_niveau`

### 10.4 SQL Assets

- `db/schema.sql`
- `db/migrations/001_conflict_procedures.sql`
- `db/seed/minimal_seed.sql`
- `gestion_salles.sql`

## 11. Technical Inventory

### 11.1 DAO Modules

- `ActivityLogDAO`
- `ActivityTypeDAO`
- `BlocDAO`
- `DashboardDAO`
- `DepartementDAO`
- `NiveauDAO`
- `ReservationDAO`
- `ReservationMapper`
- `ReservationConflictException`
- `RoomDAO`
- `ScheduleDAO`
- `UserDAO`

### 11.2 Service Modules

- `AuthService`
- `ConflictDetectionService`
- `EmailService`
- `VerificationCodeManager`

### 11.3 Utilities

- `AppConfig`
- `SessionManager`
- `SessionContext`
- `TokenStorage`
- `SecretManager`
- `AuditLogger`
- `PasswordUtils`
- `PasswordStrengthChecker`
- `PasswordFormHelper`
- `HealthCheck`
- and additional reusable UI/infra helpers under `utils/`

### 11.4 View Modules

Major view families:

- `views/Admin/*`
- `views/ChefDepartement/*`
- `views/Enseignant/*`
- `views/Login/*`
- `views/shared/*`

## 12. Testing and Quality

Current test suites include:

- `ReservationDAOTest`
- `DatabaseConnectionTest`
- `AuthServiceTest`
- `ConflictDetectionServiceTest`
- `VerificationCodeManagerTest`
- `DashboardDevTest`
- `LoginFlowDevTest`

Quality characteristics:

- domain/service/DAO tests cover core logic,
- dev/UI tests exist for important UI flows,
- runtime quality depends on valid database and environment configuration.

## 13. Build, Packaging, and Runtime Operations

### 13.1 Build Tooling

Build system: Maven with compiler, test, shading, execution, and packaging plugins.

### 13.2 Dependency Families

Project integrates the following categories:

- database/connectivity (MySQL connector, HikariCP)
- security (BCrypt)
- UI/look-and-feel (FlatLaf, layout/tooling)
- document export (POI, PDFBox)
- serialization/config/logging support

### 13.3 Artifacts and Profiles

Primary generated artifacts:

- `GestionSalles-1.0-SNAPSHOT.jar`
- `GestionSalles-1.0-SNAPSHOT-shaded.jar`

Packaging profiles are configured for installer targets (including Linux packaging assets).

### 13.4 Operational Scripts

- `extract-icons.sh`
- `post-install.sh`
- `scripts/health-check.sh`
- `src/main/resources/scripts/gestionsalles.sh`
- `src/main/resources/scripts/health-check.sh`

## 14. Repository Structure

- `src/main/java/com/gestion/salles`: application source code
- `src/main/resources`: icons/config templates/runtime scripts
- `src/test/java/com/gestion/salles`: test suites
- `db/`: schema/migration/seed SQL assets
- `docs/`: focused subsystem documentation
- `APP_FULL_REFERENCE.md`: full UX and technical reference source

## 15. Documentation Cross-References

- `APP_FULL_REFERENCE.md`
- `docs/security-and-session.md`
- `docs/database.md`
- `docs/packaging.md`

## 16. Architecture Diagram

Place your architecture PNG here:

- `docs/screenshots/architecture-layering.png`

Rendered:

![GestionSalles Architecture](docs/screenshots/architecture-layering.png)

## 17. Copyright and Usage

Copyright (c) 2026 Abdelrahman Benmoulai.  
All rights reserved.

This repository is published for presentation, review, and documentation purposes only.  
No permission is granted to copy, reuse, modify, redistribute, or integrate this codebase (in whole or in part) into other projects without explicit prior written authorization from the author.
