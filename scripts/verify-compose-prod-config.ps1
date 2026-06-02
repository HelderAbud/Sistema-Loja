# Valida sintaxe do docker-compose.prod.yml sem subir servicos (secrets dummy).
# Uso (na raiz do repositorio):
#   .\scripts\verify-compose-prod-config.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Warning "Docker nao encontrado no PATH - ignorada validacao de compose (instale Docker ou use CI)."
    exit 0
}

$prevPostgres = $env:POSTGRES_PASSWORD
$prevJwt = $env:LOJAPP_JWT_SECRET
try {
    $env:POSTGRES_PASSWORD = "verify-compose-config-dummy-pg-123456"
    $env:LOJAPP_JWT_SECRET = "verify-compose-config-dummy-jwt-32chars-min!!"
    docker compose -f docker-compose.prod.yml config --quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Error "docker compose config falhou (exit $LASTEXITCODE)."
    }
    Write-Host "OK: docker-compose.prod.yml valido (expansao de variaveis)." -ForegroundColor Green
} finally {
    if ($null -ne $prevPostgres) { $env:POSTGRES_PASSWORD = $prevPostgres } else { Remove-Item Env:POSTGRES_PASSWORD -ErrorAction SilentlyContinue }
    if ($null -ne $prevJwt) { $env:LOJAPP_JWT_SECRET = $prevJwt } else { Remove-Item Env:LOJAPP_JWT_SECRET -ErrorAction SilentlyContinue }
}
