# API Loja

Base: http://localhost:8080/api/v1. JSON. IDs string, dinheiro decimal. Erros 400/401/403/404/409; limites de requisição retornam 413, limites de autenticação retornam 429 com Retry-After, e indisponibilidade do limitador retorna 503. Erros de negócio retornam message; filtro de autenticação pode retornar corpo vazio. Listagens da gestora: page=0&size=20 (size até 100), envelope content/page/size/totalElements/totalPages.

## Login e credenciais

- POST /auth/manager/login e /auth/customer/login: email, password.
- POST /auth/customer/register: name, email, phone, password (mínimo 10 caracteres e máximo 72 bytes UTF-8), birthday opcional YYYY-MM-DD.
- POST /auth/{realm}/refresh: cookies e Origin da aplicação, realm manager/customer.
- POST /auth/{realm}/logout: cookies e Origin; revoga sessões da conta.
- POST /auth/{realm}/password: Bearer, currentPassword, newPassword; revoga sessões anteriores.

Resposta: accessToken, tokenType=Bearer, expiresIn=900, role, accountId, name. Refresh fica só em cookie HttpOnly. Envie Authorization: Bearer TOKEN em rotas privadas. Refresh/logout exigem Origin: http://localhost:3000 ou origem configurada. Cliente/gestora têm cookies separados. O access token deve ficar na memória do cliente. Refresh rotaciona o token, com inatividade máxima de sete dias e duração absoluta da família de 30 dias. Reutilizar um refresh consumido revoga a família; coordene a renovação entre requisições e abas.

```powershell
$headers = @{ Origin = 'http://localhost:3000' }
$session = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/auth/manager/login' -ContentType 'application/json' -Headers $headers -Body '{"email":"gestora@loja.local","password":"LojaDev123!"}' -SessionVariable cookies
$private = @{ Authorization = "Bearer $($session.accessToken)" }
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/manager/products' -Headers $private
```

## Catálogo e clientes

- GET /catalog/products (ativos, sem custo), /catalog/brands, /catalog/categories.
- GET/POST /manager/products; GET/PUT/DELETE /manager/products/{id}. DELETE desativa.
- Produto: name, description, imageUrl, price, cost, stock, minimumStock, brandId, categoryId, active. Edição de stock registra o delta, nunca altera estoque silenciosamente.
- POST /manager/products/{id}/stock: stock (novo saldo), reason.
- GET/POST /manager/brands e /manager/categories; PUT/DELETE /manager/brands/{id} e /manager/categories/{id}. Corpo: name. Vínculos impedem exclusão.
- GET/POST /manager/customers; GET/PUT /manager/customers/{id}. Corpo: name, phone, birthday opcional.
- GET/PUT /customer/me: dados do próprio cliente, sem credenciais.
- GET/POST /customer/addresses; PUT/DELETE /customer/addresses/{id}. Corpo: label, postalCode (8 dígitos), street, number, complement opcional, district, city, state (2 letras maiúsculas).

## Pedidos e pagamentos

- GET/POST /manager/orders e /customer/orders; GET /manager/orders/{id} e /customer/orders/{id}.
- Cliente autenticado sempre define o proprietário, ignorando customerId do corpo.
- POST /manager/orders/{id}/confirm: estoque/parcelas, uma vez.
- PATCH /manager/orders/{id}/status: status PREPARING ou FINISHED.
- POST /manager/orders/{id}/receive: recebe pedido não parcelado (confirma se NEW).
- POST /manager/installments/{id}/receive: recebe uma parcela, sem duplicar caixa.
- POST /manager/orders/{id}/cancel e /customer/orders/{id}/cancel: reposição/estorno do recebido.
- POST /customer/orders/{id}/simulate-payment: só dev/test. No parcelado, AUTHORIZED não representa recebimento.

```json
{
  "customerId": "1",
  "paymentMethod": "INSTALLMENTS",
  "installmentCount": 3,
  "delivery": false,
  "couponCode": "LOJA10",
  "items": [{ "productId": "1", "quantity": 2 }]
}
```

