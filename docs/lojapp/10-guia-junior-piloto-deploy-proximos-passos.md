# LojApp Pro — Guia completo (júnior): contexto, fluxos, piloto, deploy e próximos passos

**Documento único** de operação LojApp: *o quê* e *porquê*, ambiente local, piloto com checklists, deploy/Flyway e roadmap.

**Se o teu objetivo é “fazer tudo correr na máquina” pela primeira vez:** segue a **Parte 3** na ordem (visão geral → ferramentas → Docker → API → frontend → base de dados → integração → Git). Depois avança para as Partes 4–8 (deploy, piloto, bloqueios, roadmap).

Para quem já domina o ambiente: Partes 1 → 2 (contratos) → 5 (piloto) continuam válidas em sequência.

**Documentos de apoio:**

| Ficheiro | Função |
|----------|--------|
| `01-escopo-mvp.md` | Critérios oficiais de aceite do MVP |
| `03-implantacao-pilotos.md` | Roteiro piloto por semanas, evidências das lojas e feedback |
| `02-pilotos-e-xmls.md` | XML real/piloto e registos por loja |

---

## Parte 1 — O que estás a fazer (e porquê)

### 1.1 Objetivo do piloto

Imagina **3 lojas de teste**. Cada loja tem **a sua própria conta** (email + palavra-passe). Assim os dados **não se misturam**: produtos da Loja A não aparecem na Loja B (isolamento por `user_id`).

O teu objetivo é **repetir o mesmo roteiro** três vezes (uma vez por loja) e confirmar que tudo funciona. No fim, marcas os critérios em `01-escopo-mvp.md` **só com evidência** (testaste mesmo).

### 1.2 Glossário rápido

| Palavra | Significa |
|---------|-----------|
| **API** | O programa no servidor que responde a pedidos (criar produto, venda, etc.). |
| **Swagger** | Página no browser para testar a API sem ter ainda uma app “bonita”. |
| **JWT / token** | “Bilhete” de **acesso** (curta duração) que envias no header; o **refresh** prolonga a sessão e, na LojApp, vai em **cookie HttpOnly** (não acessível a JavaScript). |
| **Smoke test** | Teste rápido: passar por todos os passos principais para ver se nada rebenta. |
| **Flyway** | Ferramenta que aplica scripts SQL versionados ao PostgreSQL (migrations V1, V2, V3…). |

---

## Parte 2 — Fluxos da API, dados e regras (mapa mental)

**Legenda dos checklists abaixo (evolução do produto):**

- **[x]** — implementado no backend atual (revalidar com testes e piloto).
- **[ ]** — próximo passo sugerido (produto, qualidade ou operação).

**Convenção ao fechar um passo de desenvolvimento:** implementar no código, **testes verdes** (`mvn test`), e atualizar os itens **[ ]** / **[x]** na **Parte 2** deste guia (ou nota equivalente no teu processo).

### 2.0 Respostas de erro HTTP (contrato JSON)

Em falhas tratadas pela API, o corpo segue **`ApiErrorResponse`** (`GlobalExceptionHandler` e `LojappErrorController`):

| Campo | Descrição |
|-------|-----------|
| `message` | Texto legível para o utilizador ou integrador |
| `code` | Código estável (`ApiErrorCode`: ex. `VALIDATION_ERROR`, `CONFLICT`, `INTERNAL_ERROR`) |
| `timestamp` | Instante em ISO-8601 (UTC) |

**O que já existe [x]:** serialização com `@JsonPropertyOrder` (`message`, `code`, `timestamp`); OpenAPI atualizado; teste `BrandControllerTest#createBrand_blankName_returnsStructuredApiError`; SPA lê `code` em `frontend/src/api.ts` (compatível com respostas antigas que usavam o campo `error`).

**Próximos passos [ ]:** integradores externos devem migrar de `error`/`path` para `code` apenas (campo `path` deixou de existir no corpo).

**Documentação de apoio:** `README.md` (secção «Erros da API»).

### 2.1 Autenticação e sessão (JWT + refresh)

| Passo | Ação | Endpoint |
|-------|------|----------|
| 1 | Registar conta | `POST /api/v1/auth/register` |
| 2 | Iniciar sessão | `POST /api/v1/auth/login` |
| 3 | Renovar access (rotação do refresh) | `POST /api/v1/auth/refresh` |
| 4 | Terminar sessão no browser (limpa cookie de refresh) | `POST /api/v1/auth/logout` |
| 5 | Chamadas protegidas | Header `Authorization: Bearer <access_jwt>` |
| 6 | Dados do utilizador | `GET /api/v1/users/me` |

**Persistência:** tabela `users` (email, `password_hash`); refresh opaco em `refresh_tokens` (hash na BD, rotação a cada login/refresh).

**Resposta HTTP (login/registo/refresh):** corpo JSON **`{ "accessToken": "<jwt>" }`** apenas. O refresh opaco **não** vai no JSON: a API envia **`Set-Cookie`** com cookie **HttpOnly** `lojapp_rt`, path `/api/v1/auth`, `SameSite=Lax` (em `prod`, `Secure=true` via `application-prod.yml`).

**SPA (piloto):** o access JWT fica **só em memória** (Zustand); ao recarregar a página, o cliente tenta `POST /auth/refresh` com `credentials: include` para recuperar sessão sem voltar ao ecrã de login (se a cookie ainda for válida).

**O que já existe [x]:** registo com email único (409 se duplicado); login com BCrypt; falha 401 genérica no login; JWT de access com `userId`; refresh com rotação; cookie HttpOnly + `POST /auth/logout`; rate limit em memória: **login/refresh** por minuto e **registo** por hora por IP (`AuthRateLimitFilter`); política de registo configurável (`lojapp.auth.registration`: desligar, allowlist de domínios, máx. por IP/hora); confiança em **`X-Forwarded-For`** só se `LOJAPP_TRUST_FORWARD_HEADERS=true` (defeito `false`, ver `ClientIpResolver`); testes `AuthServiceTest`, `AuthControllerTest`, `ClientIpResolverTest`, integração de registo.

**Próximos passos [ ]:** política de password mais forte se for requisito; monitorização de bloqueios de auth em ambiente real; refresh em corpo JSON mantido só para compatibilidade/Swagger — integrações novas devem assumir cookie + `credentials`.

#### 2.1.1 Checklist piloto: cookies, HTTPS e CORS

Use esta lista ao ligar **frontend** e **API** em domínios diferentes (ou em piloto na Internet):

