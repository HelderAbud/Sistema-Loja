#!/usr/bin/env bash
# Dia 12 — E2E local: registo/login piloto → seed/NFe se preciso → 1 venda → prova saldo.
# Não versionar passwords. Password só em /tmp (chmod 600).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BASE="${LOJAPP_BASE_URL:-http://127.0.0.1:8081}"
EMAIL="${LOJAPP_SEED_EMAIL:-piloto@lojapp.demo}"
PASS_FILE="${LOJAPP_DIA12_PASS_FILE:-/tmp/lojapp-dia12-pass}"
EVIDENCE="/tmp/lojapp-dia12-evidence.txt"

: >"$EVIDENCE"

if [[ -z "${LOJAPP_SEED_PASSWORD:-}" ]]; then
  LOJAPP_SEED_PASSWORD="$(openssl rand -base64 18 | tr -d '/+=' | head -c 20)"
fi
PASS="$LOJAPP_SEED_PASSWORD"
printf '%s' "$PASS" >"$PASS_FILE"
chmod 600 "$PASS_FILE"
echo "PASS_LEN=${#PASS}" | tee -a "$EVIDENCE"

REG=$(curl -s -o /tmp/reg.json -w '%{http_code}' -X POST "$BASE/api/v1/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" || true)
echo "REG=$REG" | tee -a "$EVIDENCE"

LOGIN=$(curl -s -o /tmp/login.json -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
echo "LOGIN=$LOGIN" | tee -a "$EVIDENCE"
if [[ "$LOGIN" != "200" ]]; then
  echo "Login falhou (conta pode já existir com outra password). Defina LOJAPP_SEED_PASSWORD e reexecute." | tee -a "$EVIDENCE"
  cat /tmp/login.json | head -c 300; echo
  exit 2
fi

TOKEN=$(python3 -c "import json; print(json.load(open('/tmp/login.json'))['accessToken'])")
echo TOKEN_OK | tee -a "$EVIDENCE"

auth() {
  curl -s -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' "$@"
}

PRODUCTS=$(auth "$BASE/api/v1/lojapp/products")
PROD_COUNT=$(python3 -c "import json,sys; print(len(json.load(sys.stdin)))" <<<"$PRODUCTS")
echo "PRODUCTS_BEFORE_SEED=$PROD_COUNT" | tee -a "$EVIDENCE"

if [[ "$PROD_COUNT" -lt 1 ]]; then
  echo ">> seed-demo-roupas.sh" | tee -a "$EVIDENCE"
  LOJAPP_SEED_EMAIL="$EMAIL" LOJAPP_SEED_PASSWORD="$PASS" LOJAPP_BASE_URL="$BASE" \
    bash ./scripts/seed-demo-roupas.sh | tee -a "$EVIDENCE"
fi

if [[ -d ./scripts/fixtures/nfe-lote-sintetico-dia7 ]]; then
  echo ">> import-nfe-folder.sh" | tee -a "$EVIDENCE"
  LOJAPP_JWT="$TOKEN" API_BASE="$BASE" bash ./scripts/import-nfe-folder.sh \
    ./scripts/fixtures/nfe-lote-sintetico-dia7 | tee -a "$EVIDENCE" || true
fi

# Pick first product with stock > 0 via SQL (email only for piloto demo)
ROW=$(docker exec loja-postgres psql -U loja_user -d loja_db -t -A -F',' -c \
  "SELECT p.id::text, COALESCE(ib.quantity,0)::text, p.sale_price::text
   FROM products p
   LEFT JOIN inventory_balances ib ON ib.product_id=p.id AND ib.user_id=p.user_id
   WHERE p.user_id=(SELECT id FROM users WHERE email='$EMAIL')
     AND COALESCE(ib.quantity,0) > 0
   ORDER BY p.id LIMIT 1;")
if [[ -z "$ROW" ]]; then
  echo "FAIL: nenhum produto com stock>0" | tee -a "$EVIDENCE"
  exit 3
fi

PID=$(echo "$ROW" | cut -d, -f1)
QTY_BEFORE=$(echo "$ROW" | cut -d, -f2)
PRICE=$(echo "$ROW" | cut -d, -f3)
echo "PRODUCT_ID=$PID QTY_BEFORE=$QTY_BEFORE PRICE=$PRICE" | tee -a "$EVIDENCE"

STOCK_JSON=$(auth "$BASE/api/v1/lojapp/inventory/products/$PID/stock")
echo "STOCK_API_BEFORE=$STOCK_JSON" | tee -a "$EVIDENCE"

SALE_BODY="{\"productId\":$PID,\"quantity\":1,\"unitPrice\":$PRICE}"
SALE_CODE=$(curl -s -o /tmp/sale.json -w '%{http_code}' -X POST "$BASE/api/v1/lojapp/sales" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: dia12-e2e-venda-001' \
  -d "$SALE_BODY")
echo "SALE_HTTP=$SALE_CODE" | tee -a "$EVIDENCE"
head -c 300 /tmp/sale.json; echo

STOCK_AFTER_JSON=$(auth "$BASE/api/v1/lojapp/inventory/products/$PID/stock")
echo "STOCK_API_AFTER=$STOCK_AFTER_JSON" | tee -a "$EVIDENCE"

python3 - <<PY | tee -a "$EVIDENCE"
import json
before=json.loads('''$STOCK_JSON''') if '''$STOCK_JSON'''.strip().startswith('{') else {"quantity": float("$QTY_BEFORE")}
# ProductStockResponse may be bare number or object
raw_before='''$STOCK_JSON'''.strip()
raw_after='''$STOCK_AFTER_JSON'''.strip()
def qty(raw, fallback):
    try:
        d=json.loads(raw)
        if isinstance(d, (int,float)): return float(d)
        if isinstance(d, dict):
            for k in ("quantity","stock","balance"):
                if k in d: return float(d[k])
        return float(fallback)
    except Exception:
        return float(fallback)
qb=qty(raw_before, "$QTY_BEFORE")
qa=qty(raw_after, -1)
print(f"QTY_BEFORE={qb}")
print(f"QTY_AFTER={qa}")
if qa < qb:
    print("E2E_OK: saldo baixou apos venda")
else:
    print("E2E_FAIL: saldo nao baixou")
    raise SystemExit(4)
PY

echo "EVIDENCE=$EVIDENCE"
echo DONE
