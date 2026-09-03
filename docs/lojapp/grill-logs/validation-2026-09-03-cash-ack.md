# Grill — confirmação de diferença no caixa (2026-09-03)

## Decisão

MVP: uma conta = uma loja. `managerApproval` é auto-declaração, não aprovação de terceiro.

## Feito

- Glossário em `CONTEXT.md`; escopo, matriz de papéis e mapeamento do assistente.
- Mensagem 403, `@Schema` e labels na Seller area. JSON inalterado.
- Teste da mensagem + assert no `CashSessionControllerTest`.

## Fora

- Multi-funcionário; `@Version` em `CashSession`; rename do campo JSON.

## Verificação

- `./mvnw -Pci-unit-tests -Dtest=CashSessionControllerTest,CashSessionManagerApprovalRequiredExceptionTest test`
