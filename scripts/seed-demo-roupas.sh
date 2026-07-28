#!/usr/bin/env bash
# Seed local: marcas Ogochi/Hering, produtos masculinos e 2 vendas (conta piloto).
# Uso: LOJAPP_SEED_PASSWORD='...' ./scripts/seed-demo-roupas.sh
set -euo pipefail

BASE="${LOJAPP_BASE_URL:-http://localhost:8081}"
EMAIL="${LOJAPP_SEED_EMAIL:-piloto@lojapp.demo}"
PASS="${LOJAPP_SEED_PASSWORD:?Defina LOJAPP_SEED_PASSWORD}"

api() {
  local method="$1" path="$2" body="${3:-}"
  if [[ -n "$body" ]]; then
    curl -sf -X "$method" "$BASE$path" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "$body"
  else
    curl -sf -X "$method" "$BASE$path" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json"
  fi
}

json_field() {
  python3 -c "import sys,json; d=json.load(sys.stdin); print(d$1)"
}

echo ">> Login $EMAIL"
TOKEN=$(curl -sf -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | json_field "['accessToken']")

find_or_create_brand() {
  local name="$1"
  local existing
  existing=$(api GET "/api/v1/lojapp/brands" | python3 -c "
import sys,json
name=sys.argv[1]
for b in json.load(sys.stdin):
    if b.get('name','').lower()==name.lower():
        print(b['id']); break
" "$name" || true)
  if [[ -n "$existing" ]]; then
    echo "$existing"
    return
  fi
  api POST "/api/v1/lojapp/brands" "{\"name\":\"$name\"}" | json_field "['id']"
}

find_or_create_product() {
  local sku="$1" payload="$2"
  local existing
  existing=$(api GET "/api/v1/lojapp/products?size=200" | python3 -c "
import sys,json
sku=sys.argv[1]
data=json.load(sys.stdin)
for p in data.get('content',[]):
    if (p.get('sku') or '').upper()==sku.upper():
        print(p['id']); break
" "$sku" || true)
  if [[ -n "$existing" ]]; then
    echo "$existing"
    return
  fi
  api POST "/api/v1/lojapp/products" "$payload" | json_field "['id']"
}

ensure_stock() {
  local pid="$1" qty="$2"
  local stock need delta
  stock=$(api GET "/api/v1/lojapp/inventory/products/$pid/stock" | json_field "['quantity']" 2>/dev/null || echo "0")
  need=$(python3 -c "import decimal; s=decimal.Decimal('$stock'); t=decimal.Decimal('$qty'); print(1 if s < t else 0)")
  if [[ "$need" == "1" ]]; then
    delta=$(python3 -c "import decimal; print(decimal.Decimal('$qty') - decimal.Decimal('$stock'))")
    api POST "/api/v1/lojapp/inventory/adjust" \
      "{\"productId\":$pid,\"quantity\":$delta,\"reason\":\"Entrada demo portfolio\"}" >/dev/null
  fi
}

echo ">> Marcas"
OGO=$(find_or_create_brand "Ogochi")
HER=$(find_or_create_brand "Hering")
echo "   Ogochi id=$OGO | Hering id=$HER"

echo ">> Produtos"
P1=$(find_or_create_product "OGO-CAM-001" "{
  \"name\": \"Camisa Social Slim\",
  \"brandId\": $OGO,
  \"sku\": \"OGO-CAM-001\",
  \"costPrice\": 89.90,
  \"salePrice\": 189.90,
  \"minimumStock\": 2,
  \"variantColor\": \"Azul marinho\",
  \"variantSize\": \"M\"
}")
P2=$(find_or_create_product "HER-CAM-001" "{
  \"name\": \"Camiseta Basica Preta\",
  \"brandId\": $HER,
  \"sku\": \"HER-CAM-001\",
  \"costPrice\": 39.90,
  \"salePrice\": 79.90,
  \"minimumStock\": 3,
  \"variantColor\": \"Preto\",
  \"variantSize\": \"G\"
}")
P3=$(find_or_create_product "OGO-CAL-042" "{
  \"name\": \"Calca Chino Bege\",
  \"brandId\": $OGO,
  \"sku\": \"OGO-CAL-042\",
  \"costPrice\": 119.90,
  \"salePrice\": 249.90,
  \"minimumStock\": 2,
  \"variantColor\": \"Bege\",
  \"variantSize\": \"42\"
}")
echo "   P1 Camisa Ogochi id=$P1"
echo "   P2 Camiseta Hering id=$P2"
echo "   P3 Calca Ogochi id=$P3"

sale() {
  local key="$1" body="$2"
  curl -sf -X POST "$BASE/api/v1/lojapp/sales" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $key" \
    -d "$body"
}

echo ">> Stock (10 un. cada)"
ensure_stock "$P1" 10
ensure_stock "$P2" 10
ensure_stock "$P3" 10

echo ">> Vendas"
S1=$(sale "seed-demo-sale-1" "{\"productId\":$P1,\"quantity\":1,\"unitPrice\":189.90}" | json_field "['id']")
S2=$(sale "seed-demo-sale-2" "{\"productId\":$P2,\"quantity\":2,\"unitPrice\":79.90}" | json_field "['id']")
echo "   Venda 1 id=$S1 (1x Camisa Ogochi)"
echo "   Venda 2 id=$S2 (2x Camiseta Hering)"

echo "OK — recarrega o dashboard no browser (F5)."
