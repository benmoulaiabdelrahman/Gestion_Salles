# GestionSalles Complete Product and UX Reference

This document describes the app from a user-experience and workflow perspective, then maps each behavior to the technical modules that implement it.

Last updated: March 1, 2026

## 1. Product Scope

GestionSalles is a desktop application for university room and reservation operations. It supports three roles:
- `Admin`
- `Chef_Departement`
- `Enseignant`

Main business capabilities:
- user/department/bloc/room/niveau administration
- reservation creation and conflict-safe scheduling
- role-scoped schedule visualization
- password and session security controls
- auditing and recent activity visibility

## 2. UX Foundations

Global UI characteristics:
- Swing desktop app with FlatLaf styling
- navigation is card-based (screen switching without opening many windows)
- destructive actions are confirmation-gated
- long operations use background workers (`SwingWorker`) to keep UI responsive
- most management tables support search + filtering + export/print

Core interaction patterns used across screens:
- form-required fields use visual indicators and contextual error messages
- table selections enable/disable action buttons (`Modifier`, `Supprimer`) dynamically
- zero-result table states switch to explicit "Aucun résultat" messaging
- overlay/loading states are shown on dialog-heavy flows

## 3. End-to-End Startup and Access Flows

## 3.1 Cold Start

Entry point: `Main.main`

User-visible sequence:
1. app initializes logging and theme
2. app validates DB connectivity and schema
3. app attempts silent remember-me auto-login
4. if auto-login fails, `LoginFrame` is shown

Failure behaviors:
- DB unreachable: fatal dialog shown and startup exits
- schema missing: explicit schema error dialog and startup exits

## 3.2 Remember-Me Auto-Login

User expectation:
- if a valid token exists, user enters dashboard directly without login form

Actual behavior:
- token loaded from local token file
- `AuthService.authenticateWithToken(...)` validates token
- if valid, a new active DB session is created
- dashboard opens for the authenticated role
- if invalid/expired, local token is removed and login screen appears

## 3.3 Explicit Login Experience

Primary screen elements (`LoginPanel`):
- email field
- password field
- `Se souvenir de moi`
- `Mot de passe oublié ?`
- `Se connecter`
- "Première connexion ? Contacter l'université"

User flow:
1. user enters email + password
2. client-side validation runs first
3. button switches to loading state (`Connexion en cours…`)
4. auth runs off EDT
5. on success, role dashboard opens
6. on failure, inline error message is shown

Inline validation messages:
- `Veuillez remplir tous les champs`
- `Veuillez saisir votre email`
- `Veuillez saisir votre mot de passe`
- `Format d'email invalide`
- `Email ou mot de passe invalide.`

## 4. Role-Specific Dashboard Journeys

## 4.1 Admin Journey

Main nav entries:
- `Tableau de Bord`
- `Facultés`
- `Départements`
- `Salles`
- `Niveaux`
- `Utilisateurs`
- `Réservations`
- `Visualiseur d'Horaires`
- `Déconnexion`

Typical daily journey:
1. land on overview dashboard (`Apercu`)
2. inspect stats + room-type chart + recent activity
3. move into management modules (users, rooms, reservations)
4. export or print filtered data when needed
5. logout using sidebar action

Session protection behavior:
- every 60s session token is revalidated
- on invalidation (password changed/new login elsewhere), warning is shown and user is forced out

Password policy behavior:
- if `mustChangePassword=true`, admin is redirected to account settings flow

## 4.2 Chef_Departement Journey

Main nav entries:
- `Tableau de Bord`
- `Salles`
- `Niveaux`
- `Utilisateurs`
- `Réservations`
- `Visualiseur d'Horaires`
- `Déconnexion`

Scope behavior:
- operations are filtered by chef's bloc/department context
- user/room/schedule/reservation data are department-scoped

Special overview behavior:
- if no bloc is assigned, an explicit warning card is shown (`Aucun bloc assigné`) with refresh action

## 4.3 Enseignant Journey

Enseignant experience is intentionally simpler:
- overview with profile header
- embedded personal schedule panel
- settings access from header
- logout shortcut in overview footer

No left nav sidebar is used for Enseignant dashboard.

## 5. Detailed User Flows

## 5.1 Password Recovery Journey (3-step)

Container: `PasswordRecoveryContainer`

### Step 1: Email and code request

User actions:
- enter email
- click `Envoyer le code`

