# VOLTARAS — Docker Deployment

Containerized deployment for the complete VOLTARAS platform: MySQL, RabbitMQ,
Eureka Server, API Gateway, all ten microservices **and the React/Vite
frontend** on one shared Docker network.

> API verification is done through **Swagger/OpenAPI** (per project policy),
> not Postman.

## Requirements

- Docker Engine (Compose v2+) — tested with Docker 29.x / Compose v5.x
- No pre-built JARs are required: every backend Dockerfile is a multi-stage
  build that compiles its own executable JAR with Maven inside the image.
  A Java 25 JDK + Maven on the host is only needed for local development,
  running the test suite, or `mvn clean verify`.

## Stack overview

| Container          | Image                          | Port(s)                          |
|--------------------|--------------------------------|----------------------------------|
| mysql              | `mysql:8.0`                    | `${MYSQL_HOST_PORT:-3306}`       |
| rabbitmq           | `rabbitmq:3.13-management-alpine` | `${RABBITMQ_AMQP_PORT:-5672}`, `${RABBITMQ_MGMT_PORT:-15672}` |
| eureka-server      | `voltaras/eureka-server`       | 8761                             |
| api-gateway        | `voltaras/api-gateway`         | 8080                             |
| auth-service       | `voltaras/auth-service`        | 8081                             |
| user-service       | `voltaras/user-service`        | 8082                             |
| meter-reading-service | `voltaras/meter-reading-service` | 8083                          |
| bill-service       | `voltaras/bill-service`        | 8084                             |
| organization-service | `voltaras/organization-service` | 8085                           |
| payment-service    | `voltaras/payment-service`     | 8086                             |
| complaint-service  | `voltaras/complaint-service`   | 8087                             |
| notification-service | `voltaras/notification-service` | 8088                           |
| meter-management-service | `voltaras/meter-management-service` | 8089                |
| frontend                | `voltaras/frontend`                | 5173 (nginx :80)   |

The frontend is a two-stage image: `node:22-alpine` builds the Vite app with
`npm ci && npm run build`, then `nginx:1.27-alpine` serves `dist/` with SPA
fallback and proxies `/api/**` to the `api-gateway` container (same-origin,
no CORS involved). It is built with `VITE_API_BASE_URL=""` so the SPA calls
`/api/...` on its own origin.

All services use Java 25. Each backend Dockerfile is a **multi-stage build**
(identical to the `organization-service` pattern):

