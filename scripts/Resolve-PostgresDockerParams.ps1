# Credenciais Postgres por ficheiro Compose (partilhado por backup/restore).
# | ComposeFile              | POSTGRES_USER | POSTGRES_DB |
# |--------------------------|---------------|-------------|
# | docker-compose.yml       | loja_user     | loja_db     |
# | docker-compose.prod.yml  | lojapp        | lojapp      |

function Resolve-PostgresDockerParams {
    param(
        [string] $ComposeFile = "docker-compose.yml",
        [string] $DbUser,
        [string] $DbName,
        [string] $Service = "db"
    )

    $normalized = ($ComposeFile -replace '\\', '/').ToLowerInvariant()
    if ([string]::IsNullOrWhiteSpace($DbUser) -or [string]::IsNullOrWhiteSpace($DbName)) {
        if ($normalized -match 'prod') {
            if ([string]::IsNullOrWhiteSpace($DbUser)) { $DbUser = "lojapp" }
            if ([string]::IsNullOrWhiteSpace($DbName)) { $DbName = "lojapp" }
        } else {
            if ([string]::IsNullOrWhiteSpace($DbUser)) { $DbUser = "loja_user" }
            if ([string]::IsNullOrWhiteSpace($DbName)) { $DbName = "loja_db" }
        }
    }

    [PSCustomObject]@{
        ComposeFile = $ComposeFile
        DbUser      = $DbUser
        DbName      = $DbName
        Service     = $Service
    }
}

function Get-PostgresDockerContainerId {
    param(
        [string] $ComposeFile,
        [string] $Service
    )

    $cid = docker compose -f $ComposeFile ps -q $Service 2>$null
    if (-not $cid) {
        throw @"
Contentor do serviço '$Service' não encontrado ($ComposeFile).
Suba o Postgres: docker compose -f $ComposeFile up -d db
"@
    }
    return $cid.Trim()
}
