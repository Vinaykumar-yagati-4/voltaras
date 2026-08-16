# VOLTARAS — CI/CD Pipeline (Phase 1: CI)

Continuous Integration for the VOLTARAS platform, implemented as a
**GitHub Actions** workflow: `.github/workflows/ci.yml`.

> This is **Phase 1 (CI validation only)**. There is no deployment job yet —
> no image publishing, no cloud/SSH deployment, no production credentials.
> Choosing the deployment environment and building the CD stage is a later
> phase (see the roadmap at the end).

---

## 1. What CI does

Every relevant push and every pull request targeting `main` is validated with
three job groups:

```text
Push / Pull Request
        │
   ┌────┴────┐
   ↓         ↓
Backend    Frontend
Matrix       CI
   │         │
   └────┬────┘
        ↓
Docker Validation
        ↓
CI PASS / FAIL
```

| Job | What it runs |
|---|---|
| `backend` (matrix × 11) | `mvn --batch-mode clean verify` for every Maven service on **Java 25 (Temurin)**. Compiles, runs the unit/web/repository tests, and packages each JAR. |
| `frontend` | `npm ci` → `npm run lint` → `npm run build` on **Node 22 (LTS)** — the same version used by the production Docker image (`node:22-alpine`). |
| `docker-validation` | `docker compose config --quiet` (with CI-only placeholder env values, since a fresh runner has no `.env`) and a `docker build` of the **frontend** image to prove the Dockerized app still builds. |

The Docker validation job only runs **after** the backend matrix and the
frontend job succeed (`needs:`), so failed builds do not waste runner time.

## 2. When CI runs

Defined by the workflow triggers:

```yaml
on:
  push:
    branches: [main, 'feature/**']
  pull_request:
    branches: [main]
```

- **Push** to `main` or any `feature/*` branch → full CI.
- **Pull request** opened/synchronized **targeting `main`** → full CI.

Obsolescent runs are cancelled automatically: pushing twice in a row to the
same branch cancels the first run (`concurrency.cancel-in-progress: true`),
so only the latest commit on a ref is fully validated.

## 3. Backend matrix builds

All 11 Maven projects are covered by one matrix job — no duplicated YAML:

```text
api-gateway            auth-service           bill-service
complaint-service      eureka-server          meter-management-service
meter-reading-service  notification-service   organization-service
payment-service        user-service
```

Each matrix entry:

1. Checks out the repository.
2. Sets up **Java 25** (Eclipse Temurin — an actively supported OpenJDK
   distribution that ships Java 25 LTS).
3. Enables Maven dependency caching (`cache: maven` on `setup-java`), so
   `~/.m2` is shared across runs and services.
4. Runs `mvn --batch-mode clean verify` in the service's own directory.

`fail-fast: false` means one failing service does **not** cancel the others;
each service reports its own PASS/FAIL in the job list.

**Why `clean verify` works without infrastructure:** every service's test
configuration runs against an in-memory **H2 database** (MySQL mode) with
**Eureka disabled** and RabbitMQ either mocked or started lazily. No MySQL,
RabbitMQ, Eureka, or sibling microservices are needed to run the tests, so
Phase 1 validates compile + tests + packaging without starting the stack.

**Maven Wrapper:** the repository has no `mvnw` / `mvnw.cmd` / `.mvn/`
directories in any service, so the workflow uses the Maven installation that
ships with the GitHub-hosted Ubuntu runners (`mvn`). No wrapper was added —
the existing Maven/Spring Boot configuration of each service is unchanged.

## 4. Frontend lint/build

The frontend job runs in `frontend/` using the scripts that actually exist in
`frontend/package.json`:

```bash
npm ci          # install from the committed package-lock.json
npm run lint    # eslint .
npm run build   # tsc -b && vite build (production build)
```

Node 22 is used to match the production `frontend/Dockerfile`
(`node:22-alpine`). The build must produce a working `dist/`.

## 5. Docker validation

On a fresh GitHub Actions runner there is **no `.env` file**. The Compose
file interpolates `${VAR:?required}` for several variables, so the workflow
provides **CI-only placeholder values** through the job `env` block so that
`docker compose config --quiet` can parse the file:

| Variable | CI placeholder (non-production) |
|---|---|
| `MYSQL_ROOT_PASSWORD` | `ci-placeholder-root-password` |
| `DB_PASSWORD` | `ci-placeholder-db-password` |
| `RABBITMQ_DEFAULT_PASS` | `ci-placeholder-rabbit-password` |
| `JWT_SECRET` | `ci-placeholder-jwt-secret-for-compose-validation-only` |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` / `RAZORPAY_WEBHOOK_SECRET` | `ci-placeholder-*` |

These values are only used to parse/validate Compose — nothing is started,
and no real credentials are involved.

**Why only the frontend image is built:** the frontend Dockerfile is
self-contained (multi-stage: `npm ci` + `npm run build` inside the image, then
nginx). The backend service Dockerfiles are single-stage images that
`COPY target/<service>.jar` — the JARs are produced by the backend matrix job
on **separate, ephemeral runners**, so rebuilding every backend image in the
Docker job would re-run the full Maven build there. The matrix `mvn clean
verify` already guarantees the JARs build, and `docker compose config --quiet`
validates the Compose wiring, so building the frontend image plus validating
Compose is a reasonable Phase 1 Docker strategy that keeps CI time in check.

## 6. How to interpret PASS / FAIL

- **All green** → the platform compiles, all unit/web/repository tests pass,
  all JARs package, the frontend lints and builds, and the Compose file +
  frontend image are valid.
- **A backend matrix entry fails** → the failing job is named
  `backend (<service>)`, e.g. `backend (payment-service)`. Open its logs; the
  failing test/compile error is in the `mvn` step output.
- **The frontend job fails** → either lint errors (check the `Lint` step) or
  a production build error (check the `Production build` step).
- **The docker validation job is skipped** → at least one backend service or
  the frontend already failed; fix that first.

## 7. Where GitHub Actions results appear

1. Repository page → **Actions** tab → the `VOLTARAS CI` workflow.
2. Each run shows the three jobs (`backend` matrix, `frontend`,
   `docker-validation`) with PASS/FAIL status and collapsible step logs.
3. Pull requests targeting `main` show the workflow result in the PR
   **checks** section (required status checks can be configured later in
   branch protection rules).

## 8. Security

The workflow is validation-only:

- `permissions: contents: read` — no write permissions.
- No secrets are defined, referenced, or committed (`secrets:` is never used).
- The repository `.env` is gitignored and never committed; CI placeholders
  are inline, non-production values used only for Compose parsing.
- No Docker registry credentials, no cloud deployment keys, no Razorpay
  production keys.

Future deployment secrets should be configured as **GitHub Actions Secrets**
in a later phase — never in workflow files.

## 9. Local verification (before pushing)

```bash
# Backend — every service (Java 25 + Maven)
for s in eureka-server api-gateway auth-service user-service \
         meter-reading-service meter-management-service bill-service \
         organization-service payment-service complaint-service notification-service; do
  (cd $s && mvn --batch-mode clean verify) || echo "FAILED: $s"
done

# Frontend
npm --prefix frontend run lint
npm --prefix frontend run build

# Docker (needs .env or the required variables exported)
docker compose config --quiet
```

## 10. Roadmap

```text
Dockerization        DONE
        ↓
CI validation        THIS PHASE
        ↓
CI stable on GitHub
        ↓
Choose deployment environment
        ↓
CD/deployment        (not part of this phase)
        ↓
Production verification
```
