# VOLTARAS — Unified Swagger / API Documentation Portal

One page to reach the Swagger / OpenAPI UI of **every** VOLTARAS API service.

**Main URL:** <http://localhost:5173/swagger.html>

```
Developer
    |
    v
http://localhost:5173/swagger.html
    |
    +--> Auth Service Swagger
    +--> User Service Swagger
    +--> Meter Management Swagger
    +--> Meter Reading Swagger
    +--> Organization Swagger
    +--> Bill Swagger
    +--> Payment Swagger
    +--> Complaint Swagger
    +--> Notification Swagger
```

---

## 1. Why the portal exists

Every VOLTARAS microservice exposes its own Springdoc Swagger UI on its own
port (`8081..8089`). Before the portal, developers had to remember nine
different URLs:

```text
http://localhost:8081/swagger-ui/index.html   (auth)
http://localhost:8082/swagger-ui/index.html   (user)
...
http://localhost:8089/swagger-ui/index.html   (meter-management)
```

The portal replaces that with **one memorable URL** and a card per service.
It is a pure navigation/discovery page: it does **not** copy or duplicate any
API endpoint definition. The Swagger/OpenAPI documentation still comes from
the real Spring Boot services, so it always reflects the running code.

## 2. Services included

| Service | Identifier | Port | Swagger UI | OpenAPI JSON |
|---|---|---|---|---|
| Authentication Service | `auth-service` | 8081 | `http://localhost:8081/swagger-ui/index.html` | `http://localhost:8081/v3/api-docs` |
| User Service | `user-service` | 8082 | `http://localhost:8082/swagger-ui/index.html` | `http://localhost:8082/v3/api-docs` |
| Meter Reading Service | `meter-reading-service` | 8083 | `http://localhost:8083/swagger-ui/index.html` | `http://localhost:8083/v3/api-docs` |
| Bill Service | `bill-service` | 8084 | `http://localhost:8084/swagger-ui/index.html` | `http://localhost:8084/v3/api-docs` |
| Organization Service | `organization-service` | 8085 | `http://localhost:8085/swagger-ui/index.html` | `http://localhost:8085/v3/api-docs` |
| Payment Service | `payment-service` | 8086 | `http://localhost:8086/swagger-ui/index.html` | `http://localhost:8086/v3/api-docs` |
| Complaint Service | `complaint-service` | 8087 | `http://localhost:8087/swagger-ui/index.html` | `http://localhost:8087/v3/api-docs` |
| Notification Service | `notification-service` | 8088 | `http://localhost:8088/swagger-ui/index.html` | `http://localhost:8088/v3/api-docs` |
| Meter Management Service | `meter-management-service` | 8089 | `http://localhost:8089/swagger-ui/index.html` | `http://localhost:8089/v3/api-docs` |

**Not included** (no Swagger / no REST API surface):

- **API Gateway (8080)** — infrastructure entry point only; it has no
  Springdoc dependency and exposes no REST controllers of its own. All
  business traffic is routed through it at `/api/**`.
- **Eureka Server (8761)** — service registry; it has a web dashboard, not a
  Swagger-documented REST API.
- **MySQL / RabbitMQ** — infrastructure, documented elsewhere.

## 3. How the Swagger links are resolved

Each card links to the service's own published host port:

```text
http://localhost:<port>/swagger-ui/index.html
http://localhost:<port>/v3/api-docs
```

- **Docker:** `docker-compose.yml` publishes every service port to the host
  (`8081:8081` … `8089:8089`), so `http://localhost:808X/...` resolves from
  the browser. Docker-internal hostnames such as `bill-service:8084` are
  **never** used in browser links — they only work inside the Docker network.
- **Local development:** the services run on the same localhost ports, so the
  exact same links work.

The portal page itself is a static file in the frontend build
(`frontend/public/swagger.html`), served by nginx in Docker and by Vite in
dev mode — no backend code, gateway routes, or nginx proxy changes are
required.

## 4. Behaviour in Docker

```bash
docker compose up -d
```

Then open <http://localhost:5173/swagger.html> — the portal is served by the
frontend nginx container as a static file (`/usr/share/nginx/html/swagger.html`),
and every service card opens the Swagger UI of the corresponding container via
its published host port.

## 5. Authentication in Swagger ("Authorize")

Protected APIs are documented with the `bearerAuth` security scheme. To call
them from Swagger UI:

1. Open any service's Swagger UI (from the portal).
2. Click **Authorize** (top-right, lock icon).
3. Paste the JWT access token returned by `POST /api/auth/login`
   (only the token value — Swagger adds the `Bearer ` prefix).
4. Execute protected operations; the token is sent as
   `Authorization: Bearer <token>`.

Demo credentials (Docker demo stack):

- Admin: `sunny.demo@voltaras.local` / `Voltaras@123`
- Consumer: `vinay.demo@voltaras.local` / `Voltaras@123`

> The Swagger pages of services that configure an OpenAPI `Server` list the
> API Gateway (`http://localhost:8080`) as the preferred request server, so
> **Try It Out** requests go through the Gateway and keep JWT validation
> centralized. Public endpoints (login, register, forgot-password, etc.) work
> without an Authorize token.

## 6. Try It Out

Swagger's **Try It Out** executes real HTTP requests against the running
service (via the Gateway where configured). Safe GET endpoints can be tested
without side effects; write operations (POST/PATCH/DELETE) modify real data,
so prefer the demo data seeded by `docker/seed/seed-docker-demo-data.sh`.

## 7. Files

| File | Purpose |
|---|---|
| `frontend/public/swagger.html` | The portal page (plain HTML/CSS/JS, no framework, no dependencies) |
| `docs/17_API_DOCUMENTATION_PORTAL.md` | This document |

## 8. Related documents

- `docs/14_DOCKER_DEPLOYMENT.md` — how to build and start the whole stack.
- `docs/15_DOCKER_DEMO_DATA.md` — demo users/data for API verification.
