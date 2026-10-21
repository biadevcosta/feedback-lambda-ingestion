# feedback-lambda-ingestao

## Projeto
Plataforma de feedback de aulas (Tech Challenge, fase 4), serverless na AWS. O aluno envia `POST /avaliacao` com `{"descricao": string, "nota": int 0 a 10}`. Os administradores recebem e-mail de urgência e um relatório semanal.

## Os 4 repositórios
- feedback-infra: Terraform de toda a infraestrutura e a documentação geral
- feedback-lambda-ingestao: recebe a avaliação e grava no DynamoDB
- feedback-lambda-notificacao: avisa os admins por e-mail quando a urgência é alta
- feedback-lambda-relatorio: gera e envia o relatório semanal

## Convenções (definidas na tarefa 0.5; ajuste aqui se o grupo mudar)
- Região AWS: sa-east-1 (sugestão; confirmar). Prefixo de nomes: `feedback-`
- Funções: `feedback-ingestao`, `feedback-notificacao`, `feedback-relatorio`. Python 3.12
- Grupos do Cognito: `alunos` e `admins`

## Contrato de dados (igual nos 4 repositórios)
Tabela `Avaliacoes`, chave `avaliacaoId` (string, UUID). Atributos: `usuarioId` (sub do Cognito), `descricao`, `nota` (número), `urgencia` (`alta`, `media` ou `baixa`), `dataEnvio` (ISO 8601 em UTC) e `dia` (AAAA-MM-DD no horário de Brasília, UTC-3 fixo).
Índice `por-dia`: chave `dia`, ordenação `dataEnvio`. Urgência (sugestão, editável): nota 0 a 3 alta, 4 a 6 media, 7 a 10 baixa.

## Regras de trabalho
- Uma tarefa por vez do PLANO.md, só deste repositório. Confira "Depende de" antes de começar.
- Ao terminar e validar, marque `[x]` no PLANO.md e resuma em 1 ou 2 frases.
- Nunca commitar segredos, `.env`, `*.tfstate` nem `*.tfvars`. Sem chaves fixas da AWS: a esteira usa OIDC.
- Mudanças por branch e Pull Request (a main é protegida). Commits pequenos, mensagens curtas em português.
- Respostas curtas. Não cole saídas longas (resuma plan, logs e testes).
- Em dúvida entre opções, pergunte antes de decidir.
- LGPD: não registrar e-mails nem o texto das avaliações nos logs.

## Esta função
Recebe `POST /avaliacao` (API Gateway HTTP API, payload v2, JWT já validado pelo authorizer) e grava no DynamoDB.
- Estrutura: `src/handler.py` (`lambda_handler`), `tests/`, `requirements-dev.txt` (pytest, moto, boto3), `.github/workflows/ci-cd.yml` e `README.md`.
- Variável de ambiente (definida no Terraform): `TABLE_NAME`.
- Entrada: body JSON com `descricao` (texto não vazio, máximo de 1000 caracteres, sugestão) e `nota` (inteiro de 0 a 10; rejeitar texto, booleano e decimal).
- `usuarioId` vem de `event["requestContext"]["authorizer"]["jwt"]["claims"]["sub"]`.
- Resposta: 201 com `{"avaliacaoId": ...}`; 400 com mensagem clara se inválido. Nunca devolver stack trace.
- Grava: `avaliacaoId` (uuid4), `usuarioId`, `descricao`, `nota`, `urgencia`, `dataEnvio` e `dia`.
- Permissão da função: só `dynamodb:PutItem`. Usa apenas boto3 (já vem no runtime), então o zip contém só `src/`.
- Esteira `ci-cd.yml`: no PR roda pytest; no push na main roda pytest, zip de `src/` e `aws lambda update-function-code` na função `feedback-ingestao` via OIDC (`permissions: id-token: write`; role na variável de repositório `AWS_ROLE_ARN`).
- Testes (pytest e moto): notas 0, 3, 4, 6, 7 e 10; notas 11 e -1; nota como texto; `descricao` vazia; evento sem `sub`.
