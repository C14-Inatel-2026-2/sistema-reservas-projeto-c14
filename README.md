# Sistema de Reservas

API REST para reserva de recursos de uma instituição de ensino: salas de estudo, quadras e equipamentos de laboratório. Usuários (alunos, professores e funcionários) reservam um recurso por um período; o sistema impede conflitos de horário e aplica regras de cancelamento definidas por recurso.

Projeto da disciplina C14 – Engenharia de Software (Inatel, 2026/2). O projeto tem apenas backend.

## Tecnologias

- Java 21, Spring Boot 4, Maven (wrapper incluso)
- MySQL 8 no ambiente local, TiDB Cloud Starter no deploy e Flyway (migrations)
- Spring Data JPA, Bean Validation, Lombok, Spring Security Crypto (BCrypt)
- JUnit 5, Mockito, JaCoCo
- Spring Boot Actuator (saúde e commit publicado)
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

Novos cadastros armazenam a senha com BCrypt, com custo 10. A senha deve ter pelo menos 6 caracteres e no máximo 72 bytes em UTF-8; acima desse limite, o cadastro retorna HTTP 422. As respostas de cadastro, consulta e listagem não incluem senha ou hash.

A aplicação não converte registros antigos automaticamente. O único usuário legado do TiDB foi convertido administrativamente em 12/11/2026, com verificação pelo encoder do projeto e preservação de seus vínculos.

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
| RF01 | Cadastrar usuário com nome, e-mail, senha (mínimo 6 caracteres e máximo 72 bytes em UTF-8) e tipo (aluno, professor ou funcionário). | Implementado |
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
| RNF02 | Banco MySQL 8 ou TiDB compatível com MySQL, com esquema versionado por migrations Flyway. | `db/migration/` | Validado com MySQL local e TiDB Cloud via TLS |
| RNF03 | API REST com JSON e códigos HTTP padronizados: 400 (validação), 404 (não encontrado), 409 (conflito), 422 (regra de negócio). | `GlobalExceptionHandler` | Atendido |
| RNF04 | Erros tratados pelo `GlobalExceptionHandler` no formato `{status, mensagem, detalhes}`. | `ErroResponse` | Atendido |
| RNF05 | Sinalizar o build como instável quando a cobertura de linhas ficar abaixo de 40%. | Quality gate no Jenkins | Configurado; percentual atual depende do relatório do build |
| RNF06 | Automatizar build, testes, publicação de cobertura e deploy pelo Jenkins. | `Jenkinsfile` e `scripts/deploy-render.ps1` | Deploy externo preparado; configuração dos serviços e validação no Jenkins pendentes |
| RNF07 | Credenciais do banco configuráveis por variáveis de ambiente (`DB_USERNAME`, `DB_PASSWORD`). | `application.properties` | Atendido |
| RNF08 | Senhas armazenadas com hash (BCrypt), nunca em texto puro. | `SenhaConfig`, `UsuarioService` e testes de usuário | Restauração na branch; revisão e Jenkins pendentes. Usuário legado do TiDB convertido em 12/11/2026. |
| RNF09 | Disponibilizar a API por HTTPS fora da máquina do Jenkins, com banco persistente compatível com MySQL. | Docker, Render, TiDB e health check | Validado localmente; publicação externa pendente |

## CI/CD

A pipeline está no `Jenkinsfile` e roda em um agente Windows:

1. **Build:** compila e gera o `.jar`.
2. **Testes:** executa os testes Java e seis testes do script de deploy; publica ambos no JUnit.
3. **Cobertura:** publica o relatório do JaCoCo e aplica o quality gate de 40% de linhas.
4. **Deploy:** na `main`, com build bem-sucedido e cobertura aprovada, aciona o Render para o SHA testado. Aguarda `/actuator/health` com status `UP` e `/actuator/info` com o mesmo commit. PRs e branches de desenvolvimento não publicam a aplicação.

