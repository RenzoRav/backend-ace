# Backend Loja

API em Java 21, Spring Boot 4.1.1, Gradle e PostgreSQL. Autenticação com Spring Security/JWT e migrações com Flyway.

## Executar localmente

Requisitos: JDK 21 configurado em `JAVA_HOME` e Docker com Compose. Na raiz do projeto:

```powershell
docker compose up -d --wait
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

No Linux/macOS, substitua `.\gradlew.bat` por `sh ./gradlew`.

- API: http://127.0.0.1:8080 — verificação: http://127.0.0.1:8080/api/v1/health.
- PostgreSQL 17: `127.0.0.1:5433`, usuário `loja`, senha local `loja_dev`. O Compose cria `loja` e `loja_test` na primeira inicialização de um volume vazio.
- Gestora de desenvolvimento: `gestora@loja.local` / `LojaDev123!`. Essas credenciais são exclusivas do ambiente local.
- Origem padrão do frontend: `http://localhost:3000`, configurável por `FRONTEND_ORIGIN`.

Pare a API com `Ctrl+C` e o banco com `docker compose stop`; os dados permanecem no volume.

## Testes e compilação

Com o PostgreSQL disponível:

```powershell
.\gradlew.bat test
.\gradlew.bat bootJar
```

Os testes usam PostgreSQL real em `127.0.0.1:5433/loja_test`. Para alterar a conexão dos testes, use `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. Se alterar `DB_PASSWORD` no Compose, configure a mesma senha na API e nos testes. O JAR é gerado em `build/libs/`.

## API e integração

Rotas, autenticação, exemplos e regras de pedidos: [docs/API.md](docs/API.md).

O catálogo é público; `/manager` exige perfil `MANAGER` e `/customer` exige `CUSTOMER`. O frontend precisa ser integrado à API; dados demonstrativos locais não são importados automaticamente.

Pagamento e WhatsApp são simulados: simulação de pagamento só em `dev`/`test`, e links de WhatsApp não enviam mensagens automaticamente. Autorização de parcelamento não representa recebimento no caixa.

## Configuração de produção

Use o perfil `prod` e configure as variáveis do processo:

- `DB_URL`, `DB_USER` e `DB_PASSWORD`: conexão com PostgreSQL.
- `JWT_SECRET`: chave aleatória exclusiva com pelo menos 32 bytes, sem valores de exemplo.
- `FRONTEND_ORIGIN`: origem HTTPS exata do frontend, sem caminho nem barra final.
- `PORT`: porta da API, padrão `8080`.

Produção exige HTTPS, cookies Secure e bootstrap desabilitado; não combine `prod` com `dev` ou `test`. O cadastro público cria apenas clientes. A criação da primeira gestora em produção requer provisionamento administrativo; o bootstrap automático é restrito a desenvolvimento/testes.

`.env.example` lista as variáveis de referência; o Spring não carrega `.env` automaticamente. Ajuste os valores para o ambiente, incluindo a origem HTTPS em produção. O Flyway gerencia o schema e o JPA apenas o valida.
