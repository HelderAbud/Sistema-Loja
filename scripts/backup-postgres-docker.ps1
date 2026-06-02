# Backup PostgreSQL do serviço `db` (docker compose na raiz do repositório).
#
# Matriz de credenciais (override com -DbUser / -DbName se necessário):
#   docker-compose.yml      → loja_user / loja_db
#   docker-compose.prod.yml → lojapp / lojapp
#
# Uso:
#   .\scripts\backup-postgres-docker.ps1
#   .\scripts\backup-postgres-docker.ps1 -ComposeFile docker-compose.prod.yml
#   .\scripts\backup-postgres-docker.ps1 -ComposeFile docker-compose.yml -OutDir .\backups
param(
    [string] $ComposeFile = "docker-compose.yml",
    [string] $Service = "db",
    [string] $DbUser,
    [string] $DbName,
    [string] $OutDir = "backups"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

. (Join-Path $PSScriptRoot "Resolve-PostgresDockerParams.ps1")
$params = Resolve-PostgresDockerParams -ComposeFile $ComposeFile -DbUser $DbUser -DbName $DbName -Service $Service

if (-not (Test-Path $params.ComposeFile)) {
    throw "Ficheiro compose não encontrado: $($params.ComposeFile) (cwd: $(Get-Location))"
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$ts = Get-Date -Format "yyyyMMdd-HHmmss"
$dest = Join-Path $OutDir "$($params.DbName)-$ts.dump"
$remoteDump = "/tmp/lojapp-pg-backup.dump"

Write-Host "Compose: $($params.ComposeFile) | user=$($params.DbUser) db=$($params.DbName) service=$($params.Service)"

docker compose -f $params.ComposeFile exec -T $params.Service sh -c @"
pg_dump -U $($params.DbUser) -d $($params.DbName) -Fc -f $remoteDump
"@

$cid = Get-PostgresDockerContainerId -ComposeFile $params.ComposeFile -Service $params.Service
docker cp "${cid}:${remoteDump}" $dest
docker compose -f $params.ComposeFile exec -T $params.Service sh -c "rm -f $remoteDump" | Out-Null

Write-Host "Backup gravado: $dest"
