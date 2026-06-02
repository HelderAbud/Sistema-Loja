# Sobe Postgres + Redis para desenvolvimento local (docker-compose.yml) e opcionalmente a API com Maven.
#
# Requisitos: Docker Desktop (CLI no PATH ou em Program Files).
# O ficheiro .env na raiz do repo alimenta POSTGRES_PASSWORD etc. no compose.
#
# Uso (na raiz do repo):
#   .\scripts\dev-up.ps1              # so db + redis
#   .\scripts\dev-up.ps1 -StartApi    # db + redis + mvn spring-boot:run (primeiro plano)
#
param(
    [switch] $StartApi
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot

function Get-DockerExe {
    $cmd = Get-Command docker -ErrorAction SilentlyContinue
    if ($cmd -and $cmd.Source -and (Test-Path -LiteralPath $cmd.Source)) {
        return $cmd.Source
    }
    $candidates = @(
        "${env:ProgramFiles}\Docker\Docker\resources\bin\docker.exe",
        "${env:ProgramFiles(x86)}\Docker\Docker\resources\bin\docker.exe",
        "$env:LOCALAPPDATA\Programs\Docker\Docker\resources\bin\docker.exe"
    )
    foreach ($p in $candidates) {
        if ($p -and (Test-Path -LiteralPath $p)) {
            return $p
        }
    }
    return $null
}

$docker = Get-DockerExe
if (-not $docker) {
    Write-Error @"
docker.exe nao encontrado. Instale o Docker Desktop e:
  - Abra o Docker Desktop pelo menos uma vez
  - Ative 'Add CLI tools to PATH' nas definicoes, OU
  - Adicione manualmente ao PATH: C:\Program Files\Docker\Docker\resources\bin
"@
}

Write-Host "Docker: $docker" -ForegroundColor DarkGray
& $docker compose version | Out-Host
& $docker compose up -d db redis
if ($LASTEXITCODE -ne 0) {
    Write-Error "docker compose up falhou."
}

Write-Host "A aguardar Postgres em 127.0.0.1:5432 ..." -ForegroundColor Cyan
$deadline = (Get-Date).AddMinutes(3)
$ok = $false
while ((Get-Date) -lt $deadline) {
    $t = Test-NetConnection -ComputerName 127.0.0.1 -Port 5432 -WarningAction SilentlyContinue
    if ($t.TcpTestSucceeded) {
        $ok = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $ok) {
    Write-Error "Timeout: porta 5432 nao abriu. Verifique Docker Desktop e docker compose ps."
}

Write-Host "Postgres acessivel na porta 5432." -ForegroundColor Green

if (-not $StartApi) {
    Write-Host ""
    Write-Host "Proximo passo (carregar .env e subir API):" -ForegroundColor Yellow
    Write-Host '  Get-Content .env | ForEach-Object { if ($_ -match ''^\s*([^#][^=]*?)=(.*)$'') { Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim() } }'
    Write-Host '  mvn -q -DskipTests spring-boot:run'
    Write-Host ""
    Write-Host "Health:" -ForegroundColor Yellow
    Write-Host '  .\scripts\verify-deploy-health.ps1'
    exit 0
}

if (-not (Test-Path -LiteralPath (Join-Path $repoRoot ".env"))) {
    Write-Error "Ficheiro .env nao encontrado na raiz do repo."
}
Get-Content -LiteralPath (Join-Path $repoRoot ".env") | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*?)=(.*)$') {
        Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim()
    }
}

Write-Host "A iniciar API (Maven) ..." -ForegroundColor Cyan
mvn -q -DskipTests spring-boot:run
