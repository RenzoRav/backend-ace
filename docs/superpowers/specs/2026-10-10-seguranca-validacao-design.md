# Reforço de segurança e validação — desenho técnico

Data: 10/10/2026. Estado: desenho para revisão, ainda sem implementação.

## Objetivo e requisitos

Aplicar os controles propostos após o relatório de segurança, acrescentando validação consistente das entradas. Preservar Spring Boot/Gradle, PostgreSQL, Lombok, arquitetura DTO/model/controller/repository/service e integração com o frontend. Imports ficam no início dos arquivos; tipos qualificados não ficam em corpos ou anotações. Pagamentos e WhatsApp permanecem simulados somente no desenvolvimento.

Projeto de implementação: `C:\Users\renzo\Documents\backend-loja`. Alterações anteriores em documentação, migração V1 e ferramentas não entram nos commits desta implementação. A cópia antiga recebe posteriormente apenas a sincronização dos arquivos desta entrega, sem copiar dados ou sobrescrever configurações locais indiscriminadamente.

## Abordagem selecionada e alternativas

Reforçar a autenticação existente e usar PostgreSQL para limites compartilhados e sessões. Isso preserva o contrato da API e evita introduzir Redis ou um provedor de identidade. Um limitador apenas em memória seria mais simples, mas reinícios e múltiplas instâncias comprometeriam seu efeito. Redis/provedor externo seriam alternativas para maior escala, mas acrescentariam infraestrutura e modificariam a operação atual.

MFA, recuperação por email, troca de algoritmo de senhas e integração real de pagamentos ficam para projetos próprios: não serão acrescentados silenciosamente ao fluxo existente.

## 1. Dependências e configuração

- Resolver novamente as dependências e consultar OSV no momento da implementação. Atualizar versões afetadas com gerenciamento compatível do Spring Boot; a versão final será a efetivamente resolvida e testada, não uma versão presumida.
- Perfil dev escuta somente em 127.0.0.1. Produção exige segredo exclusivo com pelo menos 32 bytes, rejeita valores conhecidos/exemplos e exige origem HTTPS explícita e cookie Secure.
- Um perfil explícito prod rejeita combinação com dev/test, bootstrap habilitado e simulação. Desenvolvimento local continua acessível pelo frontend atual.
- Não confiar em X-Forwarded-For de clientes arbitrários. Endereço remoto é a identidade de rede padrão; proxies precisam de configuração explícita e documentação.
- Manter HttpOnly, SameSite Strict, validação de Origin em rotas de cookie, papéis e controle de propriedade. Respostas de autenticação usam Cache-Control: no-store. Documentar TLS no proxy de publicação.

## 2. Limitação de autenticação

- Aplicar limites antes de BCrypt em login e cadastro, com limites próprios para refresh e troca de senha. Retornar 429 e Retry-After quando excedidos.
- Usar contadores por IP, por conta normalizada e um teto global. Chaves de conta são hashes, não email em claro. Contadores são atualizados atomicamente com concorrência no PostgreSQL.
- Valores iniciais configuráveis: login por IP 30/minuto; login por conta 10/5 minutos; cadastro por IP 5/10 minutos; refresh por IP 60/minuto; troca de senha por identidade 5/5 minutos; teto de autenticação 300/minuto por aplicação. As configurações são validadas na inicialização.
- Limites expiram; não há bloqueio permanente de conta nem indicação de sua existência. Falhas não produzem mensagens que enumerem usuários.
- Limitar tamanho das chaves, expirar registros e realizar limpeza em lotes. Indisponibilidade do armazenamento responde 503 genérico, sem prosseguir como se o limite tivesse sido verificado.

## 3. Sessões e replay

- Acrescentar família de sessão e duração absoluta de 30 dias; refresh mantém janela de inatividade de sete dias, sem ultrapassar o término absoluto. Access token continua com 15 minutos.
- Login cria uma família. Refresh rotaciona dentro dela, sob bloqueio transacional consistente. Reutilização de token já consumido revoga a família; a revogação precisa ser confirmada no banco mesmo quando a resposta retorna 401.
- Access token inclui identificador da família; filtro verifica seu estado, além da versão de credenciais e estado da conta. Replay encerra também acessos associados à família, sem encerrar automaticamente outras famílias legítimas.
- Logout antigo não invalida login novo. Mudança de senha revoga todas as famílias da conta. Não armazenar tokens em claro.
- A migração invalida sessões anteriores sem família; usuários entram novamente. Não alterar V1 ou executar repair para esta funcionalidade: acrescentar migração Flyway nova, compatível com o esquema existente.
- Refresh concorrente estrito pode encerrar a família ao identificar reutilização. O frontend deve compartilhar uma única renovação em andamento por sessão; requisições esperam a mesma promessa. Se necessário, o ajuste de integração entra em commit próprio.