System checks:
- email format validation
- rate-limit validation
- user existence + active status
- email service configured
- no still-valid previous code

User-facing outcomes:
- success: proceeds to step 2
- failures include:
  - `Veuillez saisir un email valide.`
  - `Trop de tentatives. Réessayez dans 15 minutes.`
  - `Impossible d'envoyer le code. Contactez l'administrateur.`
  - `Service d'email non configuré. Contactez l'administrateur.`
  - `Un code a déjà été envoyé. Expire dans X min`

### Step 2: Code verification

Input UX details:
- six separate digit fields
- auto-focus advance per digit
- backspace navigation
- paste of full 6-digit code supported
- countdown timer with color severity transitions

Timer states:
- normal
- warning orange
- warning red
- expired state disables verify button and asks user to request new code

User actions:
- `Vérifier le code`
- `Renvoyer`

Common outcomes:
- success message then transition to step 3
- invalid code message
- expired code message

### Step 3: New password

User actions:
- enter new password + confirm
- click `Changer le mot de passe`

Validation behavior:
- both fields required
- passwords must match
- minimum strength is Medium

Success behavior:
- success panel shown
- active session invalidated
- user is logged out and returned to login path

## 5.2 Account Settings Journey

Screen sections:
- change current password directly
- forgot-password shortcut into recovery flow

Direct password-change flow:
1. enter current + new + confirmation
2. strength meter updates live
3. current password is verified first
4. on success password is updated and `mustChangePassword` flag is cleared
5. session is invalidated and logout is enforced

Lockout behavior:
- max incorrect current-password attempts: 5
- after limit: direct password change is blocked and user is steered to recovery flow

## 5.3 Reservation Management Journey (Admin and Chef)

Shared table behaviors:
- search with debounce
- filters by organizational context and user
- selection-based action enablement
- multi-select deletion
- print and Excel export
- no-result card when filters remove all rows

Visual cues:
- past reservations are shaded (gray style)

Delete behavior:
- explicit confirmation dialog
- batch delete allowed
- success/failure toast-like feedback via temporary messages

### Add/Edit reservation dialog flow

Main form concepts:
- organizational selectors (role-scoped)
- teacher/niveau/activity selection
- room type and room selection
- session slot selection (fixed time slots)
- optional recurring reservation controls
- optional group selection for group-specific activities
- online reservation toggle

Dynamic form behavior:
- recurring toggle reveals/hides recurrence date range + weekday
- activity type + niveau controls group field visibility/options
- online toggle disables physical-room selection and auto-uses online room
- dialog height adapts to visible optional sections

Validation behavior before save:
- required fields must be selected
- Friday is rejected for both one-time and recurring flows
- recurrence dates/day must be valid when recurrence is enabled
- group selection required for group-specific activity

Conflict behavior:
- pre-save conflict detection runs through conflict service
- conflict dialog supports:
  - complete duplicate message
  - single conflict details
  - multiple conflict summary
  - optional `Voir la réservation` path to inspect conflicting reservation
- save is blocked until conflicts are resolved

Save outcomes:
- add success or update success message
- recurrent overlap exception produces explicit conflict warning
- generic failures surface as error dialogs

## 5.4 Schedule Viewer Journeys

## Admin schedule viewer

Controls:
- context (`Salle`, `Enseignant`, `Niveau`)
- faculty and department filters
- specific entity selector
- date selector
- full-week checkbox (`Semaine complète` behavior)
- `Exporter PDF`
- `Imprimer`

UX behavior:
- full-week checkbox is enabled only when filter context is fully selected
- grid rows auto-adjust on resize
- table renderers format occupancy and conflicts visually

## Chef schedule viewer

Behavior is similar to Admin but organizational scope is constrained to chef's department/bloc.

## Enseignant schedule viewer (`MySchedulePanel`)

Behavior:
- automatically scoped to current teacher id
- supports day or week mode via checkbox
- supports print and PDF export
- no broad organization filter controls

PDF/Print experience:
- exported/printed output includes university header and current filter context
- schedule grid is rendered as image snapshot into print/PDF pipeline

## 5.5 Dashboard Overview and Recent Activity Experience

Overview cards:
- room count
- active user count
- reservations today
- rooms currently in use

Data loading UX:
- placeholder state while loading (`…`)
- graceful fallback on load errors
- chart panel updates with room-type distribution

