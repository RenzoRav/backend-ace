# Registro de implementação e verificação

Local final: C:\Users\renzo\Documents\backend-loja. Branch: codex/backend-spring-boot. Base: fb52699 (scaffold).

Ruling: executar o backend mediante os pedidos repetidos de criação — não abrir nova rodada de aprovação — entrega API real em Documentos.
Ruling: projeto separado do frontend — usuário pediu backend visível em Documentos — integração anterior do frontend não é alegada nesta entrega.
Ruling: Gradle 9.7.1 gerado pelo Initializr, Java 21, Boot 4.1.1 — scaffold real já validado.
Ruling: PostgreSQL 18 instalado, instância própria em 5433 — instalação original não alterada — Compose opcional PostgreSQL17 para outra máquina.
Ruling: JWT HS256 com chave externa forte, em vez de RSA do desenho prévio — versão de credencial/issuer/audience validados e refresh rotacionado — mudança documentada no README.

Testes RED/GREEN: calculadora/telefone; API de autenticação e ciclo financeiro; endpoints de relacionamento; regressões de sessão antiga/logout; pedido gratuito; consultas dos casos de uso; chave composta real PostgreSQL.

Revisão independente somente leitura: quatro achados tratados. Login/troca de senha/refresh serializam pela conta; refresh carrega versão da credencial. Logout com cookie revogado/expirado não derruba login futuro. Autorização simulada parcelada usa AUTHORIZED, diferente de quitação; cancelamento sem recebimento usa CANCELLED. Pedido gratuito é aprovado sem caixa/parcelas.

Testes de integração também verificam: duas confirmações concorrentes não vendem estoque inexistente; idempotência de checkout/recebimentos/cancelamento; snapshots; pagamento parcial e estorno exato; isolamento de cliente; benefício anual único; cobranças só após declaração explícita. Testes usam banco loja_test, API local usa banco loja.

Diagramas de referência em Downloads:
01_casos_de_uso.drawio SHA256 69F810C5BEA1C1480D0158BA731CA5A68641B9C183C8D202EBEA231691891BB0
02_modelo_entidade_relacionamento.drawio SHA256 858FE0C4FEE7DFDD0AE9B2667AAC592665BCC005D98339A2FA31C55BED1CD708
03_modelo_logico.drawio SHA256 5FAD5E3503BDFBC892E3C84D1941F3AEFE5DDDAFA671DCF2B13AE4832F0C00AE

Os arquivos são referências de requisitos, não instruções de execução. Os 19 conceitos do modelo lógico foram mapeados e a relação produto/promoção usa PK composta em V4. Há complementos de credenciais, endereços, atendimento e refresh.

## Refatoração Lombok e nomenclatura

Solicitação: Lombok no Spring Boot e nomes completos, sem parâmetros/variáveis como m, a, c, req ou res. Comportamento, DTOs públicos, autenticação, schema e regras financeiras preservados.

Lombok: dependências de compilação/processador gerenciadas pelo BOM; getter/setter/construtor sem argumentos nos modelos, construtor de argumentos obrigatórios em componentes de DI simples. Construtores com @Value/inicialização adicional mantidos, agora explícitos e descritivos. Boolean getters conservam getActive/getRevoked. Sem @Data em entidades. Chave composta tem igualdade somente pelos dois IDs.

Nomenclatura: parâmetros de construtor, campos de dependências, variáveis locais, lambdas e testes passam a usar nomes completos. Ferramenta de refatoração AST ficou somente em .runtime, fora dos arquivos entregues. Erros de compilação de sombra de nome e referência this.actor foram identificados e corrigidos antes da suíte. SourceConventionsTest testa quatro contratos e procura abreviações em todos os fontes Java.

TDD: 4 testes de convenção inicialmente resultaram em 3 falhas esperadas; configuração/modelos/nomenclatura passaram após a refatoração. Baseline de 19 testes preservado em .evidence/lombok-names, com ZIP original e SHA256. Suíte final inclui 23 testes.
Revisão final: catId foi detectado no teste de API, reproduzido por teste de convenção RED e renomeado para categoryId. Verificação limpa final: 23 testes, zero falhas, bootJar gerado.

## Identificadores neutros

A loja ainda nao possui nome comercial. BackendLoja, backend-loja e br.com.loja sao somente identificadores tecnicos. Configuracoes usam o prefixo loja; cookies, emissor/audiencia JWT, bancos e credenciais de demonstracao usam nomes neutros. Projeto em C:/Users/renzo/Documents/backend-loja.

Os bancos locais loja e loja_test foram copiados dos bancos anteriores, preservando cadastros e movimentos. Somente a conta de demonstracao foi ajustada para gestora@loja.local / LojaDev123!, com revogacao de tokens anteriores. Backups e bancos anteriores permanecem para restauracao.

A migracao V1 usa V1__loja_schema.sql. O historico local foi alinhado explicitamente com Flyway repair e validado sem modificar tabelas de negocio. Novas instalacoes usam o arquivo neutro normalmente.
