# Database Schema and Seed

## Files

- `db/schema.sql`: sanitized schema-only package (no real user/session/audit data)
- `db/migrations/001_conflict_procedures.sql`: stored procedures required by runtime conflict checks
- `db/seed/minimal_seed.sql`: minimal non-sensitive fixtures for local/dev
- `gestion_salles.sql`: schema package mirror kept at repository root for compatibility

## Required Procedures

The runtime calls these procedures from DAO/service code:

- `verifier_conflit_salle`
- `verifier_conflit_enseignant`
- `verifier_conflit_niveau`

These are included in `db/schema.sql` and in migration form under `db/migrations/`.

## Apply Order

1. Apply `db/schema.sql`.
2. Optionally apply `db/seed/minimal_seed.sql` for local fixtures.

## Security Note

The previous SQL dump included operational data (sessions/audit history/user records).
This repository now keeps schema and seed data separate to avoid sensitive data exposure.
