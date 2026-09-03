# Grill — CSV formula injection no export de comissões (2026-09-03)

## Achado

`csvCell` escapava CSV clássico mas não `= + - @ tab CR`. Nomes de vendedora/marca são input do dono da loja; o risco é quem abre o CSV no Excel.

## Feito

Prefixo `'` antes do escape de aspas/vírgula. Testes em `CommissionReportServiceTest`. Fila round-robin sem lock: **não** nesta fatia.

## Verificação

`./mvnw -Pci-unit-tests -Dtest=CommissionReportServiceTest test` (Windows) — GREEN.
