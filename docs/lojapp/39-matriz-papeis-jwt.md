# Matriz de papéis JWT (as-is vs alvo)

Contrato mínimo para endurecer autorização **depois** de HITL. Não altera API nesta fatia.

Papéis: `USER`, `ADMIN`, `REPRESENTATIVE`, `CASHIER`, `SELLER`, `MANAGER`. Tenancy continua a ser `user_id` em todas as entidades operacionais.

Plano: `.cursor/plans/plan-2026-08-18-lojapp-papeis.md`. Auth/sessão: [12](./12-contratos-autenticacao-e-sessao.md). PDV vs backoffice (assistente): [33](./33-assistente-ia-mapeamento-api-fatia-c.md) §5.

## Actual (código)

| Superfície | Quem passa hoje | Teste de linha de base |
|------------|-----------------|------------------------|
| `GET /api/v1/users/admin/list` | só `ADMIN` | `UserControllerTest`, `UserAuthorizationIntegrationTest` |
| Caixa `.../pos/cash-sessions/**` | `CASHIER`, `MANAGER`, `USER`, `ADMIN` — **não** `SELLER` / `REPRESENTATIVE` | `CashSessionControllerTest.openCashSession_withSellerRole_returnsForbidden` |
| PDV `POST .../pos/sales/finalize` | todos os papéis listados no controller, incl. `CASHIER` e `SELLER` | `PosSaleControllerTest` (USER) |
| NFe import / apply-suggestions | o mesmo saco amplo, **incl. CASHIER e SELLER** | `NfeControllerTest.importNfe_withCashierRole_isAllowed` |
| Catálogo (ex. `DELETE .../brands/{id}`) | o mesmo saco amplo, **incl. CASHIER** | `BrandControllerTest.deleteBrand_withCashierRole_returns204` |
| Anónimo (sem papel LojApp) | 403 em NFe import | `NfeControllerTest.importNfe_withAnonymousRole_returnsForbidden` |

## Alvo (próxima fatia, sem data)

Não implementar até HITL. Quando for, os testes as-is de CASHIER em NFe/marca devem **inverter** para 403.

| Superfície | Alvo |
|------------|------|
| Lista admin | `ADMIN` (já está) |
| Caixa | `CASHIER`, `MANAGER`, `USER`, `ADMIN` (já está; SELLER fora) |
| Finalize PDV | `CASHIER`, `SELLER`, `MANAGER`, `USER`, `ADMIN` (`REPRESENTATIVE` fora, alinhado a [33] §5) |
| NFe (import + sugestões) | `USER`, `ADMIN`, `MANAGER`, `REPRESENTATIVE` |
| Apagar / criar marca | `USER`, `ADMIN`, `MANAGER`, `REPRESENTATIVE` (não CASHIER/SELLER) |
| Ajuste de stock manual | mesmo conjunto que NFe/catálogo (não CASHIER de loja) |

`USER` no piloto é dono da loja demo: mantém caixa e backoffice no alvo.

## Fora desta spec

- Mudar `@PreAuthorize`.
- Novos papéis.
- Autorização no frontend como fonte de verdade.
