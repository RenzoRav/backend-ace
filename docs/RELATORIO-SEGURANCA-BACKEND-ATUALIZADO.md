# Relatório atualizado — segurança e validação

**Data:** 11/10/2026, America/Sao_Paulo. **Projeto:** `C:\Users\renzo\Documents\backend-loja`. **Branch:** `codex/backend-spring-boot`.

## 1. Resultado executivo

O backend recebeu controles de autenticação, sessões, validação, configuração e limites de consultas, acompanhados de testes negativos e de concorrência. A suíte completa executou **65 testes, zero falhas e zero erros**, e gerou o bootJar. A consulta atual a **95 dependências Maven de runtime retornou zero avisos OSV**, sem páginas pendentes. Isso descreve a consulta realizada, não garante ausência de vulnerabilidades desconhecidas.

A recuperação dos fontes anteriores foi testada numa cópia e o backup do banco foi restaurado em PostgreSQL isolado, sem substituir o banco principal. A versão recuperada passou nos **25 testes anteriores**. A API atual foi iniciada e sua porta 8080 foi observada exclusivamente em **127.0.0.1**.

**Limite importante:** o frontend encontrado em `C:\Users\renzo\Documents\frontend-loja` mantém autenticação e dados demonstrativos locais. Não foi encontrada integração real de login/refresh com esta API nessa cópia. Portanto, os controles descritos protegem o backend, e esta entrega não certifica nem demonstra integração de autenticação, renovação compartilhada ou paginação no frontend. Seus 42 testes existentes e a checagem de tipos passaram, como regressão do protótipo, não como testes de integração com o servidor.

## 2. Controles implementados

### Dependências e produção

- Tomcat foi atualizado de 11.0.24 para **11.0.26**; Jackson core/databind, de 3.1.5 para **3.1.7**. As versões foram confirmadas no runtime resolvido pelo Gradle, não apenas no arquivo de build.
- Produção exige segredo JWT externo com pelo menos 32 bytes e rejeita valores conhecidos de desenvolvimento/exemplos. Essa verificação não mede a entropia de uma chave arbitrária: sua geração e guarda continuam responsabilidade da operação.
- Produção exige origem HTTPS exata, cookie Secure e bootstrap desabilitado. A combinação prod com dev/test impede a inicialização.
- Desenvolvimento escuta em 127.0.0.1. O launcher foi ajustado para verificar a saúde por esse endereço, evitando a falha observada quando localhost resolvia para IPv6.
- Foram definidos limites de cabeçalhos de 16 KiB, conexão de 10 segundos, 200 conexões e 100 threads no Tomcat. Esses valores precisam ser dimensionados com testes de carga antes da publicação.

