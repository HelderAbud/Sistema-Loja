# Importa todos os *.xml de uma pasta via POST /api/v1/lojapp/nfe/import (Dia 7 / Passo 7.2).
# Espelha scripts/import-nfe-folder.sh para Windows.
#
# Uso (na raiz do repo):
#   $env:LOJAPP_VERIFY_EMAIL = '...'
#   $env:LOJAPP_VERIFY_PASSWORD = '...'
#   .\scripts\import-nfe-folder.ps1 -Folder .\scripts\fixtures\nfe-lote-sintetico-dia7
#
# Ou com JWT ja obtido:
#   $env:LOJAPP_JWT = 'eyJ...'
#   .\scripts\import-nfe-folder.ps1 -Folder D:\LojApp-pilotos-xmls\piloto-1-alto-volume\grandes-itens
#
# Opcional: -Recurse para incluir subpastas; -LogPath para CSV de resultado.

param(
    [Parameter(Mandatory = $true)]
    [string] $Folder,
    [string] $ApiBase = "http://localhost:8080",
    [switch] $Recurse,
    [string] $LogPath = "",
    [switch] $DryRun
)

$ErrorActionPreference = "Stop"
$api = $ApiBase.TrimEnd("/")

function Get-LojappToken {
    if ($env:LOJAPP_JWT) {
        return $env:LOJAPP_JWT.Trim()
    }
    $email = $env:LOJAPP_VERIFY_EMAIL
    $password = $env:LOJAPP_VERIFY_PASSWORD
    if (-not $email -or -not $password) {
        Write-Error "Defina LOJAPP_JWT ou LOJAPP_VERIFY_EMAIL e LOJAPP_VERIFY_PASSWORD."
    }
    $loginBody = @{ email = $email; password = $password } | ConvertTo-Json
    try {
        $loginRes = Invoke-RestMethod -Method Post -Uri "$api/api/v1/auth/login" `
            -ContentType "application/json; charset=utf-8" -Body $loginBody
    } catch {
        Write-Error "Login falhou: $_"
    }
    if (-not $loginRes.accessToken) {
        Write-Error "Resposta de login sem accessToken."
    }
    return [string] $loginRes.accessToken
}

if (-not (Test-Path -LiteralPath $Folder)) {
    Write-Error "Pasta nao encontrada: $Folder"
}

$gci = @{ LiteralPath = $Folder; Filter = "*.xml"; File = $true; ErrorAction = "SilentlyContinue" }
if ($Recurse) {
    $files = Get-ChildItem @gci -Recurse
} else {
    $files = Get-ChildItem @gci
}
$files = @($files)
if ($files.Count -eq 0) {
    Write-Error "Nenhum .xml em $Folder"
}

Write-Host "API: $api | Ficheiros: $($files.Count) | DryRun: $DryRun"

if ($DryRun) {
    $files | ForEach-Object { Write-Host "  $($_.FullName)" }
    exit 0
}

$token = Get-LojappToken
$logRows = [System.Collections.Generic.List[object]]::new()
$ok = 0
$fail = 0

foreach ($f in $files) {
    Write-Host "---- $($f.Name)"
    $raw = Get-Content -LiteralPath $f.FullName -Raw -Encoding UTF8
    $payload = @{ rawXml = $raw } | ConvertTo-Json -Depth 4
    $tmp = [System.IO.Path]::GetTempFileName()
    try {
        [System.IO.File]::WriteAllText($tmp, $payload, [System.Text.UTF8Encoding]::new($false))
        $outTmp = [System.IO.Path]::GetTempFileName()
        $httpCode = & curl.exe -sS -o $outTmp -w "%{http_code}" -X POST "$api/api/v1/lojapp/nfe/import" `
            -H "Authorization: Bearer $token" `
            -H "Content-Type: application/json; charset=utf-8" `
            --data-binary "@$tmp"
        $curlExit = $LASTEXITCODE
        $body = [System.IO.File]::ReadAllText($outTmp)
        Remove-Item -LiteralPath $outTmp -Force
        if ($curlExit -ne 0) {
            Write-Warning "curl exit $curlExit - $f"
            $fail++
            $logRows.Add([pscustomobject]@{ File = $f.Name; Http = ""; Ok = $false; Body = $body }) | Out-Null
            continue
        }
        if ($httpCode -eq "200") {
            Write-Host "OK HTTP $httpCode - $body"
            $ok++
            $logRows.Add([pscustomobject]@{ File = $f.Name; Http = $httpCode; Ok = $true; Body = $body }) | Out-Null
        } else {
            Write-Warning "FALHA HTTP $httpCode - $body"
            $fail++
            $logRows.Add([pscustomobject]@{ File = $f.Name; Http = $httpCode; Ok = $false; Body = $body }) | Out-Null
        }
    } finally {
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "Resumo: $ok OK, $fail falha(s)."

if ($LogPath) {
    $logRows | Export-Csv -LiteralPath $LogPath -NoTypeInformation -Encoding UTF8
    Write-Host "Log: $LogPath"
}

if ($fail -gt 0) {
    exit 1
}