| Tópico | O que validar |
|--------|----------------|
| **HTTPS em produção** | Com `prod`, a cookie de refresh usa **`Secure`**. O browser só envia/grava bem em **HTTPS** no site do cliente. Em `http://localhost` mantém-se `Secure=false` por defeito. |
| **`SameSite=Lax`** | A cookie `lojapp_rt` vai com pedidos **top-level** ao mesmo site (navegação normal). Fluxos em **iframe** ou POST cross-site “simples” podem não levar a cookie; evita surpresas em integrações embutidas. |
| **Path da cookie** | Path **`/api/v1/auth`**: a cookie **não** é enviada em `GET /api/v1/lojapp/...`, reduzindo superfície. O refresh só é necessário nas rotas `/auth/*`. |
| **`credentials: include` (SPA)** | Login, registo, refresh e logout devem usar **`fetch` com `credentials: 'include'`** (ou equivalente) para o browser **guardar e reenviar** a cookie. Sem isto, o access expira e o utilizador cai no login após F5. |
| **CORS (`LOJAPP_CORS_ORIGINS`)** | Lista **exacta** de origens permitidas: **esquema + host + porta** (ex.: `https://app.loja.pt`, não `*.loja.pt`). Um carácter a mais ou `http` vs `https` falha no preflight. |
| **CSRF (contexto LojApp)** | O access JWT vai no header **`Authorization`** (não em cookie de sessão da API de negócio). O risco CSRF típico de “cookie de sessão automática” é menor; o refresh HttpOnly em path fechado + **origem CORS fechada** continua essencial. |
| **Registo abusivo** | Variáveis **`LOJAPP_REGISTRATION_ENABLED`**, **`LOJAPP_REGISTRATION_ALLOWED_DOMAINS`** (CSV de domínios após `@`) e **`LOJAPP_REGISTRATION_MAX_PER_IP_PER_HOUR`** (defeito 10): ver `application.yml` → `lojapp.auth.registration`. |

**Referência de env:** `.env.example` na raiz do repositório; `README.md` (tabela de variáveis).

---

### 2.2 Marcas

| Passo | Ação | Endpoint |
|-------|------|----------|
| 1 | Listar marcas do utilizador | `GET /api/v1/lojapp/brands` |
| 2 | Criar marca | `POST /api/v1/lojapp/brands` |

**Persistência:** `brands` (`user_id`, `name` único por utilizador).

**O que já existe [x]:** listagem ordenada por nome; criação “idempotente” por nome (case insensitive) — se existir, devolve a existente.

**Próximos passos [ ]:** edição/eliminação de marca (e impacto em produtos) se o MVP precisar; testes de serviço para criação e duplicado lógico.

---

### 2.3 Produtos

| Passo | Ação | Endpoint |
|-------|------|----------|
| 1 | Listar produtos | `GET /api/v1/lojapp/products` |
| 2 | Criar produto | `POST /api/v1/lojapp/products` |
| 3 | Atualizar produto | `PUT /api/v1/lojapp/products/{id}` |

**Corpo típico:** nome, EAN, NCM, SKU, custo, preço venda, stock mínimo, `brandId` opcional.

**Persistência:** `products` (+ relação opcional com `brands`).

**O que já existe [x]:** criar e atualizar (CRU parcial, sem DELETE explícito no controller); `brandId` válido do mesmo utilizador; `brandId` null → produto sem marca (dashboard usa texto `Nao informada`).

**Próximos passos [ ]:** testes (marca inválida 404, campos obrigatórios); decisão eliminar vs. arquivar com histórico; validação EAN/SKU duplicados por loja se necessário.

**Nota para o piloto:** a listagem paginada devolve **`ProductPageResponse`** (objeto com `content`, `totalElements`, …) — **não** um array na raiz.

---

### 2.4 Importação de NFe (XML)

| Passo | Ação | Endpoint |
|-------|------|----------|
| 1 | Enviar XML bruto (JSON com campo de texto XML) | `POST /api/v1/lojapp/nfe/import` |

**Fluxo interno (resumo):**

1. Parse do XML: número da NFe, nome fornecedor, chave `chNFe`, itens (`xProd`, quantidades, valores).
2. Gravar `nfe_entries` + `nfe_items`.
3. Por linha: localizar produto por **nome (ignorando maiúsculas)** ou criar produto fallback (custo = unitário NFe; venda = custo; stock mín. 0).
4. Atualizar `cost_price` do produto com o custo da linha.
5. Movimento de stock `ENTRY` + atualização de `inventory_balances` (origem `NFE_IMPORT`).

**O que já existe [x]:** rastreabilidade cabeçalho + itens; entrada de stock; 400 se XML ilegível; **idempotência por `chNFe`:** segunda importação com a mesma chave (mesmo `user_id`) → **409 CONFLICT**; chave vazia não deduplica.

**Próximos passos [ ]:** testes com XMLs reais anonimizados (`02-pilotos-e-xmls.md`); política de retenção/anonimização de XML em produção; aceite MVP: piloto “sem digitação manual extensa”.

---

### 2.5 Ajuste manual de stock

| Passo | Ação | Endpoint |
|-------|------|----------|
| 1 | Ajustar quantidade (delta) | `POST /api/v1/lojapp/inventory/adjust` |

**O que já existe [x]:** movimento + saldo na mesma transação; produto inexistente → 404; testes de serviço (ajuste e 404).

**Próximos passos [ ]:** validação de `reason`; impedir saldo negativo se for regra obrigatória.

---

### 2.6 Alerta de stock mínimo

| Passo | Ação | Endpoint |
|-------|------|----------|
| 1 | Listar abaixo do mínimo | `GET /api/v1/lojapp/inventory/low-stock` |

**Lógica:** compara saldo em `inventory_balances` (ou zero) com `products.minimum_stock`.

**O que já existe [x]:** lista com quantidade atual e mínimo; testes com produto sem linha de saldo.

**Próximos passos [ ]:** notificações (fora do MVP); alinhar com piloto o que conta como “alerta” (só API vs. UI).

---

### 2.7 Venda simplificada

| Passo | Ação | Endpoint |
|-------|------|----------|
| 1 | Registar venda | `POST /api/v1/lojapp/sales` |

**Corpo:** `productId`, `quantity`, `unitPrice`; `unitCost` **opcional** — se omitir, usa `costPrice` do produto.

**Resposta:** id da venda (número JSON).

**O que já existe [x]:** venda + baixa de stock; **400** se stock insuficiente; custo fallback; testes (stock, 400, custo null).

**Próximos passos [ ]:** DTO de resposta mais rico se o front precisar; cenário explícito várias vendas no período (opcional).

---

### 2.8 Dashboard por marca

| Passo | Ação | Endpoint |
|-------|------|----------|
| 1 | KPIs por marca no intervalo | `GET /api/v1/lojapp/dashboard/brands?from=&to=` |

**Default:** se `to` omitido → agora; se `from` omitido → ~30 dias antes de `to`.

**Métricas:** faturamento, lucro estimado, quantidade, margem %, “giro” (Alto/Médio/Baixo), insight textual. Marca null → `Nao informada`.

**O que já existe [x]:** agregação a partir de `sales`; ordenação por **lucro decrescente** (empate → faturamento); **primeira linha** de `metrics` = marca com maior lucro no período (aceite MVP). Testes de ordenação.

**Próximos passos [ ]:** campo explícito “top marca” (opcional); performance conforme volume do piloto.

---

## Parte 3 — Preparar o ambiente (local): roteiro completo para júnior

Esta parte junta **visão geral**, **ordem correta de ferramentas**, **comandos deste repositório** e **checklists**. Muitos tutoriais na internet assumem **Node.js no backend**; aqui **não**: a API é **Java + Spring Boot** na raiz do projeto; o Node serve **só** para o SPA em `frontend/`.

### 3.0 Visão geral — o que vais fazer (e em que ordem)

