# VOLTARAS — AWS EC2 Deployment

Production-style deployment of the full VOLTARAS platform (MySQL, RabbitMQ,
Eureka, API Gateway, ten microservices, React frontend) on a single AWS EC2
instance using Docker Compose.

## Architecture on EC2

```
Internet
   │  :80 (HTTP)
   ▼
EC2 — frontend nginx container
   │  /api/**  (same-origin proxy, no CORS involved)
   ▼
api-gateway container (inside voltaras-net, no public port)
   ▼
microservices (inside voltaras-net, no public ports)
```

MySQL, RabbitMQ, Eureka and every backend service communicate over the
`voltaras-net` Docker bridge network only. The **only** public port is
`80` (frontend). No internal or database port is published to the host.

## EC2 requirements

| Item | Value |
|---|---|
| OS | Ubuntu Server 24.04 LTS |
| Instance type | `t3.small` (2 vCPU, ~1.9 GiB RAM) — **marginal**, see [Resource honesty](#resource-honesty) |
| Swap | 4 GiB (already configured) |
| Storage | 30 GiB gp3 |
| Software | Docker Engine + Docker Compose v2.24+ (`!override` merge tags), Git |

Install / verify Docker and Compose:

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin git
sudo usermod -aG docker ubuntu   # then log out and back in
docker --version
docker compose version            # must be v2.24.0 or newer
```

## Security group

| Port | Protocol | Source | Purpose |
|---|---|---|---|
| 22 | TCP | **your developer IP only** (e.g. `203.0.113.7/32`) | SSH |
| 80 | TCP | `0.0.0.0/0` | HTTP frontend |
| 443 | TCP | `0.0.0.0/0` | HTTPS — **future task**, not configured yet |

**Do not** open 3306 (MySQL), 5672 / 15672 (RabbitMQ), 8761 (Eureka),
8080 (gateway) or 8081–8089 (microservices) to the public. Those ports are
not even published to the host by the AWS compose override.

## Deployment workflow

```bash
# 1. Clone the repository (already synced with main on the target EC2)
git clone <repository-url> voltaras
cd voltaras

# 2. Create and edit the environment file — do this ON the EC2 server.
#    Never commit real credentials to Git (.env is gitignored).
cp .env.example .env
nano .env

# 3. Validate the resolved configuration
docker compose -f docker-compose.yml -f docker-compose.aws.yml config

# 4. Build images (multi-stage Dockerfiles compile every JAR internally —
#    no Maven or pre-built target/*.jar files are needed on the server)
docker compose -f docker-compose.yml -f docker-compose.aws.yml build

# 5. Start the stack
docker compose -f docker-compose.yml -f docker-compose.aws.yml up -d

# 6. Inspect health and resources
docker compose -f docker-compose.yml -f docker-compose.aws.yml ps
docker stats --no-stream
free -h
```

The frontend is then reachable at:

```
http://<EC2-PUBLIC-IP>
```

SSH example (generic — replace the placeholders):

```bash
ssh -i "<key>.pem" ubuntu@<EC2-PUBLIC-IP>
```

## Environment variables (.env)

`cp .env.example .env` and replace the sample values **directly on EC2**:

- `MYSQL_ROOT_PASSWORD`, `DB_USERNAME`, `DB_PASSWORD`
- `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`
- `JWT_SECRET` — use a strong random value of **at least 64 characters**,
  identical for the gateway and auth-service. Generate one with:
  `openssl rand -base64 48` (then remove line breaks / spaces).
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` —
  required by payment-service at startup (sandbox values are fine for a demo).
- `MAIL_*` / `PASSWORD_RESET_MAIL_PROVIDER` — optional; `console` logs the
  password-reset link instead of sending e-mail.

Never display real secrets in documentation, commits, or logs.

## CORS on AWS

Browser traffic is **same-origin** through nginx (`http://<EC2-PUBLIC-IP>`
serves the SPA and proxies `/api/**` to the gateway), so no `CORS_ALLOWED_ORIGIN_PATTERNS`
entry for the EC2 IP is required. The default localhost patterns remain in
`.env` and are harmless. If a custom domain / HTTPS is added later, revisit
this variable only if browser calls become cross-origin.

## Memory strategy (t3.small)

### JVM settings — every JVM service

Applied through `JAVA_TOOL_OPTIONS` in `docker-compose.aws.yml` (Java honors
this environment variable automatically, no code changes):

```
-Xms64m -Xmx128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC
```

Rationale:

- **`-Xmx128m`** — caps each Spring Boot heap. Typical steady-state heap for
  these demo services is ~80–110 MiB; 128 MiB is a safe ceiling.
- **`-Xms64m`** — pre-allocates the floor so the JVM does not grow/shrink
  aggressively (avoids GC churn on a 2-vCPU box).
- **`-XX:MaxMetaspaceSize=96m`** — caps class metadata; typical usage ~60–80 MiB.
- **`-XX:+UseSerialGC`** — single-threaded collector, lowest memory overhead
  and perfectly adequate for small heaps; G1 reserves more native memory.

No service is given a larger heap: the heaviest services (gateway, auth,
payment, organization) still fit comfortably in 128 MiB for this demo
workload, and uniform settings keep the matrix predictable and maintainable.

### Docker memory ceilings (`mem_limit`)

| Container | `mem_limit` | Notes |
|---|---|---|
| Each JVM service (11) | `512m` | ~2× the expected RSS (~250 MiB) — a hard ceiling, not a reservation |
| mysql | `512m` | with the tuning below, MySQL RSS is ~250–350 MiB |
| rabbitmq | `384m` | default footprint ~150–250 MiB |
| frontend (nginx) | `128m` | static file server |

`mem_limit` is honored by plain `docker compose up` (no Swarm). It only
kills a container that **exceeds** the ceiling, preventing one runaway
service from exhausting the host. `deploy.resources` is intentionally not
used — it is Swarm-oriented and would be ignored by `docker compose up`.

### MySQL tuning (AWS override only)

```yaml
command:
  - mysqld
  - --innodb-buffer-pool-size=64M
  - --performance-schema=OFF
```

- `innodb_buffer_pool_size=64M` — default is 128 MiB; the demo data is tiny.
- `performance-schema=OFF` — saves ~150–200 MiB of RAM, a meaningful saving
  on a 1.9 GiB box. It is a standard, well-supported mysqld flag; it only
  disables runtime performance monitoring.
- `max_connections` is left at the MySQL default (151) — plenty for the
  service Hikari pools.

The data volume (`mysql_data`) and schema are untouched.

### RabbitMQ

Not tuned. Its default footprint is modest (~150–250 MiB), the memory alarm
high-watermark scales with host RAM, and the management plugin stays enabled
for debugging (reachable only inside the Docker network). The 384 MiB
`mem_limit` is the only change.

### Expected total memory

| Component | Expected RSS |
|---|---|
| 11 JVMs × ~250 MiB | ~2.7 GiB |
| MySQL (tuned) | ~300 MiB |
| RabbitMQ | ~200 MiB |
| nginx | ~20 MiB |
| Docker daemon / containerd | ~200 MiB |
| Ubuntu base | ~250 MiB |
| **Total (typical)** | **~3.7 GiB** |

The instance has ~1.9 GiB RAM + 4 GiB swap, so **the stack runs but will
use swap under load**. See [Resource honesty](#resource-honesty).

## Resource monitoring

```bash
free -h                          # RAM + swap usage
docker stats                     # per-container CPU/memory (live)
docker compose -f docker-compose.yml -f docker-compose.aws.yml ps
docker compose -f docker-compose.yml -f docker-compose.aws.yml logs -f <service>
```

### Warning signs

- Swap continuously filling and never draining → memory pressure.
- `docker stats` showing a container pinned at its `mem_limit` → it is about
  to be OOM-killed.
- `OOMKilled` in `docker inspect <container>` / `docker compose ps` status →
  the container hit its ceiling or the host ran out of memory.
- Services restarting in a loop → check `docker compose logs <service>`.
- Very high load average on a 2-vCPU box → CPU saturation, not just memory.
- MySQL / RabbitMQ instability (connections refused, healthcheck failures).

## Swagger / API documentation on AWS

The unified Swagger portal (`http://localhost:5173/swagger.html`, see
`docs/17_API_DOCUMENTATION_PORTAL.md`) links to `http://localhost:8081…8089`,
which only works **locally**. On a remote EC2 instance `localhost` would mean
the visitor's own computer, and the AWS override does **not** publish the
8081–8089 ports — by design.

For remote API documentation, use the public entry point (`http://<EC2-PUBLIC-IP>`)
for normal application traffic. A future task may provide authenticated,
gateway-based remote Swagger access; the internal ports are intentionally
**not** opened for it.

## Resource honesty

The full VOLTARAS stack is **marginal on a t3.small**:

- 11 JVMs alone account for ~2.7 GiB typical RSS — already more than the
  ~1.9 GiB of physical RAM.
- The conservative settings above (small heaps, serial GC, tuned MySQL,
  memory ceilings) make the stack **stable under light demo load**, with
  swap absorbing peaks.
- If monitoring shows the warning signs above, or the platform feels
  unresponsive, **upgrade to `t3.medium` (4 GiB RAM)** — no configuration
  changes are required; the compose file and memory settings carry over.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `docker compose ... config` fails with `!override` | Compose < v2.24 — update `docker-compose-plugin` (`sudo apt-get install -y docker-compose-plugin`) |
| Frontend unreachable on `:80` | Check the security group allows 80 from `0.0.0.0/0` and `docker compose ... ps` shows `frontend` healthy |
| A service is `OOMKilled` | Check `docker stats`; if it is pinned at its `mem_limit`, raise that service's `mem_limit` in `docker-compose.aws.yml` |
| Everything is very slow | Expected on t3.small under load — verify with `free -h` / `docker stats`; consider t3.medium |
| Payment service exits | `RAZORPAY_*` values are required in `.env` |
| DB data missing | Init scripts run only on an empty volume — do **not** run `docker compose down -v` on EC2 unless a fresh database is intended |