PaymentMethod: PIX/CASH/CARD/BOLETO/INSTALLMENTS. Fora de INSTALLMENTS, a quantidade é 1. Delivery=true exige addressId próprio; frete calculado pelo servidor. Envie Idempotency-Key (até 128 caracteres) para repetição do checkout; conteúdo diferente com mesma chave retorna 409.

OrderView: snapshots/totais/items/installments/history. Status NEW → CONFIRMED → PREPARING → FINISHED; CONFIRMED pode ir direto para FINISHED. FINISHED e CANCELLED são terminais. PaymentStatus: PENDING/AUTHORIZED/APPROVED/REFUNDED/CANCELLED. Total zero é aprovado ao confirmar, sem parcelas/caixa.

## Campanhas

- GET/POST /manager/coupons; PUT/DELETE /manager/coupons/{id}. Corpo: code, type FIXED/PERCENT, value, validFrom, validUntil, usageLimit opcional. Percentual entre 0 e 100.
- GET/POST /manager/promotions; PUT/DELETE /manager/promotions/{id}. Corpo: name, percent, validFrom, validUntil, productIds. Usa só a maior promoção vigente por produto.
- GET/PUT /manager/interest-rates. Corpo: installmentCount (1–12), percent. Taxas iniciais zero. Mudanças só afetam pedidos novos.

## Financeiro e registros

- GET/POST /manager/finance. Lançamento manual: type IN/OUT, description, amount positivo. Não vincula caixa manual a pedido.
- GET /manager/inventory-movements.
- GET /manager/audit.
- GET /manager/reports/summary: contagens, recebido, saídas, saldo, custo do estoque.

## Relacionamento

- GET/POST /manager/birthday-benefits: customerId, couponId opcional, year. Aniversário cadastrado obrigatório, um benefício por cliente/ano.
- GET/POST /manager/gifts: customerId, description, date.
- GET /manager/installments/{id}/whatsapp-preview: message, url, sent=false; não registra envio.
- POST /manager/installments/{id}/declare-sent: message; registra declaração da gestora para parcela PENDING.
- GET /manager/billings.
- GET /manager/tickets; PUT /manager/tickets/{id}/reply: reply, status OPEN/RESOLVED.
- GET/POST /customer/tickets: subject, message, orderId opcional próprio.

## Banco

Flyway gerencia as migrações V1–V6, incluindo domínio, unicidade, credenciais, chave composta de promoções, limites de autenticação e famílias de sessão. JPA valida schema. Testes usam PostgreSQL real, incluindo confirmação concorrente e consistência do caixa. Produção exige variáveis externas e não habilita simulação.

## Consultas complementares dos casos de uso

- GET /catalog/products/{id}: detalhes públicos, sem custo e sem produtos inativos.
- GET /catalog/products?search=nome&brandId=1&categoryId=2: busca e filtros combinados.
- GET /manager/installments?status=PENDING: parcelas paginadas (status opcional).
- GET /manager/alerts: contagem de estoque baixo e parcelas vencidas.
- GET /manager/alerts/low-stock e /manager/alerts/overdue: detalhes paginados.
- GET /manager/customers/{id}/birthday-whatsapp-preview: mensagem de aniversário/link, sent=false, sem envio automático.
- V4 mantém a chave primária composta product_id/promotion_id do diagrama lógico.


## Limites e paginação de clientes

Corpos JSON têm limite de 256 KiB. Use GET `/customer/orders/paged`, `/customer/addresses/paged` e `/customer/tickets/paged` com `page=0&size=20` (page não negativo e size de 1 a 100). As respostas usam o envelope content/page/size/totalElements/totalPages. As rotas de listagem sem `/paged` retornam até 100 registros; acima disso, respondem 413 e indicam a rota paginada.

## Regras de negócio dos serviços

As regras abaixo descrevem o comportamento implementado em `src/main/java/br/com/loja/service`. As validações de entrada dos DTOs e as restrições do banco complementam os serviços. Datas de vigência, vencimento e alertas usam a data local do servidor (`LocalDate.now()`).

### Catálogo e estoque — CatalogService

Fonte: [CatalogService.java](../src/main/java/br/com/loja/service/CatalogService.java).