| Ordem | Etapa | O que acontece |
|------:|--------|----------------|
| 1 | **Preparar ambiente** | Instalar JDK, Maven, Node (front), Git, Docker, editor. |
| 2 | **Subir infra (Docker)** | PostgreSQL 16 a escutar (normalmente porta **5432**). |
| 3 | **Backend (API)** | `mvn spring-boot:run` na **raiz** - sobe Spring Boot, aplica **Flyway**, expõe REST em **http://localhost:8000**. |
| 4 | **Frontend (SPA)** | `cd frontend` → `npm install` → `npm run dev` — Vite em **http://localhost:3000** com **proxy** `/api` → API. |
| 5 | **Base de dados** | Schema criado/atualizado **automaticamente** ao arrancar a API (scripts em `src/main/resources/db/migration/`). **Não** há Prisma/TypeORM no sentido dos tutoriais Node; o equivalente é **Flyway + JPA**. |
| 6 | **Integração total** | Login no browser, criar dados, confirmar que a UI fala com a API e a API com o Postgres. |
| 7 | **Workflow profissional** | Branches, commits convencionais, PRs; código em camadas (ver **3.8**). |

**Checklist — visão geral**

- [ ] Percebeste que a **API não é** `cd backend && npm run dev` neste repo.
- [ ] Sabes que **porta 8000** = API e **porta 3000** = Vite (ver `frontend/vite.config.ts`).
- [ ] Tens Docker pronto **antes** de esperar que a API ligue à base de dados.

---

### 3.1 Stack real do **Loja Sistema** (assumido pelo guia)

| Camada | Tecnologia neste projeto |
|--------|---------------------------|
| **API** | Java **21**, Spring Boot **3**, Maven, Spring Web, Spring Data JPA, **Flyway**, Spring Security + **JWT** |
| **Base de dados** | **PostgreSQL** 16 (recomendado via Docker) |
| **Frontend** | React 19, **Vite** 6, TypeScript (`frontend/`) |
| **Infra local** | **Docker Compose** na raiz (`docker-compose.yml`: serviço `db` e opcionalmente `api`) |
| **Documentação viva da API** | Swagger UI - **http://localhost:8000/swagger-ui.html** (com API em perfil default) |
| **Versionamento** | Git (GitHub ou outro remoto) |

Se num tutorial aparecer **Express, NestJS ou Prisma** como backend deste repo, **ignora** — aplica a outro projeto. Aqui o fluxo é: **Postgres → Spring Boot (Flyway incluído) → React**.

---

### 3.2 Etapa 1 — Instalar ferramentas (ordem sugerida para menos fricção)

1. **JDK 21** (Temurin ou Oracle, conforme a tua política)  
   - **Para quê:** compilar e correr a API.  
   - **Como verificar:** `java -version` → major **21**.

2. **Apache Maven 3.9+**  
   - **Para quê:** `mvn spring-boot:run`, `mvn test`, empacotar JAR.  
   - **Como verificar:** `mvn -version`.

3. **Node.js LTS (20+)**  
   - **Para quê:** **apenas** o frontend (`npm install`, `npm run dev`). A API **não** usa `npm start`.  
   - **Como verificar:** `node -v`.

4. **Git**  
   - **Para quê:** clonar, branches, histórico.  
   - **Como verificar:** `git --version`.

5. **Docker Desktop** (Windows/macOS) ou Docker Engine (Linux)  
   - **Para quê:** subir Postgres sem instalar servidor manualmente; testes de integração com **Testcontainers** quando correres `mvn test` com Docker disponível.  
   - **Crítico:** o ícone/engine tem de estar **a correr** antes de `docker compose`.

6. **Editor: Cursor ou VS Code**  
   - Extensões úteis neste stack: **Extension Pack for Java** (ou equivalente: Language Support for Java + Debugger), **Docker**, para o front **ESLint** / **Prettier** se a equipa usar. **ES7+ React/Redux/React-Native snippets** é opcional (ajuda em JSX).

**Checklist — Etapa 1**

- [ ] `java -version` → 21  
- [ ] `mvn -version` → Maven OK  
- [ ] `node -v` → 20+  
- [ ] `git --version` OK  
- [ ] Docker instalado e **a correr** (barra de estado / `docker info` sem erro)  
- [ ] Editor abre a pasta **raiz** do repositório `Loja Sistema` (onde está o `pom.xml`)

---

### 3.3 Etapa 2 — Subir infra (Docker)

Na **raiz** do repositório (mesma pasta que `pom.xml` e `docker-compose.yml`):

**Só a base de dados** (fluxo típico em desenvolvimento: API na IDE/Maven, DB no Docker):

```bash
docker compose up -d db
```

**Alternativa:** `docker compose up -d` sobe também o serviço `api` (imagem buildada). Para aprenderes passo a passo, **recomenda-se** `db` + `mvn spring-boot:run` na máquina, para veres logs da API no terminal.

**O que deve ficar a correr**

- Contentor Postgres **healthy** (healthcheck no compose).  
- Porta **5432** livre na máquina (ou alteras o mapeamento no compose com consciência de que o `application.yml` tem de bater certo).

**Problemas comuns**

| Sintoma | O que verificar |
|---------|------------------|
| “Cannot connect to Docker” | Docker Desktop **não** iniciado. |
| Porta 5432 em uso | Outro Postgres local; para o serviço ou muda a porta no compose **e** no JDBC em `application.yml` / variáveis. |
| `docker compose` falha | Estás na pasta certa? Existe `docker-compose.yml`? |

**Checklist — Etapa 2**

- [ ] Docker a correr  
- [ ] `docker ps` mostra o contentor `db` (nome pode variar com prefixo do projeto)  
- [ ] Postgres aceita ligação (a API ao arrancar deixa de falhar por “Connection refused”)

---

### 3.4 Etapa 3 — Backend (API Spring Boot)

**Pasta de trabalho:** raiz do repo (onde está o `pom.xml`). **Não** procures uma pasta `backend/` com `package.json` — não é esse o modelo.

1. **(Opcional mas saudável)** Compilar sem testes:  
   `mvn -q -DskipTests package`

2. **Segredo JWT** (obrigatório para a app arrancar em muitos cenários): mínimo **32 caracteres**. PowerShell, **na mesma sessão** onde vais correr o Maven:

   ```powershell
   $env:LOJAPP_JWT_SECRET = "aqui-uma-frase-longa-e-secreta-com-32chars-min"
   ```

3. **Subir a API:**

   ```bash
   mvn spring-boot:run
   ```

**O que acontece ao arrancar**

- A JVM sobe o Spring Boot.  
- O **Flyway** corre migrations em `src/main/resources/db/migration/` — crias/atualizas tabelas.  
- Endpoints REST ficam disponíveis (prefixo típico `/api/v1/...`).  
- **Swagger:** http://localhost:8000/swagger-ui.html (porta por defeito; se mudaste `server.port`, ajusta o URL).

**Testar a API sem frontend**

- Usa **Swagger UI** ou ferramentas como **Postman** / **Insomnia**.  
- Fluxo mínimo: `POST /api/v1/auth/register` → `POST /api/v1/auth/login` → copiar **`accessToken`** do JSON (só esse campo vem no corpo) → **Authorize** no Swagger com `Bearer <token>` → chamar por exemplo `GET /api/v1/lojapp/products`.  
- O refresh fica na **cookie** `lojapp_rt` (HttpOnly): no mesmo browser/host, pedidos seguintes a `/api/v1/auth/refresh` podem usar cookie automaticamente; no Swagger, confirma em **Network** (F12) que o `Set-Cookie` foi aplicado, ou envia `POST /auth/refresh` com corpo opcional `{"refreshToken":"..."}` se tiveres o valor (ex.: teste manual).

