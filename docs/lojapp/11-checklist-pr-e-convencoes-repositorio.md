# Checklist de PR e convenções do repositório (LojApp)

Documento curto para usar **antes do primeiro push** e em cada contribuição. Complementa `AGENTS.md` e o guia `10-guia-junior-piloto-deploy-proximos-passos.md`. Para **aceite por entrega** e registo de decisões curtas, ver também `18-decisoes-e-checklist-entrega.md`.

## Estrutura mínima esperada (hoje)

| Caminho | Função |
|---------|--------|
| `src/main/java/com/lojapp/` | API Spring: `application`, `config`, `controller`, `domain`, `dto`, `entity`, `exception`, `repository`, `security`, `service`, `util`, `xml` |
| `src/main/resources/` | `application.yml`, `db/migration/` (Flyway) |
| `src/test/java/com/lojapp/` | Testes (espelhar pacotes quando fizer sentido) |
| `frontend/src/` | SPA: `api`, `components`, `features`, `hooks`, `pages`, `routes`, `services`, `theme` |
| `docs/lojapp/` | Produto, pilotos, roadmap, checklists |
| `.github/` | CI (`workflows/`), Dependabot (`dependabot.yml`) quando existir remoto GitHub |

**Regras:** controllers finos; negócio em `service` (e, no futuro, casos de uso); schema só via Flyway; DTOs separados de entidades JPA.

## Branch protection (GitHub)

Quando o repositório estiver no GitHub, configurar na **default branch** (ex. `main`):

- Exigir PR antes do merge; exigir revisão (mínimo 1) para alterações sensíveis.
- **Status checks obrigatórios:** jobs do workflow `CI` — `backend-unit`, `backend-integration`, `frontend`, `docker-image` (build da imagem + Trivy).
- Opcional: bloquear push direto, exigir branch atualizada, assinatura de commits.

Isto complementa este checklist; detalhes dependem da organização.

## Antes do primeiro `git init` / push

- Confirmar que `.gitignore` na raiz ignora `target/`, `node_modules/`, `dist/`, `.env` (já previstos).
- Não commitar XMLs fiscais reais, credenciais nem `.env` preenchido.
- Opcional: apagar pastas geradas localmente (`mvn clean`, remover `frontend/node_modules` só se fores reinstalar antes de trabalhar) para o primeiro commit ser só fonte — ou manter e confiar no ignore; o importante é **nunca** versionar esses diretórios.

## Checklist de Pull Request

### Higiene

- [ ] Diff pequeno e com título/descrição que digam *o quê* e *porquê*.
- [ ] Sem ficheiros acidentais (IDE, logs, `target/`, `node_modules/`, artefactos de Playwright em `frontend/test-results/` se aplicável).
- [ ] Sem segredos; variáveis sensíveis só em exemplo (`*.example`) ou documentação genérica.
- [ ] Se houver migração Flyway: **nova** versão em `db/migration/`, sem editar migrações já aplicadas em ambientes partilhados.

### Arquitetura e contratos

- [ ] Alterações a venda, NFe ou ajuste de stock: rever `13-estoque-concorrencia-e-idempotencia.md` (locks, transacções, `Idempotency-Key`).
- [ ] Endpoints novos ou alterados refletidos no Swagger (springdoc).
- [ ] Validação em DTOs de entrada; erros tratados de forma consistente com o resto da API.
- [ ] Mudança de contrato público (URL, payload, códigos) mencionada na descrição do PR e, se necessário, nota em `docs/lojapp/` (política de versão: `17-versionamento-api-rest.md`).

### Testes e verificação

- [ ] CI GitHub: jobs `backend-*`, `frontend` e `docker-image` (build + Trivy na imagem: CRITICAL/HIGH com fix; exceções em `.trivyignore` só com nota no PR).
- [ ] `mvn test` verde para alterações de backend (ou explicação no PR se não for possível correr).
- [ ] Para frontend: `npm run test` / `npm run lint` conforme o tipo de mudança (indicar no PR o que foi corrido).
- [ ] Bugfix: teste de regressão quando for viável.

### Segurança (auth / sessão)

- [ ] Rotas sensíveis continuam protegidas; alterações em auth/cookies/CORS alinhadas a `12-contratos-autenticacao-e-sessao.md`.

---

*Última alinhada ao roadmap em `plano-execucao-sprint-1-a-6.md`.*
