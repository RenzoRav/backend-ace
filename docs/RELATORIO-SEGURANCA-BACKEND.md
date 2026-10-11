# Relatório de segurança do backend

Data: 10/10/2026 — America/Sao_Paulo. Projeto: `C:\Users\renzo\Documents\backend-loja`. Branch: `codex/backend-spring-boot`.

## Resumo executivo

O backend possui controles importantes de autenticação, autorização e integridade dos pedidos. A revisão e os testes executados não demonstraram um desvio de autenticação, de propriedade dos dados ou de cálculo financeiro no perfil padrão. Isso não constitui certificação de segurança.

Prioridades: atualizar dependências com avisos publicados, restringir a execução de desenvolvimento ao computador local e limitar tentativas de autenticação. Existem ainda oportunidades de melhoria em revogação de famílias de refresh tokens, registros de segurança e limites das entradas.

Nesta entrega foram reorganizadas referências de tipos em **26 arquivos Java**, mantendo os imports no início, depois do package. Um teste de regressão verifica referências qualificadas no corpo dos arquivos usando a árvore sintática Java. A mudança não implementa as correções de segurança recomendadas abaixo.

## Escopo e método

- Revisão do código Java, configurações, launcher, DTOs, persistência, autenticação e fluxos de pedidos; revisão independente somente de leitura.
- Execução dos testes de integração e arquitetura com PostgreSQL de testes: baseline de 24 testes; versão alterada com **25 testes, zero falhas e zero erros**, incluindo compilação e bootJar.
- Consultas HTTP locais, sem criação de pedidos ou alteração de dados comerciais.
- Inventário de **95 artefatos Maven de runtime** resolvidos pelo Gradle e consulta por pacote/versão à [API oficial OSV](https://google.github.io/osv.dev/post-v1-querybatch/). Foram obtidos 95 resultados, sem continuação pendente, e **10 avisos em três artefatos**. Nenhum desses registros estava retirado na consulta.
- Evidência de dependências: [inventário e avisos](C:/Users/renzo/Documents/backend-loja/docs/security/DEPENDENCIAS-2026-10-10.json). Logs locais: `C:\Users\renzo\Documents\auditoria-backend`.

Os avisos de bibliotecas indicam versões afetadas, não necessariamente caminhos exploráveis neste aplicativo. Não foram executadas cargas destrutivas, exploração de negação de serviço, avaliação externa de rede, auditoria de infraestrutura de produção ou varredura histórica de segredos. A consulta de dependências não cobre JDK, sistema operacional, PostgreSQL, imagens, plugins ou dependências exclusivas de build/teste. O processo local iniciado para as verificações utilizava o perfil dev; os testes e a compilação verificaram os fontes alterados.

## Achados e recomendações

### S01 — Dependências com avisos de segurança publicados — prioridade alta

**Confirmado:** runtime resolve Tomcat `11.0.24`, Jackson core `3.1.5` e Jackson databind `3.1.5`, versões associadas aos avisos abaixo. As severidades são as dos avisos, não uma classificação de exploração comprovada do aplicativo.

| Dependência | Aviso | Severidade publicada | Condição relevante | Correção na mesma linha |
|---|---|---|---|---|
| Tomcat core | [GHSA-9xv2-5v5q-p794](https://github.com/advisories/GHSA-9xv2-5v5q-p794) | Crítica | Captura/replay em autenticação DIGEST; este código usa JWT Bearer, sem DIGEST configurado | 11.0.25 |
| Tomcat core | [GHSA-gcx9-497g-6cp6](https://github.com/advisories/GHSA-gcx9-497g-6cp6) | Crítica | Ordenação de security constraints do contêiner; não foram observadas essas constraints no projeto, que usa Spring Security | 11.0.25 |
| Tomcat core | [GHSA-h3x4-894j-xpx5](https://github.com/advisories/GHSA-h3x4-894j-xpx5) | Crítica | Autenticação FORM do contêiner; não configurada neste código | 11.0.25 |
| Jackson core | [GHSA-7hhh-6rmp-j9qf](https://github.com/advisories/GHSA-7hhh-6rmp-j9qf) | Alta | Token inválido em parser DataInput; não foi localizado uso desse parser pela aplicação | 3.1.7 |
| Jackson core | [GHSA-p6pp-m3f8-5c89](https://github.com/advisories/GHSA-p6pp-m3f8-5c89) | Alta | Expressão regular de validação numérica com custo quadrático; alcance por entradas do aplicativo não demonstrado | 3.1.7 |
| Jackson databind | [GHSA-cxp5-3px4-pw24](https://github.com/advisories/GHSA-cxp5-3px4-pw24) | Alta | Resolução quadrática de referências futuras; DTOs revisados não declaram identidade JSON desse tipo | 3.1.7 |
| Jackson databind | [GHSA-gx83-3vf8-gh7j](https://github.com/advisories/GHSA-gx83-3vf8-gh7j) | Moderada | Validador de tipos polimórficos com Comparable; default typing não observado | 3.1.6 |
| Jackson databind | [GHSA-q4xh-88c3-wmh7](https://github.com/advisories/GHSA-q4xh-88c3-wmh7) | Alta | Desserialização de Duration/XMLGregorianCalendar; esses tipos não aparecem como entradas públicas nos DTOs revisados | 3.1.6 |
| Jackson databind | [GHSA-wjgm-6hv5-3cvf](https://github.com/advisories/GHSA-wjgm-6hv5-3cvf) | Moderada | Desserialização de Path e providers de filesystem; não há entrada Path pública observada | 3.1.6 |
| Jackson databind | [GHSA-wv8q-qhhj-9h54](https://github.com/advisories/GHSA-wv8q-qhhj-9h54) | Alta | Cache ilimitado de IDs de tipos desconhecidos com Id.NAME/defaultImpl; configuração não observada nos DTOs | 3.1.7 |

**Ação:** atualizar usando uma versão compatível do gerenciamento de dependências do Spring Boot ou ajuste coordenado e testado. Na linha atualmente usada, Tomcat 11.0.25 e Jackson core/databind 3.1.7 abrangem as correções listadas. Reexecutar resolução, consulta OSV e testes após a atualização. Consultar também os [avisos oficiais Tomcat](https://tomcat.apache.org/security-11.html). Não foi feita atualização de bibliotecas nesta refatoração de imports.

### S02 — Perfil de desenvolvimento com configuração inadequada para exposição externa — alta, condicional

**Evidência:** `C:\Users\renzo\Documents\backend-loja\start-backend.ps1` seleciona dev; `src\main\resources\application-dev.properties`, linhas 2–6, habilita bootstrap, valores previsíveis de desenvolvimento e cookie sem Secure. `application.properties` não define server.address. O listener local observado foi **[::]:8080**, não exclusivamente loopback. Isso não comprova acesso externo: firewall e rede não foram avaliados.

`C:\Users\renzo\Documents\backend-loja\src\main\java\br\com\loja\service\OrderService.java`, linhas 388 em diante, permite simulação de pagamento em dev/test. Esse comportamento é intencional para o protótipo, mas inadequado em uma publicação que pretenda pagamentos reais. O perfil padrão exige segredo JWT externo, bootstrap desabilitado e cookie Secure.

**Ação:** configurar dev com server.address=127.0.0.1; criar bloqueios de inicialização para publicação com dev/test, segredo conhecido ou cookie inseguro; separar explicitamente simulação de pagamentos do ambiente de produção. Não copiar valores de desenvolvimento para produção.

**Teste a acrescentar:** inicialização de produção sem segredo válido deve falhar; simulação deve retornar 403 fora de dev/test e não alterar pedidos/recebimentos.

### S03 — Ausência de limitação de tentativas de login na aplicação — média

**Evidência:** `C:\Users\renzo\Documents\backend-loja\src\main\java\br\com\loja\controller\AuthController.java` expõe login e cadastro; `AuthService.java`, linhas 60–99, processa autenticação sem limitador. BCrypt usa custo 12 em `SecurityConfig.java`, linha 26. Não foi localizado rate limiter, CAPTCHA adaptativo ou atraso progressivo no código. Seis logins inválidos locais consecutivos receberam 401, sem 429; esta pequena amostra não é um teste de carga nem prova ausência de um controle externo.

**Impacto:** tentativas automatizadas de credenciais e consumo de CPU. CORS não impede clientes HTTP fora do navegador. Um gateway externo pode mitigar, mas sua existência/configuração não foi verificada.

**Ação:** limites combinados por IP, conta normalizada e volume global; atraso progressivo; resposta 429 com recuperação controlada. Evitar bloqueio permanente facilmente induzido por terceiros. Referência: [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).

**Teste a acrescentar:** limiar de tentativas, expiração do limite e proteção contra bloqueio indevido de contas.

### S04 — Refresh tokens sem revogação de família por replay ou idade absoluta — média, endurecimento

**Evidência:** `C:\Users\renzo\Documents\backend-loja\src\main\java\br\com\loja\service\AuthService.java`, método refresh, linha 153 em diante, revoga o token usado e emite sucessor. `src\main\java\br\com\loja\model\RefreshSession.java` não registra família/sucessor. Cada emissão abre nova validade de sete dias sem idade absoluta de sessão observada.

**Condição:** requer comprometimento prévio do refresh token. Se um atacante o rotacionar primeiro, o replay legítimo é rejeitado, mas não revoga automaticamente o sucessor do atacante. Não é um bypass de autenticação sem credenciais.

**Ação:** identificar famílias de tokens, revogar a família ao detectar reutilização e impor duração absoluta. Garantir que a revogação persista mesmo quando a resposta é um erro transacional. Preservar o controle que impede um logout antigo de encerrar um login novo. Referência: [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

**Teste a acrescentar:** replay revoga sucessor; refresh concorrente tem resultado determinístico; idade absoluta impede renovação infinita.

### S05 — Auditoria funcional não cobre eventos de autenticação — baixa

**Evidência:** `C:\Users\renzo\Documents\backend-loja\src\main\java\br\com\loja\service\AuditService.java`, linhas 15–22, registra ações de gestora. Os fluxos de AuthService não produzem registros dedicados de falha de login, replay de refresh, logout e troca de senha.

**Ação:** eventos estruturados com resultado, identificador mínimo e correlação; restringir acesso e retenção; nunca registrar senhas, JWTs, cookies ou refresh tokens. Referência: [OWASP Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html).

**Teste a acrescentar:** geração dos eventos e ausência de segredos nas mensagens.

### S06 — Limites de entrada e consultas acumulativas incompletos — baixa

**Evidência:** `C:\Users\renzo\Documents\backend-loja\src\main\java\br\com\loja\dto\AuthDtos.java`, linhas 7–13 e 25–26, não limita explicitamente todos os campos de email, telefone e senha atual. CustomerDtos possui campos de endereço sem limites explícitos. Listagens de histórico de pedidos, tickets e endereços podem crescer sem paginação em OrderService, RelationshipService e CustomerService.

**Impacto:** custo crescente de processamento/resposta e erros tardios no banco. Não foi demonstrada execução de código ou exposição de dados por esses campos.

**Ação:** alinhar limites dos DTOs ao esquema, limitar corpo de requisição, impor quotas e paginação. Referência: [OWASP REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html).

**Teste a acrescentar:** entradas excessivas retornam 400 de forma previsível; limites de páginas e tamanho máximo são respeitados.

## Controles positivos identificados

- JWT com HS256 explícito, validação de issuer/audience/tempo e duração de acesso de 15 minutos.
- BCrypt custo 12 e verificação equivalente com hash substituto quando a conta não existe.
- Refresh tokens aleatórios de 32 bytes, apenas hash SHA-256 persistido, rotação, validade de sete dias e locks de persistência.
- Verificação da versão de credenciais e do estado da gestora a cada acesso; revogação em logout/troca de senha.
- Papéis MANAGER/CUSTOMER, verificação de propriedade de pedidos, endereços e tickets; cliente do checkout derivado da identidade autenticada.
- DTO público de catálogo sem custo; senha/hash não integra resposta pública de conta.
- Preços calculados no servidor, transações, locks, validação de quantidades/valores e idempotência dos fluxos relevantes.
- Consultas JPA parametrizadas/Criteria, erros de persistência genéricos e mensagem interna de erro não publicada pelo servidor.
- PostgreSQL local configurado para loopback no launcher/compose.

**CSRF:** sua desativação não foi classificada isoladamente como falha. As rotas de negócio usam Bearer; refresh/logout validam Origin exato, inclusive rejeitando ausência; cookies são HttpOnly, SameSite=Strict e têm caminho restrito. Secure é padrão, com exceção dev descrita em S02. CORS permite uma origem configurada. A avaliação deve considerar esse conjunto, não apenas o sinalizador csrf.

## Resultados observados

| Verificação | Resultado |
|---|---|
| Baseline: gradlew.bat test --rerun-tasks --console=plain | BUILD SUCCESSFUL in 19s; exit 0; 24 testes |
| Teste novo antes da correção | BUILD FAILED in 8s; exit 1; uma falha esperada por referências qualificadas |
| Alterado: gradlew.bat clean test bootJar --console=plain | BUILD SUCCESSFUL in 25s; exit 0; 25 testes, zero falhas/erros |
| GET /api/v1/health | 200 |
| GET /api/v1/customer/orders sem autenticação | 401 |
| Mesma rota com Bearer inválido | 401 |
| POST refresh sem Origin / Origin estrangeiro | 403 / 403 |
| Seis logins inválidos de conta inexistente | 401 em todas as tentativas |
| Cabeçalhos das respostas locais acima | X-Content-Type-Options: nosniff; X-Frame-Options: DENY |

Os testes existentes cobrem controles como papéis, dados de credenciais, propriedade, alteração de senha/revogação, refresh obsoleto, logout antigo, preços, reembolsos, idempotência e concorrência de estoque. Permanecem necessários casos negativos específicos para assinatura/issuer/audience/algoritmo/expiração de JWT, matriz completa de papéis, gestora inativa, cookie Secure em produção, refresh concorrente e os casos propostos em cada achado. Não foram atribuídos resultados a testes ainda não executados.

## Plano de melhoria

1. **Antes de publicar:** atualizar dependências afetadas e repetir SCA/testes; garantir perfil de produção, segredos exclusivos, TLS/cookies Secure e ausência de simulação de pagamento.
2. **Curto prazo:** limitar autenticação, restringir listener dev e acrescentar testes negativos de autenticação/origem/perfis.
3. **Próxima etapa:** revogação de família/idade absoluta, auditoria de segurança, limites de entrada e paginação.

Conclusão: imports organizados e build/testes aprovados. O backend dispõe de boas bases, mas os achados acima continuam pendentes; passar nos testes atuais não comprova ausência de vulnerabilidades.
