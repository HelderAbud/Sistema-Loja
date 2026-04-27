# Remove frontend/node_modules, frontend/dist (e artefactos Java comuns) do índice Git
# sem apagar ficheiros no disco.
#
# Uso (na raiz do repo ou em qualquer pasta — o script resolve a raiz):
#   powershell -ExecutionPolicy Bypass -File scripts/git-untrack-frontend-artifacts.ps1
# Só commit + push se houver algo a retirar do índice:
#   (por omissão faz push após commit)
# Sem enviar para o remoto (rever antes):
#   powershell -ExecutionPolicy Bypass -File scripts/git-untrack-frontend-artifacts.ps1 -NoPush

param(
    [switch]$NoPush
)

$ErrorActionPreference = "Continue"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location -LiteralPath $repoRoot

if (-not (Test-Path -LiteralPath ".git")) {
    Write-Host "ERRO: não há pasta .git aqui." -ForegroundColor Red
    Write-Host "Abra o PowerShell na raiz do clone Git (ex.: pasta do projeto no GitHub Desktop) e volte a executar."
    exit 1
}

Write-Host "A remover do índice (cached), se existirem paths tracked..." -ForegroundColor Cyan
git rm -r --cached frontend/node_modules 2>$null
git rm -r --cached frontend/dist 2>$null
git rm -r --cached target 2>$null
git rm -r --cached build 2>$null

git diff --cached --quiet 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "OK: nada estava versionado nessas pastas (ou já tinha sido retirado). Nenhum commit necessário." -ForegroundColor Green
    exit 0
}

git commit -m "chore: remove tracked build artifacts from git index"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Commit falhou. Verifique: git status" -ForegroundColor Yellow
    exit $LASTEXITCODE
}

if ($NoPush) {
    Write-Host "Commit criado localmente. Executa quando quiseres: git push" -ForegroundColor Yellow
    exit 0
}

Write-Host "A enviar para o remoto..." -ForegroundColor Cyan
git push
exit $LASTEXITCODE
