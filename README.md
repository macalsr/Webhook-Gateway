# Spring Boot Webhook Processor (Generic Gateway)

A minimal, production-minded webhook ingestion API built with **Java 21** and **Spring Boot 4**.

This project focuses on the “hard parts” of webhooks:
- **Signature verification (HMAC-SHA256)**
- **Replay protection (timestamp window)**
- **Idempotency** (no duplicate processing for the same event)
- **Persistence** (store raw payload + status for auditing and troubleshooting)

It is intentionally **generic**: you can plug any provider (Stripe, Shopify, internal systems, etc.) by configuring a `source` and a shared secret.

---

## Features (MVP)

- `POST /webhooks/{source}`
- Reads the **raw request body** (no reformatting)
- Validates:
  - `X-Timestamp` header (epoch seconds)
  - `X-Signature` header (HMAC-SHA256)
  - replay window (default: **300s**)
- Extracts:
  - `eventKey` (unique id from provider)
  - `payload` (JSON object)
- Stores event in DB with:
  - `source`, `external_event_id`, `payload`, `status`, `received_at`
- Idempotency:
  - Unique constraint on `(source, external_event_id)`
  - Duplicate requests return **200 OK**
  - New requests return **201 Created** + `Location` header

---

## Request Contract

### Endpoint
`POST /webhooks/{source}`

### Required headers
- `X-Timestamp`: epoch seconds (e.g. `1767225900`)
- `X-Signature`: `sha256=<hex>` (or just `<hex>`)

### Body (JSON)
```json
{
  "eventKey": "evt_123",
  "payload": {
    "hello": "world"
  }
}
````

### Signature algorithm

The signature is computed from:

```
message = "<timestamp>.<rawBody>"
signature = HMAC_SHA256(secret, message)  -> hex string
```

---

## Responses

### 201 Created (new event)

* `Location: /webhooks/{source}/{eventKey}`
* JSON response body with stored metadata

### 200 OK (duplicate event)

* Same response body, but indicates the event was already known

### 401 Unauthorized

* Missing/invalid signature
* Invalid timestamp
* Timestamp outside replay window

### 404 Not Found

* Unknown `source` (no configured secret)

### 400 Bad Request

* Invalid JSON
* Missing `eventKey` or `payload`

---

## Database Schema

Table: `webhook_event`

Columns:

* `id` (UUID, PK)
* `source` (varchar)
* `external_event_id` (varchar)  ✅ unique with source
* `payload` (text)
* `status` (varchar)
* `received_at` (timestamp)
* `processed_at` (timestamp, nullable)

Constraints:

* Unique: `(source, external_event_id)` (idempotency)

---

## Project Structure (high level)

```
com.mariaribeiro.webhookprocessor
├── config/                    # properties, clock, etc
└── webhook/
    ├── api/                   # controllers + DTOs
    ├── application/           # use cases / services
    ├── domain/                # pure domain model + exceptions
    ├── infrastructure/        # crypto + persistence adapters
    └── port/                  # ports (interfaces)
```

---

## Running Locally

### Option A: Run with Docker (recommended)

1. Start Postgres:

```bash
docker compose up -d
```

2. Run the app (local profile):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

### Option B: Run with H2 (tests/dev)

The test profile uses H2 to run integration tests without containers.

---

## Quick Test with curl

Example:

```bash
RAW='{"eventKey":"evt_123","payload":{"hello":"world"}}'
TS=1767225900
SECRET='secret-123'

# You can compute signature using any tool. Example in Python:
SIG=$(python - <<'PY'
import hmac, hashlib
secret = b"secret-123"
ts = "1767225900"
raw = '{"eventKey":"evt_123","payload":{"hello":"world"}}'
msg = f"{ts}.{raw}".encode()
print(hmac.new(secret, msg, hashlib.sha256).hexdigest())
PY
)

curl -i -X POST "http://localhost:8080/webhooks/stripe" \
  -H "Content-Type: application/json" \
  -H "X-Timestamp: $TS" \
  -H "X-Signature: sha256=$SIG" \
  --data "$RAW"
```

---

## Notes / Design Decisions

* **Raw body is used for signature validation** to avoid JSON reformatting issues.
* **Replay protection** blocks old or future timestamps beyond the configured window.
* **Idempotency** is enforced at the database level via a unique constraint.
* The system stores webhook events for **auditability** and **debugging**.
* Processing/dispatching to external systems is intentionally out of scope for the MVP.

---

## Next Improvements (Roadmap)

* Async processing pipeline (queue + worker)
* Retry/backoff and error categorization
* Per-source adapters (provider-specific extractors)
* Dead-letter handling / quarantine
* Admin endpoints to query events by status/source/date
* Metrics + structured logging (correlation IDs)

---

## Tech Stack

* Java 21
* Spring Boot 4
* Spring Web (REST)
* Spring Data JPA
* Liquibase (migrations)
* PostgreSQL (local via Docker)
* H2 (test profile)
* JUnit 5 + MockMvc for tests
