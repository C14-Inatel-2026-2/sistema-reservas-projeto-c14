# Sistema de Reservas

API REST para reserva de recursos de uma instituição de ensino: salas de estudo, quadras e equipamentos de laboratório. Usuários (alunos, professores e funcionários) reservam um recurso por um período; o sistema impede conflitos de horário e aplica regras de cancelamento definidas por recurso.

Projeto da disciplina C14 – Engenharia de Software (Inatel, 2026/2). O projeto tem apenas backend.

## Tecnologias

- Java 21, Spring Boot 4, Maven (wrapper incluso)
- MySQL 8 e Flyway (migrations)
- Spring Data JPA, Bean Validation, Lombok
- JUnit 5, Mockito, JaCoCo
- Jenkins (CI/CD)

## Estrutura

```
backend/
  src/main/java/com/reservas/sistemareservas/
    controller/   endpoints REST
    service/      regras de negócio
    repository/   acesso a dados (Spring Data JPA)
    entity/       entidades JPA
    dto/          objetos de entrada e saída da API
    exception/    exceções e tratamento global de erros
  src/main/resources/
    application.properties
    db/migration/ scripts Flyway
  src/test/       testes unitários
Jenkinsfile       pipeline de CI/CD
```

## Como executar

Pré-requisitos: JDK 21 e MySQL rodando em `localhost:3306`. O banco `sistema_reservas` é criado automaticamente, e as tabelas são criadas pelo Flyway na primeira execução.

As credenciais do banco são lidas das variáveis `DB_USERNAME` e `DB_PASSWORD` (padrão: `root`/`root`).

```bash
cd backend
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run      # Windows
```

A API sobe em `http://localhost:8080`.

## Testes

```bash
cd backend
./mvnw test        # Linux/macOS
.\mvnw.cmd test    # Windows
```

