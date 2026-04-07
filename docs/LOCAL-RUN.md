# Run the backend locally (Postgres + secrets)

## Option A — Environment variables only

No extra files. Set `DB_HOST`, `DB_PASSWORD`, `DB_JDBC_PARAMS`, etc., as in [AWS-POSTGRES.md](./AWS-POSTGRES.md), then start the app (IDE or `mvn spring-boot:run`).

## Option B — `application-local.yml` (recommended for daily dev)

1. Copy the example file:

   `backend/src/main/resources/application-local.example.yml`  
   → `backend/src/main/resources/application-local.yml`

2. Edit `application-local.yml`: RDS URL, `password`, `connto.jwt.secret`.  
   That file is **gitignored** — it will not be committed.

3. Activate Spring profile **`local`**:

   - **IDE**: Run configuration → *Active profiles* → `local`
   - **Maven**: `mvn spring-boot:run -Dspring-boot.run.profiles=local`
   - **PowerShell**: `backend/run-local.ps1` (requires `mvn` on `PATH`)

`application-local.yml` overrides the same keys from `application.yml` (datasource, JWT, OTP dev flag, logging).