Recent activity panel:
- title: `Activité Récente`
- item includes icon/photo, summary, relative time (`à l'instant`, `il y a X min`, etc.)
- activity scope:
  - admin sees global today activity
  - chef sees bloc-scoped today activity

## 6. Operational User Experience States

Logout paths:
- explicit logout button from dashboard
- enforced logout after session invalidation
- enforced logout after password reset/change success

Error surface strategy:
- inline label errors for form-level validation
- temporary message bars for non-fatal operation outcomes
- modal dialogs for critical conflicts and unrecoverable errors

Responsiveness strategy:
- data loading and authentication execute in background workers
- button text/state communicates pending operations (`Vérification...`, `Modification...`, etc.)

## 7. Security and Session UX Implications

User-visible security behavior:
- remember-me token may auto-login, but still creates a fresh active session
- session invalidation is immediate and visible to user via warning dialog
- password recovery and change flows both terminate active sessions
- verification codes expire and are time-bound with visible countdown
- brute-force password-change attempts are capped in settings flow

## 8. Feature-to-Code Map (for maintenance)

Primary entry and shell:
- `com/gestion/salles/Main.java`
- `com/gestion/salles/views/Login/LoginFrame.java`
- `com/gestion/salles/views/shared/dashboard/DashboardFrameBase.java`

Authentication and recovery:
- `com/gestion/salles/services/AuthService.java`
- `com/gestion/salles/services/VerificationCodeManager.java`
- `com/gestion/salles/views/shared/recovery/PasswordRecoveryStep1Panel.java`
- `com/gestion/salles/views/shared/recovery/PasswordRecoveryStep2Panel.java`
- `com/gestion/salles/views/shared/recovery/PasswordRecoveryStep3Panel.java`
- `com/gestion/salles/views/shared/settings/AccountSettingsPanel.java`

Reservations and conflict UX:
- `com/gestion/salles/views/Admin/ReservationManagementPanel.java`
- `com/gestion/salles/views/ChefDepartement/ReservationManagementPanel.java`
- `com/gestion/salles/views/Admin/ReservationDialog.java`
- `com/gestion/salles/views/ChefDepartement/ReservationDialog.java`
- `com/gestion/salles/services/ConflictDetectionService.java`

Schedules and exports:
- `com/gestion/salles/views/Admin/ScheduleViewerPanel.java`
- `com/gestion/salles/views/ChefDepartement/ScheduleViewerPanel.java`
- `com/gestion/salles/views/Enseignant/MySchedulePanel.java`
- `com/gestion/salles/views/shared/management/ManagementExportUtils.java`

Overview and activity:
- `com/gestion/salles/views/shared/dashboard/DashboardOverviewPanelBase.java`
- `com/gestion/salles/views/shared/RecentActivityPanel.java`

## 9. Supporting Technical Inventory (condensed)

DAOs:
- `ActivityLogDAO`, `ActivityTypeDAO`, `BlocDAO`, `DashboardDAO`, `DepartementDAO`, `NiveauDAO`, `ReservationDAO`, `RoomDAO`, `ScheduleDAO`, `UserDAO`

Services:
- `AuthService`, `ConflictDetectionService`, `EmailService`, `VerificationCodeManager`

Key DB tables:
- `utilisateurs`, `reservations`, `salles`, `niveaux`, `blocs`, `departements`, `activity_log`, `audit_log`, `active_sessions`, `verification_codes`

Key stored procedures:
- `verifier_conflit_salle`
- `verifier_conflit_enseignant`
- `verifier_conflit_niveau`

## 10. What This Document Adds Compared to Previous Reference

This file is intentionally experience-centric. It documents:
- what users do on each screen
- what they see when validation fails
- what happens on success/failure branches
- how role scope changes behavior
- how session/security events appear in UI

For pure architectural inventory details, keep `APP_DETAILED.md` as the compact technical companion.

## 11. Architecture (Detailed)

## 11.1 Layered Structure

- Entry layer:
  - `Main` orchestrates app bootstrap and routing.
- Presentation layer:
  - `views/*` packages (role dashboards, management screens, dialogs, shared components).
- Application/service layer:
  - `services/*` (`AuthService`, `ConflictDetectionService`, `EmailService`, `VerificationCodeManager`).
- Data access layer:
  - `dao/*` classes encapsulating SQL and mapping.
- Domain layer:
  - `models/*` entities and enum-rich value objects.
