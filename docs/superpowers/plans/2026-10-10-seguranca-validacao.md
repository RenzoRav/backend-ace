# Segurança e validação — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (execução nesta sessão) ou superpowers:subagent-driven-development se escolhido pelo usuário. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o reforço de segurança aprovado conceitualmente, com validação, testes negativos, commits separados e novo relatório.

**Architecture:** Reforçar os fluxos atuais com persistência PostgreSQL para limites e famílias de sessão. Manter contratos externos quando possível; alterações necessárias de paginação e concorrência de refresh são entregues junto com seus consumidores frontend. Validadores e filtros têm responsabilidade isolada.

**Tech Stack:** Java 21, Spring Boot, Gradle, PostgreSQL, Flyway, Lombok, Nuxt e testes existentes.

**Spec:** `C:\Users\renzo\Documents\backend-loja\docs\superpowers\specs\2026-10-10-seguranca-validacao-design.md`.

## Global Constraints

- Backend: `C:\Users\renzo\Documents\backend-loja`; frontend: `C:\Users\renzo\Documents\frontend-loja`. Caminhos abaixo são relativos a essas raízes explicitamente identificadas.
- Imports ficam no início dos arquivos; nomes completos, sem variáveis abreviadas; conservar Lombok.
- Preservar mudanças locais anteriores; não reescrever V1 nem reparar seu histórico nesta entrega.
- Access token: 15 minutos. Refresh: inatividade de sete dias e término absoluto em 30 dias. Sessões anteriores exigirão novo login.
- Corpo JSON: 256 KiB. Senha nova: mínimo de dez caracteres e máximo de 72 bytes UTF-8, sem truncamento.
- Login: 30/IP/minuto e 10/conta/5 minutos; cadastro: 5/IP/10 minutos; refresh: 60/IP/minuto; senha: 5/identidade/5 minutos; autenticação global: 300/minuto. Valores configuráveis, positivos e validados.
- Desenvolvimento em 127.0.0.1; produção com segredo exclusivo, origem HTTPS, cookie Secure e sem simulação/bootstrap.
- Eventos de segurança sem segredos, retenção de 30 dias; consultas parametrizadas e propriedade dos dados preservadas.
- Não acrescentar MFA, recuperação por email ou pagamentos reais sem projeto específico.

## Review Focus

1. Refresh simultâneo no navegador deve compartilhar renovação, sem revogação causada por uma corrida normal — tarefa 5.
2. JSON chunked deve respeitar 256 KiB mesmo sem Content-Length — tarefa 3.
3. Senha Unicode deve ser medida em bytes, não somente em caracteres — tarefa 2.
4. Banco indisponível não deve liberar autenticação ignorando o limitador — tarefa 4.
5. Exceção após replay deve persistir revogação, não desfazê-la na transação — tarefa 5.

## Tarefa 1 — Configuração, dependências e baseline

**Files:** backend `build.gradle`, `src/main/resources/application.properties`, `application-dev.properties`, novo `application-prod.properties`, `.env.example`, `config/SecurityEnvironmentValidator.java` dentro de `src/main/java/br/com/loja`; teste `src/test/java/br/com/loja/config/SecurityEnvironmentValidatorTest.java`.

**Interfaces:** validador recebe Environment e propriedades de segurança, executa na inicialização e rejeita combinações inseguras; demais tarefas consomem propriedades verificadas.

