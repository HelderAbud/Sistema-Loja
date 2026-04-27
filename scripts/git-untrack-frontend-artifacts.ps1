# Remove frontend/node_modules, frontend/dist (e artefactos Java comuns) do índice Git
# sem apagar ficheiros no disco.
#
# Uso:  powershell -ExecutionPolicy Bypass -File scripts/git-untrack-frontend-artifacts.ps1
# Pode executar a partir de qualquer pasta; o script muda para a raiz do repositório (pai de /scripts).

$ErrorActionPreference = "Continue"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location -LiteralPath $repoRoot

if (-not (Test-Path -LiteralPath ".git")) {
    Write-Host "ERRO: não há pasta .git aqui." -ForegroundColor Red
    Write-Host "Abra o PowerShell na raiz do clone Git (ex.: pasta do projeto no GitHub Desktop) e volte a executar."
    exit 1
}

Write-Host "A remover do índice (cached)..." -ForegroundColor Cyan
git rm -r --cached frontend/node_modules 2>$null
git rm -r --cached frontend/dist 2>$null
git rm -r --cached target 2>$null
git rm -r --cached build 2>$null

git add .gitignore 2>$null
git commit -m "remove arquivos desnecessários"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Commit falhou (nada a commitar ou erro). Verifique: git status" -ForegroundColor Yellow
    exit $LASTEXITCODE
}

Write-Host "A enviar para o remoto..." -ForegroundColor Cyan
git push
exit $LASTEXITCODE
