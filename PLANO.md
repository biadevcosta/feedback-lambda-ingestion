# Plano - feedback-lambda-ingestao

Tarefas extraídas da planilha de planejamento. Siga a ordem e respeite "Depende de".
Ao validar uma tarefa, troque `[ ]` por `[x]`.

Como pedir ao Claude: `Execute a tarefa X.Y do PLANO.md` (uma por sessão; use /clear entre tarefas).

## Tarefas deste repositório

### Fase 3 - Ingestão

- [ ] **3.5** handler.py: validar descricao e nota (0 a 10), calcular a urgência, ler o usuarioId do token e gravar no DynamoDB
  - Validar: Teste local passa com entrada válida e inválida
  - Depende de: 0.5
  - Obs: Sugestão de urgência: nota 0-3 alta, 4-6 média, 7-10 baixa

- [ ] **3.6** Testes unitários (pytest) para a validação e a regra de urgência
  - Validar: pytest verde
  - Depende de: 3.5

- [ ] **3.7** Workflow de CI/CD: testes, zip e aws lambda update-function-code via OIDC
  - Validar: Push na main atualiza a função
  - Depende de: 3.4, 3.6
  - Obs: Este workflow serve de modelo para os outros dois repositórios

### Fase 7 - Documentação e entrega

- [ ] **7.3** README da função: entrada, saída, variáveis de ambiente, permissões e como testar
  - Validar: README completo
  - Depende de: 3.8

## Pré-requisitos em outros repositórios (não fazer aqui)

Confirme com o time que estas tarefas já foram concluídas antes de começar.

- **0.5** (grupo): Definir convenções: região, prefixo de nomes (feedback-), tags e nomes das 3 funções
- **3.4** (feedback-infra): Role OIDC do repositório feedback-lambda-ingestao, só com lambda:UpdateFunctionCode nesta função
- **3.8** (grupo): Teste ponta a ponta: sem token (401), com token (grava) e nota fora do intervalo (400)