**Checklist — Etapa 3**

- [ ] Consola Maven **sem** stacktrace fatal ao fim do arranque  
- [ ] Logs indicam Flyway aplicado (ou “already up to date”)  
- [ ] Swagger abre no browser  
- [ ] Login devolve JWT e um endpoint protegido responde **200** com o token

---

### 3.5 Etapa 4 — Frontend (React + Vite)

**Pasta:** `frontend/`.

```bash
cd frontend
npm install
npm run dev
```

**O que acontece**

- O servidor de desenvolvimento Vite sobe em **http://localhost:3000** (definido em `vite.config.ts`).  
- Os pedidos a **`/api/...`** são **encaminhados por proxy** para **http://localhost:8000** - desde que a API esteja a correr.

**Variáveis de ambiente (sobretudo em build / produção)**

- **`VITE_API_BASE`**: URL **pública** da API no build do SPA (sem barra final). Em desenvolvimento local, o proxy costuma bastar; em **produção** defines o host real da API. Detalhes: `README.md` e comentários em `LoginPage.tsx`.
- **`VITE_CSP_CONNECT_SRC`**: se a API for noutro domínio, lista de origens (separadas por espaço) a acrescentar a `connect-src` da **CSP** gerada no build (`frontend/vite.config.ts`). Em dev, o cabeçalho CSP é mais permissivo (HMR).

**Problemas comuns**

| Sintoma | Causa provável |
|---------|----------------|
| Erro 502 / "Falha no proxy" no browser | API **não** está na porta 8000 ou não arrancou. |
| Login “não liga” | CORS ou URL errada em cenário **sem** proxy (ex.: build estático sem `VITE_API_BASE`). |

**Checklist — Etapa 4**

- [ ] `npm install` concluiu sem erros bloqueantes  
- [ ] `npm run dev` indica URL local (ex.: `:3000`)  
- [ ] Página de login abre  
- [ ] Após registar/iniciar sessão, dados da loja carregam (há comunicação com a API)

---

### 3.6 Etapa 5 — Base de dados (schema e migrations)

Neste projeto **não** corres `npx prisma migrate` nem `npm run migration` para criar tabelas.

- **Flyway** aplica ficheiros `V1__....sql`, `V2__....sql`, … **automaticamente** quando a API arranca.  
- **Dados iniciais:** não há “seed” obrigatório de demo; crias utilizador via **`/auth/register`** ou inserts manuais em ambiente de teste.

**Validação rápida**

- [ ] Na primeira subida da API, não há erro Flyway na consola.  
- [ ] Tabelas existem (podes confirmar com cliente SQL ou com endpoints que listam dados).  
- [ ] Se migrares de dados antigos e falhar **V3** (EAN duplicado), vê **Parte 4.4** deste guia.

**Checklist — Etapa 5**

- [ ] Migrations aplicadas (API arrancou uma vez com sucesso após Docker DB)  
- [ ] Consegues registar utilizador e gravar produtos (prova escrita na BD via API)

---

### 3.7 Etapa 6 — Integração total (ponta a ponta)

Objetivo: **browser → SPA → API → PostgreSQL → resposta → UI**.

**Teste real mínimo (alinhado ao produto)**

1. Registar conta e fazer login na UI.  
2. Criar **marca** e **produto** (fluxos do piloto).  
3. Listar produtos e ver dados consistentes.  
4. (Opcional) Importar NFe, venda, dashboard — **Parte 5** deste guia é o smoke test completo.

**Checklist — Etapa 6**

- [ ] Fluxo feliz sem erro vermelho no consola do browser (F12)  
- [ ] Sem surpresa de **CORS** em desenvolvimento (proxy + mesma origem no Vite)  
- [ ] Dados na **BD** persistem após F5; a **sessão** do SPA renova com a cookie de refresh (access só em memória — sem token em `localStorage`)

---

### 3.8 Etapa 7 — Organização profissional (código + Git)

**Estrutura esperada no backend (conceito)**

- Pacotes sob `src/main/java/com/lojapp/`: **controllers** finos, **services** com regras, **repositories** JPA, **domain** / entidades. DTOs para pedidos/respostas HTTP.  
- **Validação:** Bean Validation (`@Valid`, `@NotNull`, …) nos DTOs — o equivalente “tipo Zod/Joi” no ecossistema Java que mais se aproxima aqui.  
- **Erros:** handler global (ex.: `GlobalExceptionHandler`) devolvendo corpo estável (`message`, `code`, `timestamp`) — **Parte 2.0**.  
- **Logs:** logging Spring + boas práticas de não expor segredos.

**Git — fluxo simples**

```text
git checkout main          # ou develop, conforme o repo
git pull
git checkout -b feature/nome-curto-descritivo
# ... alterações ...
git add -p                 # preferível a "git add ." cego
git commit -m "feat: descrição curta em imperativo"
git push -u origin feature/nome-curto-descritivo
```

Depois: **Pull Request** → revisão → merge na branch alvo (`develop` / `main`, conforme política da equipa).

**Sinais de fragilidade (evitar)**

- Lógica pesada dentro do controller.  
- SQL ou regras de negócio espalhadas sem serviço.  
- Ignorar tratamento de erros HTTP e mensagens ao utilizador.

---

### 3.9 Erros comuns — resumo “júnior”

| # | Sintoma | Primeira coisa a verificar |
|---|---------|----------------------------|
| 1 | “Nada corre” / API não liga à BD | Docker **e** contentor Postgres; credenciais alinhadas com `application.yml` / compose. |
| 2 | API não arranca por JWT | `LOJAPP_JWT_SECRET` definido e **longo** o suficiente. |
| 3 | Frontend em branco / 502 em `/api` | API na **8000**? `mvn spring-boot:run` ativo? |
| 4 | Erro ao migrar (Flyway) | Logs na consola; se for **V3** e dados legados, **Parte 4.4**. |
| 5 | CORS em **produção** | `LOJAPP_CORS_ORIGINS` com o domínio exato do frontend (ver **Parte 4.1**). |
| 6 | “Segui um tutorial Node” | Volta à **3.1** — stack deste repo é **Java + Spring** na raiz. |

---

### Checklist 0 — Antes de começares (sessão de trabalho)

- [ ] Projeto **Loja Sistema** na máquina; **Java 21** e **Maven** (ver `README.md`).
- [ ] Sabes abrir linha de comandos na **raiz** do projeto e em `frontend/`.
- [ ] **Docker** com Postgres: `docker compose up -d db` na raiz (recomendado).
- [ ] (2 minutos) Lê `03-implantacao-pilotos.md` — secção “Como fazer — passo a passo” para ver exemplos JSON.

**Porquê:** sem estas bases, perdes tempo com versão Java errada, base de dados desligada ou JWT em falta.

### Checklist 1 — Ligar a “máquina” (uma vez por sessão de testes)

- [ ] PostgreSQL a correr (Docker ou local alinhado com `application.yml`).
- [ ] Segredo JWT seguro (32+ caracteres), ex. PowerShell:  
  `$env:LOJAPP_JWT_SECRET = "aqui-uma-frase-longa-e-secreta-32chars"`
