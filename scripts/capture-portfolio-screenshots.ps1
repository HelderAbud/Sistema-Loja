# A1 — Gera PNG 01–06 em docs/screenshots/ via Playwright (API + frontend reais).
# Pré-requisitos:
#   1. docker compose up -d  (Postgres)
#   2. API em http://localhost:8081  (mvn spring-boot:run ou container loja-api)
#   3. Frontend: cd frontend && npm run dev  (porta 5173)
#   4. Conta demo registada com produtos/vendas (ex.: piloto@lojapp.demo)
#
# Uso:
#   $env:LOJAPP_SCREENSHOT_PASSWORD = 'sua-password-demo'
#   .\scripts\capture-portfolio-screenshots.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

if ([string]::IsNullOrWhiteSpace($env:LOJAPP_SCREENSHOT_PASSWORD)) {
    throw 'Defina LOJAPP_SCREENSHOT_PASSWORD (password da conta demo). Nao commitar.'
}

$base = if ($env:LOJAPP_BASE_URL) { $env:LOJAPP_BASE_URL.TrimEnd('/') } else { 'http://localhost:8081' }
try {
    $null = Invoke-WebRequest -Uri "$base/actuator/health" -UseBasicParsing -TimeoutSec 5
} catch {
    throw "API nao responde em $base/actuator/health — suba a API antes da captura."
}

# Frontend: Playwright sobe `npm run dev` se :5173 nao estiver acessivel (reuseExistingServer).
try {
    $null = Invoke-WebRequest -Uri 'http://127.0.0.1:5173/' -UseBasicParsing -TimeoutSec 3
    Write-Host 'Frontend ja a correr em :5173'
} catch {
    Write-Host 'Frontend nao detetado — Playwright vai iniciar npm run dev...'
}

$env:LOJAPP_CAPTURE_SCREENSHOTS = '1'
if (-not $env:LOJAPP_SCREENSHOT_EMAIL) { $env:LOJAPP_SCREENSHOT_EMAIL = 'piloto@lojapp.demo' }

Push-Location frontend
try {
    npx playwright test --config=playwright.capture.config.ts
} finally {
    Pop-Location
}

$out = Join-Path $root 'docs\screenshots'
Get-ChildItem $out -Filter '*.png' | ForEach-Object { Write-Host "OK: $($_.Name)" }
Write-Host ''
Write-Host 'GIF (manual): grave 10-20s com ScreenToGif/ShareX -> docs/screenshots/07-fluxo-principal.gif'
Write-Host 'Depois: descomente imagens no README.md e marque CHECKLIST_FINAL Passo 4.'
