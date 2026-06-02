# Restore PostgreSQL a partir de ficheiro pg_dump -Fc (CUIDADO: --clean apaga objetos existentes).
#
# Matriz de credenciais: ver cabeçalho em backup-postgres-docker.ps1
#
# Uso:
#   .\scripts\restore-postgres-docker.ps1 -BackupPath .\backups\loja_db-20260602-120000.dump
#   .\scripts\restore-postgres-docker.ps1 -BackupPath .\backups\lojapp-....dump -ComposeFile docker-compose.prod.yml
param(
    [Parameter(Mandatory = $true)]
    [string] $BackupPath,
    [string] $ComposeFile = "docker-compose.yml",
    [string] $Service = "db",
    [string] $DbUser,
    [string] $DbName
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

. (Join-Path $PSScriptRoot "Resolve-PostgresDockerParams.ps1")
$params = Resolve-PostgresDockerParams -ComposeFile $ComposeFile -DbUser $DbUser -DbName $DbName -Service $Service

if (-not (Test-Path $BackupPath)) {
    throw "Backup não encontrado: $BackupPath"
}
$full = (Resolve-Path $BackupPath).Path
$remoteDump = "/tmp/lojapp-pg-restore.dump"

Write-Host "Compose: $($params.ComposeFile) | user=$($params.DbUser) db=$($params.DbName)"
Write-Host "Restore a partir de: $full"

$cid = Get-PostgresDockerContainerId -ComposeFile $params.ComposeFile -Service $params.Service
docker compose -f $params.ComposeFile exec -T $params.Service sh -c "rm -f $remoteDump" | Out-Null
docker cp $full "${cid}:${remoteDump}"

docker compose -f $params.ComposeFile exec -T $params.Service sh -c @"
pg_restore -U $($params.DbUser) -d $($params.DbName) --clean --if-exists $remoteDump
"@

docker compose -f $params.ComposeFile exec -T $params.Service sh -c "rm -f $remoteDump" | Out-Null
Write-Host "Restore concluído em $($params.DbName) ($($params.ComposeFile))"