## 4. Validação de entrada e respostas

- Email: obrigatório quando aplicável, formato válido e até 255 caracteres. Nome e campos de endereço: limites alinhados às colunas, até 255 caracteres onde o esquema usa esse tamanho.
- Telefone: entrada até 32 caracteres; preservar normalização brasileira existente e validar o resultado. CEP com oito dígitos; UF com duas letras válidas. Datas de nascimento não futuras; intervalos de campanhas coerentes.
- Senhas: manter mínimo atual de dez caracteres para novas senhas; impor máximo de 72 bytes UTF-8 para compatibilidade com BCrypt, com validador específico. Login/senha atual também respeitam o limite de bytes. Não truncar nem normalizar senhas.
- Identificadores: formato positivo compatível com IDs atuais. Coleções validam elementos não nulos e conteúdo; limites existentes de itens são preservados. Valores monetários mantêm validação decimal e regras de negócio no servidor.
- JSON inválido/entrada incompatível retorna 400 previsível sem detalhe interno. Corpo JSON fica limitado a 256 KiB, incluindo leitura chunked; cabeçalhos e tempo de leitura recebem limites documentados. Não registrar o corpo da requisição.
- Não transformar texto livre em HTML nem remover caracteres válidos como substituto de parametrização/escape de saída. As consultas continuam parametrizadas.
- Paginação de listas grandes exige contrato explícito: adicionar parâmetros opcionais e adaptar consumidores existentes juntos. Não cortar listas silenciosamente. Limites de tamanho de páginas e ordenação recebem allowlist.

## 5. Eventos de segurança

Registrar sucesso/falha de login, bloqueio por limite, renovação, replay, logout e mudança de senha. Eventos contêm código fixo, horário, resultado e identificador mínimo de correlação; nunca incluem senha, token, cookie ou conteúdo de formulário. Persistência independente da transação que poderá falhar garante registro das rejeições. Definir retenção de 30 dias e limpeza em lotes; documentar acesso restrito e integração com monitoramento.

## 6. Verificação e critérios de aceitação

- Testes primeiro para campos excessivos, senha multibyte, datas, IDs, elementos nulos e leitura de corpo com/sem Content-Length.
- Rate limit: concorrência, limiar, Retry-After, expiração, separação de chaves, conta inexistente e indisponibilidade do armazenamento.
- Sessão: replay revoga sucessor e JWT da família; outras famílias permanecem válidas; renovação concorrente; validade absoluta; senha e logout antigo.
- JWT: assinatura, issuer, audience, expiração e algoritmo inválidos rejeitados; gestora inativa; matriz de papéis e propriedade.
- Perfis: configuração insegura de produção falha; dev usa loopback; simulação fora de dev/test responde 403 sem alteração financeira.
- Auditoria: rejeições ficam registradas mesmo com rollback de negócio; eventos não contêm segredos.
- Executar testes completos e bootJar; repetir inventário OSV, documentando avisos restantes e alcance não comprovado. Executar smoke test de integração com frontend e regressão de imports em todos os fontes alterados.

## Entrega e reversibilidade

Commits separados: dependências/configuração, validação, limites, sessões/migração, auditoria, integração necessária e relatório. Cada commit recebe verificação pertinente; relatório final identifica testes efetivamente executados e riscos restantes, sem certificação absoluta.

Guardar snapshot do estado anterior incluindo mudanças locais preexistentes, diff e evidências. Testar recuperação em cópia separada; preservar a versão corrigida no projeto principal. Como migração de banco e código precisam ser compatíveis, rollback de produção exige backup do banco anterior e janela controlada; restaurar somente código não equivale a desfazer a migração. Testes destrutivos e recuperação do banco ficam limitados ao banco de testes e cópias isoladas.

Novo relatório: `C:\Users\renzo\Documents\backend-loja\docs\RELATORIO-SEGURANCA-BACKEND-ATUALIZADO.md`.

## Referências de desenho

- [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
- [RFC 9700, proteção de refresh tokens](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14).
- [OWASP Input Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html).
- [OWASP Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html).
- [OSV API](https://google.github.io/osv.dev/post-v1-querybatch/).