O relatório de cobertura é gerado em `backend/target/site/jacoco/index.html`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/usuarios` | Cadastra usuário |
| GET | `/usuarios` | Lista usuários |
| GET | `/usuarios/{id}` | Busca usuário |
| POST | `/recursos/salas` | Cadastra sala de estudo |
| POST | `/recursos/quadras` | Cadastra quadra |
| POST | `/recursos/equipamentos` | Cadastra equipamento de laboratório |
| GET | `/recursos` | Lista recursos |
| GET | `/recursos/{id}` | Busca recurso |
| PUT | `/recursos/{id}/regra-cancelamento` | Define a regra de cancelamento do recurso |
| GET | `/recursos/{id}/regra-cancelamento` | Consulta a regra de cancelamento |
| POST | `/reservas` | Cria reserva |
| GET | `/reservas?usuarioId=&recursoId=` | Lista reservas (filtros opcionais) |
| GET | `/reservas/{id}` | Busca reserva |
| PATCH | `/reservas/{id}/confirmar` | Confirma reserva pendente |
| PATCH | `/reservas/{id}/cancelar` | Cancela reserva (corpo opcional: `{"motivo": "..."}`) |

Exemplo de criação de reserva:

POST /reservas

```json
{
  "usuarioId": 1,
  "recursoId": 2,
  "dataHoraInicio": "2026-11-05T14:00:00",
  "dataHoraFim": "2026-11-05T16:00:00"
}
```

Erros seguem o formato `{"status": 422, "mensagem": "...", "detalhes": []}`.

## Requisitos funcionais

| ID | Requisito | Status |
|---|---|---|
| RF01 | Cadastrar usuário com nome, e-mail, senha (mínimo 6 caracteres) e tipo (aluno, professor ou funcionário). | Implementado |
| RF02 | Impedir o cadastro de dois usuários com o mesmo e-mail. | Implementado |
| RF03 | Listar usuários e buscar usuário por id, sem expor a senha. | Implementado |
| RF04 | Cadastrar recursos de três tipos: sala de estudo (capacidade, projetor), quadra (modalidade, coberta) e equipamento de laboratório (laboratório, número de patrimônio). | Implementado |
| RF05 | Impedir o cadastro de dois equipamentos com o mesmo número de patrimônio. | Implementado |
| RF06 | Listar recursos e buscar recurso por id. | Implementado |
| RF07 | Definir e consultar a regra de cancelamento de um recurso: antecedência mínima em horas, percentual de multa (0 a 100) e se o cancelamento é permitido. | Implementado |
| RF08 | Criar reserva de um recurso para um usuário, com data e hora de início e fim. | Implementado |
| RF09 | Rejeitar reserva com fim igual ou anterior ao início, ou com início no passado. | Implementado |
| RF10 | Rejeitar reserva que se sobreponha a outra reserva não cancelada do mesmo recurso. Os períodos são tratados como [início, fim), então uma reserva pode começar no horário em que outra termina. | Implementado |
| RF11 | Criar toda reserva com status PENDENTE e permitir a confirmação apenas de reservas pendentes. | Implementado |
| RF12 | Cancelar reserva com motivo opcional, exceto quando ela já estiver cancelada ou quando o recurso não permitir cancelamento. | Implementado |
| RF13 | Aplicar o percentual de multa da regra quando o cancelamento ocorrer com antecedência menor que a mínima. | Implementado |
| RF14 | Listar reservas, com filtro opcional por usuário e/ou recurso, e buscar reserva por id. | Implementado |
| RF15 | Autenticar usuários e permitir que apenas o dono da reserva a cancele. | Pendente |
| RF16 | Ativar e desativar recursos; recursos inativos não aceitam novas reservas. | Pendente |
| RF17 | Impedir o cancelamento de reservas já iniciadas ou encerradas. | Pendente |
| RF18 | Editar e excluir usuários, recursos e reservas. | Pendente |

## Requisitos não funcionais

| ID | Requisito | Verificação | Status |
|---|---|---|---|
| RNF01 | Backend em Java 21 com Spring Boot 4, build com Maven via wrapper. | `backend/pom.xml` | Atendido |
| RNF02 | Banco MySQL com esquema versionado por migrations Flyway. | `db/migration/` | Atendido |
| RNF03 | API REST com JSON e códigos HTTP padronizados: 400 (validação), 404 (não encontrado), 409 (conflito), 422 (regra de negócio). | `GlobalExceptionHandler` | Atendido |
| RNF04 | Erros tratados pelo `GlobalExceptionHandler` no formato `{status, mensagem, detalhes}`. | `ErroResponse` | Atendido |
| RNF05 | Sinalizar o build como instável quando a cobertura de linhas ficar abaixo de 40%. | Quality gate no Jenkins | Configurado; percentual atual depende do relatório do build |
| RNF06 | Automatizar build, testes, publicação de cobertura e deploy pelo Jenkins. | `Jenkinsfile` | Implementado; execução depende do agente Jenkins |
| RNF07 | Credenciais do banco configuráveis por variáveis de ambiente (`DB_USERNAME`, `DB_PASSWORD`). | `application.properties` | Atendido |
| RNF08 | Senhas armazenadas com hash (BCrypt), nunca em texto puro. | — | Pendente |

## CI/CD

A pipeline está no `Jenkinsfile` e roda em um agente Windows:

1. **Build:** compila e gera o `.jar`.
2. **Testes:** executa os testes e publica os resultados do JUnit.
3. **Cobertura:** publica o relatório do JaCoCo e aplica o quality gate de 40% de linhas.
4. **Deploy:** sobe a aplicação na porta 8081 e verifica se ela responde.

## Gestão do projeto

[Quadro no Jira](https://reservas-c14.atlassian.net/jira/software/projects/SCRUM/boards/1) · [Backlog](https://reservas-c14.atlassian.net/jira/software/projects/SCRUM/boards/1/backlog)

O quadro foi organizado em 30/10/2026 com Scrum adaptado à equipe de dois integrantes. A sprint atual, **Sprint E5**, vai de 30/10 a 13/11/2026. Seu objetivo é consolidar a documentação da E5, vincular código e testes e revisar os critérios de aceite das funcionalidades existentes. As melhorias novas ficam no backlog.

Vinícius organiza o Jira, os requisitos e a documentação; Raphael mantém a pipeline e participa da revisão. Ambos desenvolvem e revisam as alterações do outro. Para formalizar os papéis do Scrum, a proposta deste PR é Vinícius como Scrum Master e Raphael como Product Owner, acumulando o papel de desenvolvedores. Essa divisão precisa ser confirmada pelo grupo na revisão.

O planejamento seleciona os itens no início da sprint. O acompanhamento ocorre pelo quadro e pelo chat do grupo, com atualização a cada mudança de status e registro dos impedimentos. A revisão do incremento e a retrospectiva ficam para o encerramento da sprint. O histórico dessas práticas começa com a adoção do Jira; as funcionalidades anteriores foram relacionadas ao código retrospectivamente.

Fluxo de trabalho:

- **To Do:** item planejado, aguardando execução.
- **In Progress:** implementação, documentação ou validação em andamento.
- **In Review:** aguardando revisão de PR ou validação dos critérios de aceite.
- **Done:** critérios verificados e alteração integrada.

Para iniciar um item (DoR), a história deve ter objetivo, critérios de aceite, prioridade, requisito relacionado e responsável, com dependências identificadas. Para concluir (DoD), os critérios devem ser atendidos, os testes relevantes devem passar, a documentação deve estar atualizada e o PR deve ser aprovado por outro integrante e integrado. Alterações de código também precisam passar pela pipeline.

Cada mudança usa uma branch e um PR; o outro integrante revisa, aprova e faz o merge. O PR deve citar a chave do Jira, por exemplo `SCRUM-8`. O cartão recebe o link do PR e dos testes correspondentes.

O acompanhamento considera quantidade de itens por status, itens concluídos por sprint e tempo entre **In Progress** e **Done**, a partir do histórico do Jira. Ainda não há sprint concluída nem medição histórica de tempo de ciclo.

### Histórias e rastreabilidade

Situação em 30/10/2026. O Jira concentra os critérios de aceite e o status atualizado.

| História | Requisitos | Situação | Código e testes |
|---|---|---|---|
| [US01 / SCRUM-5 — Reservar recurso](https://reservas-c14.atlassian.net/browse/SCRUM-5) | RF08–RF10 | In Review; funcionalidade existente, validação parcial | [PR #1](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/1), [PR #2](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/2), [API](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/b760cab), [ReservaServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/ReservaServiceTest.java), [ValidadorHorarioReservaTest](backend/src/test/java/com/reservas/sistemareservas/service/ValidadorHorarioReservaTest.java) e [testes de limites](backend/src/test/java/com/reservas/sistemareservas/service/ValidadorHorarioReservaLimitesTest.java) |
| [US02 / SCRUM-6 — Cancelar reserva](https://reservas-c14.atlassian.net/browse/SCRUM-6) | RF07, RF12–RF13 | In Review; funcionalidade existente, validação parcial | [API](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/b760cab), [testes do serviço](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/102fc18), [PR #2](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/2), [ReservaServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/ReservaServiceTest.java) e [RegraCancelamentoServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/RegraCancelamentoServiceTest.java) |
| [US03 / SCRUM-7 — Desativar recurso](https://reservas-c14.atlassian.net/browse/SCRUM-7) | RF16 | To Do; implementação pendente | PR e testes pendentes |
| [US04 / SCRUM-8 — Armazenar senha com hash](https://reservas-c14.atlassian.net/browse/SCRUM-8) | RNF08 | To Do; implementação pendente | PR e testes pendentes |
| [US05 / SCRUM-9 — Impedir cancelamento após o início](https://reservas-c14.atlassian.net/browse/SCRUM-9) | RF17 | To Do; implementação pendente | PR e testes pendentes |

Na US01, faltam testes do início no passado, da consulta real de conflitos no banco e dos códigos HTTP. Na US02, faltam testes da segunda tentativa de cancelamento e dos códigos HTTP. Os testes de intervalos e os mocks de repositório não comprovam o comportamento da consulta SQL. Os arquivos foram inspecionados nesta revisão; a execução local ficou pendente porque o ambiente disponível tem JDK 17 e o projeto exige JDK 21.

Cada integrante deve cadastrar ao menos uma história. As cinco histórias acima estão na conta do Vinícius; o cadastro da história do Raphael ainda está pendente. A E5 também depende da revisão e integração deste README, da conferência do acesso do monitor ao Jira e do envio dos links pelo líder.

As tarefas de acompanhamento são [documentação / SCRUM-1](https://reservas-c14.atlassian.net/browse/SCRUM-1), [revisão do README / SCRUM-2](https://reservas-c14.atlassian.net/browse/SCRUM-2) e [participação, acesso e envio / SCRUM-3](https://reservas-c14.atlassian.net/browse/SCRUM-3). Elas substituíram os cartões de exemplo criados no cadastro inicial.

## Uso de IA

Foram usados Claude Opus 5.5, na sessão anterior, e Codex, na organização da E5, para leitura do repositório, rascunho de requisitos e documentação, apoio ao Jenkinsfile e cadastro das histórias no Jira. Os requisitos pendentes foram separados do comportamento já implementado, e as histórias em revisão registram as lacunas dos testes.

Exemplos de prompts reais desta conversa:

| Prompt | Resultado utilizado ou ajustado |
|---|---|
| “na verdade, achei tudo otmo, pode aplicar tudo” | Primeira versão do README com requisitos; o texto sobre credenciais foi ajustado para reconhecer os valores padrão existentes. |
| “confira se ta tudo certo e vamos continuar” | Conferência das US01 e US02 no Jira; o tipo dos cartões foi corrigido de Tarefa para História. |
| “pode continuar voce mesmo?” | Cadastro das US03, US04 e US05 como pendentes, com critérios de aceite e requisitos relacionados. |

A IA foi usada como apoio à redação e à execução de tarefas; a revisão e a defesa das decisões cabem aos integrantes. Antes da entrega final, cada integrante deve complementar este registro com seu uso de IA, as sugestões descartadas e as atividades realizadas sem IA.

## Equipe

- Raphael Ferreira de Oliveira
- Vinícius Santos Araújo
