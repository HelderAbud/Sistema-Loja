# Seed de vendas para a conta piloto@lojapp.demo (APENAS desenvolvimento local / demo).
# Usa POST /api/v1/lojapp/sales (linha a linha) com Idempotency-Key.
# Requer: API a correr; Postgres com produtos da conta piloto. Password abaixo e credencial de demo — nunca usar em producao.

$ErrorActionPreference = 'Stop'
$BASE = 'http://localhost:8080'
$EMAIL = 'piloto@lojapp.demo'
$PASSWORD = '87654321'
$TARGET_SALES = 30
$RNG = [System.Random]::new(20260211)

function Get-InventoryRows {
    $sql = @"
SELECT p.id::text, p.sale_price::text, COALESCE(ib.quantity, 0)::text
FROM products p
LEFT JOIN inventory_balances ib
  ON ib.product_id = p.id AND ib.user_id = p.user_id
WHERE p.user_id = (SELECT id FROM users WHERE email = '$EMAIL')
ORDER BY p.id;
"@
    $raw = & wsl -- docker exec loja-postgres psql -U loja_user -d loja_db -t -A -F',' -c $sql
    if ($LASTEXITCODE -ne 0) { throw "psql falhou (exit $LASTEXITCODE)" }
    $rows = @()
    foreach ($line in $raw) {
        $t = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($t)) { continue }
        $parts = $t -split ',', 3
        if ($parts.Count -lt 3) { continue }
        $qty = [decimal]$parts[2]
        if ($qty -lt [decimal]0.001) { continue }
        $rows += [pscustomobject]@{
            Id        = [long]$parts[0]
            UnitPrice = [decimal]$parts[1]
            Qty       = $qty
        }
    }
    return $rows
}

$loginBody = @{ email = $EMAIL; password = $PASSWORD } | ConvertTo-Json -Compress
$auth = Invoke-RestMethod -Uri "$BASE/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($auth.accessToken)" }

Write-Host "[OK] Login $EMAIL"

$pool = [System.Collections.Generic.List[object]]::new()
foreach ($r in (Get-InventoryRows)) { [void]$pool.Add($r) }

if ($pool.Count -eq 0) {
    throw "Nenhum produto com stock > 0 para $EMAIL. Corra antes o seed de produtos/stock."
}

Write-Host "[INFO] Produtos com stock: $($pool.Count)"

$ok = 0
$fail = 0

for ($i = 1; $i -le $TARGET_SALES; $i++) {
    $candidates = $pool | Where-Object { $_.Qty -ge [decimal]0.001 }
    if ($candidates.Count -eq 0) {
        Write-Host "[STOP] Stock esgotado após $ok vendas."
        break
    }
    $pick = $candidates[$RNG.Next(0, $candidates.Count)]
    $cap = [math]::Min(4, [int][math]::Floor([double]$pick.Qty))
    if ($cap -lt 1) { continue }
    $qtySell = [decimal]$RNG.Next(1, $cap + 1)
    if ($qtySell -gt $pick.Qty) { $qtySell = [decimal]$pick.Qty }

    $bodyObj = [ordered]@{
        productId = $pick.Id
        quantity  = $qtySell
        unitPrice = $pick.UnitPrice
    }
    $body = ($bodyObj | ConvertTo-Json -Compress)
    $idemp = "seed-venda-$($i.ToString('000'))"

    try {
        $null = Invoke-RestMethod -Uri "$BASE/api/v1/lojapp/sales" -Method Post -Headers ($headers + @{ 'Idempotency-Key' = $idemp }) -ContentType 'application/json' -Body $body
        $pick.Qty -= $qtySell
        $ok++
    } catch {
        $fail++
        $code = $null
        if ($_.Exception.Response) { $code = $_.Exception.Response.StatusCode.value__ }
        Write-Host "[WARN] Venda $i falhou HTTP $code - $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "===== RESUMO VENDAS ====="
Write-Host "Vendas registadas (HTTP 200): $ok"
Write-Host "Falhas: $fail"

$countSql = "SELECT COUNT(*)::text FROM sales WHERE user_id = (SELECT id FROM users WHERE email = '$EMAIL');"
$totalSales = (& wsl -- docker exec loja-postgres psql -U loja_user -d loja_db -t -A -c $countSql).Trim()
Write-Host ('Total de linhas na tabela sales: ' + $totalSales)