- [ ] `mvn spring-boot:run` na raiz — consola **sem** erro vermelho no arranque.
- [ ] Browser: **http://localhost:8000/swagger-ui.html** abre (porta por defeito; ajuste se mudou `server.port`).

**Se falhar:** sem API não há piloto — resolve DB e arranque primeiro.

### OpenAPI — grupo «LojApp — Produtos» (alinhamento do contrato)

- [ ] Swagger → **«LojApp — Produtos»** → **GET** `/api/v1/lojapp/products`.
- [ ] Documentação mostra paginação (`page`, `size`, `sort`) e filtros `brandId`, `q`, `lowStock`.
- [ ] Resposta **200** = schema **`ProductPageResponse`** (não array na raiz).
- [ ] Após **Authorize** com JWT, **Try it out** devolve JSON com `content` e `totalElements`.
- [ ] (Opcional) `lowStock=true` ou `q=` com parte do nome — resultado faz sentido.

---

## Parte 4 — Operação e deploy (quando sair do localhost)

### 4.1 Variáveis e perfil `prod`

| Obrigatório em produção | Descrição |
|-------------------------|-----------|
| `LOJAPP_JWT_SECRET` | Segredo HMAC para JWT: **aleatório e longo** (mín. 32 bytes). Não usar placeholder de desenvolvimento. |
| `SPRING_DATASOURCE_URL` | JDBC PostgreSQL (ex.: `jdbc:postgresql://host:5432/lojapp`). |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | Credenciais (ou secrets do hosting). |
| `LOJAPP_CORS_ORIGINS` | Origens do frontend (lista separada por vírgulas). Obrigatório alinhar com o host real do SPA. |
| `LOJAPP_TRUST_FORWARD_HEADERS` | `true` **só** se a API estiver atrás de reverse proxy que **sanitiza** `X-Forwarded-For` (ex.: nginx na frente). Defeito `false`: o rate limit de auth usa `getRemoteAddr()` e ignora o cabeçalho forjado pelo cliente. |

**Cookies e HTTPS:** com perfil `prod`, `lojapp.auth.refresh-cookie-secure=true` — o refresh HttpOnly exige **HTTPS** no browser (ou o browser não grava a cookie corretamente em site seguro).

Arranque exemplo:

```bash
java -jar lojapp-api.jar --spring.profiles.active=prod
```

Com **`prod`:** `application-prod.yml` desliga Swagger/OpenAPI; essas rotas deixam de ser públicas na segurança. A app **falha no arranque** se o JWT for vazio ou valor de desenvolvimento (`ProductionSecurityConfig`).

Variáveis adicionais úteis: `LOJAPP_MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` (métricas só atrás de rede de confiança); `SPRING_DATASOURCE_*` já suportam substituição por env no `application.yml` — ver `.env.example` na raiz do repo.

### 4.1.1 Passo manual obrigatório — rate limit distribuído com Redis (sequência completa)

Esta secção é o **passo a passo manual** para colocar em produção a implementação já feita no código (`rate-limit-mode=redis` em `prod`).

#### Ordem de execução (faça nesta sequência)

1. **Provisionar Redis**
   - Escolher serviço (Redis gerido do cloud ou container dedicado).
   - Garantir rede privada/VPC ou ACL para a API.
2. **Configurar variáveis de ambiente da API**
   - `LOJAPP_RATE_LIMIT_MODE=redis`
   - `SPRING_DATA_REDIS_HOST=<host_redis>`
   - `SPRING_DATA_REDIS_PORT=<porta_redis>` (normalmente `6379`)
   - Se houver auth: `SPRING_DATA_REDIS_PASSWORD=<senha>`
   - Se houver TLS/SSL: configurar `SPRING_DATA_REDIS_SSL_ENABLED=true` (ou equivalente do teu provider).
3. **Manter fallback seguro**
   - Não remover o fallback para memória: se Redis cair, a API continua a responder (com proteção local por instância).
4. **Subir API em `prod`**
   - `java -jar lojapp-api.jar --spring.profiles.active=prod`
5. **Validar arranque e logs**
   - Confirmar que não existe erro de conexão Redis no arranque.
   - Se aparecer aviso de fallback para memória, tratar como incidente de infraestrutura.
6. **Teste funcional do rate limit**
   - Fazer burst de `POST /api/v1/auth/login` e `POST /api/v1/auth/register`.
   - Confirmar retorno `429` após exceder limite.
7. **Teste multi-instância (obrigatório em cluster)**
   - Subir 2 instâncias da API.
   - Repetir burst alternando chamadas entre instâncias.
   - Confirmar que o limite é **partilhado** (contador único via Redis).
8. **Operação contínua**
   - Monitorar quantidade de `429` por IP/rota.
   - Definir alerta para aumento anormal (ataque/bruteforce ou configuração agressiva).

#### Checklist de execução manual (marcar só com evidência)

- [ ] Redis provisionado e acessível pela API (rede + credenciais + TLS quando aplicável).
- [ ] Variáveis `LOJAPP_RATE_LIMIT_MODE` e `SPRING_DATA_REDIS_*` configuradas no ambiente `prod`.
- [ ] API iniciou com `--spring.profiles.active=prod` sem erro de Redis.
- [ ] Teste de burst em `/api/v1/auth/login` devolveu `429` no limite esperado.
- [ ] Teste de burst em `/api/v1/auth/register` devolveu `429` no limite esperado.
- [ ] Teste com 2 instâncias confirmou rate limit partilhado.
- [ ] Monitorização de `429` por IP/rota criada (dashboard/alerta).
- [ ] Procedimento de incidente definido: se Redis cair, verificar log de fallback e abrir ação corretiva.

#### Comandos e validação rápida (exemplo)

```bash
# Linux/macOS (exemplo)
export LOJAPP_RATE_LIMIT_MODE=redis
export SPRING_DATA_REDIS_HOST=redis-interno
export SPRING_DATA_REDIS_PORT=6379
export SPRING_DATA_REDIS_PASSWORD='***'
java -jar lojapp-api.jar --spring.profiles.active=prod
```

```powershell
# Windows PowerShell (exemplo)
$env:LOJAPP_RATE_LIMIT_MODE="redis"
$env:SPRING_DATA_REDIS_HOST="redis-interno"
$env:SPRING_DATA_REDIS_PORT="6379"
$env:SPRING_DATA_REDIS_PASSWORD="***"
java -jar lojapp-api.jar --spring.profiles.active=prod
```

### 4.1.2 Passo manual obrigatório — guardrails de importação NFe (tamanho e volume)

Esta secção cobre o que tens de configurar manualmente para os novos limites de importação NFe:

- limite de tamanho de XML (`max-xml-chars`);
- limite de número de itens por NFe (`max-items`).

#### Ordem de execução (faça nesta sequência)

1. **Definir política operacional**
   - Escolher limite máximo de caracteres por XML (ex.: `2_000_000` a `12_000_000` conforme operação).
   - Escolher limite máximo de itens por NFe (ex.: `300`, `500` ou `1000`).
2. **Configurar variáveis no ambiente**
   - `LOJAPP_NFE_IMPORT_MAX_XML_CHARS=<valor>`
   - `LOJAPP_NFE_IMPORT_MAX_ITEMS=<valor>`
3. **Subir API com perfil alvo**
   - `java -jar lojapp-api.jar --spring.profiles.active=prod`