O Jenkins continua usando um agente Windows com JDK 21. As credenciais `render-deploy-hook` e `render-service-url` precisam ser cadastradas antes de executar o novo deploy. A validação completa no Jenkins depende dessa configuração.

## Deploy externo

O ambiente usa a API no **Render Free** e banco **TiDB Cloud Starter, MySQL Compatible**, ambos em Virginia (`us-east-1` no TiDB). A publicação externa ainda está pendente. A restauração do BCrypt nesta branch precisa ser revisada e integrada antes da publicação para uso.

### Testar com Docker localmente

Copie `.env.example` para `.env` e preencha `DB_PASSWORD` e `MYSQL_ROOT_PASSWORD` com senhas de teste. Depois, na raiz do repositório:

```powershell
docker compose -p c14-reservas up -d --build
.\scripts\Test-LocalDeploy.ps1
docker compose -p c14-reservas down
```

A API fica em `http://127.0.0.1:8081`. O teste cria registros fictícios, reinicia a aplicação, verifica a persistência e interrompe e religa o banco para validar o health check. O volume do MySQL é preservado pelo comando `down`. A aplicação usa Java 21 e roda com usuário sem privilégios, limitada a 512 MB no Compose.

### Banco no TiDB Cloud

Crie uma instância **Starter**, modo **MySQL Compatible**, AWS **N. Virginia (`us-east-1`)**, com **Monthly Spending Limit = US$ 0**. A franquia inclui 5 GiB de armazenamento de linhas e 50 milhões de Request Units por mês; ao esgotar a franquia, o acesso é limitado. Não habilite planos pagos. [Planos e limites](https://docs.pingcap.com/tidbcloud/select-cluster-tier/).

No SQL Editor da instância, execute [scripts/preparar-tidb.sql](scripts/preparar-tidb.sql) **antes da primeira inicialização da API**. Ele habilita `tidb_enable_check_constraint`, cria `sistema_reservas` com `utf8mb4_0900_ai_ci` e exibe os valores para conferência. O primeiro resultado deve ser `1`, e a collation deve ser `utf8mb4_0900_ai_ci`. As tabelas continuam sendo criadas pela migration V1 original do Flyway.

Em **Connect**, obtenha host, porta, usuário completo (incluindo o prefixo da instância) e senha. Restrinja o firewall aos endereços de saída do serviço no Render; permita o IP local apenas durante a validação. As credenciais ficam nas variáveis de ambiente do serviço.

### API no Render

Após integrar os PRs, crie um **Web Service** na região **Virginia**, com o repositório público do grupo, branch `main`, linguagem **Docker**, Dockerfile `./Dockerfile`, contexto `.` e plano **Free**. Defina o health check como `/actuator/health` e desative o deploy automático; o Jenkins aciona a publicação pelo hook. O arquivo `render.yaml` registra essa configuração. [Docker no Render](https://render.com/docs/docker).

Configure estas variáveis no Render:

| Variável | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | JDBC URL abaixo, substituindo `HOST` e `PORTA` pelos valores do TiDB |
| `DB_USERNAME` | Usuário completo informado pelo TiDB |
| `DB_PASSWORD` | Senha do banco |

```text
jdbc:mysql://HOST:PORTA/sistema_reservas?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3&serverTimezone=UTC
```

A conexão verifica o certificado e o hostname usando as autoridades certificadoras do Java 21; não é necessário criar um truststore próprio. A senha fica separada em `DB_PASSWORD`. As credenciais não devem entrar no Git. A porta é lida de `PORT`, fornecida pelo Render. O pool usa até três conexões, com renovação em cinco minutos e keepalive de dois minutos.

O Render Free suspende serviços após 15 minutos sem tráfego, e a retomada pode levar cerca de um minuto. Abra a aplicação antes da demonstração. [Limites do plano Free](https://render.com/docs/free).

### Acionar pelo Jenkins

Cadastre duas credenciais do tipo **Secret text**, com estes IDs:

| ID | Conteúdo |
|---|---|
| `render-deploy-hook` | Deploy Hook obtido nas configurações do serviço no Render |
| `render-service-url` | URL HTTPS pública da API, sem barra final |

O script envia o SHA da pipeline no parâmetro `ref` do hook e espera por até 15 minutos pela aplicação saudável nessa versão. O hook não é escrito nos logs. Um build instável por cobertura abaixo de 40% não executa o deploy. [Deploy hooks do Render](https://render.com/docs/deploy-hooks).

Os seis testes desse script podem ser executados sem Jenkins nem acesso ao Render:

```powershell
.\scripts\Test-DeployRender.ps1 -ReportPath backend/target/surefire-reports/TEST-DeployRender.xml
```

### Validação do deploy

Em 05/11/2026: imagem Docker gerada; 36 testes Java e seis testes do script passaram. Com MySQL 8.4 real e perfil `prod`, o teste local confirmou cadastro de usuário/recurso/reserva, persistência após reiniciar, HTTP 503 sem banco e recuperação após religá-lo.

Em 12/11/2026: a aplicação executou com TiDB 8.5.2 local e limite de 512 MB. A migration original passou; foram verificados cadastro, reserva, conflito de horário (HTTP 409), consulta com filtros, confirmação e cancelamento. Os três `CHECK` e as chaves estrangeiras rejeitaram dados inválidos. Os registros persistiram após reiniciar a API e o banco, e o health check retornou HTTP 503 com o banco interrompido. O consumo observado da API foi de aproximadamente 352 MB.

Na mesma data, a imagem Docker foi conectada ao TiDB Cloud Starter 8.5.3 em Virginia, com `sslMode=VERIFY_IDENTITY` e as autoridades certificadoras padrão do Java 21. A migration V1 original criou as tabelas; os três `CHECK` e as chaves estrangeiras rejeitaram inserções inválidas. Cadastro, e-mail único, reserva, conflito de horário e confirmação passaram. Após substituir o contêiner da API, o usuário e a reserva confirmada continuaram salvos no banco. Também passaram os 36 testes Java e os seis testes do script de deploy, estes no PowerShell 5.1 e 7. A URL pública no Render e a execução da pipeline no Jenkins ainda estão pendentes.

Para testar o ambiente publicado e guardar os IDs de registros fictícios:

```powershell
.\scripts\Test-DeployExterno.ps1 -BaseUrl https://SERVICO.onrender.com -Commit SHA_COMPLETO -Fase Criar
# Depois de um novo deploy, confira os mesmos registros:
.\scripts\Test-DeployExterno.ps1 -BaseUrl https://SERVICO.onrender.com -Commit SHA_PUBLICADO -Fase Conferir
```

O estado do teste fica em `backend/target/deploy-smoke.json`, fora do Git. Só marque o deploy como concluído depois de validar a versão publicada, o fluxo de reserva e a persistência após redeploy.

## Refactorings

Tarefas: [reservas / SCRUM-12](https://reservas-c14.atlassian.net/browse/SCRUM-12) e [recursos e testes / SCRUM-13](https://reservas-c14.atlassian.net/browse/SCRUM-13).

| Autor | Smell | Refactoring e motivo | Evidência | Situação |
|---|---|---|---|---|
| Vinícius | `criar` mistura validação, montagem e persistência. | Extrair `montarReserva`, separando a construção da entidade das verificações. | [PR #8](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/8), commit `476febb` | Aplicado na branch; revisão e Jenkins pendentes |
| Vinícius | `cancelar` concentra regra, cálculo de multa e mudança de estado. | Extrair `calcularPercentualMulta`, preservando o mesmo instante e a comparação de antecedência. | [PR #8](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/8), commit `1b6f428` | Aplicado na branch; revisão e Jenkins pendentes |
| Raphael | Mapeamento extenso dos tipos de recurso. | Extrair os mapeamentos específicos de `RecursoResponse`, preservando campos e proxies. | PR pendente | Planejado |
| Raphael | Preparação repetida nos testes de cancelamento. | Extrair um helper de preparação em `ReservaServiceTest`, mantendo as verificações. | PR pendente | Planejado |

Em 12/11/2026, `mvnw.cmd -B -ntp clean verify` com JDK 21 passou antes e depois dos dois refactorings de Vinícius: 40 testes, sem falhas. A cobertura de linhas foi de 61,93% para 62,50%. Os quatro testes novos e as verificações adicionais foram registrados antes das extrações, no commit `4ec64e1`; não contam como refactorings.

Resumos da validação local: [antes](docs/evidencias/refactor-reservas-antes.json) e [depois](docs/evidencias/refactor-reservas-depois.json). Os relatórios completos JUnit e JaCoCo foram preservados localmente. Os builds do Jenkins antes e depois ainda precisam ser executados e vinculados.

Esta branch combina o deploy do PR #7 e a restauracao do BCrypt do PR #11. Integrar o #11 antes do #8. Os builds do Jenkins antes/depois continuam pendentes.

## Gestão do projeto

[Quadro no Jira](https://reservas-c14.atlassian.net/jira/software/projects/SCRUM/boards/1) · [Backlog](https://reservas-c14.atlassian.net/jira/software/projects/SCRUM/boards/1/backlog)

O quadro foi organizado em 30/10/2026 com Scrum adaptado à equipe de dois integrantes. A sprint atual, **Sprint E5**, vai de 30/10 a 13/11/2026. Seu objetivo é consolidar a documentação da E5, vincular código e testes e revisar os critérios de aceite das funcionalidades existentes. As melhorias novas ficam no backlog.

Vinícius organiza o Jira, os requisitos e a documentação; Raphael mantém a pipeline e participa da revisão. Ambos desenvolvem e revisam as alterações do outro. Nos papéis do Scrum, Vinícius atua como Scrum Master e Raphael como Product Owner, responsável por priorizar o backlog, e ambos acumulam o papel de desenvolvedores.

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

Situação em 31/10/2026. O Jira concentra os critérios de aceite e o status atualizado.

| História | Requisitos | Situação | Código e testes |
|---|---|---|---|
| [US01 / SCRUM-5 — Reservar recurso](https://reservas-c14.atlassian.net/browse/SCRUM-5) | RF08–RF10 | In Review; funcionalidade existente, validação parcial | [PR #1](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/1), [PR #2](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/2), [API](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/b760cab), [ReservaServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/ReservaServiceTest.java), [ValidadorHorarioReservaTest](backend/src/test/java/com/reservas/sistemareservas/service/ValidadorHorarioReservaTest.java) e [testes de limites](backend/src/test/java/com/reservas/sistemareservas/service/ValidadorHorarioReservaLimitesTest.java) |
| [US02 / SCRUM-6 — Cancelar reserva](https://reservas-c14.atlassian.net/browse/SCRUM-6) | RF07, RF12–RF13 | In Review; funcionalidade existente, validação parcial | [API](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/b760cab), [testes do serviço](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/102fc18), [PR #2](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/2), [ReservaServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/ReservaServiceTest.java) e [RegraCancelamentoServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/RegraCancelamentoServiceTest.java) |
| [US03 / SCRUM-7 — Desativar recurso](https://reservas-c14.atlassian.net/browse/SCRUM-7) | RF16 | To Do; implementação pendente | PR e testes pendentes |
| [US04 / SCRUM-8 — Armazenar senha com hash](https://reservas-c14.atlassian.net/browse/SCRUM-8) | RNF08 | In Review; restauração do BCrypt sobre a base com deploy externo | [SenhaConfig](backend/src/main/java/com/reservas/sistemareservas/config/SenhaConfig.java), [UsuarioService](backend/src/main/java/com/reservas/sistemareservas/service/UsuarioService.java), [UsuarioServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/UsuarioServiceTest.java) e [UsuarioControllerTest](backend/src/test/java/com/reservas/sistemareservas/controller/UsuarioControllerTest.java) |
| [US05 / SCRUM-9 — Impedir cancelamento após o início](https://reservas-c14.atlassian.net/browse/SCRUM-9) | RF17 | To Do; implementação pendente | PR e testes pendentes |
| [US06 / SCRUM-10 — Cadastrar usuário](https://reservas-c14.atlassian.net/browse/SCRUM-10) | RF01–RF02 | In Review; funcionalidade existente, validação parcial | [API](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/b760cab), [testes do serviço](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/102fc18), [teste do controller](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/commit/695b328), [UsuarioServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/UsuarioServiceTest.java) e [UsuarioControllerTest](backend/src/test/java/com/reservas/sistemareservas/controller/UsuarioControllerTest.java) |
| [US07 / SCRUM-11 — Deploy externo](https://reservas-c14.atlassian.net/browse/SCRUM-11) | RNF06, RNF09 | In Progress; TiDB Cloud validado, publicação no Render e pipeline pendentes | [PR #7](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/7), [Dockerfile](Dockerfile), [Jenkinsfile](Jenkinsfile), [script de deploy](scripts/deploy-render.ps1), [testes do script](scripts/Test-DeployRender.ps1) e [teste de persistência externa](scripts/Test-DeployExterno.ps1) |

Na US01 e na US02, os testes adicionais de início no passado e segunda tentativa de cancelamento estão no PR #8, ainda em revisão. A execução local com TiDB real já comprovou o conflito HTTP 409 e os fluxos de reserva. Os mocks continuam sendo testes isolados e não executam a consulta SQL.

Na US06, os testes do controller cobrem criação com HTTP 201, consultas sem senha ou hash, e-mail inválido com HTTP 400, limite de senha com HTTP 422 e usuário inexistente com HTTP 404. Os casos unitários de HTTP 409, nome vazio, senha curta e tipo ausente ainda não estão nessa classe. Em 12/11/2026, a restauração do BCrypt com a configuração de deploy passou em 44 testes Java e seis testes PowerShell; os builds do Jenkins continuam pendentes.

Cada integrante cadastrou ao menos uma história: as US01 a US05 estão na conta do Vinícius, e a US06, na do Raphael. A E5 ainda depende da revisão e integração deste README, da conferência do acesso do monitor ao Jira e do envio dos links pelo líder.

As tarefas de acompanhamento são [documentação / SCRUM-1](https://reservas-c14.atlassian.net/browse/SCRUM-1), [revisão do README / SCRUM-2](https://reservas-c14.atlassian.net/browse/SCRUM-2) e [participação, acesso e envio / SCRUM-3](https://reservas-c14.atlassian.net/browse/SCRUM-3). Elas substituíram os cartões de exemplo criados no cadastro inicial.

## Uso de IA

Foram usados Claude Opus 5.5, na sessão anterior, e Codex, na organização da E5, para leitura do repositório, rascunho de requisitos e documentação, apoio ao Jenkinsfile e cadastro das histórias no Jira. Os requisitos pendentes foram separados do comportamento já implementado, e as histórias em revisão registram as lacunas dos testes.

A IA foi usada como apoio à redação e à execução de tarefas; a revisão e a defesa das decisões cabem aos integrantes. Antes da entrega final, cada integrante deve complementar este registro com seu uso de IA, as sugestões descartadas e as atividades realizadas sem IA.

No deploy externo, Codex apoiou Dockerfile, configuracao por variaveis, script do Jenkins, testes e documentacao.

## Equipe

- Raphael Ferreira de Oliveira
- Vinícius Santos Araújo
