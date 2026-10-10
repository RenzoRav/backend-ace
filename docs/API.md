# API Florir

Base: http://localhost:8080/api/v1. JSON. IDs string, dinheiro decimal. Erros 400/401/403/404/409. Erros de negócio retornam message; filtro de autenticação pode retornar corpo vazio. Listagens da gestora: page=0&size=20 (size até 100), envelope content/page/size/totalElements/totalPages.

## Login e credenciais

- POST /auth/manager/login e /auth/customer/login: email, password.
- POST /auth/customer/register: name, email, phone, password (10–72 caracteres), birthday opcional YYYY-MM-DD.
- POST /auth/{realm}/refresh: cookies e Origin da aplicação, realm manager/customer.
- POST /auth/{realm}/logout: cookies e Origin; revoga sessões da conta.
- POST /auth/{realm}/password: Bearer, currentPassword, newPassword; revoga sessões anteriores.

Resposta: accessToken, tokenType=Bearer, expiresIn=900, role, accountId, name. Refresh fica só em cookie HttpOnly. Envie Authorization: Bearer TOKEN em rotas privadas. Refresh/logout exigem Origin: http://localhost:3000 ou origem configurada. Cliente/gestora têm cookies separados.

```powershell
$headers = @{ Origin = 'http://localhost:3000' }
$session = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/auth/manager/login' -ContentType 'application/json' -Headers $headers -Body '{"email":"gestora@florir.local","password":"FlorirDev123!"}' -SessionVariable cookies
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
  "couponCode": "FLORIR10",
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

Flyway V1 cria domínio/complementos, V2 unicidade de nomes ignorando caixa, V3 versão de credencial do refresh. JPA valida schema. Testes usam PostgreSQL real, incluindo confirmação concorrente e consistência do caixa. Produção exige variáveis externas e não habilita simulação.

## Consultas complementares dos casos de uso

- GET /catalog/products/{id}: detalhes públicos, sem custo e sem produtos inativos.
- GET /catalog/products?search=nome&brandId=1&categoryId=2: busca e filtros combinados.
- GET /manager/installments?status=PENDING: parcelas paginadas (status opcional).
- GET /manager/alerts: contagem de estoque baixo e parcelas vencidas.
- GET /manager/alerts/low-stock e /manager/alerts/overdue: detalhes paginados.
- GET /manager/customers/{id}/birthday-whatsapp-preview: mensagem de aniversário/link, sent=false, sem envio automático.
- V4 mantém a chave primária composta product_id/promotion_id do diagrama lógico.