4. **Validar comportamento com XML dentro do limite**
   - Importação deve funcionar normalmente em `POST /api/v1/lojapp/nfe/import`.
5. **Validar rejeição por limite de XML**
   - Enviar XML propositalmente acima do limite.
   - Esperado: **400** com mensagem sobre limite de caracteres.
6. **Validar rejeição por quantidade de itens**
   - Enviar NFe com itens acima de `LOJAPP_NFE_IMPORT_MAX_ITEMS`.
   - Esperado: **400** com mensagem sobre limite de itens.
7. **Ajustar valor com base em operação real**
   - Se houver falsos bloqueios legítimos, aumentar limite com critério.
   - Se houver abuso/risco de carga, reduzir e monitorar.

#### Checklist de execução manual (marcar só com evidência)

- [ ] Política de limites definida (tamanho XML + itens por NFe) e documentada internamente.
- [ ] `LOJAPP_NFE_IMPORT_MAX_XML_CHARS` configurada no ambiente.
- [ ] `LOJAPP_NFE_IMPORT_MAX_ITEMS` configurada no ambiente.
- [ ] API iniciada no perfil correto e sem erro de configuração.
- [ ] Importação válida (dentro do limite) testada com sucesso.
- [ ] Rejeição por XML acima do limite validada com retorno HTTP 400.
- [ ] Rejeição por número de itens acima do limite validada com retorno HTTP 400.
- [ ] Limites revistos após teste em dados reais de piloto (sem bloquear operação legítima).

#### Comandos e validação rápida (exemplo)

```bash
# Linux/macOS (exemplo)
export LOJAPP_NFE_IMPORT_MAX_XML_CHARS=12000000
export LOJAPP_NFE_IMPORT_MAX_ITEMS=1000
java -jar lojapp-api.jar --spring.profiles.active=prod
```

```powershell
# Windows PowerShell (exemplo)
$env:LOJAPP_NFE_IMPORT_MAX_XML_CHARS="12000000"
$env:LOJAPP_NFE_IMPORT_MAX_ITEMS="1000"
java -jar lojapp-api.jar --spring.profiles.active=prod
```

### 4.2 PostgreSQL com Docker (local ou servidor simples)

Na raiz existe `docker-compose.yml` (Postgres 16):

```bash
docker compose up -d
```

Valores por omissão alinhados com `application.yml` — **trocar** user/password em ambiente real.

### 4.3 Flyway — versões no repositório (`src/main/resources/db/migration`)

O Flyway aplica estes scripts **por ordem** ao arranque da API (base nova ou em evolução). **Não edites** ficheiros já aplicados em ambientes partilhados; para mudar schema, adiciona sempre um novo `V5__...`, `V6__...`, etc.

| Versão | Ficheiro | Resumo |
|--------|----------|--------|
| **V1** | `V1__users.sql` | Utilizadores / autenticação |
| **V2** | `V2__lojapp_core.sql` | Núcleo LojApp (marcas, produtos, NFe, stock, vendas, …) |
| **V3** | `V3__products_ean_unique.sql` | Índice único parcial: **um EAN por utilizador** (EAN não vazio) |
| **V4** | `V4__products_brands_updated_at.sql` | Colunas `updated_at` em `products` e `brands` |
| **V5** | `V5__products_updated_at_trigger.sql` | Trigger `updated_at` (produtos/marcas) |
| **V6** | `V6__refresh_audit_role.sql` | `refresh_tokens`, `audit_logs`, coluna `app_role` em `users` |
| **V7** | `V7__inventory_nfe_constraints.sql` | `CHECK` saldo ≥ 0 e `movement_type`; índice `nfe_entries(access_key)` |

Instalações **novas** aplicam V1 → V7 em sequência. O problema típico em bases **com dados antigos** aparece na **V3** (ver secção seguinte).

### 4.4 Flyway — falha na migration `V3__products_ean_unique.sql`

A V3 cria índice único parcial: **um EAN por utilizador** (quando `ean` não é vazio).

**Sintoma:** erro ao criar `uq_products_user_ean`.

**Causa típica:** duas linhas em `products` com mesmo `user_id` e mesmo `ean` (não nulo).

**O que fazer:**

1. Localizar duplicados:

   ```sql
   SELECT user_id, ean, count(*) FROM products
   WHERE ean IS NOT NULL AND length(trim(ean)) > 0
   GROUP BY user_id, ean
   HAVING count(*) > 1;
   ```

2. Corrigir dados (fundir, anular EAN errado, apagar duplicado de teste) **antes** de reaplicar a migration.

3. Se a migration ficou a meio: procedimento Flyway habitual (`repair`, backup+staging, etc.). **Não** apagar versões Flyway em produção sem processo acordado.

Em instalações **novas** sem dados legados, a V3 aplica sem conflito.

### 4.5 Verificação local (qualidade)

```bash
mvn test
```

---

## Parte 5 — Piloto: uma loja (repetir 3× com emails diferentes)

Faz **na ordem**. Email **novo** por loja (`piloto1@...`, `piloto2@...`, `piloto3@...`).

### Parte A — Conta e token

- [ ] **Registar:** `POST /api/v1/auth/register` (password ≥ 8 caracteres).
- [ ] **Login:** `POST /api/v1/auth/login` se precisares de token novo.
- [ ] Copiar **`accessToken`**.
- [ ] Swagger → **Authorize** → `Bearer ` + token (espaço após `Bearer`) → Authorize / Close.

### Parte B — Dados da loja

- [ ] Criar **marca:** `POST /api/v1/lojapp/brands`.
- [ ] Criar **produto** com `brandId` certo: `POST /api/v1/lojapp/products`.
- [ ] **Listar produtos:** `GET /api/v1/lojapp/products` — confirmar objeto com **`content`** e **`totalElements`**.

### Parte C — NFe e stock

- [ ] Importar XML: `POST /api/v1/lojapp/nfe/import` (`rawXml`).
- [ ] Duplicar o mesmo XML: esperar **409**.
- [ ] (Opcional) Ajuste manual: `POST /api/v1/lojapp/inventory/adjust`.

### Parte D — Alerta e venda

- [ ] Stock baixo: `GET /api/v1/lojapp/inventory/low-stock`.
- [ ] Venda com stock suficiente: `POST /api/v1/lojapp/sales` (podes omitir `unitCost`).
- [ ] Vender **mais** que o stock: esperar **400**.
- [ ] Resposta da venda = **id** (anotar se quiseres rastrear).

### Parte E — Dashboard

- [ ] `GET /api/v1/lojapp/dashboard/brands`.
- [ ] **Primeira linha** de `metrics` = marca com **maior lucro** no período (não necessariamente maior faturamento).

### Parte F — Fechar esta loja

- [ ] Uma linha em `03-implantacao-pilotos.md` (email, data, smoke OK).
- [ ] Se falhou: nota curta (passo + mensagem de erro).

### Checklist 3 — Três lojas

- [ ] Loja 1: Partes A–F com email **A**.
- [ ] Loja 2: idem com email **B** (conta nova).
- [ ] Loja 3: idem com email **C** (conta nova).
- [ ] Confirmar isolamento: dados da Loja 1 **não** aparecem logado como Loja 2.

### Checklist 4 — Fechar o MVP no documento certo

Só com evidência real:

