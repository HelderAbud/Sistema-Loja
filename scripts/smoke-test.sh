#!/usr/bin/env bash
set -euo pipefail

# Smoke test objetivo:
# health -> auth (register/login) -> brands -> products -> sale -> stock
# Usa dados isolados por timestamp para evitar colisão.

API_BASE="${API_BASE:-http://localhost:8000}"
API_BASE="${API_BASE%/}"
AUTO_REGISTER="${AUTO_REGISTER:-true}"
PASSWORD="${SMOKE_PASSWORD:-Smoke12345!}"
EMAIL="${SMOKE_EMAIL:-smoke.$(date +%s)@example.com}"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERRO: comando '$cmd' não encontrado." >&2
    exit 1
  fi
}

require_cmd curl
require_cmd python3

json_escape() {
  python3 - "$1" <<'PY'
import json,sys
print(json.dumps(sys.argv[1]))
PY
}

http_code() {
  local method="$1"
  local path="$2"
  local data="${3:-}"
  local auth="${4:-}"
  local tmp
  tmp="$(mktemp)"
  local code
  if [[ -n "$auth" ]]; then
    code=$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$API_BASE$path" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      -H "Authorization: Bearer $auth" \
      ${data:+-d "$data"})
  else
    code=$(curl -sS -o "$tmp" -w "%{http_code}" -X "$method" "$API_BASE$path" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json" \
      ${data:+-d "$data"})
  fi
  RESPONSE_BODY="$(cat "$tmp")"
  rm -f "$tmp"
  echo "$code"
}

extract_json_field() {
  local body="$1"
  local field="$2"
  python3 - "$body" "$field" <<'PY'
import json,sys
obj=json.loads(sys.argv[1])
parts=sys.argv[2].split(".")
cur=obj
for p in parts:
    if p.isdigit():
        cur=cur[int(p)]
    else:
        cur=cur[p]
print(cur)
PY
}

echo "==> Healthcheck..."
code=$(http_code "GET" "/actuator/health")
if [[ "$code" != "200" ]]; then
  echo "FALHA: /actuator/health retornou HTTP $code"
  echo "$RESPONSE_BODY"
  exit 1
fi
echo "OK: API health UP."

register_payload=$(cat <<JSON
{"email":$(json_escape "$EMAIL"),"password":$(json_escape "$PASSWORD")}
JSON
)

if [[ "$AUTO_REGISTER" == "true" ]]; then
  echo "==> Tentando registro ($EMAIL)..."
  reg_code=$(http_code "POST" "/api/v1/auth/register" "$register_payload")
  if [[ "$reg_code" == "200" ]]; then
    echo "OK: registro concluído."
  else
    echo "AVISO: registro retornou HTTP $reg_code (seguindo para login)."
  fi
fi

echo "==> Login..."
login_code=$(http_code "POST" "/api/v1/auth/login" "$register_payload")
if [[ "$login_code" != "200" ]]; then
  echo "FALHA: login retornou HTTP $login_code"
  echo "$RESPONSE_BODY"
  exit 1
fi
TOKEN="$(extract_json_field "$RESPONSE_BODY" "accessToken")"
if [[ -z "${TOKEN:-}" ]]; then
  echo "FALHA: login sem accessToken."
  exit 1
fi
echo "OK: login com JWT."

suffix="$(date +%s)"
brand_name="Smoke Brand $suffix"

echo "==> Criando marca..."
brand_payload="{\"name\":$(json_escape "$brand_name")}"
brand_code=$(http_code "POST" "/api/v1/lojapp/brands" "$brand_payload" "$TOKEN")
if [[ "$brand_code" != "200" && "$brand_code" != "201" ]]; then
  echo "FALHA: criação de marca retornou HTTP $brand_code"
  echo "$RESPONSE_BODY"
  exit 1
fi
BRAND_ID="$(extract_json_field "$RESPONSE_BODY" "id")"
echo "OK: marca criada id=$BRAND_ID"

echo "==> Criando produto..."
product_payload=$(cat <<JSON
{"name":"Smoke Product $suffix","brandId":$BRAND_ID,"costPrice":10.00,"salePrice":15.00,"minimumStock":1.000}
JSON
)
product_code=$(http_code "POST" "/api/v1/lojapp/products" "$product_payload" "$TOKEN")
if [[ "$product_code" != "200" && "$product_code" != "201" ]]; then
  echo "FALHA: criação de produto retornou HTTP $product_code"
  echo "$RESPONSE_BODY"
  exit 1
fi
PRODUCT_ID="$(extract_json_field "$RESPONSE_BODY" "id")"
echo "OK: produto criado id=$PRODUCT_ID"

echo "==> Ajustando estoque..."
adjust_payload="{\"productId\":$PRODUCT_ID,\"quantity\":5.000,\"reason\":\"SMOKE_TEST\"}"
adjust_code=$(http_code "POST" "/api/v1/lojapp/inventory/adjust" "$adjust_payload" "$TOKEN")
if [[ "$adjust_code" != "200" && "$adjust_code" != "201" && "$adjust_code" != "204" ]]; then
  echo "FALHA: ajuste de estoque retornou HTTP $adjust_code"
  echo "$RESPONSE_BODY"
  exit 1
fi
echo "OK: estoque ajustado."

echo "==> Registrando venda..."
sale_payload="{\"productId\":$PRODUCT_ID,\"quantity\":1.000,\"unitPrice\":15.00,\"unitCost\":10.00}"
sale_code=$(http_code "POST" "/api/v1/lojapp/sales" "$sale_payload" "$TOKEN")
if [[ "$sale_code" != "200" && "$sale_code" != "201" ]]; then
  echo "FALHA: venda retornou HTTP $sale_code"
  echo "$RESPONSE_BODY"
  exit 1
fi
echo "OK: venda registrada."

echo "==> Validando leitura de estoque..."
stock_code=$(http_code "GET" "/api/v1/lojapp/inventory/products/$PRODUCT_ID/stock" "" "$TOKEN")
if [[ "$stock_code" != "200" ]]; then
  echo "FALHA: consulta de estoque retornou HTTP $stock_code"
  echo "$RESPONSE_BODY"
  exit 1
fi
echo "OK: estoque consultado."

echo "==> Validando listagens principais..."
brands_code=$(http_code "GET" "/api/v1/lojapp/brands" "" "$TOKEN")
products_code=$(http_code "GET" "/api/v1/lojapp/products?page=0&size=20" "" "$TOKEN")
sales_code=$(http_code "GET" "/api/v1/lojapp/sales?page=0&size=20" "" "$TOKEN")
if [[ "$brands_code" != "200" || "$products_code" != "200" || "$sales_code" != "200" ]]; then
  echo "FALHA: listagens retornaram códigos inválidos:"
  echo "brands=$brands_code products=$products_code sales=$sales_code"
  exit 1
fi

echo
echo "SMOKE TEST OK: fluxo crítico validado."
echo "Usuário teste: $EMAIL"
