# Run the backend with **AWS RDS PostgreSQL** (no Docker)

Your Spring Boot app only needs a reachable PostgreSQL database. With **Amazon RDS**, Postgres runs in AWS; you still run the JAR or IDE on your PC and connect over the network.

## 1. Create RDS (AWS Console)

1. Open **RDS** → **Create database**.
2. Engine: **PostgreSQL** (15 or 16).
3. Templates: **Free tier** (optional, for learning).
4. **DB instance identifier**: e.g. `connto-db`.
5. **Master username** / **Master password**: choose values and store them securely (you will map them to `DB_USER` / `DB_PASSWORD`, or create an app user below).
6. **Instance configuration**: smallest class is fine for dev (`db.t4g.micro` / `db.t3.micro`).
7. **Storage**: defaults are OK.
8. **Connectivity**:
   - **Public access**: **Yes** (simplest for dev from your laptop; tighten for production).
   - **VPC security group**: create new or use existing — **inbound rule**: **PostgreSQL (5432)** from **My IP** (not `0.0.0.0/0` unless you accept the risk).
9. **Initial database name**: `connto` (matches default `DB_NAME`).

Create the database and wait until status is **Available**. Copy the **endpoint** hostname (e.g. `connto-db.xxxxxxxxxxxx.us-east-1.rds.amazonaws.com`).

## 1b. Create RDS with AWS CLI (PowerShell)

Prerequisites: **AWS CLI v2** configured (`aws configure` or `aws login`), and IAM rights for EC2 (VPC/subnets/SG) and RDS.

From the repo:

```powershell
cd C:\Users\mahender.kandagatla\Documents\connto-app\infra
.\create-rds-connto.ps1 -MasterPassword 'YourStr0ng!Secret'
```

Optional: `-Region ap-south-1` (default), `-MyIpCidr 203.0.113.10/32` if auto IP detection fails, `-EngineVersion 16.4` to pin a version (default lets AWS choose).

The script uses the **default VPC**, builds a **DB subnet group** (two AZs), creates a **security group** allowing **5432 from your public IP**, then creates **`connto-db`** with database **`connto`**. It prints **`$env:DB_*`** lines at the end.

**Security:** the master password is passed on the command line (visible in shell history). Prefer a throwaway dev password or type it via `Read-Host -AsSecureString` in a custom wrapper.

## 2. (Optional) App user

You can use the master user for dev, or in RDS run SQL:

```sql
CREATE USER connto WITH PASSWORD 'choose-a-strong-password';
GRANT ALL PRIVILEGES ON DATABASE connto TO connto;
```

For schema ownership after first migration, you may use master first, or grant on `public` as needed.

## 3. Environment variables (Windows PowerShell example)

Run these in the same session before starting the app, or set them in your IDE **Run configuration**:

```powershell
$env:DB_HOST = "connto-db.xxxxxxxxxxxx.us-east-1.rds.amazonaws.com"
$env:DB_PORT = "5432"
$env:DB_NAME = "connto"
$env:DB_USER = "connto"
$env:DB_PASSWORD = "your-password"
$env:DB_JDBC_PARAMS = "?sslmode=require"
$env:JWT_SECRET = "use-a-long-random-secret-at-least-32-chars"
```

Then start the backend from `backend` (Maven/IDE).

**Why `DB_JDBC_PARAMS`?** RDS expects TLS; `sslmode=require` enables encryption on the JDBC URL.

## 4. Verify

- From your machine, port **5432** to the RDS endpoint must be allowed in the **security group**.
- If connection still fails, check: wrong endpoint, security group, corporate firewall, or **Public access = No** without VPN.

## 5. Production notes

- Prefer **Private access** + VPN or bastion, not public RDS.
- Use **Secrets Manager** for `DB_PASSWORD` and `JWT_SECRET`.
- Restrict security groups to your app subnets or bastion only.
