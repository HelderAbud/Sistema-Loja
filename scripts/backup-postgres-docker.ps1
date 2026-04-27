# Backup PostgreSQL do serviço `db` (docker compose na raiz do repositório).
# Uso: .\scripts\backup-postgres-docker.ps1 [-ComposeFile docker-compose.yml] [-OutDir .\backups]
param(
    [string] $ComposeFile = "docker-compose.yml",
    [string] $Service = "db",
    [string] $OutDir = "backups"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

if (-not (Test-Path $ComposeFile)) {
    throw "Ficheiro compose não encontrado: $ComposeFile (cwd: $(Get-Location))"
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$ts = Get-Date -Format "yyyyMMdd-HHmmss"
$dest = Join-Path $OutDir "lojapp-$ts.dump"

docker compose -f $ComposeFile exec -T $Service sh -c "pg_dump -U lojapp -d lojapp -Fc -f /tmp/lojapp.dump"
$cid = docker compose -f $ComposeFile ps -q $Service
if (-not $cid) {
    throw "Contentor do serviço '$Service' não encontrado. Compose a correr?"
}
docker cp "${cid}:/tmp/lojapp.dump" $dest
Write-Host "Backup gravado: $dest"
