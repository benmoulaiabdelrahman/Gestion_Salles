# Security and Session Configuration

## Required Environment Variables

Set these before starting the application in production:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_DRIVER` (example: `com.mysql.cj.jdbc.Driver`)
- `DB_USESSL` (`true` recommended)
- `GESTION_SALLES_EMAIL_SENDER`
- `GESTION_SALLES_EMAIL_APP_PASSWORD`
- `GESTION_SALLES_SECRET` (optional but recommended to control verification secret)

The app will fail fast if:

- email config is missing or uses placeholder values
- DB config is missing
- DB uses insecure defaults (`root` + blank password)

## Local Secret Files

When generated automatically, these files are stored with owner-only permissions (`600` on POSIX systems):

- `~/.gestion-salles/app.secret`
- `~/.GestionSalles/session.token`

If broader permissions are detected, the app logs a warning and attempts to restrict access.

## Session Policy

Current policy is **single active session per email**:

- each new login invalidates previous sessions for the same user (`active_sessions`)
- DAO write operations require authenticated thread-local context
- the context now cross-checks DB session validity (when session token is available), reducing stale-thread access risk
