# Diagnostico rapido do repo + ambiente (retomar trabalho apos pausa).
#
# Uso (na raiz do repositorio):
#   .\scripts\destravar-estado.ps1
#   .\scripts\destravar-estado.ps1 -DockerUp -Maven
#   .\scripts\destravar-estado.ps1 -ApiBase "http://127.0.0.1:8080" -FullApi
#
# FullApi requer variaveis LOJAPP_VERIFY_EMAIL e LOJAPP_VERIFY_PASSWORD (ver verify-api-env.ps1).

param(
    [string] $ApiBase = "http://localhost:8080",
    [switch] $DockerUp,
    [switch] $Maven,
    [switch] $SkipGit,
    [switch] $FullApi,
    [switch] $PlanHints,
    [switch] $FrontendDeps
)

$ErrorActionPreference = "Continue"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot

function Write-Step($msg) {
    Write-Host ""
    Write-Host "=== $msg ===" -ForegroundColor Cyan
}

Write-Host "LojApp - destravar-estado (repo: $repoRoot)"

if (-not $SkipGit) {
    Write-Step "Git"
    try {
        git rev-parse --is-inside-work-tree 2>$null | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "nao e um repo git" }
        git branch --show-current
        git status -sb
        Write-Host "--- ultimos 5 commits ---"
        git --no-pager log -5 --oneline
    } catch {
        Write-Warning "Git: $_"
    }
}

if ($DockerUp) {
    Write-Step 'Docker (compose up -d)'
    docker compose up -d
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "docker compose falhou (ver mensagem acima)."
    }
}

if ($Maven) {
    Write-Step 'Maven (package, sem testes)'
    mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "mvn package falhou."
    }
}

if ($FrontendDeps) {
    Write-Step "Frontend (npm install)"
    $fe = Join-Path $repoRoot "frontend"
    if (-not (Test-Path -LiteralPath $fe)) {
        Write-Warning "Pasta frontend nao encontrada."
    } else {
        Push-Location $fe
        try {
            npm install
        } finally {
            Pop-Location
        }
    }
}

Write-Step "Health da API ($ApiBase)"
$healthUrl = ($ApiBase.TrimEnd("/")) + "/actuator/health"
$curl = Get-Command curl.exe -ErrorAction SilentlyContinue
if (-not $curl) {
    Write-Warning "curl.exe nao encontrado; instale ou use health manualmente: $healthUrl"
} else {
    $tmp = [System.IO.Path]::GetTempFileName()
    try {
        $httpStatus = & curl.exe -sS -o $tmp -w "%{http_code}" $healthUrl
        $code = $LASTEXITCODE
        if ($code -ne 0) {
            Write-Warning "curl falhou (exit $code). A API esta a correr em $ApiBase ?"
        } else {
            $body = Get-Content -LiteralPath $tmp -Raw -ErrorAction SilentlyContinue
            Write-Host "HTTP: $httpStatus"
            Write-Host $body
            if ($httpStatus -ne "200") {
                Write-Warning "Health nao retornou 200 - suba a API (mvn spring-boot:run) ou verifique a porta."
            }
        }
    } finally {
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
    }
}

if ($PlanHints) {
    Write-Step "Proximos itens abertos (plano 14 dias)"
    $plan = Join-Path $repoRoot ".cursor\plans\plano-execucao-14-dias-demo-portfolio.md"
    if (-not (Test-Path -LiteralPath $plan)) {
        Write-Warning "Ficheiro de plano nao encontrado: $plan"
    } else {
        Get-Content -LiteralPath $plan |
            Where-Object { $_ -match '^\s*-\s+\[\s*\]\s+' } |
            Select-Object -First 20 |
            ForEach-Object { Write-Host $_ }
    }
}

if ($FullApi) {
    Write-Step "Verificacao API com credenciais (verify-api-env.ps1)"
    if (-not $env:LOJAPP_VERIFY_EMAIL -or -not $env:LOJAPP_VERIFY_PASSWORD) {
        Write-Warning "Defina LOJAPP_VERIFY_EMAIL e LOJAPP_VERIFY_PASSWORD para usar -FullApi."
    } else {
        $env:API_BASE = $ApiBase.TrimEnd("/")
        & (Join-Path $PSScriptRoot "verify-api-env.ps1")
    }
}

Write-Host ""
Write-Host "Concluido. Se o health falhou: docker compose up -d ; mvn spring-boot:run" -ForegroundColor DarkGray
Write-Host "Opcional: .\scripts\destravar-estado.ps1 -PlanHints -DockerUp -Maven -FrontendDeps" -ForegroundColor DarkGray