- O catálogo público lista somente produtos ativos e não expõe custo. Consultar um produto inativo pela rota pública retorna 404. O preço exibido no catálogo é o preço cadastrado; a promoção é aplicada no cálculo do pedido.
- A busca usa o nome do produto sem distinguir maiúsculas e minúsculas, aceita até 255 caracteres e pode ser combinada com marca e categoria.
- Cadastro e edição exigem marca e categoria existentes. Nome do produto perde espaços nas extremidades; preço e custo são arredondados para duas casas decimais.
- `stock` representa o saldo final desejado. O serviço calcula a diferença para o saldo anterior e registra `INITIAL` no cadastro ou `ADJUSTMENT` na edição/ajuste. Diferença zero não gera movimentação de estoque.
- O ajuste específico de estoque guarda o motivo informado. Alterações de saldo usam bloqueio do produto para evitar alterações concorrentes sobre o mesmo saldo.
- Excluir um produto apenas o desativa; não apaga seus registros nem o histórico de pedidos.
- Nomes de marcas e categorias são únicos sem distinguir maiúsculas e minúsculas, após remover espaços nas extremidades. Nomes repetidos retornam 409. As listas são ordenadas por nome; exclusões com vínculos são impedidas pelas restrições do banco.
- Identificadores devem representar números inteiros positivos compatíveis com `Long`. Paginação exige `page >= 0` e `1 <= size <= 100`; a paginação compartilhada usa ordenação por ID.

### Clientes e endereços — CustomerService e PhoneNumbers

Fontes: [CustomerService.java](../src/main/java/br/com/loja/service/CustomerService.java) e [PhoneNumbers.java](../src/main/java/br/com/loja/service/PhoneNumbers.java).

- O telefone é normalizado removendo caracteres não numéricos. O prefixo brasileiro `55` é retirado quando o número completo tem 12 ou 13 dígitos. O resultado deve ter 10 ou 11 dígitos e começar de 1 a 9.
- O telefone normalizado é único entre clientes. Cadastro ou alteração para um telefone já utilizado retorna 409. Nomes de clientes perdem espaços nas extremidades.
- O cadastro de cliente pela gestora cria os dados comerciais do cliente, sem criar uma conta de login. A criação de credenciais ocorre no cadastro público de `AuthService`; telefone já cadastrado também bloqueia esse cadastro público.
- `/customer/me` consulta e altera o cliente associado à conta autenticada. Os dados retornados não incluem senha ou hash.
- Endereços são sempre associados ao cliente autenticado. Consulta de dados, edição ou exclusão que verifique propriedade retorna 404 quando o registro pertence a outro cliente, conforme [ActorService.java](../src/main/java/br/com/loja/security/ActorService.java).
- A lista de endereços consulta somente registros do próprio cliente e segue os limites de paginação descritos acima.

### Criação e cálculo do pedido — OrderService

Fonte: [OrderService.java](../src/main/java/br/com/loja/service/OrderService.java).

- No checkout do cliente, o proprietário é obtido da sessão autenticada. No checkout da gestora, `customerId` identifica um cliente existente.
- Itens com o mesmo produto têm as quantidades somadas. Na criação, o produto precisa estar ativo e ter saldo suficiente; falta de estoque retorna 409. Criar o pedido não reserva nem desconta estoque.
- O preço é calculado no servidor. Para cada produto, aplica-se somente a maior promoção vigente, incluindo as datas inicial e final. O preço unitário promocional é arredondado antes de ser multiplicado pela quantidade.
- Um único cupom pode ser aplicado sobre o subtotal já promocional. Seu código é normalizado para maiúsculas e sem espaços nas extremidades. Cupom inexistente, fora da vigência ou com limite esgotado retorna 400.
- O uso do cupom conta pedidos que não estejam `CANCELLED`, inclusive pedidos `NEW`. Cancelar um pedido deixa de consumir esse limite. A verificação do cupom ocorre sob bloqueio para coordenar compras concorrentes.
- Entrega acrescenta R$ 12,00 e exige endereço pertencente ao cliente do pedido. Retirada tem frete zero. Endereço de outro cliente retorna 404.
- Para `INSTALLMENTS`, deve existir taxa cadastrada para a quantidade solicitada. Para os demais meios, o serviço grava uma parcela como quantidade e não aplica juros de parcelamento.
- O pedido guarda cópias dos nomes e telefone do cliente, endereço de entrega, nomes do produto/marca/categoria, preço unitário e custo. Alterações posteriores nos cadastros não recalculam esses dados nem os totais do pedido.
- A criação registra o status `NEW` no histórico. `Idempotency-Key` é opcional; se enviada, não pode ser vazia nem exceder 128 caracteres e é separada por papel e conta. Repetir a chave com os mesmos dados retorna o pedido existente; dados diferentes retornam 409. A comparação usa a representação do DTO e o cliente, portanto reordenar itens também pode alterar a comparação.