- [ ] Guardar snapshot do working tree e hash, inventariar alterações anteriores; executar `gradlew.bat test --rerun-tasks --console=plain` e registrar resultado real.
- [ ] Criar testes `rejectsProductionWithDevelopmentProfile`, `rejectsKnownSecret`, `rejectsInsecureCookieOrOrigin`, `acceptsValidProductionConfiguration`: `assertThrows(IllegalStateException.class, validator::validate)` nos casos inseguros, `assertDoesNotThrow(validator::validate)` no válido; segredo válido aleatório somente no fixture.
- [ ] Executar `gradlew.bat test --tests '*SecurityEnvironmentValidatorTest' --console=plain`; confirmar vermelho antes da implementação.
- [ ] Implementar `void validate()` e binding de propriedades; configurar loopback dev, no-store nas respostas de autenticação e limites de cabeçalhos/timeout. Verificar TLS/proxy sem confiar indiscriminadamente em cabeçalhos encaminhados.
- [ ] Inventariar runtime, consultar OSV e escolher atualização compatível; registrar versões resolvidas e não apenas declaradas.
- [ ] Executar teste direcionado e `gradlew.bat clean test bootJar --console=plain`, repetir OSV. Commit somente dos arquivos desta tarefa: `fix: endurecer configuracao e atualizar dependencias`.

## Tarefa 2 — Validação consistente de DTOs

**Files:** backend DTOs AuthDtos, CustomerDtos, OrderDtos, CampaignDtos, RelationshipDtos, FinanceDtos e CatalogDtos; novos `validation/Utf8ByteSize.java`, `Utf8ByteSizeValidator.java`, `PositiveIdentifier.java`, `PositiveIdentifierValidator.java`; `exception/ApiExceptionHandler.java`. Teste `src/test/java/br/com/loja/validation/RequestValidationTest.java`.

**Interfaces:** `Utf8ByteSizeValidator implements ConstraintValidator<Utf8ByteSize,String>`; `PositiveIdentifierValidator implements ConstraintValidator<PositiveIdentifier,String>` aceita IDs numéricos positivos representáveis em Long; null opcional somente onde contrato permite. Controllers continuam consumindo os DTOs existentes.

- [ ] Testar email de 256 caracteres, endereço de 256 caracteres, telefone de 33 caracteres, aniversário futuro, ID negativo/zero/acima de Long, item nulo e senha Unicode de 73 bytes: `assertFalse(validator.validate(input).isEmpty())`. Campos opcionais ausentes válidos: `assertTrue(validator.validate(input).isEmpty())`. Testar também JSON HTTP inválido retornando 400.
- [ ] Executar `gradlew.bat test --tests '*RequestValidationTest' --console=plain`; registrar falhas previstas.
- [ ] Implementar anotações, limites de 255 para campos compatíveis com esquema, 32 para telefone, CEP/UF, validação de datas e intervalos de campanhas; manter regras de dinheiro e normalização atual. Aplicar máximo UTF-8 de 72 bytes em senhas novas/atuais/login, sem transformá-las.
- [ ] Tratar rejeições de validação com mensagem previsível, sem rejectedValue ou detalhes internos. Não incluir senhas nas mensagens.
- [ ] Executar teste direcionado, arquitetura/imports e integração atual. Commit: `feat: validar entradas e limites dos DTOs`.

## Tarefa 3 — Limite real do corpo HTTP

**Files:** novos backend `security/RequestBodyLimitFilter.java`, `LimitedBodyRequest.java`; modificar `config/SecurityConfig.java`; teste `src/test/java/br/com/loja/security/RequestBodyLimitFilterTest.java`.

**Interfaces:** `RequestBodyLimitFilter(long maximumBytes)` aplica 262144 bytes em corpos JSON e produz 413 genérico; wrapper expõe fluxo limitado respeitando o contrato ServletInputStream. Não duplica leitura ilimitada em memória.

- [ ] Criar testes de Content-Length excessivo, corpo chunked excessivo, corpo válido exatamente no limite e ausência de corpo. Para excesso: `assertEquals(413,response.getStatus())`; para válido: confirmar passagem à cadeia e bytes completos.
- [ ] Executar `gradlew.bat test --tests '*RequestBodyLimitFilterTest' --console=plain`; confirmar vermelho.
- [ ] Implementar contagem durante leitura e tratamento uniforme de excesso, antes de desserialização; não depender apenas do tamanho declarado.
- [ ] Executar testes do filtro e DTOs; commit: `feat: limitar leitura de corpos JSON`.

## Tarefa 4 — Limites compartilhados e eventos de segurança

