param(
    [string]$OutputZip = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$repoName = Split-Path $repoRoot -Leaf
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

if ([string]::IsNullOrWhiteSpace($OutputZip)) {
    $OutputZip = Join-Path $repoRoot "$repoName-source-safe-$timestamp.zip"
}

$outputZipResolved = [System.IO.Path]::GetFullPath($OutputZip)

$excludes = @(
    ".git",
    ".env",
    "backup.sql",
    "target",
    "frontend/node_modules",
    "frontend/dist",
    "frontend/playwright-report",
    "frontend/test-results",
    "frontend/blob-report"
)

$stagingDir = Join-Path ([System.IO.Path]::GetTempPath()) "$repoName-safezip-$timestamp"
if (Test-Path $stagingDir) {
    Remove-Item -Recurse -Force $stagingDir
}
New-Item -ItemType Directory -Path $stagingDir | Out-Null

function Should-Exclude([string]$relativePath) {
    $normalized = $relativePath.Replace("\", "/")
    foreach ($pattern in $excludes) {
        $p = $pattern.Replace("\", "/").Trim("/")
        if ($normalized -eq $p -or $normalized.StartsWith("$p/")) {
            return $true
        }
    }
    return $false
}

Write-Host "Preparando pacote seguro em: $outputZipResolved"
Write-Host "Repositorio: $repoRoot"

$allEntries = Get-ChildItem -Path $repoRoot -Recurse -Force
foreach ($entry in $allEntries) {
    $full = $entry.FullName
    $relative = $full.Substring($repoRoot.Path.Length).TrimStart("\")
    if ([string]::IsNullOrWhiteSpace($relative)) { continue }

    if (Should-Exclude $relative) { continue }

    $destination = Join-Path $stagingDir $relative
    if ($entry.PSIsContainer) {
        if (-not (Test-Path $destination)) {
            New-Item -ItemType Directory -Path $destination | Out-Null
        }
    } else {
        $parent = Split-Path $destination -Parent
        if (-not (Test-Path $parent)) {
            New-Item -ItemType Directory -Path $parent | Out-Null
        }
        Copy-Item -Path $full -Destination $destination -Force
    }
}

if (Test-Path $outputZipResolved) {
    Remove-Item -Force $outputZipResolved
}

Compress-Archive -Path (Join-Path $stagingDir "*") -DestinationPath $outputZipResolved -CompressionLevel Optimal

Remove-Item -Recurse -Force $stagingDir

Write-Host "ZIP criado com sucesso: $outputZipResolved"
Write-Host "Exclusoes aplicadas: $($excludes -join ", ")"