### Valores e parcelas — PricingCalculator

Fonte: [PricingCalculator.java](../src/main/java/br/com/loja/service/PricingCalculator.java).

- Valores monetários usam duas casas decimais com arredondamento `HALF_UP`. Subtotal, desconto, frete e percentual de juros não podem ser negativos.
- O desconto é limitado ao subtotal: não desconta frete e não produz saldo negativo. O cálculo é `base = subtotal - desconto + frete`, `juros = base × percentual / 100` e `total = base + juros`, com arredondamento monetário em cada etapa indicada pelo calculador.
- A taxa é aplicada uma vez sobre a base, incluindo o frete. Não é multiplicada pelo número de parcelas nem calculada como juros compostos.
- A divisão aceita de 1 a 12 parcelas e exige pelo menos um centavo por parcela. Os centavos restantes são distribuídos nas primeiras parcelas, preservando o total: R$ 100,00 em três parcelas resulta em R$ 33,34, R$ 33,33 e R$ 33,33.
- Pedido com total zero segue a exceção de quitação automática na confirmação e não passa pela divisão em parcelas.

### Confirmação, status e recebimento — OrderService

- Confirmar um pedido `NEW` verifica novamente o estoque, desconta as quantidades e registra movimentos `ORDER_OUT`. A operação é transacional e bloqueia pedido/produtos; uma falha não deve deixar confirmação parcial.
- Após a confirmação, `stockEffect` fica `APPLIED`. Repetir a confirmação em `CONFIRMED`, `PREPARING` ou `FINISHED` não desconta novamente nem recria parcelas. Confirmar pedido cancelado retorna 409.
- As parcelas são criadas na confirmação de pedidos `INSTALLMENTS` com total positivo. A primeira vence um mês após a data da confirmação; as demais vencem nos meses seguintes. O pedido gratuito recebe `paymentStatus=APPROVED`, sem parcelas e sem entrada no caixa.
- A alteração de status permite `CONFIRMED -> PREPARING`, `CONFIRMED -> FINISHED` e `PREPARING -> FINISHED`. Repetir o mesmo status não cria nova transição; outras transições retornam 409.
- Finalizar um pedido não exige pagamento aprovado. `FINISHED` encerra o fluxo de status e impede cancelamento, mas ainda permite registrar recebimentos pendentes.
- Receber um pedido não parcelado confirma-o se estiver `NEW`, registra uma entrada `IN` pelo total positivo e marca o pagamento como `APPROVED`. Repetir o recebimento de um pagamento aprovado não duplica a entrada.
- Receber o pedido inteiro com meio `INSTALLMENTS` retorna 400: o recebimento deve ser feito por parcela. Parcela recebida fica `PAID`, ganha data de pagamento e gera entrada pelo seu valor. Repetir o recebimento de parcela paga não duplica caixa; parcela ou pedido cancelado retorna 409.
- O pagamento do parcelado fica `APPROVED` quando todas as parcelas estiverem pagas. O serviço não exige ordem de recebimento nem que o vencimento já tenha ocorrido.
- A simulação só está disponível em `dev`/`test`; fora desses perfis retorna 403. Para parcelado com total positivo, confirma o pedido e define `AUTHORIZED`, sem gerar recebimento. Para os demais meios, executa o fluxo de recebimento do pedido. Não há comunicação com provedor de pagamento.

### Cancelamento e estorno — OrderService

