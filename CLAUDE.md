# feedback-lambda-ingestao

## Projeto
Plataforma de feedback de aulas (Tech Challenge, fase 4), serverless na AWS. O aluno envia `POST /evaluations` com `{"description": string, "score": int 0 a 10}`. Os administradores recebem e-mail de urgência e um relatório semanal.

## Os 4 repositórios
- feedback-infra: Terraform de toda a infraestrutura e a documentação geral
- feedback-lambda-ingestao: recebe a avaliação e grava no DynamoDB
- feedback-lambda-notificacao: avisa os admins por e-mail quando a urgência é alta
- feedback-lambda-relatorio: gera e envia o relatório semanal

## Convenções (definidas na tarefa 0.5; ajuste aqui se o grupo mudar)
- Região AWS: sa-east-1 (São Paulo) - CONFIRMADA. Prefixo de nomes: `feedback-`
- Funções: `feedback-ingestao`, `feedback-notificacao`, `feedback-relatorio`. Java 21 (Maven), AWS SDK v2
- Grupos do Cognito: `students` e `admins`
- Idioma: código Java, comentários, nomes de variáveis/recursos de código e README em inglês. CLAUDE.md e PLANO.md continuam em português.
- Custos: a conta AWS **não tem free tier**. Antes de pedir pra criar/alterar qualquer recurso na AWS (fora do Terraform da infra, ex.: testar algo manualmente), avise o custo estimado e peça confirmação.

## Contrato de dados (igual nos 4 repositórios)
Tabela `Evaluations`, chave `evaluationId` (string, UUID). Atributos: `userId` (sub do Cognito), `description`, `score` (número), `urgency` (`high`, `medium` ou `low`), `submittedAt` (ISO 8601 em UTC) e `day` (AAAA-MM-DD no horário de Brasília, UTC-3 fixo).
Índice `by-day`: chave `day`, ordenação `submittedAt`. Urgência (sugestão, editável): score 0 a 3 high, 4 a 6 medium, 7 a 10 low.

## Regras de trabalho
- Uma tarefa por vez do PLANO.md, só deste repositório. Confira "Depende de" antes de começar.
- Ao terminar e validar, marque `[x]` no PLANO.md e resuma em 1 ou 2 frases.
- Nunca commitar segredos, `.env`, `*.tfstate` nem `*.tfvars`. Sem chaves fixas da AWS: a esteira usa OIDC.
- Mudanças por branch e Pull Request (a main é protegida). Commits pequenos, mensagens curtas em português.
- Respostas curtas. Não cole saídas longas (resuma plan, logs e testes).
- Em dúvida entre opções, pergunte antes de decidir.
- LGPD: não registrar e-mails nem o texto das avaliações nos logs.

## Esta função
Recebe `POST /evaluations` (API Gateway HTTP API, payload v2, JWT já validado pelo authorizer) e grava no DynamoDB.
- Estrutura: `pom.xml`, `src/main/java/com/feedback/ingestao/IngestionHandler.java` (implementa `RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>`), `src/test/java/...`, `.github/workflows/ci-cd.yml` e `README.md`.
- Dependências: `aws-lambda-java-core`, `aws-lambda-java-events`, AWS SDK v2 (`dynamodb`), `maven-shade-plugin` (gera o uber jar do deploy).
- Variável de ambiente (definida no Terraform): `TABLE_NAME`.
- Entrada: body JSON com `description` (texto não vazio, máximo de 1000 caracteres, sugestão) e `score` (inteiro de 0 a 10; rejeitar texto, booleano e decimal).
- `userId` vem de `event.getRequestContext().getAuthorizer().getJwt().getClaims().get("sub")`.
- Resposta: 201 com `{"evaluationId": ...}`; 400 com mensagem clara se inválido. Nunca devolver stack trace.
- Grava: `evaluationId` (`UUID.randomUUID()`), `userId`, `description`, `score`, `urgency`, `submittedAt` e `day`.
- Permissão da função: só `dynamodb:PutItem`. Dependências mínimas para manter o jar pequeno (custo de cold start).
- Esteira `ci-cd.yml`: no PR roda `mvn test`; no push na main roda `mvn test`, `mvn package` (jar via shade) e `aws lambda update-function-code` na função `feedback-ingestao` via OIDC (`permissions: id-token: write`; role na variável de repositório `AWS_ROLE_ARN`).
- Testes (JUnit5 + Mockito, mockando `DynamoDbClient`): scores 0, 3, 4, 6, 7 e 10; scores 11 e -1; score como texto; `description` vazia; evento sem `sub`.
