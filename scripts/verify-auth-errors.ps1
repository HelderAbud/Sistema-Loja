# Verifica 401 JSON em recurso protegido sem token e com Bearer invalido (Dia 8).
#
# Uso:
#   .\scripts\verify-auth-errors.ps1
#   .\scripts\verify-auth-errors.ps1 -ApiBase "http://127.0.0.1:8080"

param(
    [string] $ApiBase = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$api = $ApiBase.TrimEnd("/")
$url = "$api/api/v1/lojapp/products?page=0&size=1"

if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
    Write-Error "curl.exe nao encontrado."
}

function Invoke-Expect401 {
    param(
        [string] $Label,
        [string[]] $ExtraCurlArgs
    )
    $out = [System.IO.Path]::GetTempFileName()
    try {
        $allArgs = @("-sS", "-o", $out, "-w", "%{http_code}") + $ExtraCurlArgs + @($url)
        $code = & curl.exe @allArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Error "curl falhou ($Label) exit=$LASTEXITCODE"
        }
        $body = [System.IO.File]::ReadAllText($out)
        Write-Host "[$Label] HTTP $code"
        Write-Host $body
        if ($code -ne "401") {
            Write-Error "Esperado HTTP 401, obtido $code ($Label)"
        }
        if ($body -notmatch "UNAUTHORIZED") {
            Write-Warning "Corpo nao contem UNAUTHORIZED - verificar ($Label)."
        }
    } finally {
        Remove-Item -LiteralPath $out -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "=== Sem Authorization ===" -ForegroundColor Cyan
Invoke-Expect401 -Label "no-auth" -ExtraCurlArgs @()

Write-Host ""
Write-Host "=== Bearer invalido ===" -ForegroundColor Cyan
Invoke-Expect401 -Label "bad-bearer" -ExtraCurlArgs @("-H", "Authorization: Bearer not-a-valid-jwt")

Write-Host ""
Write-Host "OK: respostas 401 coerentes com SecurityConfig + JwtAuthFilter." -ForegroundColor Green
