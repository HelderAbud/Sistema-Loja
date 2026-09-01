#!/usr/bin/env bash
# Seed: marcas Ogochi/Malwee/Hering, 36 SKUs masculinos (catálogo demo) e 2 vendas.
# Uso (Ubuntu/Git Bash):
#   LOJAPP_SEED_PASSWORD='...' ./scripts/seed-demo-roupas.sh
# Produção (Railway), com a conta que já criaste:
#   LOJAPP_BASE_URL='https://sistema-loja-production-7608.up.railway.app' \
#   LOJAPP_SEED_EMAIL='teu-email' LOJAPP_SEED_PASSWORD='...' ./scripts/seed-demo-roupas.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CATALOG="${LOJAPP_SEED_CATALOG:-$ROOT/scripts/fixtures/seed-demo-catalog.json}"
BASE="${LOJAPP_BASE_URL:-http://localhost:8081}"
EMAIL="${LOJAPP_SEED_EMAIL:-piloto@lojapp.demo}"
PASS="${LOJAPP_SEED_PASSWORD:?Defina LOJAPP_SEED_PASSWORD}"
STOCK_QTY="${LOJAPP_SEED_STOCK:-10}"

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

echo ">> Login $EMAIL @ $BASE"
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

find_product_id() {
  local sku="$1"
  api GET "/api/v1/lojapp/products?size=200" | python3 -c "
import sys,json
sku=sys.argv[1]
data=json.load(sys.stdin)
for p in data.get('content',[]):
    if (p.get('sku') or '').upper()==sku.upper():
        print(p['id']); break
" "$sku" || true
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
MLW=$(find_or_create_brand "Malwee")
HER=$(find_or_create_brand "Hering")
echo "   Ogochi id=$OGO | Malwee id=$MLW | Hering id=$HER"

if [[ ! -f "$CATALOG" ]]; then
  echo "Catálogo em falta: $CATALOG" >&2
  exit 1
fi

echo ">> Produtos ($CATALOG)"
while IFS=$'\t' read -r sku payload; do
  pid=$(find_or_create_product "$sku" "$payload")
  echo "   $sku id=$pid"
  ensure_stock "$pid" "$STOCK_QTY"
done < <(python3 -c "
import json, sys
brands = {'ogochi': int(sys.argv[1]), 'malwee': int(sys.argv[2]), 'hering': int(sys.argv[3])}
with open(sys.argv[4], encoding='utf-8') as f:
    rows = json.load(f)
for row in rows:
    brand = brands[row['brandName'].strip().lower()]
    payload = {
        'name': row['name'],
        'brandId': brand,
        'sku': row['sku'],
        'costPrice': row['costPrice'],
        'salePrice': row['salePrice'],
        'minimumStock': row['minimumStock'],
        'variantColor': row.get('variantColor'),
        'variantSize': row.get('variantSize'),
    }
    print(row['sku'] + '\t' + json.dumps(payload, ensure_ascii=False, separators=(',', ':')))
" "$OGO" "$MLW" "$HER" "$CATALOG")

sale() {
  local key="$1" body="$2"
  curl -sf -X POST "$BASE/api/v1/lojapp/sales" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $key" \
    -d "$body"
}

P_CAMISA=$(find_product_id "OGC-CMS-002")
P_CAMISETA=$(find_product_id "HRG-CTS-025")

echo ">> Vendas"
if [[ -n "$P_CAMISA" && -n "$P_CAMISETA" ]]; then
  S1=$(sale "seed-demo-sale-ogc-cms-002" "{\"productId\":$P_CAMISA,\"quantity\":1,\"unitPrice\":229.90}" | json_field "['id']")
  S2=$(sale "seed-demo-sale-hrg-cts-025" "{\"productId\":$P_CAMISETA,\"quantity\":2,\"unitPrice\":89.90}" | json_field "['id']")
  echo "   Venda 1 id=$S1 (1x Camisa Linho Ogochi)"
  echo "   Venda 2 id=$S2 (2x Camiseta Hering)"
else
  echo "   (omitidas — SKUs de venda não encontrados)"
fi

echo "OK — recarrega o dashboard no browser (F5)."
