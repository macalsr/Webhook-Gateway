# Webhook Gateway / Processor (Spring Boot + PostgreSQL)

Gateway genérico para receber webhooks de múltiplas fontes (ex.: `shopify`, `trustedform`, `generic`) com:
- validação de assinatura (HMAC-SHA256)
- persistência do evento bruto (raw)
- rastreabilidade (status, timestamps)
- base para processamento assíncrono (futuro)

> Objetivo: ser um “ponto de entrada” confiável e audível para webhooks, sem acoplar o core à regra específica de cada provider.

---

## 1) Conceitos e Negócio

### Por que genérico?
Webhooks variam muito por provider, mas o *core* de entrada é praticamente sempre o mesmo:
- autenticar (assinatura)
- registrar o evento recebido
- responder rápido (ACK)
- processar depois (quando necessário)

O sistema trata o payload como JSON bruto (`payload_json`) e usa `source` para direcionar regras específicas futuramente.

### Fontes (sources)
Lista inicial (pode crescer):
- `shopify`
- `trustedform`
- `generic` (default / testes)

---

## 2) Contrato mínimo do Evento (persistido)

Campos mínimos a persistir:

| Campo | Tipo | Descrição |
|---|---|---|
| `event_id` | UUID/String | ID interno do evento (gerado no recebimento) |
| `source` | String | Origem do webhook (`shopify`, `trustedform`, etc.) |
| `payload_json` | JSON/Text | Body recebido (raw) |
| `received_at` | Timestamp | Momento do recebimento |
| `status` | String | Estado de processamento (`RECEIVED`, `PROCESSED`, `FAILED`, etc.) |

### Status sugeridos
- `RECEIVED` — persistido com sucesso
- `VALIDATION_FAILED` — assinatura inválida / payload inválido
- `PROCESSING` — em processamento (futuro)
- `PROCESSED` — processado (futuro)
- `FAILED` — falha (futuro)
- `DEAD_LETTER` — excedeu tentativas (futuro)

---

## 3) Endpoint (Contrato HTTP)

### Receber webhook
`POST /webhooks/{source}`

**Path params**
- `source`: string (ex.: `shopify`, `trustedform`, `generic`)

**Headers**
- `Content-Type: application/json`
- `X-Signature: <hex do HMAC-SHA256 do body>` (ver assinatura)

**Body**
- JSON livre (depende do provider). O sistema **não valida schema do provider** neste estágio.

**Respostas**
- `202 Accepted`: evento aceito e persistido
- `401 Unauthorized`: assinatura inválida
- `400 Bad Request`: payload inválido (não parseável, etc.)
- `404 Not Found`: source não suportado (opcional, se você quiser restringir)

---

## 4) Assinatura (HMAC-SHA256)

### Regra
- O sender calcula `HMAC-SHA256` do **body raw** (string exata enviada).
- Usa um segredo compartilhado por `source`.
- Envia o resultado no header: `X-Signature`.

**Formato recomendado da assinatura**
- hex lowercase (ex.: `a3f1...`)

### Pseudocódigo (sender)
```

signature = HEX( HMAC_SHA256(secret, raw_body_bytes) )
send header: X-Signature: signature

```

### Importante
- Assinatura deve ser calculada sobre o **body bruto**, não sobre “JSON reformatado”.
- No servidor, compare em **constant time** (evitar timing attack).

### Segredos por source
Configurar algo assim:
- `WEBHOOK_SECRET_SHOPIFY`
- `WEBHOOK_SECRET_TRUSTEDFORM`
- `WEBHOOK_SECRET_GENERIC`

---

## 5) Banco de dados (PostgreSQL)

### Tabela sugerida: `webhook_event`
Campos típicos:
- `event_id` (PK)
- `source`
- `payload_json`
- `received_at`
- `status`
- (opcional) `signature`
- (opcional) `headers_json`
- (opcional) `error_message`

> Mesmo mantendo só os “mínimos”, vale muito guardar `headers_json` e `signature` pra debug/auditoria.

---

## 6) Migrations (Liquibase)

Este projeto usa **Liquibase** para versionar schema.

### Estrutura esperada
```

src/main/resources/db/changelog/db.changelog-master.xml
src/main/resources/db/changelog/changes/001-init.xml

````

No startup, o Liquibase cria:
- `databasechangelog`
- `databasechangeloglock`
- suas tabelas de domínio (ex.: `webhook_event`)

---

## 7) Rodando local (Docker + App)

### Subir o Postgres
Na pasta onde está seu `docker-compose.yml`:
```bash
docker compose up -d
docker compose ps
````

### Rodar a aplicação (profile local)

Pelo IntelliJ:

* VM options / env: `-Dspring.profiles.active=local`

Ou via Maven:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 8) Configuração (application-local)

Exemplo (YAML):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/webhook
    username: webhook
    password: webhook
    driver-class-name: org.postgresql.Driver

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: public

  jpa:
    hibernate:
      ddl-auto: none

logging:
  level:
    liquibase: INFO
    org.springframework.jdbc: INFO
    com.zaxxer.hikari: INFO
```

---

## 9) Exemplo de request (cURL)

### Exemplo para source `generic`

```bash
curl -X POST "http://localhost:8080/webhooks/generic" \
  -H "Content-Type: application/json" \
  -H "X-Signature: <hex_hmac_sha256_do_body>" \
  -d '{
    "type": "user.created",
    "id": "abc-123",
    "payload": { "name": "Maria" }
  }'
```

Resposta esperada:

* `202 Accepted`

---

## 10) Checklist do “Definition of Done” (MVP)

* [ ] Endpoint `POST /webhooks/{source}` criado
* [ ] Validação `X-Signature` com HMAC-SHA256 (por source)
* [ ] Persistência do evento raw em `webhook_event`
* [ ] `status=RECEIVED` ao persistir
* [ ] Retorno `202` ao aceitar
* [ ] Liquibase criando tabelas automaticamente no startup
* [ ] Healthcheck no actuator (`/actuator/health`)

---

## 11) Próximos incrementos (futuro)

* Processamento assíncrono (fila / scheduler)
* Retry com backoff
* Dead-letter status
* Idempotência (deduplicação por provider-id)
* Normalização por source (adapters)
* Observabilidade (correlation id, métricas)