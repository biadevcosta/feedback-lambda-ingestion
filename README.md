# feedback-lambda-ingestion

**Ingestion** Lambda for the Class Feedback Platform (Tech Challenge phase 4).

Receives `POST /evaluations` through API Gateway (HTTP API, payload v2, JWT already validated by the Cognito authorizer) and writes the evaluation to DynamoDB.

- **AWS function:** `feedback-ingestion` · **Runtime:** Java 21 (Maven) · **Region:** `sa-east-1`
- **Permission:** only `dynamodb:PutItem` on the `Evaluations` table
- **Environment variable:** `TABLE_NAME`

## Request

```json
{ "description": "non-empty text, up to 1000 characters", "score": 7 }
```

`userId` comes from `event.getRequestContext().getAuthorizer().getJwt().getClaims().get("sub")`.

## Response

- `201` → `{"evaluationId": "..."}`
- `400` → clear validation message (never a stack trace)

## Urgency rule

| Score | Urgency |
|---|---|
| 0 to 3 | `high` |
| 4 to 6 | `medium` |
| 7 to 10 | `low` |

## Structure (in progress)

```
pom.xml
src/main/java/com/feedback/ingestion/IngestionHandler.java
src/test/java/...              # JUnit5 + Mockito
.github/workflows/ci-cd.yml
```

## Related repositories

`feedback-infra` · `feedback-lambda-notification` · `feedback-lambda-report`

## Status

Track progress in [PLANO.md](PLANO.md).
