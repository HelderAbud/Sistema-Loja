# Restore PostgreSQL a partir de ficheiro pg_dump -Fc (CUIDADO: pode apagar dados com --clean).
# Uso: .\scripts\restore-postgres-docker.ps1 -BackupPath .\backups\lojapp-20260101-120000.dump
param(
    [Parameter(Mandatory = $true)]
    [string] $BackupPath,
    [string] $ComposeFile = "docker-compose.yml",
    [string] $Service = "db"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

if (-not (Test-Path $BackupPath)) {
    throw "Backup não encontrado: $BackupPath"
}
$full = (Resolve-Path $BackupPath).Path

docker compose -f $ComposeFile exec -T $Service sh -c "rm -f /tmp/lojapp.restore.dump"
$cid = docker compose -f $ComposeFile ps -q $Service
if (-not $cid) {
    throw "Contentor do serviço '$Service' não encontrado."
}
docker cp $full "${cid}:/tmp/lojapp.restore.dump"
docker compose -f $ComposeFile exec -T $Service sh -c "pg_restore -U lojapp -d lojapp --clean --if-exists /tmp/lojapp.restore.dump"
Write-Host "Restore concluído a partir de $full"