- [x] Abrir `docs/lojapp/01-escopo-mvp.md`.
- [x] Marcar **[x]** em cada critério **só** se testaste / utilizador piloto confirmou. *(cumprido com evidência 2026-04-24)*
- [x] A secção “Cobertura técnica” do `01` descreve a API; o **[x]** do piloto confirma **uso real**.

---

## Parte 6 — Se ficares bloqueado

Para um **mapa rápido** (Docker, JWT, proxy Vite, CORS, Flyway), vê também **Parte 3.9**.

1. **401** nos endpoints LojApp → **Authorize** de novo com token fresco.  
2. **400 em venda** → ler `message` no JSON de erro (Parte **2.0**); muitas vezes **stock insuficiente**.  
3. **4xx/5xx com corpo JSON** → usar `code` para classificar (`VALIDATION_ERROR`, `CONFLICT`, …) e `message` para detalhe.  
4. **Erro ao arrancar** → PostgreSQL parado ou credenciais/`application.yml` ≠ Docker.  
5. **Erro em prod** sobre `LOJAPP_JWT_SECRET` → segredo forte definido (não o de desenvolvimento)?  
6. **Flyway V3 (EAN duplicado)** → ver Parte **4.4** (duplicados de EAN).

---

## Parte 7 — Lacunas pós-v3 e próximos passos (priorizado)

Esta secção transforma o estado atual num **plano executável**: primeiro garantir operação real do piloto, depois UX de baixo esforço e por fim melhorias de escala.

### 7.1 Lacunas técnicas restantes (pós-v3)

| Lacuna | Estado atual | Impacto | Decisão prática |
|------|---------------|---------|-----------------|
| **Dropdown de produtos com muitos itens** | **Resolvido:** `PilotoSaleTab` usa autocomplete com `GET /api/v1/lojapp/products?q=` (debounce). | — | Manter; catálogos grandes ficam usáveis. |
| **NFe namespaced sem teste end-to-end** | **Automatizado:** `NfeImportStockIntegrationTest` cobre XML simples, namespaced e **vários** `<det>` com `SEM GTIN` / sem `cEAN` (**Docker** necessário para correr). | XMLs **reais** das lojas podem ter variantes (assinatura, layout) | Passo **7.2.1**: validar ficheiros reais e registar em `02-pilotos-e-xmls.md`. |
| **NFe sem guardrails de volume** | **Resolvido no backend:** limites configuráveis `LOJAPP_NFE_IMPORT_MAX_XML_CHARS` e `LOJAPP_NFE_IMPORT_MAX_ITEMS`; rejeição 400 fora do limite. | Sem configuração de ambiente, podes bloquear XML legítimo ou aceitar carga excessiva. | Definir e validar checklist da Parte **4.1.2** em cada ambiente. |
| **Rate limiter distribuído (opcional por ambiente)** | `AuthRateLimitFilter` suporta `memory` (default) e `redis` (`LOJAPP_RATE_LIMIT_MODE`) com fallback local | Em cluster, se ficar em `memory`, cada instância limita separadamente | Em produção com múltiplas réplicas: usar `redis` e validar checklist da Parte **4.1.1** |
| **App frontend acoplado (auth + routing + UI)** | **Resolvido:** `App.tsx` usa `AuthRoute` e `ProtectedLayout` (`frontend/src/routes/*`). | Sem validação manual, pode haver regressão em redirect/login/logout. | Executar checklist da secção **7.3 Passo 5c** após cada alteração de rotas/auth. |
| **Venda sem aviso prévio de stock insuficiente** | **Resolvido:** aviso inline + botão desativado quando `quantity > stockQty`; backend mantém **400**. | — | — |
| **Erros HTTP inconsistentes / portfólio** | **Resolvido (2026-04-24):** corpo `{ message, code, timestamp }`; README; regressão em `BrandControllerTest`; tokens CSS + `PageHeader` no front (catálogo). | Integradores antigos que esperavam `error`/`path` | Migrar clientes para `code`; SPA já aceita legado `error` |

### 7.2 Prioridade 1 — executar o piloto (esta semana)

#### Passo 1 — Coletar e validar XMLs reais das 3 lojas

- [ ] Recolher XMLs conforme `02-pilotos-e-xmls.md` (mínimo: com namespace, múltiplos itens, `cEAN` vazio e campos opcionais ausentes).
- [ ] Executar validação com `NfeXmlParser` para cada arquivo antes de go-live.
- [ ] Registar quais XMLs passaram/falharam e o motivo.

**Como validar (ordem sugerida):**

1. **Sanidade do parser (CI local):** `mvn -q test -Dtest=NfeXmlParserTest` — não lê os teus ficheiros, confirma que o módulo de parse está verde.
2. **Por ficheiro real:** com a API em perfil **default** (Swagger ligado), `POST /api/v1/lojapp/nfe/import` com `rawXml` (ou script `scripts/import-nfe-folder.sh` em Git Bash/WSL). Anonimiza ou usa conta de teste; **não** commits com dados sensíveis.
3. **Integração completa (opcional):** com Docker a correr, `mvn test -Dtest=NfeImportStockIntegrationTest` — valida Postgres + Flyway + stock.

**Registo:** preencher a tabela em `02-pilotos-e-xmls.md` (secção «Registo Passo 7.2»).

**Explicação:** isto reduz risco de surpresa em produção; o parser pode funcionar em XML simples e falhar em variante real.

#### Passo 2 — Executar smoke test completo para 3 lojas

- [x] Criar 3 contas (`piloto1`, `piloto2`, `piloto3`).
- [x] Rodar o fluxo da **Parte 5 (A até F)** para cada loja.
- [x] Preencher `03-implantacao-pilotos.md` com data, conta e resultado.
- [x] Confirmar isolamento de dados entre lojas.

**Evidência (2026-04-24):** contas e fluxo registados em `03-implantacao-pilotos.md` («Contas piloto com smoke OK»); critérios alinhados em `docs/lojapp/01-escopo-mvp.md`.

**Explicação:** sem esse ciclo 3x com evidência, o MVP ainda não está validado no mundo real.

#### Passo 3 — Deploy com perfil de produção

- [ ] Subir com `docker-compose.prod.yml`.
- [ ] Definir no ambiente: `LOJAPP_JWT_SECRET` e `POSTGRES_PASSWORD`.
- [ ] Confirmar `SPRING_PROFILES_ACTIVE=prod`.
- [ ] Verificar Swagger/OpenAPI desligado em produção.

**Execução sugerida (PowerShell, pasta raiz do repo):**

```powershell
$env:POSTGRES_PASSWORD = "defina-uma-password-forte"
$env:LOJAPP_JWT_SECRET = "defina-um-segredo-aleatorio-com-pelo-menos-32-caracteres"
docker compose -f docker-compose.prod.yml up -d --build
```

**Verificações rápidas:**

- Saúde: `curl -s http://localhost:8000/actuator/health` (ajusta porta se alterares o mapeamento no compose).
- OpenAPI desligado: `curl -s -o NUL -w "%{http_code}" http://localhost:8000/v3/api-docs` - esperado **404** (ou não **200**), pois `springdoc.api-docs.enabled: false` em `application-prod.yml`.
- Login: `POST http://localhost:8000/api/v1/auth/login` com JSON email/password; deve devolver JWT.

**Explicação:** este passo garante segurança mínima e evita “falso positivo” de ambiente local.

