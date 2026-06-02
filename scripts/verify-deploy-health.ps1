# Valida GET /actuator/health, /readiness e /liveness (Dia 9 / go-live).
#
# Uso:
#   .\scripts\verify-deploy-health.ps1
#   .\scripts\verify-deploy-health.ps1 -ApiBase "https://api.exemplo.com"

param(
    [string] $ApiBase = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$base = $ApiBase.TrimEnd("/")

if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
    Write-Error "curl.exe nao encontrado."
}

function Test-HealthPath {
    param(
        [string] $PathSuffix,
        [string] $Label
    )
    $url = "$base/actuator/health$PathSuffix"
    $tmp = [System.IO.Path]::GetTempFileName()
    try {
        $code = & curl.exe -sS -o $tmp -w "%{http_code}" $url
        if ($LASTEXITCODE -ne 0) {
            Write-Error "curl falhou ($Label): exit $LASTEXITCODE"
        }
        $body = [System.IO.File]::ReadAllText($tmp)
        Write-Host "[$Label] HTTP $code" -ForegroundColor Cyan
        Write-Host $body
        if ($code -ne "200") {
            Write-Error "Esperado HTTP 200 para $url"
        }
        if ($body -notmatch '"status"\s*:\s*"UP"') {
            Write-Error "Resposta sem status UP ($Label): $url"
        }
    } finally {
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "API: $base" -ForegroundColor DarkGray
Test-HealthPath -PathSuffix "" -Label "health"
Test-HealthPath -PathSuffix "/readiness" -Label "readiness"
Test-HealthPath -PathSuffix "/liveness" -Label "liveness"

Write-Host ""
Write-Host "OK: health / readiness / liveness com status UP." -ForegroundColor Green
