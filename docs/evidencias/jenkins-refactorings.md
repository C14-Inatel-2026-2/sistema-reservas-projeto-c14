# Jenkins antes e depois dos refactorings

As duas referências usam os mesmos testes, BCrypt e infraestrutura de deploy. No código de produção, a diferença é a extração dos dois métodos em `ReservaService`.

Use um job Pipeline separado para validação, com agente Windows e JDK 21:

| Campo | Valor |
|---|---|
| Definition | Pipeline script from SCM |
| SCM | Git |
| Repository URL | `https://github.com/vin1sss/sistema-reservas-projeto-c14.git` |
| Branch Specifier, primeiro build | `*/ci/reservas-antes` |
| Branch Specifier, segundo build | `*/refactor/reservas` |
| Script Path | `Jenkinsfile` |

Executar primeiro a referência anterior e depois a posterior. Conferir em cada build: SHA, resultado SUCCESS, 48 testes Java e seis testes PowerShell sem falhas, JUnit publicado e relatório JaCoCo com pelo menos 40% de linhas. Guardar os links e prints dos dois builds.

O stage Deploy deve ficar sem execução nessas branches. A publicação é reservada à `main` e será validada após configurar Render e credenciais.

Resultados locais em 12/11/2026: 64,18% de cobertura antes e 64,71% depois. Esses resultados ainda precisam ser confirmados no Jenkins.

Para integrar o código, revisar e fazer merge do PR #11 antes do PR #8. O PR #8 já contém a restauração do BCrypt, preservando as duas contribuições.