**Files:** novos models `AuthenticationLimit`, `SecurityEvent`, repositories correspondentes, services `AuthenticationLimitService`, `SecurityEventService`, `SecurityRetentionService`, filtro `AuthenticationLimitFilter`; controllers AuthController e configuração SecurityConfig; nova migração `V5__authentication_limits_and_security_events.sql`. Testes `AuthenticationLimitIntegrationTest`, `SecurityEventIntegrationTest`.

**Interfaces:** `LimitDecision(boolean allowed,long retryAfterSeconds)`; `AuthenticationLimitService.check(String scope,String hashedKey,int maximum,Duration window)` usa transação independente e atualização atômica. `SecurityEventService.record(String event,String result,String actorReference,String correlationId)` persiste em REQUIRES_NEW sem dados de formulário. Filter controla IP/global; controller controla conta/identidade antes de BCrypt e usa os limites da especificação.

- [ ] Testar a próxima tentativa após o limiar retornando 429/Retry-After; Clock injetado permite expiração sem sleeps. Testar chaves independentes, concorrência sem ultrapassar orçamento, conta inexistente com resposta uniforme e falha do armazenamento retornando 503.
- [ ] Testar evento preservado após rollback da autenticação; inspecionar persistência para comprovar ausência de senha/token/email em claro. Testar limpeza por lotes e retenção, preservando eventos recentes.
- [ ] Executar `gradlew.bat test --tests '*AuthenticationLimitIntegrationTest' --tests '*SecurityEventIntegrationTest' --console=plain`; confirmar vermelho.
- [ ] Implementar tabelas/indexes, services, filtro/controles e códigos fixos de evento. O resultado 503 não contém mensagem do banco. Limitar chaves, apagar contadores expirados em lotes e não confiar em X-Forwarded-For arbitrário.
- [ ] Executar direcionados e suíte completa. Commit: `feat: limitar autenticacao e registrar eventos de seguranca`.

## Tarefa 5 — Famílias de sessão, replay e integração de refresh

**Files:** backend novo model `SessionFamily`, repository `SessionFamilyRepository`, service `SessionFamilyService`, nova migração `V6__session_families.sql`; modificar RefreshSession, RefreshSessionRepository, AuthService, CredentialVersionFilter e SecurityConfig. Teste `src/test/java/br/com/loja/security/SessionFamilyIntegrationTest.java`. Frontend composables `app/composables/useAccount.ts`, `useManager.ts`, `useBusiness.ts`, `useClientFlow.ts` somente onde fazem chamadas de sessão; novo `app/utils/sessionRefresh.ts` e teste `tests/session-refresh.spec.ts`.

**Interfaces:** `SessionFamilyService.create(String role,Long accountId,Instant now)`; `rotate(String role,String rawToken)` retorna sessão válida ou rejeição; operações de replay concluem revogação em transação independente antes de produzir 401. `boolean isActive(String familyId,String role,Long accountId,Instant now)` é consumido pelo filtro JWT. JWT recebe claim `sessionFamilyId`.

- [ ] Testar login/refresh preservando família, replay encerrando sucessor e access token, outra família ainda válida, validade absoluta de 30 dias, inatividade de sete dias, gestora inativa, troca de senha e logout antigo. Usar Clock controlado, não sleeps. Após replay, consultar o banco em nova transação e confirmar revogação persistida.
- [ ] Testar requisições paralelas com locks e confirmar política estrita de replay; no frontend, duas chamadas de renovação simultâneas compartilham uma operação HTTP e recebem o mesmo resultado, inclusive propagação de falha.
- [ ] Executar `gradlew.bat test --tests '*SessionFamilyIntegrationTest' --console=plain` e teste frontend pelo runner configurado em package.json; registrar vermelho.
- [ ] Implementar migração que revoga refresh tokens legados sem família, locks em ordem consistente, emissão de família e validação de estado na autorização. Não armazenar tokens em claro nem reescrever migrações existentes.
- [ ] Implementar `refreshSessionOnce(realm,operation)` no util frontend, compartilhando promessa em andamento e liberando a chave em finally; verificar todos os consumidores de renovação existentes.
- [ ] Executar suíte backend completa e testes/build frontend. Commits distintos: `feat: revogar familias de sessao por replay` e `fix: compartilhar renovacao de sessao no frontend`.

