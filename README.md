# Sistema de Reservas

API REST para reserva de recursos de uma instituição de ensino: salas de estudo, quadras e equipamentos de laboratório. Usuários (alunos, professores e funcionários) reservam um recurso por um período; o sistema impede conflitos de horário e aplica regras de cancelamento definidas por recurso.

Projeto da disciplina C14 – Engenharia de Software (Inatel, 2026/2). O projeto tem apenas backend.

**API pública:** [https://sistema-reservas-c14.onrender.com](https://sistema-reservas-c14.onrender.com).
Para conferir no navegador: [saúde da API](https://sistema-reservas-c14.onrender.com/actuator/health), [versão publicada](https://sistema-reservas-c14.onrender.com/actuator/info) e [recursos cadastrados](https://sistema-reservas-c14.onrender.com/recursos).

O Render Free pode hibernar após 15 minutos sem tráfego. A primeira chamada após esse período pode levar cerca de um minuto.

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

Em 14/11/2026, a revisão do PR #14 combinada à `main` passou em 55 testes Java e seis testes PowerShell, sem falhas ou testes ignorados, com 65,90% de cobertura de linhas. A árvore testada corresponde ao merge `8ebce6e`. Essa execução foi local; a execução final do Jenkins é uma evidência separada. [Resumo da validação](docs/evidencias/validacao-local-final.json).

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
| PATCH | `/recursos/{id}/desativar` | Desativa o recurso (ele deixa de aceitar novas reservas) |
| PATCH | `/recursos/{id}/ativar` | Reativa o recurso para novas reservas |
| PUT | `/recursos/{id}/regra-cancelamento` | Define a regra de cancelamento do recurso |
| GET | `/recursos/{id}/regra-cancelamento` | Consulta a regra de cancelamento |
| POST | `/reservas` | Cria reserva |
| GET | `/reservas?usuarioId=&recursoId=` | Lista reservas (filtros opcionais) |
| GET | `/reservas/{id}` | Busca reserva |
| PATCH | `/reservas/{id}/confirmar` | Confirma reserva pendente |
| PATCH | `/reservas/{id}/cancelar` | Cancela reserva ainda não iniciada (422 se já começou; corpo opcional: `{"motivo": "..."}`) |

Exemplo de criação de reserva:

POST /reservas

```json
{
  "usuarioId": 1,
  "recursoId": 2,
  "dataHoraInicio": "2026-12-15T14:00:00",
  "dataHoraFim": "2026-12-15T16:00:00"
}
```

Erros seguem o formato `{"status": 422, "mensagem": "...", "detalhes": []}`.
Use IDs existentes e um período futuro livre ao repetir o exemplo.

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
| RF12 | Cancelar reserva com motivo opcional antes do início, exceto quando ela já estiver cancelada ou quando o recurso não permitir cancelamento. | Implementado |
| RF13 | Aplicar o percentual de multa da regra quando o cancelamento ocorrer com antecedência menor que a mínima. | Implementado |
| RF14 | Listar reservas, com filtro opcional por usuário e/ou recurso, e buscar reserva por id. | Implementado |
| RF15 | Autenticar usuários e permitir que apenas o dono da reserva a cancele. | Pendente |
| RF16 | Ativar e desativar recursos; recursos inativos não aceitam novas reservas. | Implementado no PR #14; aceite na versão pública atual pendente |
| RF17 | Impedir o cancelamento de reservas já iniciadas ou encerradas. | Implementado no PR #14; aceite na versão pública atual pendente |
| RF18 | Editar e excluir usuários, recursos e reservas. | Pendente |

Desativar um recurso preserva seu cadastro e as reservas existentes. O bloqueio de cancelamento a partir do início vale tanto para reservas pendentes quanto para confirmadas. Autenticação, autorização e edição/exclusão continuam fora deste incremento.

## Requisitos não funcionais

| ID | Requisito | Verificação | Status |
|---|---|---|---|
| RNF01 | Backend em Java 21 com Spring Boot 4, build com Maven via wrapper. | `backend/pom.xml` | Atendido |
| RNF02 | Banco MySQL 8 ou TiDB compatível com MySQL, com esquema versionado por migrations Flyway. | `db/migration/` | Validado com MySQL local e TiDB Cloud via TLS |
| RNF03 | API REST com JSON e códigos HTTP padronizados: 400 (validação), 404 (não encontrado), 409 (conflito), 422 (regra de negócio). | `GlobalExceptionHandler` | Atendido |
| RNF04 | Erros tratados pelo `GlobalExceptionHandler` no formato `{status, mensagem, detalhes}`. | `ErroResponse` | Atendido |
| RNF05 | Sinalizar o build como instável quando a cobertura de linhas ficar abaixo de 40%. | Quality gate no Jenkins | Configurado; percentual atual depende do relatório do build |
| RNF06 | Automatizar build, testes, publicação de cobertura e deploy pelo Jenkins. | `Jenkinsfile` e `scripts/deploy-render.ps1` | Serviço publicado e hook validado localmente; credenciais e execução no Jenkins pendentes |
| RNF07 | Credenciais do banco configuráveis por variáveis de ambiente (`DB_USERNAME`, `DB_PASSWORD`). | `application.properties` | Atendido |
| RNF08 | Senhas armazenadas com hash (BCrypt), nunca em texto puro. | `SenhaConfig`, `UsuarioService` e testes de usuário | Integrado pelo PR #11. Usuário legado do TiDB convertido em 12/11/2026. |
| RNF09 | Disponibilizar a API por HTTPS fora da máquina do Jenkins, com banco persistente compatível com MySQL. | Docker, Render, TiDB e health check | API pública e persistência após novo deploy verificadas em 13/11/2026 |

## CI/CD

A pipeline está no `Jenkinsfile` e roda em um agente Windows:

1. **Build:** compila e gera o `.jar`.
2. **Testes:** executa os testes Java e seis testes do script de deploy; publica ambos no JUnit.
3. **Cobertura:** publica o relatório do JaCoCo e aplica o quality gate de 40% de linhas.
4. **Deploy:** na `main`, com build bem-sucedido e cobertura aprovada, aciona o Render para o SHA testado. Aguarda `/actuator/health` com status `UP` e `/actuator/info` com o mesmo commit. PRs e branches de desenvolvimento não publicam a aplicação.

O Jenkins continua usando um agente Windows com JDK 21. As credenciais `render-deploy-hook` e `render-service-url` precisam ser cadastradas antes de executar o novo deploy. A validação completa no Jenkins depende dessa configuração.

## Deploy externo

A API está publicada no **Render Free**, com banco **TiDB Cloud Starter, MySQL Compatible**, ambos em Virginia (`us-east-1` no TiDB). O BCrypt está integrado pelo PR #11. A execução completa pelo Jenkins ainda depende do cadastro das credenciais e dos builds.

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

No ambiente publicado, a lista de acesso do TiDB contém apenas os intervalos `74.220.49.0/24` e `74.220.57.0/24`, informados pelo Render para o serviço em Virginia em 13/11/2026. A regra de acesso de qualquer IP foi removida. Consulte **Connect > Outbound IP Addresses** do serviço ao revisar essa configuração.

### API no Render

O serviço existente usa a região **Virginia**, o repositório público do grupo, branch `main`, linguagem **Docker**, Dockerfile `./Dockerfile`, contexto `.` e plano **Free**. O health check é `/actuator/health`, e o deploy automático está desativado; o Jenkins aciona a publicação pelo hook. Para reproduzir essa configuração, consulte `render.yaml`. [Docker no Render](https://render.com/docs/docker).

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
| `render-service-url` | `https://sistema-reservas-c14.onrender.com` |

O script envia o SHA da pipeline no parâmetro `ref` do hook e espera por até 15 minutos pela aplicação saudável nessa versão. O hook não é escrito nos logs. Um build instável por cobertura abaixo de 40% não executa o deploy. [Deploy hooks do Render](https://render.com/docs/deploy-hooks).

Os seis testes desse script podem ser executados sem Jenkins nem acesso ao Render:

```powershell
.\scripts\Test-DeployRender.ps1 -ReportPath backend/target/surefire-reports/TEST-DeployRender.xml
```

### Validação do deploy

Em 05/11/2026: imagem Docker gerada; 36 testes Java e seis testes do script passaram. Com MySQL 8.4 real e perfil `prod`, o teste local confirmou cadastro de usuário/recurso/reserva, persistência após reiniciar, HTTP 503 sem banco e recuperação após religá-lo.

Em 12/11/2026: a aplicação executou com TiDB 8.5.2 local e limite de 512 MB. A migration original passou; foram verificados cadastro, reserva, conflito de horário (HTTP 409), consulta com filtros, confirmação e cancelamento. Os três `CHECK` e as chaves estrangeiras rejeitaram dados inválidos. Os registros persistiram após reiniciar a API e o banco, e o health check retornou HTTP 503 com o banco interrompido. O consumo observado da API foi de aproximadamente 352 MB.

Na mesma data, a imagem Docker foi conectada ao TiDB Cloud Starter 8.5.3 em Virginia, com `sslMode=VERIFY_IDENTITY` e as autoridades certificadoras padrão do Java 21. A migration V1 original criou as tabelas; os três `CHECK` e as chaves estrangeiras rejeitaram inserções inválidas. Cadastro, e-mail único, reserva, conflito de horário e confirmação passaram. Após substituir o contêiner da API, o usuário e a reserva confirmada continuaram salvos no banco. Também passaram os 36 testes Java e os seis testes do script de deploy, estes no PowerShell 5.1 e 7. Naquela etapa, a URL pública no Render e a execução da pipeline no Jenkins ainda estavam pendentes.

Em 13/11/2026, o Render publicou a `main` no commit `1dbd0f707ee3c3112d2a43f5bb04d3474732f5df`. A URL HTTPS respondeu com saúde `UP` e o SHA esperado. Passaram 12 grupos de verificações pela API, incluindo os três tipos de recurso, validações de cadastro, unicidade, conflito de horário, períodos adjacentes, confirmação e regras de cancelamento. Após um novo deploy pelo hook, o usuário e a reserva confirmada permaneceram salvos. Essa execução partiu da máquina local; a execução final da `main` no Jenkins continua pendente. [Evidência da publicação](docs/evidencias/deploy-publico.json).

Na revisão dos critérios do Jira, passaram também os 12 critérios das histórias US01, US02 e US06 na mesma versão pública: sobreposição parcial das 14h–16h com 15h–17h recusada, período adjacente aceito, períodos inválidos rejeitados, multa configurável de 20% aplicada conforme a antecedência, cancelamento bloqueado sem alterar o status e cadastro dos três tipos de usuário sem senha/hash nas respostas. [Resultados por critério](docs/evidencias/criterios-publicos.json). A revisão final e a execução final da `main` no Jenkins continua pendente.

Para repetir a verificação e guardar os IDs de registros fictícios:

```powershell
.\scripts\Test-CriteriosApi.ps1 -BaseUrl https://sistema-reservas-c14.onrender.com -Commit SHA_COMPLETO -StatePath backend/target/aceite-api.json
# Depois de um novo deploy, confira os mesmos registros:
.\scripts\Test-DeployExterno.ps1 -BaseUrl https://sistema-reservas-c14.onrender.com -Commit SHA_PUBLICADO -Fase Conferir -StatePath backend/target/aceite-api.json
```

O estado do teste e o resumo `aceite-api.resultado.json` ficam em `backend/target`, fora do Git. O script usa outras reservas para cancelamento, preservando a reserva confirmada para a prova de persistência. A conclusão da CI/CD exige também a execução e as evidências do Jenkins.

## Refactorings

Tarefas: [reservas / SCRUM-12](https://reservas-c14.atlassian.net/browse/SCRUM-12) e [recursos e testes / SCRUM-13](https://reservas-c14.atlassian.net/browse/SCRUM-13).

| Autor | Smell | Refactoring e motivo | Evidência | Situação |
|---|---|---|---|---|
| Vinícius | `criar` mistura validação, montagem e persistência. | Extrair `montarReserva`, separando a construção da entidade das verificações. | [PR #8](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/8), commit `476febb` | Integrado pelo PR #8; Jenkins verde antes e depois |
| Vinícius | `cancelar` concentra regra, cálculo de multa e mudança de estado. | Extrair `calcularPercentualMulta`, preservando o mesmo instante e a comparação de antecedência. | [PR #8](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/8), commit `1b6f428` | Integrado pelo PR #8; Jenkins verde antes e depois |
| Raphael | Mapeamento extenso dos tipos de recurso: `RecursoResponse.de` concentra o mapeamento de todos os tipos em um só método (método longo). | Extrair `deSala`, `deQuadra`, `deEquipamento` e `deRecursoBase`. O motivo é deixar `de` apenas escolhendo o tipo e isolar cada mapeamento, o que facilita ler e alterar um tipo sem mexer nos outros; campos e proxies foram preservados. | [PR #13](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/13), commit `3828d14` | Integrado após revisão de Vinícius; Jenkins verde antes e depois conforme os resumos |
| Raphael | Preparação repetida nos testes de cancelamento: os mesmos mocks de reserva e de regra são configurados em cada teste (código duplicado). | Extrair os helpers `dadoReservaExistente` e `dadoRegraDoRecurso` em `ReservaServiceTest`. O motivo é reduzir a duplicação e deixar cada teste mostrando só o que o diferencia; as verificações não mudaram, e foram dois helpers porque um teste não pode configurar o repositório de regras. | [PR #13](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/13), commit `b4beaac` | Integrado após revisão de Vinícius; Jenkins verde antes e depois conforme os resumos |

Em 12/11/2026, `mvnw.cmd -B -ntp clean verify` com JDK 21 passou antes e depois dos dois refactorings de Vinícius: 40 testes, sem falhas. A cobertura de linhas foi de 61,93% para 62,50%. Os quatro testes novos e as verificações adicionais foram registrados antes das extrações, no commit `4ec64e1`; não contam como refactorings.

Resumos da validação local: [antes](docs/evidencias/refactor-reservas-antes.json) e [depois](docs/evidencias/refactor-reservas-depois.json). Os relatórios completos JUnit e JaCoCo foram preservados localmente. Os builds do Jenkins antes e depois foram executados em 13/11/2026 e estão resumidos mais abaixo.

O PR #8 combinou o deploy do PR #7 e a restauração do BCrypt do PR #11, que foi integrado antes dele. Os builds do Jenkins antes e depois estão registrados mais abaixo.

Na base integrada, as duas referências passaram em 48 testes Java e seis testes PowerShell. A cobertura de linhas foi de 64,18% antes para 64,71% depois. Os testes e a infraestrutura são iguais nas duas referências; no código de produção mudam apenas as duas extrações em `ReservaService`.

Referências para o Jenkins: `ci/reservas-antes` e `refactor/reservas`, no fork do Vinícius. [Configuração dos builds](docs/evidencias/jenkins-refactorings.md) · [resultado local antes](docs/evidencias/refactor-integrado-antes.json) · [resultado local depois](docs/evidencias/refactor-integrado-depois.json).

Em 13/11/2026, o Jenkins executou a pipeline sobre os refactorings de Raphael: `4a2f333` (antes, a `main`) e `9efac7e` (depois, branch `refactor/recursos-e-testes`). Os dois builds terminaram com sucesso, com 54 testes (48 Java e seis do script de deploy), sem falhas, e o stage Deploy pulado por não ser a `main`. A cobertura de linhas foi de 64,71% para 65,22%; o aumento vem dos métodos extraídos, executados pelos mesmos testes. Resumos: [antes](docs/evidencias/refactor-recursos-antes.json) e [depois](docs/evidencias/refactor-recursos-depois.json).

Na mesma data, o Jenkins executou a pipeline sobre as referências dos refactorings de Vinícius, no fork dele: `0466e2c` (antes, `ci/reservas-antes`) e `29ee4af` (depois, `refactor/reservas`, integrado pelo PR #8). Os dois builds terminaram com sucesso, com 54 testes (48 Java e seis do script de deploy), sem falhas, e o stage Deploy pulado. A cobertura de linhas foi de 64,18% para 64,71%, igual à validação local registrada acima.

## Gestão do projeto

[Quadro no Jira](https://reservas-c14.atlassian.net/jira/software/projects/SCRUM/boards/1) · [Backlog](https://reservas-c14.atlassian.net/jira/software/projects/SCRUM/boards/1/backlog)

O quadro foi organizado em 30/10/2026 com Scrum adaptado à equipe de dois integrantes. A Sprint E5 foi planejada para 30/10 a 13/11, com documentação, rastreabilidade e revisão das funcionalidades. O fechamento do projeto prosseguiu em 14/11, prazo prorrogado da entrega final. A revisão e a retrospectiva da sprint ainda precisam ser registradas com o grupo.

Vinícius organiza Jira, requisitos, documentação e deploy externo; Raphael mantém o Jenkins e prioriza o backlog. No Scrum adaptado, Vinícius atua como facilitador/Scrum Master, Raphael como Product Owner, e ambos desenvolvem e revisam código. O acompanhamento ocorreu pelo quadro e pelo chat do grupo; as funcionalidades anteriores à adoção do Jira foram relacionadas retrospectivamente. Não há registro de uma cadência regular de reuniões formais.

### Fluxo, DoR e DoD

- **To Do:** item planejado, aguardando execução.
- **In Progress:** implementação, documentação ou validação em andamento.
- **In Review:** aguardando revisão de PR ou validação dos critérios.
- **Done:** critérios verificados, testes e pipeline aprovados, documentação atualizada e alteração integrada após revisão de outro integrante.

Para iniciar um item (DoR), a história deve ter objetivo, critérios de aceite, prioridade, requisito relacionado, responsável e dependências identificadas. O Jira mantém esses dados e os critérios no formato Dado/Quando/Então. O estado técnico abaixo não substitui a conferência do DoD de cada cartão.

As alterações usam branches, Conventional Commits e PRs. O outro integrante revisa antes do merge. Vinícius trabalha por fork porque sua conta continua sem permissão de escrita no repositório da organização; Raphael realiza os merges. Os PRs e testes são vinculados às histórias no Jira.

### Histórias e rastreabilidade

Situação técnica em 14/11/2026. O aceite público de 13/11 foi realizado em `1dbd0f7`; a versão final exige nova conferência após a pipeline.

| História | Requisitos | Situação técnica | PRs e testes |
|---|---|---|---|
| [US01 / SCRUM-5 — Reservar recurso](https://reservas-c14.atlassian.net/browse/SCRUM-5) | RF08–RF10 | Implementada; quatro critérios públicos verificados em 13/11; aceite final pendente | PRs [#1](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/1), [#2](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/2) e [#8](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/8); [ReservaServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/ReservaServiceTest.java), [ValidadorHorarioReservaTest](backend/src/test/java/com/reservas/sistemareservas/service/ValidadorHorarioReservaTest.java) e [limites](backend/src/test/java/com/reservas/sistemareservas/service/ValidadorHorarioReservaLimitesTest.java) |
| [US02 / SCRUM-6 — Cancelar reserva](https://reservas-c14.atlassian.net/browse/SCRUM-6) | RF07, RF12–RF13 | Implementada; quatro critérios públicos verificados em 13/11; aceite final pendente | PRs [#2](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/2) e [#8](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/8); [ReservaServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/ReservaServiceTest.java) e [RegraCancelamentoServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/RegraCancelamentoServiceTest.java) |
| [US03 / SCRUM-7 — Desativar recurso](https://reservas-c14.atlassian.net/browse/SCRUM-7) | RF16 | Implementada e revisada; aceite público pendente | [PR #14](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/14); [RecursoServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/RecursoServiceTest.java) e caso de recurso inativo em [ReservaServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/ReservaServiceTest.java) |
| [US04 / SCRUM-8 — Armazenar senha com hash](https://reservas-c14.atlassian.net/browse/SCRUM-8) | RNF08 | BCrypt integrado; cadastro público verificado; pipeline final pendente | PRs [#6](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/6) e [#11](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/11); [UsuarioServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/UsuarioServiceTest.java) e [UsuarioControllerTest](backend/src/test/java/com/reservas/sistemareservas/controller/UsuarioControllerTest.java) |
| [US05 / SCRUM-9 — Impedir cancelamento após o início](https://reservas-c14.atlassian.net/browse/SCRUM-9) | RF17 | Implementada e revisada; aceite público pendente | [PR #14](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/14); [ReservaServiceCancelamentoIniciadaTest](backend/src/test/java/com/reservas/sistemareservas/service/ReservaServiceCancelamentoIniciadaTest.java) |
| [US06 / SCRUM-10 — Cadastrar usuário](https://reservas-c14.atlassian.net/browse/SCRUM-10) | RF01–RF02 | Implementada; quatro critérios públicos verificados em 13/11; aceite final pendente | PRs [#6](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/6) e [#11](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/11); [UsuarioServiceTest](backend/src/test/java/com/reservas/sistemareservas/service/UsuarioServiceTest.java) e [UsuarioControllerTest](backend/src/test/java/com/reservas/sistemareservas/controller/UsuarioControllerTest.java) |
| [US07 / SCRUM-11 — Deploy externo](https://reservas-c14.atlassian.net/browse/SCRUM-11) | RNF06, RNF09 | API pública e persistência verificadas; execução final pelo Jenkins pendente | PRs [#7](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/7) e [#12](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/12); [Jenkinsfile](Jenkinsfile), [testes do script](scripts/Test-DeployRender.ps1), [verificação HTTP](scripts/Test-CriteriosApi.ps1) e [evidência](docs/evidencias/deploy-publico.json) |

Os [12 critérios públicos de US01, US02 e US06](docs/evidencias/criterios-publicos.json) complementam os testes unitários. Mocks de repositórios verificam regras em isolamento; os scripts HTTP verificam a aplicação conectada ao banco real. A validação local mais recente passou em 55 testes Java e seis PowerShell. Cada integrante cadastrou ao menos uma história: US01 a US05 na conta de Vinícius e US06 na de Raphael. A E5 foi enviada pelo líder; a confirmação de acesso do monitor ao Jira continua pendente.

### Métricas e dinâmica

Na consulta ao GitHub em 14/11/2026, antes deste fechamento documental, havia 14 PRs: 13 integrados e um fechado sem merge. Os PRs #13 e #14 receberam aprovação de Vinícius antes da integração. São métricas do repositório; o encerramento e as métricas da sprint ainda dependem da conferência do Jira. Não foram medidos tempo de ciclo ou velocidade por pontos.

Raphael concentrou a implementação inicial da API e da pipeline. Vinícius contribuiu com validação de horários, testes, quality gate, BCrypt, deploy externo, documentação, Jira e dois refactorings de reservas. Raphael aplicou os dois refactorings de recursos/testes e as funcionalidades do PR #14. Os quatro refactorings estão integrados.

O bloqueio de permissão foi contornado com fork e revisão por PR. Durante a integração do deploy, o BCrypt foi revertido e depois restaurado no PR #11. A sequência mostrou a necessidade de atualizar a branch sobre a `main`, resolver conflitos preservando ambas as funcionalidades e validar a combinação antes do merge. Para os próximos ciclos, o grupo deve distribuir trabalho de código mais cedo, usar alterações menores e registrar evidências da pipeline junto de cada revisão.

As tarefas de acompanhamento são [documentação / SCRUM-1](https://reservas-c14.atlassian.net/browse/SCRUM-1), [revisão do README / SCRUM-2](https://reservas-c14.atlassian.net/browse/SCRUM-2) e [participação, acesso e envio / SCRUM-3](https://reservas-c14.atlassian.net/browse/SCRUM-3). O checklist dos 14 critérios e das pendências de envio está em [docs/entrega-final.md](docs/entrega-final.md).

## Uso de IA

Vinícius utilizou Claude Opus 5.5 e Codex; Raphael, ChatGPT e Claude. O apoio incluiu pesquisa, código, testes, refactorings, CI/CD, documentação e organização do Jira. Os integrantes são responsáveis pelas decisões, revisão e validação dos resultados.

## Equipe

- Raphael Ferreira de Oliveira
- Vinícius Santos Araújo
