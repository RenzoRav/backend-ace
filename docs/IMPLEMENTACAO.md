# Backend Florir

Local: C:\Users\renzo\Documents\backend-florir. Projeto independente do frontend.

Pedido: Spring Boot, Gradle, PostgreSQL, DTO/model/controller/repository/service. Login real; pagamento e WhatsApp simulados.

Execução autorizada pelo pedido explícito de criação, sem nova rodada de aprovação. Spring Initializr gerou Boot 4.1.1, Java 21 e Gradle 9.7.1 (substitui versão de wrapper prevista no desenho). PostgreSQL 18 já instalado: instância isolada na porta 5433, sem alterar banco existente.

Sequência: 1. scaffold; 2. cálculo monetário e testes; 3. esquema/modelos/repositórios; 4. autenticação e autorização; 5. catálogo e clientes; 6. pedidos/estoque/financeiro; 7. relacionamento e documentação; 8. testes de integração e empacotamento. Commits separados por alteração funcional.

A entrega deste pedido cria a API em Documentos. A conexão do frontend anterior não deve ser confundida com a criação da API.