- Infrastructure/util layer:
  - `database/DatabaseConnection` and `utils/*` (session, secrets, audit, UI helpers).

## 11.2 Package Map

- `com.gestion.salles`
- `com.gestion.salles.dao`
- `com.gestion.salles.database`
- `com.gestion.salles.models`
- `com.gestion.salles.services`
- `com.gestion.salles.utils`
- `com.gestion.salles.views.Admin`
- `com.gestion.salles.views.ChefDepartement`
- `com.gestion.salles.views.Enseignant`
- `com.gestion.salles.views.Login`
- `com.gestion.salles.views.shared.*`

## 11.3 Core Runtime Data Flow

1. UI event triggers service/DAO action.
2. Service performs validation/coordination/business checks.
3. DAO executes SQL via pooled `DatabaseConnection`.
4. Model objects are mapped and returned.
5. UI updates on EDT; long jobs happen via `SwingWorker`.

## 11.4 Concurrency Model

- Swing UI thread (EDT) for rendering and interaction.
- `SwingWorker` for async form submits and data loads.
- dedicated async executor in dashboard overview for stats fan-out.
- periodic session timer checks every 60 seconds on dashboards.

## 12. Security Implementation (Detailed)

## 12.1 Authentication and Credentials

- password hashing/verification via BCrypt (`PasswordUtils`).
- login supports credential auth and remember-me token auth.
- password char arrays are explicitly zeroed after use in auth flows.

## 12.2 Session Security

- active sessions persisted in `active_sessions` table.
- one active session token per user email (`createSession` invalidates previous).
- token validation uses constant-time byte comparison (`MessageDigest.isEqual`).
- session last-seen heartbeat updates every minute while dashboard is open.
- forced logout on invalid session or post-password-change/reset.

## 12.3 Recovery Code Security

- verification codes are generated as 6 digits.
- plaintext codes are never persisted; HMAC hash is stored.
- code validation uses constant-time comparison against stored hash.
- expiry is enforced (countdown UX + server check).
- in-memory attempt-rate limiting is enforced for recovery requests.

## 12.4 Secret Management

Verification secret resolution order:
1. JVM property `app.verification.secret`
2. env var `GESTION_SALLES_SECRET`
3. secure file `~/.gestion-salles/app.secret`

## 12.5 Token and Secret File Hardening

- remember-me token file: `~/.GestionSalles/session.token`
- secret file: `~/.gestion-salles/app.secret`
- permission hardening attempts owner-only files where supported.

## 12.6 Configuration Hardening

Database (`DatabaseConnection`):
- env-first resolution for DB settings
- required config values must be non-blank
- rejects insecure default (`root` + blank password)

Email (`AppConfig`):
- env-first email sender/app-password
- placeholder detection/fail-fast to avoid accidental insecure deploys

## 12.7 Auditing

- `AuditLogger` records sensitive recovery/password events.
- audit log writes include integrity-centric behavior and failure handling.

## 12.8 Security-Related UX Outcomes

- lockout messaging on repeated bad current-password attempts
- recovery request block messaging on rate-limit or bad state
- immediate session invalidation warnings and logout behavior

## 13. Data Model and Schema

## 13.1 Domain Models

- `ActivityItemData`
- `ActivityLog`
- `ActivityType`
- `Bloc`
- `Conflict`
- `DashboardScope`
- `Departement`
- `Niveau`
- `Reservation`
- `Room`
- `ScheduleEntry`
- `User`

Key enums:
- `User.Role`: `Admin`, `Chef_Departement`, `Enseignant`
- `Reservation.ReservationStatus`: `CONFIRMEE`, `EN_ATTENTE`, `ANNULEE`
- `Reservation.DayOfWeek`
- `Conflict.ConflictType`

## 13.2 Database Assets

Schema files:
- `db/schema.sql`
- `gestion_salles.sql`

Tables:
- `active_sessions`
- `activity_log`
- `audit_log`
- `blocs`
- `departements`
- `historique_reservations`
- `niveaux`
- `reservations`
- `salles`
- `types_activites`
- `utilisateurs`
- `verification_codes`

Stored procedures:
- `verifier_conflit_salle`
- `verifier_conflit_enseignant`
- `verifier_conflit_niveau`

Migration and seed:
- `db/migrations/001_conflict_procedures.sql`
- `db/seed/minimal_seed.sql`

