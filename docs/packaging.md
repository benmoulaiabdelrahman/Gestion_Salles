# Packaging Hooks

## `extract-icons.sh`

Purpose:

- extracts a PNG icon from `src/main/resources/icons/app_icon.ico`
- writes the resulting `app_icon.png` to `target/icons/` for packaging tools that need PNG input
- falls back to `University_of_Laghouat_logo_64x64.png` if extraction tooling is unavailable

When to run:

- before Linux packaging when icon conversion is required

## `post-install.sh`

Purpose:

- creates `/usr/share/applications/gestionsalles.desktop` after installation
- resolves the installed executable path dynamically (`gestionsalles` or `GestionSalles`)
- sets `StartupWMClass` and icon path for desktop integration

When to run:

- as a post-install hook in `.deb/.rpm` packaging workflows

## `src/main/resources/scripts/gestionsalles.sh`

Purpose:

- app launcher wrapper for packaged runtime
- loads `GESTION_SALLES_SECRET` from `~/.gestion-salles/app.secret` when env var is absent
- sets Linux AWT/X11 properties used by the desktop app

When to run:

- as the main executable script shipped with the app package

## `src/main/resources/scripts/health-check.sh`

Purpose:

- runs `com.gestion.salles.utils.HealthCheck` from the packaged jar
- validates DB connection, `verification_codes` table presence, and secret availability

Usage:

```bash
./src/main/resources/scripts/health-check.sh GestionSalles-1.0-SNAPSHOT-shaded.jar
```
