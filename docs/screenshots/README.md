# Screenshots (portfolio)

Coloque aqui **capturas reais** da aplicação (PNG ou WebP). Sem imagens, o GitHub não transmite o esforço visual do projeto.

## Ficheiros obrigatórios (alinhados ao README)

| Ficheiro | Conteúdo |
|----------|----------|
| `01-login.png` | Ecrã de login / registo |
| `02-dashboard.png` | Dashboard com KPIs, gráficos e ABC |
| `03-vendas.png` | Separador **Vendas** (histórico ou nova venda) |
| `04-estoque.png` | Separador **Stock** / inventário |
| `05-importacao-xml.png` | Importação de NFe (XML) |
| `06-relatorios.png` | Relatórios: por exemplo tabela de marcas/ABC no dashboard, ou export futuro |

## Como gerar

1. Suba a API (`mvn spring-boot:run` ou Docker) e o frontend (`cd frontend && npm run dev`).
2. Abra `http://localhost:3000`, faça login e navegue por cada separador.
3. Capture a janela (Windows: **Win+Shift+S** ou ferramenta do browser).
4. Guarde com os nomes acima e descomente o bloco de imagens no `README.md` na raiz do repositório.

## Limpar índice Git se já commitou `node_modules` ou `dist`

Na raiz do repositório onde existe `.git`:

```bash
git rm -r --cached frontend/node_modules frontend/dist target build 2>/dev/null || true
git add .gitignore
git commit -m "chore: stop tracking build artifacts and dependencies"
```

PowerShell:

```powershell
git rm -r --cached frontend/node_modules 2>$null
git rm -r --cached frontend/dist 2>$null
git rm -r --cached target 2>$null
git rm -r --cached build 2>$null
git add .gitignore
git commit -m "chore: stop tracking build artifacts and dependencies"
```