## 14. Full Technical Inventory

## 14.1 DAO Inventory

- `ActivityLogDAO.java`
- `ActivityTypeDAO.java`
- `BlocDAO.java`
- `DashboardDAO.java`
- `DepartementDAO.java`
- `NiveauDAO.java`
- `ReservationConflictException.java`
- `ReservationDAO.java`
- `ReservationMapper.java`
- `RoomDAO.java`
- `ScheduleDAO.java`
- `UserDAO.java`

## 14.2 Service Inventory

- `AuthService.java`
- `ConflictDetectionService.java`
- `EmailService.java`
- `VerificationCodeManager.java`

## 14.3 Utility Inventory

- `AnimatedIconButton.java`
- `AppConfig.java`
- `AuditLogger.java`
- `BarChartPanel.java`
- `DataRefreshListener.java`
- `DialogCallback.java`
- `GeneratePasswordHash.java`
- `GroupSpecificCellPanel.java`
- `HealthCheck.java`
- `PasswordFormHelper.java`
- `PasswordStrengthChecker.java`
- `PasswordUtils.java`
- `RefreshablePanel.java`
- `RoundImage.java`
- `ScheduleGridCellPanel.java`
- `SecretManager.java`
- `SeededUserPasswordSeeder.java`
- `SessionContext.java`
- `SessionException.java`
- `SessionManager.java`
- `ThemeConstants.java`
- `TokenStorage.java`
- `UIUtils.java`

## 14.4 View Inventory

Admin views:
- `views/Admin/BlocDialog.java`
- `views/Admin/BlocManagementPanel.java`
- `views/Admin/BlocTablePrintable.java`
- `views/Admin/Dashboard.java`
- `views/Admin/DashboardOverviewPanel.java`
- `views/Admin/DepartementDialog.java`
- `views/Admin/DepartementManagementPanel.java`
- `views/Admin/DepartementTablePrintable.java`
- `views/Admin/NiveauDialog.java`
- `views/Admin/NiveauManagementPanel.java`
- `views/Admin/NiveauTablePrintable.java`
- `views/Admin/ReservationDialog.java`
- `views/Admin/ReservationManagementPanel.java`
- `views/Admin/ReservationTablePrintable.java`
- `views/Admin/RoomDialog.java`
- `views/Admin/RoomManagementPanel.java`
- `views/Admin/RoomTablePrintable.java`
- `views/Admin/ScheduleTableCellRenderer.java`
- `views/Admin/ScheduleTableModel.java`
- `views/Admin/ScheduleViewerPanel.java`
- `views/Admin/UserDialog.java`
- `views/Admin/UserManagementPanel.java`
- `views/Admin/UserTablePrintable.java`

Chef_Departement views:
- `views/ChefDepartement/DashboardChef.java`
- `views/ChefDepartement/DashboardOverviewPanel.java`
- `views/ChefDepartement/NiveauManagementPanel.java`
- `views/ChefDepartement/ReservationDialog.java`
- `views/ChefDepartement/ReservationManagementPanel.java`
- `views/ChefDepartement/RoomManagementPanel.java`
- `views/ChefDepartement/ScheduleTableCellRenderer.java`
- `views/ChefDepartement/ScheduleViewerPanel.java`
- `views/ChefDepartement/UserManagementPanel.java`

Enseignant views:
- `views/Enseignant/Dashboard.java`
- `views/Enseignant/DashboardOverviewPanel.java`
- `views/Enseignant/MySchedulePanel.java`
- `views/Enseignant/ScheduleTableCellRenderer.java`

Login views:
- `views/Login/ContactAdminDialog.java`
- `views/Login/LoginFrame.java`
- `views/Login/LoginPanel.java`
- `views/Login/PasswordStrengthMeter.java`

