# VOLTARAS Backend — Docker Deployment

Containerized deployment for the complete VOLTARAS backend: MySQL, RabbitMQ,
Eureka Server, API Gateway and all ten microservices on one shared Docker
network.

> API verification is done through **Swagger/OpenAPI** (per project policy),
> not Postman.

## Requirements

- Docker Engine (Compose v2+) — tested with Docker 29.x / Compose v5.x
- Java 25 JDK + Maven (to build the service JARs)

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

All services use Java 25 (`eclipse-temurin:25-jre`). Each Dockerfile is a
single-stage image built from the service's pre-built JAR:

```dockerfile
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/<service>.jar app.jar
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Quick start

```bash
# 1. Build all service JARs (the Dockerfiles copy these)
mvn -f eureka-server/pom.xml clean package -DskipTests
mvn -f api-gateway/pom.xml clean package -DskipTests
# ... repeat for every service (see "Building the JARs" below)

# 2. Create the environment file
cp .env.example .env
#    → edit the sample passwords / secrets in .env

# 3. Build and start the stack
docker compose up -d --build

# 4. Watch health
docker compose ps
```

### Building the JARs

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
docker compose build           # build images (JARs must exist)
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

| Service | Swagger UI |
|---|---|
| auth-service | http://localhost:8081/swagger-ui.html |
| user-service | http://localhost:8082/swagger-ui.html |
| meter-reading-service | http://localhost:8083/swagger-ui.html |
| bill-service | http://localhost:8084/swagger-ui.html |
| organization-service | http://localhost:8085/swagger-ui.html |
| payment-service | http://localhost:8086/swagger-ui.html |
| complaint-service | http://localhost:8087/swagger-ui.html |
| notification-service | http://localhost:8088/swagger-ui.html |
| meter-management-service | http://localhost:8089/swagger-ui.html |

### 3. RabbitMQ management UI

Open <http://localhost:15672> (or `RABBITMQ_MGMT_PORT`), credentials from
`RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS`.

### 4. API smoke test through the gateway (JWT)

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
