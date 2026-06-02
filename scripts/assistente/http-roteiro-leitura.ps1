# Requer: LOJAPP_BASE_URL e LOJAPP_ACCESS_TOKEN na sessão (ver env.example).
# Apenas GET — seguro para smoke / assistente em modo leitura.
# Uso: . .\scripts\assistente\http-roteiro-leitura.ps1

$ErrorActionPreference = "Stop"
$base = $env:LOJAPP_BASE_URL
if (-not $base) { throw "Defina LOJAPP_BASE_URL" }
$token = $env:LOJAPP_ACCESS_TOKEN
if (-not $token) { throw "Defina LOJAPP_ACCESS_TOKEN (sem commitar)" }

$h = @{ Authorization = "Bearer $token" }

Write-Host "GET /products ..."
Invoke-RestMethod -Uri "$base/api/v1/lojapp/products?page=0&size=5" -Headers $h | ConvertTo-Json -Depth 5

Write-Host "GET /inventory/low-stock ..."
Invoke-RestMethod -Uri "$base/api/v1/lojapp/inventory/low-stock" -Headers $h | ConvertTo-Json -Depth 5

Write-Host "GET /dashboard/inventory-kpis ..."
Invoke-RestMethod -Uri "$base/api/v1/lojapp/dashboard/inventory-kpis" -Headers $h | ConvertTo-Json -Depth 5

$productId = $env:LOJAPP_PRODUCT_ID
if ($productId) {
    Write-Host "GET /inventory/products/$productId/stock ..."
    Invoke-RestMethod -Uri "$base/api/v1/lojapp/inventory/products/$productId/stock" -Headers $h | ConvertTo-Json
}