- Pedido `FINISHED` não pode ser cancelado. Repetir o cancelamento de um pedido já `CANCELLED` retorna o estado atual, sem novos efeitos.
- Se o estoque foi aplicado, o cancelamento repõe as quantidades, registra `CANCELLATION_IN` e altera `stockEffect` para `REVERSED`. Pedido ainda sem baixa não gera reposição.
- O estorno corresponde ao saldo positivo efetivamente recebido no pedido: entradas menos saídas financeiras vinculadas. Gera uma saída `OUT` e marca `REFUNDED`; sem saldo recebido, marca o pagamento como `CANCELLED`.
- Somente parcelas `PENDING` são canceladas. Parcelas já pagas permanecem como histórico; o estorno é registrado no caixa do pedido.
- O status do pedido passa a `CANCELLED`, com registro no histórico. O estorno é um lançamento interno, sem transferência automática por um provedor financeiro.

### Cupons, promoções e juros — CampaignService

Fonte: [CampaignService.java](../src/main/java/br/com/loja/service/CampaignService.java).

- A data final de cupom ou promoção não pode ser anterior à inicial. Cupons aceitam `FIXED` ou `PERCENT`; percentual não pode ultrapassar 100. Os DTOs também rejeitam valores negativos e limitam promoções a percentuais de 0 a 100.
- Códigos de cupom são gravados em maiúsculas e sem espaços nas extremidades, com unicidade no banco. `usageLimit` ausente significa sem limite de quantidade; quando informado, deve ser ao menos 1. `usageCount` exclui pedidos cancelados.
- Uma promoção associa produtos existentes. IDs repetidos na lista são deduplicados; editar a promoção substitui suas associações, e excluí-la remove primeiro os vínculos com produtos.
- Taxas são cadastradas ou atualizadas pela quantidade de parcelas, de 1 a 12, com percentual não negativo. Mudanças em taxas, cupons ou promoções não recalculam pedidos já criados.
- Excluir cupons sujeitos a vínculos existentes pode ser impedido pelas restrições do banco, retornando conflito.

### Caixa e indicadores — FinanceService e DashboardService

Fontes: [FinanceService.java](../src/main/java/br/com/loja/service/FinanceService.java) e [DashboardService.java](../src/main/java/br/com/loja/service/DashboardService.java).

- Lançamento manual registra `IN` ou `OUT`, descrição e valor positivo validado pelo DTO e arredondado para duas casas. Não recebe vínculo com pedido/parcela e não altera a situação de pagamento de pedidos.
- O resumo considera todas as entradas e saídas registradas, inclusive movimentos manuais e estornos; `saldo = entradas - saídas`. Não usa o total de pedidos como dinheiro recebido.
- O custo do estoque é a soma de `custo × saldo` de todos os produtos, inclusive inativos. Contagens de clientes, produtos e pedidos também são gerais, sem filtro de período ou status nesse resumo.
- Estoque baixo significa produto ativo com `stock <= minimumStock`. Produto inativo não entra nesse alerta.
- Parcela vencida significa `PENDING` com vencimento anterior à data atual. Parcela que vence hoje ainda não entra no alerta de atraso.
- A consulta de parcelas aceita os filtros `PENDING`, `PAID` ou `CANCELLED`; outro status retorna 400. Sem filtro, consulta todos os status.

### Benefícios, WhatsApp e atendimento — RelationshipService

Fonte: [RelationshipService.java](../src/main/java/br/com/loja/service/RelationshipService.java).

- Benefício de aniversário exige cliente existente e aniversário cadastrado. O cupom é opcional e, se informado, deve existir. A combinação cliente/ano é única no banco; repetição retorna 409.
- A concessão não exige que hoje seja o aniversário e não valida a vigência do cupom nesse momento. A elegibilidade do cupom é verificada no checkout. A prévia de aniversário também exige apenas que a data esteja cadastrada.
- Brindes registram cliente, descrição e data informada. Esse cadastro não movimenta estoque nem caixa automaticamente.
- Prévias de WhatsApp apenas montam mensagem e link `wa.me` com o telefone do cliente, sempre com `sent=false`. Não enviam mensagem nem registram cobrança ou concedem benefício automaticamente. A prévia de parcela não exige status `PENDING`.
- Declarar envio de cobrança exige parcela `PENDING`; caso contrário retorna 409. Registra a mensagem informada e o horário da declaração, sem verificar envio real nem alterar pagamento. Cada declaração aceita cria um registro; não há deduplicação nesse serviço.
- Chamado é criado para o cliente autenticado, começa `OPEN` e pode indicar um pedido próprio. Vincular pedido de outro cliente retorna 404.
- A gestora pode responder com status `OPEN` ou `RESOLVED`. Nova resposta substitui a resposta armazenada; o serviço não mantém uma conversa de múltiplas mensagens e permite reabrir um chamado ao informar `OPEN`.