Evidências: [configuração de segurança](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/config/SecurityConfig.java), [validador de ambiente](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/config/SecurityEnvironmentValidator.java) e [inventário atualizado](C:/Users/renzo/Documents/backend-loja/docs/security/DEPENDENCIAS-2026-10-11.json). Os avisos e correções do contêiner constam na [fonte oficial Tomcat](https://tomcat.apache.org/security-11.html).

### Limitação de autenticação

Os orçamentos usam PostgreSQL, atualização atômica e janelas com expiração. Não existe bloqueio permanente da conta.

| Controle | Orçamento padrão |
|---|---|
| Login e demais chamadas de autenticação sem orçamento próprio, por IP | 30/minuto |
| Login, por realm e conta normalizada | 10/5 minutos |
| Cadastro, por IP | 5/10 minutos |
| Refresh, por IP | 60/minuto |
| Troca de senha, por identidade autenticada | 5/5 minutos |
| Autenticação global, por banco compartilhado | 300/minuto |

As rejeições retornam 429 e Retry-After. Limites por conta são aplicados antes de BCrypt. O endereço usado é o remoto da conexão; X-Forwarded-For arbitrário não decide a identidade do cliente. Chaves de conta são hashes, com tamanho limitado; contadores expirados são limpos em lotes. Falha do armazenamento do limitador rejeita a autenticação com 503 genérico. Os testes gerais usam orçamentos maiores para não consumir uns aos outros; testes específicos demonstram os limiares e a expiração.

Foram testados orçamento concorrente, renovação de janela e uma corrida entre limpeza e atualização. A limpeza agora revalida a expiração na linha efetivamente apagada, evitando remover um contador recém-renovado. Evidências: [repository](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/repository/AuthenticationLimitRepository.java), [service](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/service/AuthenticationLimitService.java). Referência: [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).

### Famílias de sessão e prevenção de replay

- Login cria uma família de sessão. Refresh rotaciona dentro da mesma família, com bloqueios consistentes no banco.
- Reutilização de refresh token consumido revoga a família. O filtro verifica sua validade em cada uso do JWT; o sucessor e os access tokens dessa família deixam de autorizar acessos.
- Outras famílias legítimas da conta permanecem válidas após replay. A troca de senha e o logout válido preservam a revogação ampla já existente no projeto; um logout obsoleto não encerra um login novo no cenário de regressão testado.
- Access token dura 15 minutos. Sua validação exige iat e exp, duração positiva de no máximo 900 segundos, issuer/audience válidos e algoritmo HS256. iat excessivamente futuro é rejeitado. A tolerância temporal do validador padrão continua aplicável.
- Refresh possui janela de inatividade de sete dias, limitada ao término absoluto da família em 30 dias.
- Refresh tokens continuam aleatórios e somente seu hash é persistido. A migração V6 revoga tokens legados sem família; sessões anteriores exigem novo login.
- Uma falha da gravação independente de auditoria não desfaz a revogação por replay. Nesse caso, um registro operacional mínimo sinaliza indisponibilidade da auditoria, sem credenciais ou tokens.

Os testes reproduziram a falha anterior em que uma exceção de auditoria revertia a revogação, e passaram após a correção. Também cobrem refresh simultâneo, expiração de inatividade/absoluta, sucessor revogado, família independente e uso de JWT após replay. Evidências: [AuthService](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/service/AuthService.java), [filtro de credenciais](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/security/CredentialVersionFilter.java). A relação entre rotação, detecção de reutilização e revogação segue os princípios da [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14); não se afirma implementação de um servidor OAuth completo.

### Validação, requisições e consultas

- Email até 255 caracteres; telefone de entrada até 32, preservando normalização brasileira; campos de endereço limitados; CEP e UF validados; aniversários futuros rejeitados.
- Senhas novas preservam mínimo de dez caracteres e máximo de **72 bytes UTF-8**, compatível com BCrypt. Login/senha atual também recebem limite em bytes. Não ocorre truncamento ou normalização de senha.
- IDs positivos e representáveis em Long; validação dos elementos de coleções e rejeição de itens nulos. Permanecem as regras de valores monetários, quantidade e intervalos de campanhas no servidor.
- Corpo JSON limitado a **256 KiB durante a leitura**, independentemente de Content-Length, retornando 413 ao exceder. Entrada JSON inválida retorna 400 sem detalhe interno. O filtro lê um buffer limitado, não um corpo ilimitado para depois medir.
- Pedidos, endereços e chamados da cliente possuem variantes explícitas `/paged`, com page não negativo e size entre 1 e 100; o banco pagina antes de carregar resultados, e o filtro de propriedade continua obrigatório.
- Rotas legadas dessas três listas continuam entregando listas até 100 registros; acima disso, retornam 413 com indicação da rota paginada. Não há truncamento silencioso. Consumidores que desejarem mais registros precisam adotar as novas rotas.
- Testes demonstram duas páginas sem perda/duplicação, isolamento entre clientes e rejeição explícita do limite legado.

Evidências: [validadores](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/validation/Utf8ByteSizeValidator.java), [limite de corpo](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/security/RequestBodyLimitFilter.java), [paginação de clientes](C:/Users/renzo/Documents/backend-loja/src/main/java/br/com/loja/service/CustomerService.java). Referência: [OWASP Input Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html).

### Auditoria e proteção de cookies

Eventos de login aceito/rejeitado, cadastro aceito, refresh aceito/rejeitado, replay, logout, troca de senha e limites de autenticação são gravados com códigos fixos e referências hasheadas. A gravação independente sobrevive ao rollback de negócio no caso testado. Senhas, JWTs, cookies e refresh tokens não são campos de evento. Hashes são pseudonimização, não anonimização garantida.

Contadores expirados e eventos com mais de 30 dias são limpos em lotes. A validação de Origin em rotas de cookie foi preservada e ajustada ao caminho decodificado do servlet. Cookies são HttpOnly e SameSite=Strict, Secure em produção e possuem caminho restrito que inclui o context path. Um teste com cliente HTTP e armazenamento de cookies demonstra login/refresh em `/shop`; isso não substitui testes em todos os navegadores.

Respostas de autenticação recebem no-store. As verificações locais observaram nosniff e X-Frame-Options: DENY. A desativação de CSRF não foi tratada isoladamente como vulnerabilidade: os controles de Bearer, Origin, cookie e CORS devem ser avaliados juntos. Referência: [OWASP Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html).

## 3. Verificação executada

| Verificação | Resultado observado |
|---|---|
| Baseline: gradlew.bat test --rerun-tasks --console=plain | BUILD SUCCESSFUL in 27s; exit 0; 25 testes |
| Final: gradlew.bat clean test bootJar --console=plain | BUILD SUCCESSFUL in 39s; exit 0; 65 testes, zero falhas/erros |
| Fonte anterior restaurada + banco recuperado isolado | BUILD SUCCESSFUL in 27s; exit 0; 25 testes |
| OSV runtime | 95 pacotes, 95 resultados, zero avisos, zero páginas pendentes |
| Frontend existente: npm test | 42 testes, 42 aprovados, zero falhas |
| Frontend existente: npm run typecheck | exit 0 |
| GET /api/v1/health | 200, UP |
| Rota privada sem credencial / JWT inválido | 401 / 401 |
| Refresh sem Origin / Origin estrangeiro | 403 / 403 |
| Listener de desenvolvimento observado | 127.0.0.1:8080 |
| Banco principal após inicialização | Flyway V1–V6 |

Também passaram os testes de cookies em produção, bloqueio de simulação de pagamento fora de dev/test sem novo movimento financeiro, JWT adulterado/sem assinatura, claims ausentes/incorretas, gestora inativa e proteção de propriedades já coberta pela suíte existente. A revisão independente encontrou dois defeitos importantes, ambos reproduzidos com testes que falharam antes e passaram depois. A nova revisão não apontou regressão crítica/importante confirmada nos arquivos examinados.

Logs e evidências: `C:\Users\renzo\Documents\seguranca-backend-2026-10-10`. A data do diretório identifica o início do trabalho; este relatório e a conclusão da verificação são de 11/10/2026.

## 4. Riscos e providências restantes

1. **Integração frontend — pendente:** a interface atual é demonstrativa. Login real, compartilhamento de renovação entre requisições/abas e consumo da paginação não foram integrados ou demonstrados nesta entrega. A política estrita de replay pode encerrar uma família se um consumidor enviar o mesmo refresh simultaneamente; clientes reais precisam coordenar a renovação.
2. **Publicação — alta prioridade:** configurar HTTPS/TLS, proxy confiável, origem real e hosts compatíveis com cookies SameSite. O teste local não demonstra configuração externa, firewall, certificados ou gateway. Nunca publicar o perfil dev por meio de proxy como se fosse produção.
3. **Segredos e privilégios — alta prioridade:** gerar chave JWT aleatória, guardar fora do repositório, separar credenciais de migração e runtime e restringir acesso ao banco. A role local de aplicação observada não tem SUPERUSER nem CREATEDB, mas é proprietária dos bancos da aplicação e possui poderes de DDL; isso não equivale à separação de privilégios recomendada para produção.
4. **Disponibilidade:** limites da aplicação não eliminam ataques volumétricos ou saturação anterior ao backend. Validar capacidade, janelas e orçamento global sob carga, adicionar proteção no gateway e sincronizar relógios entre instâncias.
5. **Auditoria:** aplicar controle de acesso, retenção organizacional, coleta externa e alertas. Em falha de auditoria de replay, a revogação é prioritária; a trilha no banco pode ficar incompleta e o log operacional precisa ser monitorado. Outros eventos rejeitados antes do controller não constituem uma trilha universal de todas as requisições.
6. **Evolução:** planejar MFA para gestora, recuperação segura de conta, gestão/rotação de segredos, limpeza de histórico de sessões expiradas e revisão periódica de permissões conforme o cenário de publicação. Esses recursos não foram adicionados como simulações de segurança.
7. **Cobertura:** não houve pentest externo, exploração destrutiva, benchmark de carga, auditoria de JDK/OS/imagens/plugins ou certificação ASVS. A consulta OSV cobre o runtime Maven resolvido; informações publicadas podem mudar. Referência de método: [API OSV](https://google.github.io/osv.dev/post-v1-querybatch/).

## 5. Commits e recuperação

Os controles foram separados em commits por funcionalidade: `2726352` configuração/dependências; `10e3806` validação; `a935b31` limite de JSON; `dc0901d` limitação/auditoria; `46a2831` famílias de sessão; `3365039` paginação; `af786fe` correções de falha/concorrência e testes ampliados. A correção do launcher e este relatório recebem commit próprio na conclusão.

As alterações locais anteriores de README, documentação de desenvolvimento, migração V1 e ferramentas foram preservadas, sem incluí-las nos commits de segurança. V5/V6 foram acrescentadas; V1 não foi reescrita por esta implementação.

O snapshot original possui hash preservado. `ROLLBACK.sh` valida esse hash e restaura **apenas** a cópia `rollback-copy`; ele não desfaz o banco ou o projeto principal. O backup anterior do banco foi restaurado em uma instância temporária de PostgreSQL em loopback, e essa instância foi encerrada após os testes. Uma recuperação de produção requer restauração coordenada de código e banco; simplesmente executar o código antigo contra V5/V6 não representa rollback completo.

**Conclusão:** reforço de segurança e validação do backend implementado e verificado. Imports continuam no início dos arquivos. Os riscos de implantação e a integração real do frontend permanecem explicitamente separados dos resultados comprovados da API.
