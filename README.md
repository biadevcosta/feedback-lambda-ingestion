# feedback-lambda-ingestao

Lambda de **ingestão** da Plataforma de Feedback de Aulas (Tech Challenge fase 4).

Recebe `POST /avaliacao` pelo API Gateway (HTTP API, payload v2, JWT já validado pelo authorizer do Cognito) e grava a avaliação no DynamoDB.

- **Função AWS:** `feedback-ingestao` · **Runtime:** Python 3.12 · **Região:** `sa-east-1`
- **Permissão:** apenas `dynamodb:PutItem` na tabela `Avaliacoes`
- **Variável de ambiente:** `TABLE_NAME`

## Entrada

```json
{ "descricao": "texto não vazio, até 1000 caracteres", "nota": 7 }
```

`usuarioId` vem de `event.requestContext.authorizer.jwt.claims.sub`.

## Saída

- `201` → `{"avaliacaoId": "..."}`
- `400` → mensagem clara de validação (nunca stack trace)

## Regra de urgência

| Nota | Urgência |
|---|---|
| 0 a 3 | `alta` |
| 4 a 6 | `media` |
| 7 a 10 | `baixa` |

## Estrutura (em construção)

```
src/handler.py          # lambda_handler
tests/
requirements-dev.txt    # pytest, moto, boto3
.github/workflows/ci-cd.yml
```

## Repositórios relacionados

`feedback-infra` · `feedback-lambda-notificacao` · `feedback-lambda-relatorio`

## Status

Acompanhe o andamento em [PLANO.md](PLANO.md).
