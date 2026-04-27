# Implantacao dos Pilotos (Semanas 9-12)

## Plano de rastreio (Cursor)

Checklist vivo, smoke test da API e tabela das 3 lojas: **`.cursor/plans/piloto-mvp-rastreio.md`**.  
Criterios de aceite do produto: **`01-escopo-mvp.md`** (secao piloto + cobertura tecnica).

## Ordem de implantacao
1. Piloto 1
2. Piloto 2
3. Piloto 3

Sempre implantar de forma sequencial para aprender com cada loja antes da proxima.

## Roteiro por loja
1. Setup de acesso e validacao de login.
2. Cadastro inicial de marcas e produtos base.
3. Importacao assistida das primeiras 3 NFes.
4. Conferencia de estoque apos importacao.
5. Teste de registro de venda e baixa de estoque.
6. Uso do dashboard de marca para decisao de compra.

## Treinamento (60-90 min)
- Bloco 1: NFe e estoque (30 min)
- Bloco 2: cadastro e vendas (20 min)
- Bloco 3: dashboard por marca (20 min)
- Bloco 4: duvidas e checklist final (10-20 min)

## Checklist de feedback semanal
- [ ] Teve erro de importacao de NFe?
- [ ] Estoque ficou divergente?
- [ ] Dashboard ajudou em compra/reposicao?
- [ ] Qual tela consumiu mais tempo da equipe?
- [ ] Qual ajuste de usabilidade e prioritario?

## Critérios de estabilidade do piloto
- 0 bug bloqueante por 7 dias consecutivos
- Importacao de NFe executada sem suporte manual frequente
- Usuario-chave da loja consegue usar dashboard sozinho
