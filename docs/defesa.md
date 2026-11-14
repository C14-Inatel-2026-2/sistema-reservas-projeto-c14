# Preparação para a defesa

Estudar o projeto inteiro e conferir cada explicação no código. A preparação inclui revisão conjunta e uma simulação de perguntas e respostas de até 20 minutos.

## Domínio e requisitos

**O que o sistema faz?** Gerencia usuários, recursos de uma instituição e reservas por período. Os recursos são salas de estudo, quadras e equipamentos de laboratório. A API cria reservas pendentes, permite confirmação e aplica regras de cancelamento.

**O que é requisito funcional nesse projeto?** Um comportamento oferecido pelo sistema, como recusar sobreposição de horários do mesmo recurso (RF10). Um requisito não funcional define uma condição de qualidade ou operação, como disponibilizar a API por HTTPS fora do Jenkins (RNF09).

**Como uma história chega ao teste?** O Jira registra objetivo, prioridade e critérios Dado/Quando/Então. O README liga a história ao PR e aos testes. Por exemplo, US03/SCRUM-7 está no PR #14 e em `RecursoServiceTest`; o caso de recurso inativo está em `ReservaServiceTest`.

**Como funciona a desativação?** `RecursoService` altera `ativo` dentro de uma transação. O cadastro e as reservas existentes permanecem. `ReservaService.criar` recusa novas reservas para recurso inativo. As rotas são `PATCH /recursos/{id}/desativar` e `/ativar`.

## Arquitetura e dados

**Qual o caminho de uma requisição?** Controller recebe o DTO e a validação de entrada; service aplica regras; repository acessa o banco; DTO de resposta define o JSON. `GlobalExceptionHandler` converte erros em HTTP 400, 404, 409 ou 422.

**Por que separar essas responsabilidades?** Regras no service podem ser testadas sem servidor HTTP ou banco real. DTOs separam o contrato da API das entidades persistidas, incluindo a remoção de senha/hash das respostas.

**Por que herança JOINED?** `Recurso` mantém os campos comuns em uma tabela, e cada subtipo possui sua tabela ligada pelo ID. `RecursoResponse.de` remove proxies com `Hibernate.unproxy` antes de escolher o mapeamento específico.

**Qual a diferença entre Flyway e Hibernate?** Flyway aplica a migration V1 versionada. Hibernate faz o mapeamento entre objetos e tabelas; `ddl-auto=none` preserva o schema gerenciado pelo Flyway. O TiDB teve `CHECK` habilitado antes da primeira migration.

**Por que alterar a entidade sem chamar save?** Nos métodos transacionais que carregam uma entidade pelo repositório, a entidade permanece gerenciada pelo JPA. A alteração é detectada e gravada no commit da transação. Conferir `confirmar`, `cancelar` e `alterarDisponibilidade`.

## Reservas e cancelamento

**Quando há conflito de horário?** A consulta considera reservas não canceladas do mesmo recurso: `inicioExistente < fimSolicitado` e `fimExistente > inicioSolicitado`. Intervalos são `[início, fim)`, permitindo períodos adjacentes. Reservas de recursos diferentes são independentes.

**Isso resolve duas requisições simultâneas?** A verificação seguida da gravação não garante exclusão entre requisições concorrentes. Essa limitação permanece; os testes existentes verificam os cenários sequenciais.

**Quando cancelar é recusado?** Se a reserva já está cancelada, se seu início já chegou ou passou, ou se a regra do recurso proíbe cancelamento. O bloqueio por horário vale para PENDENTE e CONFIRMADA. `ReservaServiceCancelamentoIniciadaTest` cobre os dois estados.

**Como funciona a multa?** O service captura o instante uma vez, calcula horas inteiras por `Duration.toHours()` e aplica o percentual se a antecedência for estritamente menor que a mínima. Sem regra, retorna zero. O sistema informa percentual; não realiza cobrança.

## Testes

**O que os testes com mocks comprovam?** Regras e interações em isolamento. Mockito substitui repositórios, sem executar sua consulta SQL. Um teste negativo passa quando confirma a rejeição esperada, como uma exceção para regra inexistente.

**Qual teste não usa mocks?** `ValidadorHorarioReservaTest` e seus testes de limites executam a classe real, incluindo intervalos adjacentes, sobreposição e virada do dia.

**O que o MockMvc verifica?** Requisições HTTP simuladas, validação, códigos e JSON do controller. A configuração standalone usada no projeto não inicia a aplicação completa com o banco.

