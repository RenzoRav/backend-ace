# Backend Florir

Projeto criado em **C:\Users\renzo\Documents\backend-florir**.

API Spring Boot **4.1.1**, Java **21**, Gradle Wrapper **9.7.1**, PostgreSQL, Flyway e Spring Security. Camadas: **dto**, **model**, **controller**, **repository** e **service**, além de config/security/exception. A pasta é independente do frontend-florir; os dados do frontend local não são importados automaticamente.

## Iniciar neste computador

Abra o PowerShell nesta pasta e execute:

```powershell
.\start-backend.ps1
```

O script usa o PostgreSQL 18 já instalado, em uma **instância isolada na porta 5433**. Os dados ficam em .runtime/postgres; a instalação/banco anterior na porta 5432 permanece intacta. A API inicia em **http://localhost:8080**. Verificação pública: **http://localhost:8080/api/v1/health**.

Conta de gestora no perfil **dev**: **gestora@florir.local** / **FlorirDev123!**. Não há cadastro público de gestoras. Cliente se cadastra por POST /api/v1/auth/customer/register. Senhas são armazenadas com BCrypt, nunca retornadas nos DTOs. Os bancos florir e florir_test usam florir / florir_dev neste ambiente local.

Para parar só a API: .\stop-backend.ps1. O script não apaga dados nem para a instalação original do PostgreSQL.

## Desenvolvimento e testes

```powershell
.\gradlew.bat test
.\gradlew.bat bootJar
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Os testes usam **PostgreSQL real** em 127.0.0.1:5433/florir_test, não H2. Antes de testar, inicie o banco com start-backend.ps1 ou com Docker. Em outra máquina, configure um PostgreSQL local ou execute **docker compose up -d**. Não execute o contêiner na porta 5433 enquanto a instância local já estiver usando essa porta. O Compose usa PostgreSQL 17 e cria os bancos florir e florir_test na primeira inicialização de um volume vazio. URL/credenciais dos testes podem ser sobrescritas por variáveis SPRING_DATASOURCE_URL/USERNAME/PASSWORD.

## API

Contrato e exemplos: [docs/API.md](docs/API.md). Base /api/v1. Catálogo público; rotas /manager exigem MANAGER e /customer exigem CUSTOMER. Paginação do gestor: page=0&size=20, limite de 100 itens. IDs são strings nos DTOs; dinheiro é BigDecimal/NUMERIC no backend.

JWT de acesso: 15 minutos. Refresh: cookie HttpOnly, SameSite=Strict, separado por cliente/gestora, validade de 7 dias, rotação e hash persistido. Logout/troca de senha revogam as sessões e versões antigas. Chave HMAC-SHA256 externa em produção; emissor/audiência e versão da credencial são validados. Rotas que usam cookies exigem Origin configurada. O access token deve ficar na memória do cliente.

Pagamento e WhatsApp permanecem **simulados**: simular pagamento só em dev/test; preparar link de WhatsApp não registra envio; a gestora declara o envio explicitamente. No parcelado, AUTHORIZED significa autorização simulada, não recebimento. Só receber parcela gera entrada no caixa. Não há chamada a provedor financeiro ou envio automático de mensagem.

## Regras implementadas

- Pedido NEW não reserva estoque; confirmação desconta uma vez, com bloqueios transacionais.
- Parcelas são geradas uma vez na confirmação; recebimento repetido não duplica caixa.
- Cancelamento repõe o estoque aplicado, cancela parcelas abertas e estorna só o valor recebido. FINISHED/CANCELLED são terminais.
- Maior promoção vigente por produto, seguida de um cupom. Frete local: R$12 entrega, zero retirada. Juros simples; centavos são distribuídos sem perder o total.
- Pedido guarda snapshots de preços, custos, nomes e endereço; mudanças no catálogo não alteram pedidos antigos.
- Total zero é quitado automaticamente, sem parcelas nem lançamento financeiro.
- Idempotency-Key protege repetição do checkout, por conta e conteúdo. Conflitos de payload retornam 409.
- Histórico de status, movimentações e auditoria permanecem registrados.
- Endereços, pedidos e atendimento do cliente verificam o proprietário no servidor.

## Correspondência dos diagramas

As 19 entidades de referência existem no schema V1: Cliente/customer, Pedido/purchase_order, ItemPedido/order_item, Produto/product, Marca/brand, Categoria/category, BeneficioAniversario/birthday_benefit, Brinde/gift, TaxaJuros/interest_rate, Cupom/coupon, Parcela/installment, Cobranca/billing_record, MovimentacaoEstoque/inventory_movement, ProdutoPromocao/product_promotion, Promocao/promotion, UsuarioGestor/manager, LogAuditoria/audit_log, HistoricoStatus/status_history e MovimentacaoFinanceira/financial_movement.

A associação produto/promoção usa chave primária composta (product_id,promotion_id), como no modelo lógico. Complementos da API: customer_account, customer_address, support_ticket e refresh_session. Migrações V2 normalizam unicidade de marca/categoria; V3 associa refresh à versão da credencial e V4 adequa a chave composta produto/promoção. ddl-auto=validate: Flyway é responsável pelo schema.

## Configuração sem perfil dev

Defina DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET (mínimo 32 bytes) e FRONTEND_ORIGIN antes de iniciar. O perfil dev é **explícito** no script; sem ele, senha e chave não têm padrão. Cookie Secure está ativo por padrão. Para criar a primeira gestora, defina BOOTSTRAP_ENABLED=true, MANAGER_EMAIL e MANAGER_PASSWORD; após a criação, desligue o bootstrap. .env.example é referência, não arquivo carregado pelo Spring. A versão Gradle gerada substituiu a prevista na etapa de desenho; o PostgreSQL local 18 foi testado.

A integração do frontend anterior é uma etapa distinta: esta entrega cria e executa a API em Documentos, sem alegar que as telas existentes já usam esses endpoints.
## Lombok e nomes completos

O projeto usa Lombok em tempo de compilação (compileOnly/annotationProcessor), com a versão gerenciada pelo Spring Boot. Modelos usam @Getter, @Setter e @NoArgsConstructor; dependências simples usam @RequiredArgsConstructor. Construtores que têm @Value ou inicialização adicional continuam explícitos, com parâmetros completos, por exemplo this.managerRepository = managerRepository.

Não se usa @Data nas entidades, para evitar equals/hashCode/toString sobre associações JPA e credenciais. A chave composta ProductPromotionId usa @EqualsAndHashCode apenas nos dois identificadores. lombok.config mantém o contrato getActive()/getRevoked() existente. DTOs permanecem records, sem adicionar anotações redundantes.

Variáveis e parâmetros usam nomes completos em produção e nos testes: manager, customerAccount, product, purchaseOrder, installment, managerRepository, passwordEncoder, request e response. Identificadores de protocolo/API como id e url foram preservados. SourceConventionsTest verifica a configuração do Lombok, os acessores gerados e nomes de variáveis/parâmetros, além da suíte funcional PostgreSQL.

Referências: https://projectlombok.org/setup/gradle e https://projectlombok.org/features/GetterSetter.
