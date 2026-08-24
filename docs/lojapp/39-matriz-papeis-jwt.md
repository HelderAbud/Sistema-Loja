# Matriz de papéis JWT

Contrato de autorização por superfície. Tenancy continua a ser `user_id` em todas as entidades operacionais.

Papéis: `USER`, `ADMIN`, `REPRESENTATIVE`, `CASHIER`, `SELLER`, `MANAGER`.

Plano da fatia de documentação: `.cursor/plans/plan-2026-08-18-lojapp-papeis.md`. Endurecimento: `feat/harden-jwt-roles-d`. Auth/sessão: [12](./12-contratos-autenticacao-e-sessao.md). PDV vs backoffice: [33](./33-assistente-ia-mapeamento-api-fatia-c.md) §5.

## Código actual (após endurecimento)

| Superfície | Quem passa | Teste |
|------------|------------|--------|
| `GET /api/v1/users/admin/list` | só `ADMIN` | `UserControllerTest`, `UserAuthorizationIntegrationTest` |
| Caixa `.../pos/cash-sessions/**` | `CASHIER`, `MANAGER`, `USER`, `ADMIN` — **não** `SELLER` / `REPRESENTATIVE` | `CashSessionControllerTest.openCashSession_withSellerRole_returnsForbidden` |
| PDV `POST .../pos/sales/finalize` | `CASHIER`, `SELLER`, `MANAGER`, `USER`, `ADMIN` — **não** `REPRESENTATIVE` | `PosSaleControllerTest.finalizeSale_withRepresentativeRole_returnsForbidden` |
| NFe import / apply-suggestions | `USER`, `ADMIN`, `MANAGER`, `REPRESENTATIVE` | `NfeControllerTest.importNfe_withCashierRole_returnsForbidden` |
| Criar / atualizar / apagar marca | `USER`, `ADMIN`, `MANAGER`, `REPRESENTATIVE` | `BrandControllerTest.deleteBrand_withCashierRole_returnsForbidden` |
| Listar marcas | papéis operacionais incl. `CASHIER` / `SELLER` | `BrandControllerTest.listBrands_withCashierRole_returnsJson` |
| Ajuste de stock manual | `USER`, `ADMIN`, `MANAGER`, `REPRESENTATIVE` | `InventoryControllerTest.adjustStock_withCashierRole_returnsForbidden` |
| Criar / atualizar produto | `USER`, `ADMIN`, `MANAGER`, `REPRESENTATIVE` | `ProductControllerTest.createProduct_withCashierRole_returnsForbidden` |
| Criar fornecedor | `USER`, `ADMIN`, `MANAGER`, `REPRESENTATIVE` | `SupplierControllerTest.createSupplier_withCashierRole_returnsForbidden` |
| Criar coleção / modelo | `USER`, `ADMIN`, `MANAGER`, `REPRESENTATIVE` | `ProductCollectionControllerTest` / `ProductModelControllerTest` |
| Listar produtos | papéis operacionais incl. `CASHIER` | `ProductControllerTest.listProducts_withCashierRole_returnsPagedEnvelope` |
| Listar vendedoras | `USER`, `ADMIN`, `CASHIER`, `SELLER`, `MANAGER` — **não** `REPRESENTATIVE` | `SellerControllerTest.listSellers_withCashierRole_returnsJson` |
| Criar vendedora | `USER`, `ADMIN`, `MANAGER` | `SellerControllerTest.createSeller_withCashierRole_returnsForbidden` |
| Listar / criar regras de comissão | `USER`, `ADMIN`, `MANAGER` | `CommissionRuleControllerTest.listRules_withCashierRole_returnsForbidden` |
| Anónimo (sem papel LojApp) | 403 em NFe import | `NfeControllerTest.importNfe_withAnonymousRole_returnsForbidden` |

`USER` no piloto é dono da loja demo: mantém caixa e backoffice.

## Fora desta fatia

- Novos papéis.
- Autorização no frontend **espelha** a API no piloto (abas NFe / Nova venda e formulários), mas a API continua a ser a fonte de verdade.
