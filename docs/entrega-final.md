# Conferência da entrega final

Atualizado em 14/11/2026. **Prazo prorrogado: 14/11/2026 às 15h (Brasília).** Raphael envia o link do repositório na tarefa **Refactoring + Entrega final** do Teams. O envio ainda precisa ser confirmado.

## Critérios da NP2

| # | Critério | Evidência disponível | Falta concluir |
|---|---|---|---|
| 1 | Aplicação funcional | [API pública](https://sistema-reservas-c14.onrender.com/actuator/health), [aceite e persistência](evidencias/deploy-publico.json). | Repetir o aceite após o deploy da versão final, incluindo as funcionalidades do PR #14. |
| 2 | Gerenciamento de dependências | [Maven](../backend/pom.xml), wrapper e build reproduzido com JDK 21. | Guardar o build da `main` final no Jenkins. |
| 3 | Testes unitários relevantes | 55 testes Java e seis PowerShell, sem falhas; 65,90% de linhas na [validação local](evidencias/validacao-local-final.json). | Guardar JUnit e JaCoCo do build final. |
| 4 | CI/CD | [Jenkinsfile](../Jenkinsfile), quality gate de 40% e deploy por SHA. A E4 foi avaliada positivamente. | Confirmar as credenciais, executar a pipeline da `main` e conferir o SHA público. |
| 5 | Versionamento e contribuições | Contribuições dos dois integrantes, com código, testes, pipeline e dois refactorings por pessoa. | Conferência dos integrantes e esclarecimento da participação de Eduardo com o monitor. |
| 6 | Revisão por PR | PRs #8, #11, #12, #13 e #14 integrados; aprovações de Vinícius em [#13](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/13#pullrequestreview-5896455373) e [#14](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/14#pullrequestreview-5904414254). | Revisar e integrar este fechamento documental. |
| 7 | README completo | Instalação, execução, endpoints, requisitos, URL, rastreabilidade e refactorings. | Conferir a versão final integrada e retirar pendências somente com evidência. |
| 8 | Histórias de usuário | Sete histórias no Jira, com critérios e rastreabilidade no README; [12 critérios públicos verificados](evidencias/criterios-publicos.json). US03 e US05 implementadas no PR #14. | Aceite público das novas funcionalidades e atualização dos cartões após conferir o DoD. |
| 9 | Metodologia de desenvolvimento | Papéis, acompanhamento, DoR/DoD e métricas reais do repositório no README. | Registrar revisão, retrospectiva e métricas de encerramento da sprint. |
| 10 | Dinâmica de desenvolvimento | Divisão real, fork, impedimentos, conflito de integração e lições no README. | Conferência do relato pelos integrantes. |
| 11 | Refactoring | Quatro extrações integradas em [#8](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/8) e [#13](https://github.com/C14-Inatel-2026-2/sistema-reservas-projeto-c14/pull/13), com resumos antes/depois e revisões. | Preservar links ou prints dos builds relatados pelo Raphael, além dos resumos versionados. |
| 12 | Uso transparente de IA | Declaração geral dos dois integrantes no README. | Conferência das informações pelos integrantes. |
| 13 | Defesa Q&A | [Guia de estudo](defesa.md), arquivos, testes e PRs identificados. | Confirmar data do grupo e realizar revisão conjunta e simulação de até 20 minutos. |
| 14 | Engenharia de Software geral | Requisitos, código, testes e pipeline relacionados no README. | Conferir o mesmo estado final no GitHub, Jenkins, Jira e aplicação. |

O quadro registra evidências e pendências; não atribui notas nem substitui a avaliação da disciplina.

## Estado integrado

- `main` conferida: `8ebce6e1fa08057bcad578bd0ed03e663255946b`, com PRs #13 e #14 integrados.
- A árvore dessa versão é idêntica à combinação local testada na revisão do PR #14: 55 testes Java e seis PowerShell, cobertura de 65,90%. [Resumo](evidencias/validacao-local-final.json).
- Os quatro refactorings estão integrados. O README registra Jenkins verde antes/depois conforme os resultados fornecidos por Raphael; os arquivos JSON de 12/11 são registros locais daquela data.
- Os resumos dos builds de Raphael estão em [antes](evidencias/refactor-recursos-antes.json) e [depois](evidencias/refactor-recursos-depois.json). As referências de Vinícius são `0466e2c` e `29ee4af`, descritas no README e no [guia de validação](evidencias/jenkins-refactorings.md).
- O aceite público e a persistência já comprovados pertencem a `1dbd0f7`. Esses registros permanecem históricos até nova validação na versão final.
- O hook foi disponibilizado para a configuração do Jenkins. O cadastro das credenciais e a execução final ainda precisam de confirmação.

## Conferência antes do envio

1. Integrar a documentação revisada e executar a pipeline da `main`.
2. Guardar build bem-sucedido, JUnit, JaCoCo e SHA completo. A cobertura de linhas deve ser de pelo menos 40%.
3. Conferir `/actuator/health` com `UP` e `/actuator/info` com o mesmo SHA do GitHub e do Jenkins.
4. Executar `scripts/Test-CriteriosApi.ps1` com dados fictícios. Validar também desativação/reativação e bloqueio de cancelamento após o início, incluídos no PR #14.
5. Guardar uma reserva confirmada, realizar outro deploy e conferir sua persistência com `scripts/Test-DeployExterno.ps1 -Fase Conferir`. Aguardar o novo deploy ficar `Live`, mesmo ao publicar o mesmo SHA.
6. Atualizar Jira conforme os critérios comprovados; registrar revisão e retrospectiva reais. Confirmar acesso do monitor.
7. Conferir os links e evidências, preservando credenciais fora dos arquivos e prints.
8. Raphael envia o link do repositório da organização no Teams antes das 15h e guarda a confirmação. A conclusão do código não comprova o envio.

## Defesa

As apresentações foram anunciadas para 16/11 a 04/12; o horário específico do grupo ainda precisa ser confirmado. Ambos devem explicar o projeto inteiro e abrir código, testes, commits e builds quando solicitado. Antes da apresentação, iniciar o Jenkins e acessar o Render para aguardar a retomada do serviço gratuito.
