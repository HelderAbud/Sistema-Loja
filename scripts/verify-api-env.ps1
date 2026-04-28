# Verifica login + JWT + GET marcas/produtos (Passo 1 / Passo 2 sem Swagger).
#
# Uso:
#   $env:LOJAPP_VERIFY_EMAIL = 'piloto1+...@loja-exemplo.com'
#   $env:LOJAPP_VERIFY_PASSWORD = '...'
#   $env:API_BASE = 'http://localhost:8080'   # opcional
#   .\scripts\verify-api-env.ps1

$ErrorActionPreference = "Stop"

$base = $env:API_BASE
if (-not $base) { $base = "http://localhost:8080" }
$base = $base.TrimEnd("/")

$email = $env:LOJAPP_VERIFY_EMAIL
$password = $env:LOJAPP_VERIFY_PASSWORD
if (-not $email -or -not $password) {
  Write-Error "Defina LOJAPP_VERIFY_EMAIL e LOJAPP_VERIFY_PASSWORD (e opcionalmente API_BASE)."
}

$loginBody = @{ email = $email; password = $password } | ConvertTo-Json
try {
  $loginRes = Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/login" `
    -ContentType "application/json" -Body $loginBody
} catch {
  Write-Error "Login falhou: $_"
}

$token = $loginRes.accessToken
if (-not $token) {
  Write-Error "Resposta de login sem accessToken."
}

Write-Host "Login OK (JWT obtido)."

$headers = @{ Authorization = "Bearer $token"; Accept = "application/json" }

try {
  $brands = Invoke-RestMethod -Method Get -Uri "$base/api/v1/lojapp/brands" -Headers $headers
} catch {
  Write-Error "GET brands falhou: $_"
}
$nBrands = @($brands).Count
Write-Host "GET brands OK — $nBrands marca(s)."

try {
  $productsUriBuilder = [System.UriBuilder]::new("$base/api/v1/lojapp/products")
  $productsQuery = [System.Web.HttpUtility]::ParseQueryString([string]::Empty)
  $productsQuery["page"] = "0"
  $productsQuery["size"] = "20"
  $productsQuery["sort"] = "name,asc"
  $productsUriBuilder.Query = $productsQuery.ToString()
  $products = Invoke-RestMethod -Method Get -Uri $productsUriBuilder.Uri.AbsoluteUri -Headers $headers
} catch {
  Write-Error "GET products falhou: $_"
}
$total = $products.totalElements
Write-Host "GET products OK — totalElements=$total (pagina 0)."

Write-Host "Verificacao concluida: API em $base responde com esta conta."
