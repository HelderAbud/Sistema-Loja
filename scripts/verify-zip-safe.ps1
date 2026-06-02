param(
    [Parameter(Mandatory = $true)]
    [string]$ZipPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $ZipPath)) {
    throw "ZIP nao encontrado: $ZipPath"
}

$forbiddenPatterns = @(
    @{ Label = ".env"; Regex = "(^|/)\.env$" },
    @{ Label = "backup.sql"; Regex = "(^|/)backup\.sql$" },
    @{ Label = "target/"; Regex = "(^|/)target/" },
    @{ Label = "node_modules/"; Regex = "(^|/)node_modules/" },
    @{ Label = ".git/"; Regex = "(^|/)\.git/" },
    @{ Label = "application-local"; Regex = "application-local\.(properties|yml|yaml)$" },
    @{ Label = "id_rsa / .pem"; Regex = "(^|/)(id_rsa|.*\.pem|.*\.ppk)$" }
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $ZipPath))
$hits = @()

$entryCount = $zip.Entries.Count
try {
    foreach ($entry in $zip.Entries) {
        $name = $entry.FullName.Replace("\", "/")
        foreach ($rule in $forbiddenPatterns) {
            if ($name -match $rule.Regex) {
                $hits += [PSCustomObject]@{ Rule = $rule.Label; Path = $name }
            }
        }
    }
} finally {
    $zip.Dispose()
}

if ($hits.Count -gt 0) {
    Write-Host "FALHA: artefactos proibidos dentro do ZIP:" -ForegroundColor Red
    $hits | Format-Table -AutoSize
    exit 1
}

Write-Host "OK: ZIP sem artefactos proibidos ($entryCount entradas verificadas)."
exit 0
