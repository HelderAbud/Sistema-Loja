# Workflow Cursor + Superpowers (LojApp)

**Função:** guia operacional para trabalhar neste repositório com o **Cursor** e o plugin **Superpowers**, em linha com `AGENTS.md`, **fatias verticais** e **contrato antes de improvisar** (alinhado ao resumo do método Rheyder que usas no teu fluxo pessoal).

**Playbook longo (opcional):** se tens a pasta de Skills noutro sítio, a cópia canónica do passo a passo completo chama-se `superpowers-cursor-playbook.md`. Este ficheiro resume o que precisas no dia a dia **dentro do LojApp**.

---

## 1. Estrutura no repositório

| Caminho | Uso |
|---------|-----|
| `AGENTS.md` | Visão geral, comandos (`mvn`, Docker, frontend), regras de arquitetura e segurança. |
| `.cursor/rules/` | Regras persistentes (backend, testes, migrations, contratos API). |
| `.cursor/plans/` | Planos aprovados: `plan-AAAA-MM-DD-<slug>.md`. Trilha **Normal**: plano antes de diffs grandes. |
| `docs/lojapp/` | Contratos de domínio, piloto, MVP, workflow (este ficheiro). |
| `CHECKLIST_FINAL.md` (raiz) | Auditoria portfólio / pré-GitHub; cruza com as fatias do plano Rheyder. |

---

## 2. Quando usar o fluxo completo

**Ritual completo** (brainstorm → plano → implementação em fatias → verificação → revisão): feature nova, bug difícil, refatoração arriscada, regra de negócio importante, alteração a **contrato REST**, **Flyway**, integração ou **auth**.

**Fluxo curto:** rename, typo, ajuste cosmético, alteração trivial num único ficheiro — podes ir direto; o custo do processo pode ser maior que o benefício.

No Cursor, tarefas não triviais: preferir **Plan Mode** e só executar após reveres o plano.

---

## 3. Ciclo recomendado (tarefa não trivial)

1. **Brainstorming** (skill Superpowers) — objetivo, critério de sucesso, riscos e o que fica fora de escopo.
2. **Plano** — Plan Mode ou ficheiro `plan-*` em `.cursor/plans/` com fatias, comandos de verificação e gates humanos (migrations, API pública, segredos).
3. **Implementação** — diffs pequenos; preferir **fatias verticais** (valor ou comportamento fechado ponta a ponta relativamente ao recorte acordado).
4. **Verificação** — na raiz do repo: `./mvnw -Pci-unit-tests test` (e integração com Docker quando aplicável, ver `AGENTS.md`); frontend: `npm run lint`, `npm run test`; se tocaste na API: `GET /actuator/health`, Swagger local.
5. **Revisão** — diff revisável; em mudanças sensíveis, gate humano antes de merge (ver `AGENTS.md` → Segurança).
6. **Entrega** — commit ou PR com bloco **Validação**: comandos corridos + resultado (pass/fail).

---

## 4. Ligação ao método Rheyder (resumo)

- **Triagem:** escolher trilha **Simple** / **Normal** / **Complex** / **Hotfix** antes de acelerar; subir ou descer de trilha se a realidade mudar.
- **Interface-design:** não expandir superfície pública (API, tipos, ecrã) sem contrato mínimo registado (spec, plano, OpenAPI, ou bullets no plano).
- **Drift:** se a implementação divergir materialmente do plano, atualiza o plano ou regista a divergência **antes** do merge.

Plano piloto **Normal** já no repo: `.cursor/plans/plan-2026-05-04-rheyder-piloto-trilha-normal.md`.

---

## 5. Onde aprofundar

- Piloto local, deploy, portfólio: `10-guia-junior-piloto-deploy-proximos-passos.md`
- Índice de prioridades: `00-indice-prioridades-sistema.md`
- Decisões e aceite: `18-decisoes-e-checklist-entrega.md`
- PR e convenções: `11-checklist-pr-e-convencoes-repositorio.md`
- PRD / QA: `31-checklist-producao-prd-lojapp.md`

---

## 6. Instalação do plugin Superpowers no Cursor

```text
/add-plugin superpowers
```

Reinicia a sessão do agente depois de instalar e faz um teste pedindo um plano pequeno antes de codar.