### Autenticação e sessões — AuthService e SessionFamilyService

Fontes: [AuthService.java](../src/main/java/br/com/loja/service/AuthService.java) e [SessionFamilyService.java](../src/main/java/br/com/loja/service/SessionFamilyService.java).

- Emails são normalizados para minúsculas e sem espaços nas extremidades. O cadastro público rejeita email de conta de cliente ou telefone de cliente já existente e cria cliente, conta e sessão juntos.
- Login de gestora exige conta ativa. Conta inexistente ou senha incorreta retorna a mesma mensagem de credenciais inválidas; a senha é verificada contra um hash substituto quando a conta não existe.
- Cada novo login/cadastro cria uma família de sessão com validade absoluta de 30 dias. O access token dura 15 minutos. O refresh contém 32 bytes aleatórios e somente seu hash SHA-256 é persistido; sua validade é o menor prazo entre sete dias e o término da família.
- Renovar exige papel/conta compatíveis, credencial atual, token válido e família ativa. A renovação revoga o refresh anterior e emite outro dentro da mesma família, sem prolongar o limite absoluto.
- Reutilizar um refresh já revogado revoga a família correspondente e retorna 401. Outras famílias da conta não são revogadas por esse fluxo. A revogação é preservada mesmo se a gravação de auditoria desse evento falhar.
- Logout com refresh válido revoga todas as sessões e famílias da conta naquele papel e incrementa a versão das credenciais. Refresh ausente, desconhecido, expirado ou já revogado não encerra um login posterior.
- Alterar senha exige a senha atual correta; após a alteração, todas as sessões da conta naquele papel são revogadas. O cliente precisa autenticar novamente.

### Limites e auditoria — serviços de apoio

Fontes: [AuthenticationLimitService.java](../src/main/java/br/com/loja/service/AuthenticationLimitService.java), [AuditService.java](../src/main/java/br/com/loja/service/AuditService.java), [SecurityEventService.java](../src/main/java/br/com/loja/service/SecurityEventService.java) e [SecurityRetentionService.java](../src/main/java/br/com/loja/service/SecurityRetentionService.java).

- O limitador consome contadores compartilhados no PostgreSQL, por escopo e hash da chave, em transação independente. A janela expirada reinicia a contagem; tentativas rejeitadas não prolongam a janela atual. Não há bloqueio permanente da conta.
- Os filtros/controladores aplicam, por padrão: login por IP de 30/minuto, login por papel/conta de 10/5 minutos, cadastro por IP de 5/10 minutos, refresh por IP de 60/minuto, troca de senha por identidade de 5/5 minutos e limite global de autenticação de 300/minuto. As demais rotas POST de autenticação, incluindo logout e troca de senha, também compartilham o orçamento de login por IP. Esgotamento retorna 429 com `Retry-After`; falha do armazenamento do limitador retorna 503.
- A auditoria de negócio só grava ações realizadas por `MANAGER` nos pontos que chamam `AuditService`: gestora, ação, entidade, identificador e data. Ela não constitui um registro de todas as ações dos clientes.
- Eventos de segurança aceitam `LOGIN`, `REGISTER`, `REFRESH`, `REPLAY`, `LOGOUT`, `PASSWORD` e `RATE_LIMIT`, com resultado `ALLOWED`, `DENIED` ou `REVOKED`. A referência do ator é armazenada como hash, sem campos de senha ou token, e a gravação usa transação independente.
- A limpeza de segurança começa após dez minutos e, por padrão, repete com intervalo de dez minutos após cada execução. Remove contadores expirados e eventos de segurança com mais de 30 dias, em lotes de até 1.000 registros de cada tipo. Essa rotina não apaga auditoria de negócio nem histórico de pedidos.
