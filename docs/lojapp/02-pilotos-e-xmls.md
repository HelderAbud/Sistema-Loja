# Pilotos e Coleta de XMLs

## Lojas piloto
Definir 3 lojas com perfis diferentes:
- Piloto 1: loja com volume alto de NFe
- Piloto 2: loja com mix forte de marcas
- Piloto 3: loja com operacao menor (time reduzido)

## Dados minimos por piloto
- Nome da loja
- Responsavel operacional
- Contato (telefone e WhatsApp)
- Quantidade media de notas por semana
- Quantidade aproximada de SKUs ativos

## Parser no backend (`NfeXmlParser`)

- O serviço usa XML **namespace-aware** e `getElementsByTagNameNS("*", …)` para aceitar prefixos (`nfe:`) e o layout oficial (default namespace `http://www.portalfiscal.inf.br/nfe`).
- O fornecedor é lido preferindo **`emit/xNome`**; em XMLs muito atípicos pode cair no primeiro `xNome` do documento.
- **Assinatura digital**, `Signature` e variantes de estrutura ainda devem ser validadas com **XMLs reais** das 3 lojas (esta checklist).
- Testes unitários cobrem XML simplificado e um exemplo com namespace; não substituem os ficheiros reais dos pilotos.

## Checklist de coleta de XML (10 arquivos)
- [ ] 4 XMLs com muitos itens (20+)
- [ ] 3 XMLs com poucos itens
- [ ] 2 XMLs com marcas diferentes no mesmo arquivo
- [ ] 1 XML com campos faltantes para teste de fallback

## Validacao tecnica por XML
Para cada arquivo:
1. Confirmar leitura de numero da nota e fornecedor.
2. Confirmar leitura dos itens e quantidade.
3. Confirmar leitura de custo unitario.
4. Confirmar criacao/atualizacao de produto.
5. Confirmar movimentacao de estoque de entrada.

## Registo Passo 7.2 (XMLs reais — preencher à medida que recolhes)

**Não commits** com XML contendo dados pessoais ou chaves reais sem anonimização. Guarda cópias de trabalho fora do Git ou num ramo privado com `.gitignore`.

| # | Ficheiro / descrição | Loja piloto | Parser (`NfeXmlParserTest` / leitura manual) | `POST .../nfe/import` (API) | Stock / produtos OK? | Notas |
|---|----------------------|-------------|---------------------------------------------|----------------------------|----------------------|-------|
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |

**Roteiro rápido:** ver `docs/lojapp/10-guia-junior-piloto-deploy-proximos-passos.md` → **7.2 Passo 1** («Como validar»).

## Metricas da fase piloto
- Tempo de lancamento de nota antes/depois
- Divergencia de estoque semanal
- Uso do dashboard por marca (acessos semanais)
- Decisoes de compra baseadas em marca

## Cadencia operacional
- Reuniao semanal de 20 min por piloto
- Registro de feedback em checklist padrao
- Priorizar correcao de bloqueadores de operacao
