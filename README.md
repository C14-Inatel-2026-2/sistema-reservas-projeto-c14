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
./mvnw test
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

```json
POST /reservas
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
| RNF04 | Respostas de erro sempre no formato `{status, mensagem, detalhes}`. | `ErroResponse` | Atendido |
| RNF05 | Cobertura de linhas dos testes de no mínimo 40%; abaixo disso o build fica instável. | Quality gate no Jenkins | Atendido |
| RNF06 | Toda alteração passa por build, testes, cobertura e deploy automáticos. | `Jenkinsfile` | Atendido |
| RNF07 | Credenciais do banco configuráveis por variáveis de ambiente (`DB_USERNAME`, `DB_PASSWORD`). | `application.properties` | Atendido |
| RNF08 | Senhas armazenadas com hash (BCrypt), nunca em texto puro. | — | Pendente |

## CI/CD

A pipeline está no `Jenkinsfile` e roda em um agente Windows:

1. **Build:** compila e gera o `.jar`.
2. **Testes:** executa os testes e publica os resultados do JUnit.
3. **Cobertura:** publica o relatório do JaCoCo e aplica o quality gate de 40% de linhas.
4. **Deploy:** sobe a aplicação na porta 8081 e verifica se ela responde.

## Equipe

- Raphael Ferreira de Oliveira
- Vinícius Santos Araújo