```dockerfile
# Stage 1: build — Maven compiles the executable JAR inside the image
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B      # cached dependency layer
COPY src ./src
RUN mvn clean package -DskipTests -B  # tests are run by CI / mvn verify

# Stage 2: runtime — slim JRE, non-root user
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd -r voltaras && useradd -r -g voltaras voltaras
COPY --from=build /app/target/<service>.jar app.jar
USER voltaras
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

This means a **fresh Git clone can be deployed with `docker compose up -d
--build` directly** — no `target/*.jar` files need to exist on the host first.
Each backend directory ships a `.dockerignore` that excludes `target/` (and
never `src/`) so the build context stays clean.

## Quick start

```bash
# 1. Create the environment file
cp .env.example .env
#    → edit the sample passwords / secrets in .env

# 2. Build and start the whole stack (backend + frontend)
#    Multi-stage Dockerfiles build every service JAR internally, so no
#    Maven step is needed on the host.
docker compose up -d --build

# 3. Watch health
docker compose ps
```

> The frontend container depends on a healthy `api-gateway`, so starting the
> stack (`docker compose up -d`) brings up the whole platform and the app is
> served at <http://localhost:5173>. To start only the frontend against an
> already-running backend: `docker compose up -d frontend`.

### Building the JARs (host-side, optional)

The Docker images compile their own JARs, so this is **not** required for
`docker compose build`. Host-side Maven is only needed for local development
or to run the test suite (CI does `mvn --batch-mode clean verify` per service):

```bash
for s in eureka-server api-gateway auth-service user-service \
         meter-reading-service meter-management-service bill-service \
         organization-service payment-service complaint-service notification-service; do
  mvn -f $s/pom.xml clean package -DskipTests
done
```

### Useful commands

```bash
docker compose config          # validate the compose file
docker compose build           # build images (JARs are built inside each image)
docker compose up -d           # start all containers
docker compose ps              # container status
docker compose logs -f <svc>   # follow one service's logs
docker compose down            # stop (keeps named volumes)
docker compose down -v         # stop and delete volumes (fresh DB)
```

## Environment variables

All configuration lives in `.env` (copy of `.env.example`). Values used by the
stack:

| Variable | Purpose |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root password (required) |
| `DB_USERNAME` / `DB_PASSWORD` | Application DB user used by every service |
| `MYSQL_HOST_PORT` | MySQL host port (default `3306`) |
| `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS` | RabbitMQ credentials |
| `RABBITMQ_AMQP_PORT` / `RABBITMQ_MGMT_PORT` | RabbitMQ host ports |
| `JWT_SECRET` | Shared JWT secret (gateway + auth-service, ≥ 32 bytes) |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Allowed browser origins for the gateway |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP for auth-service password reset (optional) |
| `PASSWORD_RESET_MAIL_PROVIDER` | `smtp` or `console` (logs the reset link) |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` / `RAZORPAY_WEBHOOK_SECRET` | Razorpay sandbox credentials (payment-service requires them at startup) |

> `.env` is gitignored. Never commit real credentials. If a local MySQL or
> RabbitMQ already occupies the default ports, change `MYSQL_HOST_PORT` /
> `RABBITMQ_AMQP_PORT` / `RABBITMQ_MGMT_PORT` in `.env`.

## Databases

On first start, `docker/mysql/init/01-create-voltaras-databases.sql` creates
all nine databases and grants the application user:

`auth_db`, `user_db`, `meter_db`, `meter_management_db`, `bill_db`,
`organization_db`, `payment_db`, `complaint_db`, `notification_db`

Tables are created by each service at startup (`ddl-auto: update`).

## Container-to-container configuration

| Setting | In-Docker value |
|---|---|
| Eureka URL | `http://eureka-server:8761/eureka/` |
| MySQL host | `mysql` (port `3306`) |
| RabbitMQ host | `rabbitmq` (port `5672`) |
| Redis | not used by any service — skipped (see below) |

Services whose `application.yml` hardcodes `localhost` (auth, user,
meter-reading, meter-management, bill) are overridden **through compose
environment variables** (`SPRING_DATASOURCE_URL`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`)
— no service config was rewritten.

## Verification

### 1. Eureka dashboard

Open <http://localhost:8761> — all ten services plus the gateway should be
registered: `API-GATEWAY`, `AUTH-SERVICE`, `USER-SERVICE`,
`METER-READING-SERVICE`, `METER-MANAGEMENT-SERVICE`, `ORGANIZATION-SERVICE`,
`BILL-SERVICE`, `PAYMENT-SERVICE`, `NOTIFICATION-SERVICE`, `COMPLAINT-SERVICE`.

### 2. Swagger / OpenAPI (direct per service)

> **Unified portal:** open <http://localhost:5173/swagger.html> to reach every
> service's Swagger UI from one page (see `docs/17_API_DOCUMENTATION_PORTAL.md`).
> The links below are the direct per-service URLs the portal points to.

| Service | Swagger UI |
|---|---|
| auth-service | http://localhost:8081/swagger-ui/index.html |
| user-service | http://localhost:8082/swagger-ui/index.html |
| meter-reading-service | http://localhost:8083/swagger-ui/index.html |
| bill-service | http://localhost:8084/swagger-ui/index.html |
| organization-service | http://localhost:8085/swagger-ui/index.html |
| payment-service | http://localhost:8086/swagger-ui/index.html |
| complaint-service | http://localhost:8087/swagger-ui/index.html |
| notification-service | http://localhost:8088/swagger-ui/index.html |
| meter-management-service | http://localhost:8089/swagger-ui/index.html |

### 3. RabbitMQ management UI

Open <http://localhost:15672> (or `RABBITMQ_MGMT_PORT`), credentials from
`RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS`.

### 4. Frontend app

Open <http://localhost:5173> — the production React app. Logins, dashboard,
bills, wallet and admin flows all call `/api/**` on the same origin, which
nginx proxies to the gateway, so everything works from a single URL.

To keep using the dev server instead (hot reload), run `npm run dev` in
`frontend/` and open the URL Vite prints (it auto-picks another port, e.g.
5174, while the container holds 5173). The dev server calls the gateway
directly on `http://localhost:8080`.

### 5. API smoke test through the gateway (JWT)

```bash
# Register a consumer
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Test User","email":"docker.test@gmail.com","phone":"9000000001",
       "password":"DockerTest1234","confirmPassword":"DockerTest1234","address":"Addr"}'

# Login and capture the access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"docker.test@gmail.com","password":"DockerTest1234"}' \
  | python -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')

# Complaint categories (public, no auth)
curl http://localhost:8080/api/complaints/categories

# Create a complaint with the CONSUMER token
curl -X POST http://localhost:8080/api/complaints \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"category":"BILLING","subject":"High bill","description":"Please review my latest bill."}'
```

For an ADMIN token: register a second user and promote it to `ADMIN` in
`auth_db` (add a row in `user_roles` referencing the `ADMIN` role), then use
that token for an admin-protected API such as `GET /api/admin/complaints`.

## Demo data (30 demo users)

For local API/Swagger verification the stack ships with an idempotent demo
seeder that creates 30 demo users (1 ADMIN + 29 CONSUMER), profiles, a demo
organization with memberships, meters, verified meter readings, bills, wallet
payments and complaints — through the API Gateway, with no real data and no
hard-coded secrets. See `docs/15_DOCKER_DEMO_DATA.md` for full details.

```bash
# Windows PowerShell (from the repository root)
powershell -ExecutionPolicy Bypass -File docker/seed/seed-docker-demo-data.ps1

# bash / Git Bash / WSL
bash docker/seed/seed-docker-demo-data.sh
```

Common password for all demo users: `Voltaras@123`

- Admin: `sunny.demo@voltaras.local`
- Consumer: `vinay.demo@voltaras.local` (and 28 more `<name>.demo@voltaras.local`)

The seeder is safe to re-run (idempotent) and never touches the existing
`docker.consumer.test@gmail.com` / `docker.admin.test@gmail.com` accounts.

## AWS EC2 deployment

For a production-style deployment on AWS EC2 (public frontend on port 80,
no internal ports exposed, conservative memory limits for a small instance)
use the AWS compose override:

```bash
docker compose -f docker-compose.yml -f docker-compose.aws.yml up -d --build
```

See `docs/18_AWS_EC2_DEPLOYMENT.md` for the full EC2 guide.

## Redis — intentionally skipped

No VOLTARAS service declares a Redis dependency or configuration
(`redis` has **zero matches** in `pom.xml` / `application.yml` across the
repository). Redis only appears in roadmap/future plans, so it is **not**
added to the stack. If a future service needs caching, add a `redis` service
(with `redis_data` volume) and wire only that service to it.

## Legacy `backend/` module — excluded

The `backend/` module is the original monolith (port 8084 — same as
bill-service) and is **not** part of this microservice deployment. It is not
built or containerized here. Note: `backend/src/main/resources/application.yaml`
contains a hardcoded database password — it should be externalized before the
module is ever used again.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Service restarts / unhealthy | `docker compose logs <svc>` — typical cause: MySQL/RabbitMQ not ready yet (they recover on restart) |
| MySQL init not applied | Init scripts run only on an empty volume → `docker compose down -v` and up again |
| Payment service exits | `RAZORPAY_*` env vars are required at startup — set sandbox values in `.env` |
| Port already in use | Local services occupy the port → stop them, or remap host ports in `.env` |
| Auth service can't send mail | SMTP is optional; set `PASSWORD_RESET_MAIL_PROVIDER=console` to log reset links |
