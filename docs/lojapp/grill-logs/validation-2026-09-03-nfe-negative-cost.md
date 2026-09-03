# Grill — vUnCom negativo na NFe (2026-09-03)

## Achado

`quantity` rejeita <= 0 em `NfeStockReceiptLine`; `unitCost` ia direto para `costPrice`/`salePrice`. Zero (brinde) é válido.

## Feito

Compact constructor em `ParsedNfeItem`. `parse` re-lança `NfeXmlUnreadableException` para o `catch (Exception)` não mascarar a mensagem.

## Verificação

`./mvnw -Pci-unit-tests -Dtest=NfeXmlParserTest test` — GREEN.
