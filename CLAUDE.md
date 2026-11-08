# feedback-lambda-ingestion

## Projeto
Plataforma de feedback de aulas (Tech Challenge, fase 4), serverless na AWS. O aluno envia `POST /evaluations` com `{"description": string, "score": int 0 a 10}`. Os administradores recebem e-mail de urgência e um relatório semanal.

## Os 4 repositórios
- feedback-infra: Terraform de toda a infraestrutura e a documentação geral
- feedback-lambda-ingestion: recebe a avaliação e grava no DynamoDB
- feedback-lambda-notification: avisa os admins por e-mail quando a urgência é alta
- feedback-lambda-report: gera e envia o relatório semanal

## Convenções (definidas na tarefa 0.5; ajuste aqui se o grupo mudar)
- Região AWS: sa-east-1 (São Paulo) - CONFIRMADA. Prefixo de nomes: `feedback-`
- Funções: `feedback-ingestion`, `feedback-notification`, `feedback-report`. Java 21 (Maven), AWS SDK v2
- Grupos do Cognito: `students` e `admins`
- Idioma: código Java, comentários, nomes de variáveis/recursos de código e README em inglês. CLAUDE.md e PLANO.md continuam em português.
- Custos: a conta AWS **não tem free tier**. Antes de pedir pra criar/alterar qualquer recurso na AWS (fora do Terraform da infra, ex.: testar algo manualmente), avise o custo estimado e peça confirmação.
- Economia de requisições (orçamento de US$ 4/mês, sem dinheiro para excedente): o projeto é para entregar e rodar poucas vezes. Testes contra a AWS real só com o mínimo de chamadas, uma por vez e de forma manual. Proibido teste de carga, loops, scripts repetitivos ou agendamentos extras de teste. Testes automatizados (`mvn test`) usam mocks e nunca chamam a AWS. Antes de qualquer chamada real à API ou à AWS, diga quantas chamadas serão feitas.

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
- Arquitetura e testes: seguir a skill `java-lambda-standard` (hexagonal: `domain/`, `application/`, `adapter/in/apigateway/`, `adapter/out/dynamodb/`, `config/`). `IngestionHandler.java` na raiz do pacote só faz o bootstrap (é a classe que o Terraform referencia). Build com `./mvnw verify` (testes, ArchUnit e JaCoCo com mínimo de 80%).
- Dependências: `aws-lambda-java-core`, `aws-lambda-java-events`, AWS SDK v2 (`dynamodb` com `url-connection-client`), `jackson-databind` (só no adaptador de entrada), `maven-shade-plugin` (gera o uber jar do deploy). Testes: JUnit5, Mockito, AssertJ, ArchUnit e `aws-lambda-java-serialization`.
- Variável de ambiente (definida no Terraform): `TABLE_NAME`.
- Entrada: body JSON com `description` (texto não vazio, máximo de 1000 caracteres, sugestão) e `score` (inteiro de 0 a 10; rejeitar texto, booleano e decimal).
- `userId` vem de `event.getRequestContext().getAuthorizer().getJwt().getClaims().get("sub")`.
- Resposta: 201 com `{"evaluationId": ...}`; 400 com mensagem clara se inválido. Nunca devolver stack trace.
- Grava: `evaluationId` (`UUID.randomUUID()`), `userId`, `description`, `score`, `urgency`, `submittedAt` e `day`.
- Permissão da função: só `dynamodb:PutItem`. Dependências mínimas para manter o jar pequeno (custo de cold start).
- Esteira `ci-cd.yml`: no PR roda `mvn test`; no push na main roda `mvn test`, `mvn package` (jar via shade) e `aws lambda update-function-code` na função `feedback-ingestion` via OIDC (`permissions: id-token: write`; role na variável de repositório `AWS_ROLE_ARN`).
- Testes por camada (padrão da skill): domínio puro (scores 0 a 10 e limites, `description` vazia/longa, `day` em UTC-3); caso de uso com o repositório mockado; mapper e controller com eventos reais de `src/test/resources/events/` (score como texto, booleano e decimal, evento sem `sub`, erro 500 sem detalhes, logs sem dados pessoais); repositório com `DynamoDbClient` mockado.