Shared views:
- `views/shared/RecentActivityPanel.java`
- `views/shared/dashboard/DashboardFrameBase.java`
- `views/shared/dashboard/DashboardHeaderPanel.java`
- `views/shared/dashboard/DashboardHeaderUpdatable.java`
- `views/shared/dashboard/DashboardOverlayPanel.java`
- `views/shared/dashboard/DashboardOverviewPanelBase.java`
- `views/shared/dashboard/DashboardStatCard.java`
- `views/shared/dashboard/DashboardStatsData.java`
- `views/shared/dashboard/RecentActivityRefreshable.java`
- `views/shared/management/FormValidationUtils.java`
- `views/shared/management/ManagementExportUtils.java`
- `views/shared/management/ManagementHeaderBuilder.java`
- `views/shared/management/TablePrintableBase.java`
- `views/shared/recovery/PasswordRecoveryContainer.java`
- `views/shared/recovery/PasswordRecoveryFlow.java`
- `views/shared/recovery/PasswordRecoveryNavigator.java`
- `views/shared/recovery/PasswordRecoveryStep1Panel.java`
- `views/shared/recovery/PasswordRecoveryStep2Panel.java`
- `views/shared/recovery/PasswordRecoveryStep3Panel.java`
- `views/shared/reservations/ReservationManagementUIHelper.java`
- `views/shared/schedule/SchedulePanelBase.java`
- `views/shared/schedule/ScheduleTableSupport.java`
- `views/shared/schedule/ScheduleUiText.java`
- `views/shared/settings/AccountSettingsPanel.java`
- `views/shared/users/UserManagementUIHelper.java`
- `views/shared/users/UserTableCellRenderers.java`

## 14.5 Test Inventory

- `com/gestion/salles/dao/ReservationDAOTest.java`
- `com/gestion/salles/database/DatabaseConnectionTest.java`
- `com/gestion/salles/services/AuthServiceTest.java`
- `com/gestion/salles/services/ConflictDetectionServiceTest.java`
- `com/gestion/salles/services/VerificationCodeManagerTest.java`
- `com/gestion/salles/views/Admin/DashboardDevTest.java`
- `com/gestion/salles/views/Login/LoginFlowDevTest.java`

## 15. Configuration, Resources, and Scripts

## 15.1 Environment Variables

Database:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_DRIVER`
- `DB_USESSL`

Email:
- `GESTION_SALLES_EMAIL_SENDER`
- `GESTION_SALLES_EMAIL_APP_PASSWORD`

Secrets:
- `GESTION_SALLES_SECRET`
- optional JVM property: `app.verification.secret`

## 15.2 Resource Files

- `src/main/resources/database.properties`
- `src/main/resources/email.properties`
- `src/main/resources/logging.properties`
- `src/main/resources/icons/*`
- `src/main/resources/scripts/gestionsalles.sh`
- `src/main/resources/scripts/health-check.sh`

## 15.3 Root Scripts and Docs

Scripts:
- `scripts/health-check.sh`
- `extract-icons.sh`
- `post-install.sh`

Docs:
- `IMPROVEMENTS.md`
- `APP_DETAILED.md`
- `docs/security-and-session.md`
- `docs/database.md`
- `docs/packaging.md`

## 16. Build, Dependencies, and Packaging

## 16.1 Build Tooling

Maven plugins in use:
- `maven-compiler-plugin`
- `maven-resources-plugin`
- `maven-jar-plugin`
- `maven-shade-plugin`
- `maven-surefire-plugin`
- `exec-maven-plugin`
- profile plugins for `jpackage` + dependency/resource copy

## 16.2 Main Runtime Dependencies

- MySQL JDBC connector
- HikariCP
- jBCrypt
- SLF4J
- FlatLaf (+ extras + Roboto font pack)
- MigLayout
- JavaMail
- Gson
- Jackson
- Apache POI
- PDFBox
- Log4j API/Core
- JCalendar

## 16.3 Artifacts and Profiles

Build artifacts:
- `target/GestionSalles-1.0-SNAPSHOT.jar`
- `target/GestionSalles-1.0-SNAPSHOT-shaded.jar`

Installer profiles:
- `windows-installer` (MSI)
- `linux-installer` (DEB)

## 17. Observability and Health

- logging configured through `logging.properties`
- rolling logs under `logs/`
- `HealthCheck` utility validates DB access + critical table/secret checks
- CLI wrappers provided in `scripts/health-check.sh` and packaged scripts

## 18. Current Known Constraints

- primary persistence is MySQL; local setup must provide valid DB credentials
- some dev tests are UI/dev harness style and not strict CI-grade UI automation
- operational quality depends on correct env-based secret/email configuration

## 19. Document Coverage Statement

This file now combines:
- user journeys and UX behavior
- architecture and layer mapping
- security controls and session model
- schema/procedure inventory
- module/file inventories
- build/dependency/packaging/runtime operations

It is intended to be the single comprehensive app reference in this repository.