### 7.3 Prioridade 2 — melhorias de UX (antes ou durante piloto)

#### Passo 4 — Aviso inline de stock insuficiente na venda

- [x] No `PilotoSaleTab`, comparar `Number(quantity)` com `stockQty`.
- [x] Renderizar aviso condicional e desativar submit quando excede saldo.
- [x] Manter validação backend atual (400) como proteção final.

**Explicação:** melhoria de baixo esforço e alto impacto; evita tentativas de venda inválidas durante o piloto.

#### Passo 5 — Teste de integração com XML namespaced real

- [x] Expandir `NfeImportStockIntegrationTest` com XML em namespace Portal Fiscal (`importNfe_namespacedXml_increasesBalance`).
- [x] Cobrir no **mesmo** fluxo de integração: múltiplos `<det>`/`<prod>`, `SEM GTIN` e ausência de `cEAN` (`importNfe_namespacedXml_multipleItems_emptyOrSemGtinCean_increasesBothBalances`). XMLs reais das lojas continuam recomendados no Passo 1 para variantes adicionais.
- [x] Validar persistência: saldo no banco após import (método namespaced; requer Docker para executar).

**Explicação:** fecha a lacuna entre teste unitário do parser e o comportamento completo no banco.

#### Passo 5a — Configurar limites de importação NFe por ambiente

- [x] Implementar no backend limites configuráveis de tamanho (`max-xml-chars`) e itens (`max-items`) com erro 400 fora do limite.
- [ ] Definir valores operacionais por ambiente e aplicar no deploy (`LOJAPP_NFE_IMPORT_MAX_XML_CHARS`, `LOJAPP_NFE_IMPORT_MAX_ITEMS`).
- [ ] Validar com XML real de piloto que o limite não bloqueia operação legítima.

**Explicação:** evita payload excessivo/transações longas sem comprometer notas fiscais reais.

#### Passo 5b — Contrato de erros e base de UI (2026-04-24)

- [x] **`ApiErrorResponse`:** apenas `message`, `code`, `timestamp` (ver Parte **2.0**).
- [x] **README:** secção «Erros da API» + nota sobre screenshots em `docs/screenshots/`.
- [x] **Frontend:** `theme/tokens.css` (escala `--space-*`), componente `components/ui/PageHeader.tsx`, uso em `ProductsBrowseTab`.

**Explicação:** alinha documentação e cliente ao contrato estável de erro; melhora reutilização visual incremental.

#### Passo 5c — Navegação protegida no frontend (AuthRoute + ProtectedLayout)

- [x] Refatorar `App.tsx` para composição de rotas, removendo acoplamento direto de auth + UI.
- [x] Criar `frontend/src/routes/AuthRoute.tsx` para encapsular login/register e redirecionamento de sessão.
- [x] Criar `frontend/src/routes/ProtectedLayout.tsx` para proteger rotas privadas (`/piloto/*`).
- [x] Validar com `npm run lint` e `npm run test -- --run`.

**Checklist manual de validação da navegação protegida (passo a passo):**

**Coberto automaticamente (já validado por comando):**

- [x] Build/lint do frontend sem regressão após refatoração de rotas (`npm run lint`).
- [x] Suite de testes frontend verde após refatoração (`npm run test -- --run`).

**Depende de validação manual no browser (não coberto por teste automatizado atual):**

- [ ] Acessar `/piloto/products` sem token/sessão → deve redirecionar para `/login`.
- [ ] Fazer login válido → deve ir para `/piloto/products`.
- [ ] Com sessão ativa, abrir `/login` → deve redirecionar para `/piloto/products`.
- [ ] Navegar entre tabs de `/piloto/*` sem perder sessão.
- [ ] Executar logout no workspace → deve limpar sessão e voltar para `/login`.
- [ ] Após logout, tentar abrir URL privada diretamente (`/piloto/sales`) → deve voltar para `/login`.

**Explicação:** garante que autenticação e roteamento continuam corretos após refatorações de estrutura no frontend.

### 7.4 Prioridade 3 — depois de validar o piloto

#### Passo 6 — Autocomplete de produtos (busca lazy)

- [x] Substituir select fixo por autocomplete no frontend (`PilotoSaleTab`).
- [x] Consultar `GET /api/v1/lojapp/products?q=termo` com debounce.
- [x] Estado “a pesquisar…” na lista de sugestões.

**Explicação:** melhora muito a operação quando o catálogo cresce e reduz erro de seleção.

#### Passo 7 — Rate limiting distribuído (escala)

- [x] Implementar modo distribuído com Redis em `/api/v1/auth/login`, `/api/v1/auth/refresh` e `/api/v1/auth/register`, mantendo fallback para memória.
- [ ] Ativar `LOJAPP_RATE_LIMIT_MODE=redis` no ambiente alvo e validar com teste multi-instância (Parte **4.1.1**).
- [ ] Monitorar contagem de bloqueios (`429`) por IP e por endpoint.

**Explicação:** necessário quando tiver mais de 1 instância ou reinícios frequentes em produção.

#### Passo 8 — Features pós-MVP

- [ ] Só avaliar após 3 lojas validadas + 7 dias sem bug bloqueante.
- [ ] Priorizar por retorno real do piloto (não por suposição).
- [ ] Candidatas: alertas WhatsApp, PDV completo, relatórios exportáveis.

**Explicação:** protege foco da equipa e evita crescer produto sem validação de uso.

### 7.5 Checklist final — “tudo rodando” ponta a ponta

Marca como concluído apenas com evidência:

- [x] `mvn test` verde (incluindo testes novos de integração NFe namespaced; integração Testcontainers só corre com Docker).
- [ ] Ambiente `prod` sobe com variáveis obrigatórias e sem Swagger público — fechar após **Passo 3** acima e marcar em `01-escopo-mvp.md` → *Deploy em perfil prod validado*.
- [x] Smoke test A–F aprovado nas 3 lojas (evidência em `03-implantacao-pilotos.md`, 2026-04-24).
- [x] `01-escopo-mvp.md` atualizado com critérios de aceite marcados por evidência.
- [x] Backlog pós-piloto separado em «corrigir agora» vs «evolução» em `29-resumo-executivo-status-riscos-proximos-passos.md` (evidência histórica 2026-04-24).

---

## Parte 8 — Onde está o quê

| Ficheiro | Para quê |
|----------|----------|
| `10-guia-junior-piloto-deploy-proximos-passos.md` (este) | Roteiro júnior (Parte 3: ferramentas → Docker → API → front → integração), fluxos API, piloto, deploy, Flyway V1+, roadmap |
| `01-escopo-mvp.md` | Critérios oficiais |
| `02-pilotos-e-xmls.md` | XMLs de teste / piloto |
| `03-implantacao-pilotos.md` | Implantação por loja, evidências smoke e feedback |
| `29-resumo-executivo-status-riscos-proximos-passos.md` | Backlog «corrigir agora» vs «evolução», riscos e próximos passos |
| Swagger UI | Contrato vivo da API |

---

*Este guia existe para não saltares passos: primeiro contexto; se fores novo no repo, **Parte 3** cobre da instalação de ferramentas à integração total; depois fluxos detalhados (Parte 2), deploy quando necessário (Parte 4), piloto com evidência (Parte 5), bloqueios (Parte 6) e roadmap (Partes 7–8).*