## Tarefa 6 — Respostas limitadas com consumidores compatíveis

**Files:** backend services/controller Order, Customer e Relationship, DTO PageView; frontend `app/composables/useOrders.ts`, `useCustomers.ts`, `useClientFlow.ts`, `useBusiness.ts` e telas consumidoras; teste backend `ResponsePaginationIntegrationTest` e frontend `tests/pagination.spec.ts`.

**Interfaces:** novas variantes paginadas explícitas retornam `PageView<T>` com parâmetros page/size; size entre 1 e 100, page não negativo. Rotas existentes são adaptadas junto com os consumidores, sem truncamento silencioso; ordenação permitida por allowlist. Verificar todos os chamadores antes de trocar o contrato.

- [ ] Testar página negativa, size 0/101 e ordenação não permitida retornando 400; duas páginas sem duplicação/perda e verificação de propriedade em cada página. Consumidor frontend deve acessar próxima página, não presumir lista completa.
- [ ] Executar testes novos e confirmar vermelho.
- [ ] Implementar paginação no banco, não sublista após carregamento integral; preservar autorização e consumo da API.
- [ ] Executar testes/build dos dois projetos. Commits backend/frontend separados para paginação e adaptação.

## Tarefa 7 — Matriz de segurança, relatório e rollback

**Files:** backend novo `src/test/java/br/com/loja/security/SecurityMatrixIntegrationTest.java`; atualização de testes existentes somente quando contrato mudou; `docs/RELATORIO-SEGURANCA-BACKEND-ATUALIZADO.md`; inventário runtime e verificação em docs/security. Evidências externas em nova pasta de Documentos, sem segredos.

**Interfaces:** usa API pública e controles das tarefas anteriores, não cria comportamento novo fora do escopo.

- [ ] Testar JWT com assinatura, algoritmo, issuer, audience e expiração inválidos retornando 401; papéis incorretos 403; dados de outro cliente inacessíveis; cookie Secure/no-store em produção; Origin ausente/estrangeiro no refresh 403; simulação fora de dev/test 403 sem alterações financeiras.
- [ ] Executar `gradlew.bat clean test bootJar --console=plain`; executar testes/build frontend e smoke test local. Registrar comandos, entradas, saída literal, exit status e contagem real de testes.
- [ ] Repetir OSV no runtime final; distinguir dependência afetada de exploração demonstrada. Novo relatório compara achados anteriores, correções e riscos restantes, incluindo TLS, proxies, backups, segredos e controles de infraestrutura não testados.
- [ ] Guardar MODIFIED_FILE.zip, DIFF_FILE.patch, VERIFICATION.txt e ROLLBACK.sh; recuperar snapshot anterior em cópia, comparar hashes e testar. Migrações são testadas em PostgreSQL isolado com backup/restauração próprios; não desfazer produção para testar rollback.
- [ ] Sincronizar fontes alterados na cópia antiga sem sobrescrever configuração/banco; rodar verificação de imports nas duas pastas. Documentar divergências preexistentes, se houver.
- [ ] Reabrir relatório e evidências; revisar mudanças e executar revisão final independente conforme skill de revisão. Commit: `test: verificar controles e atualizar relatorio de seguranca`.

## Autorrevisão do plano

Os sete blocos cobrem dependências/configuração, limites, sessões, validação/respostas, auditoria, integração, regressão e reversibilidade da especificação. Os cinco modos de falha do Review Focus têm testes nas tarefas proprietárias. Recursos de segurança não são certificados pelo simples sucesso do build; o relatório informa o alcance real de cada evidência. Execução começa após revisão deste plano e escolha do método pelo usuário.