**Como se verifica a aplicação com banco real?** Os scripts HTTP acessam a API conectada ao TiDB; verificam cadastro, reservas, conflitos e persistência após novo deploy. Esses testes complementam os unitários. A evidência pública histórica usa `1dbd0f7`; repetir na versão final.

**Qual a validação local mais recente?** 55 testes Java e seis PowerShell, sem falhas, com 65,90% de linhas na árvore integrada de `8ebce6e`. Conferir `docs/evidencias/validacao-local-final.json`. Cobertura mostra execução de linhas; as asserções e cenários determinam o que foi verificado.

## Dependências, segurança e CI/CD

**Qual o papel do Maven?** O POM define dependências e plugins. O wrapper reproduz a ferramenta; as fases compilam, testam e empacotam o JAR executável. O projeto exige JDK 21.

**BCrypt já autentica o usuário?** Protege o armazenamento da senha, com salt gerado pelo encoder e custo 10. A entrada tem limite de 72 bytes UTF-8 e a resposta não expõe senha/hash. Login e autorização continuam no backlog.

**O que a pipeline executa?** Build, testes Java/PowerShell, JUnit/JaCoCo, quality gate de 40% de linhas e deploy. Cobertura insuficiente torna o build instável e bloqueia a publicação. PRs e branches de desenvolvimento também não publicam.

**Qual a relação com integração contínua?** Automatizar build e testes fornece retorno sobre cada integração. A prática também depende de integrar alterações frequentemente e resolver falhas; os conflitos tardios do projeto mostram uma oportunidade de melhorar o fluxo.

**Como identificar a versão publicada?** O script envia o SHA testado ao hook do Render e espera saúde `UP` e o mesmo commit em `/actuator/info`. Comparar GitHub, Jenkins e API. O deploy público inicial foi acionado localmente; guardar separadamente a execução final pelo Jenkins.

**Por que Render e TiDB?** A API precisa executar fora da máquina do Jenkins. Docker fornece Java 21; o TiDB mantém os dados fora do disco temporário do Render. A conexão usa TLS `VERIFY_IDENTITY`, com credenciais por variáveis de ambiente. O serviço gratuito pode hibernar.

## Refactorings e processo

**Quais são os dois refactorings de Vinícius?** Extração de `montarReserva` e `calcularPercentualMulta` no PR #8. Separaram construção e cálculo das demais responsabilidades, preservando contrato, mensagens, status, regras e instante usado no cancelamento.

**Quais são os dois de Raphael?** Extração de mapeamentos por tipo em `RecursoResponse` e de helpers de preparação em `ReservaServiceTest`, no PR #13. Campos, tratamento de proxies, cenários e verificações foram preservados.

**Por que BCrypt e o PR #14 não contam como refactorings?** Eles alteram o comportamento oferecido. Refactoring reorganiza a estrutura preservando comportamento. Os testes de caracterização foram registrados separadamente das extrações.

**Como demonstrar a validação antes/depois?** Abrir os commits e os resumos em `docs/evidencias/`. Raphael registrou builds verdes das referências de ambos. Manter acessíveis os links ou prints desses builds e a pipeline final; a revisão local não substitui o Jenkins.

**Como o grupo trabalhou?** Raphael concentrou a API inicial e a pipeline; Vinícius ampliou suas contribuições com testes, BCrypt, deploy e refactorings. O fork contornou a falta de permissão. O Jira foi adotado em 30/10, com rastreabilidade retrospectiva do trabalho anterior.

**O quadro torna o processo Scrum automaticamente?** O grupo precisa explicar papéis, acompanhamento, critérios de entrada/conclusão e práticas realizadas. O README descreve Scrum adaptado e as limitações observadas; a revisão e a retrospectiva precisam de registro real.

**Como as ferramentas de IA participaram?** Conferir a declaração geral no README e explicar as decisões e validações efetivamente realizadas. Ambos devem conseguir navegar e executar o código que apresentam.

## Conferência antes da apresentação

Abrir serviços de reservas, recursos e usuários; DTOs; testes; migration; `Jenkinsfile`; scripts; PRs #8/#11/#13/#14; Jira; JaCoCo; builds antes/depois; URL pública e comprovante do envio. Confirmar data do grupo, iniciar o Jenkins e acessar o Render para permitir sua retomada. Cada integrante deve demonstrar uma decisão, um teste com mock, um sem mock, um caso negativo e um refactoring próprio.
