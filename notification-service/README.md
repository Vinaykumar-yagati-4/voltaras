# VOLTARAS Notification Service

## Purpose

The **Notification Service** stores in-app notifications for VOLTARAS users
and exposes REST APIs to view and manage them. Notifications are created in
two ways:

1. **Event-driven** – the service consumes RabbitMQ domain events published
   by the other VOLTARAS services (bill generated, payment success, recharge
   success, complaint status changed) and converts each event into a stored
   notification.
2. **Manual** – an **ADMIN** can create a manual notification for any user
   through `POST /api/notifications/admin`.

The service is responsible for:

- Storing notifications in MySQL (`notification_db`)
- Consuming RabbitMQ events through four durable queues bound to the
  `voltaras.notification.exchange` topic exchange
- Serving user notification APIs (`GET /api/notifications`,
  `GET /api/notifications/unread`, `PATCH /api/notifications/{id}/read`,
  `PATCH /api/notifications/read-all`,
  `GET /api/notifications/count/unread`)
- Serving ADMIN-only notification APIs (`POST /api/notifications/admin`,
  `GET /api/notifications/admin/user/{authUserId}`)
- Enforcing authorization through the identity headers injected by the
  API Gateway (`X-User-Id`, `X-User-Role`)

## Port

- **Default port:** `8088`
- Configurable via `server.port` in `application.yml` or runtime argument
  `--server.port=<port>`.

## Architecture

```
API Gateway (8080) ── JWT validation, injects X-User-Id / X-User-Role
        │
        ▼
Notification Service (8088) ── MySQL notification_db
        │
        ├── REST APIs  ──► users view/read notifications, ADMIN creates manuals
        │
        └── RabbitMQ listeners (voltaras.notification.exchange)
                 ├── voltaras.bill.generated.queue      (notification.bill.generated)
                 ├── voltaras.payment.success.queue     (notification.payment.success)
                 ├── voltaras.recharge.success.queue    (notification.recharge.success)
                 └── voltaras.complaint.status.queue    (notification.complaint.status)
```

Inter-service communication: the Notification Service is a pure **consumer**
of RabbitMQ events. The producer services (Bill, Payment, Complaint, ...)
publish JSON events to the exchange with the routing keys listed above; this
service never calls other services synchronously.

## RabbitMQ Topology

| Component | Name | Routing key |
|-----------|------|-------------|
| Exchange (durable topic) | `voltaras.notification.exchange` | – |
| Queue | `voltaras.bill.generated.queue` | `notification.bill.generated` |
| Queue | `voltaras.payment.success.queue` | `notification.payment.success` |
| Queue | `voltaras.recharge.success.queue` | `notification.recharge.success` |
| Queue | `voltaras.complaint.status.queue` | `notification.complaint.status` |

Events are JSON-serialized (`Jackson2JsonMessageConverter`):

- `BillGeneratedEvent` → `BILL_GENERATED` notification
- `PaymentCompletedEvent` → `PAYMENT_SUCCESS` notification
- `RechargeSuccessfulEvent` → `RECHARGE_SUCCESS` notification
- `ComplaintStatusChangedEvent` → `COMPLAINT_STATUS_UPDATED` notification

Transient failures are retried in the container (3 attempts); after retries
are exhausted the message is dropped (`default-requeue-rejected: false`) so
poison messages never loop forever.

## Database

- **Name:** `notification_db` (MySQL), created with
  `CREATE DATABASE IF NOT EXISTS notification_db;`
- Schema is managed with `spring.jpa.hibernate.ddl-auto=update` like the
  other services.
- Table: `notifications` with indexes on
  `auth_user_id`, `(auth_user_id, status)`, `status` and `created_at`.

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET`  | `/api/notifications` | User | Get my notifications (newest first) |
| `GET`  | `/api/notifications/unread` | User | Get my unread notifications |
| `PATCH`| `/api/notifications/{id}/read` | User | Mark one notification as read (owner only) |
| `PATCH`| `/api/notifications/read-all` | User | Mark all my notifications as read |
| `GET`  | `/api/notifications/count/unread` | User | Get my unread count (badge) |
| `POST` | `/api/notifications/admin` | ADMIN | Create a manual notification |
| `GET`  | `/api/notifications/admin/user/{authUserId}` | ADMIN | View notifications of a user |

## Security Rules

- Authentication is performed by the API Gateway (JWT). The Gateway injects
  `X-User-Id` and `X-User-Role` headers; this service never trusts
  client-supplied user IDs.
- User APIs scope every read/update to the authenticated `X-User-Id`:
  marking a notification owned by another user returns `404 RESOURCE_NOT_FOUND`.
- Admin APIs require `X-User-Role = ADMIN` (or `ROLE_ADMIN`, consistent with
  the other VOLTARAS services); any other role receives
  `403 ACCESS_DENIED`.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `notification_db` | MySQL database name |
| `NOTIFICATION_DB_USERNAME` | `root` | MySQL username |
| `NOTIFICATION_DB_PASSWORD` | *(required)* | MySQL password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka server URL |
| `JPA_DDL_AUTO` | `update` | Hibernate DDL mode |
| `RABBITMQ_LISTENER_AUTO_STARTUP` | `true` | Set `false` when RabbitMQ is not running |

## Run Command

```bash
# 1. Start MySQL, RabbitMQ and Eureka, then create the database
CREATE DATABASE IF NOT EXISTS notification_db;

# 2. Start the service
cd notification-service
mvn spring-boot:run
```

## Swagger

- Direct: `http://localhost:8088/swagger-ui/index.html` (also served at
  `http://localhost:8088/swagger-ui.html`)
- API calls can be executed through the Gateway route
  `/api/notifications/**`.

## Test Command

```bash
cd notification-service
mvn clean test
```

Tests run against an in-memory H2 database (MySQL mode) with Eureka and
RabbitMQ listeners disabled.
