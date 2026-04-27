#!/usr/bin/env bash
# Verifica login + JWT + GET marcas/produtos (Passo 1 Swagger / Passo 2 prod sem Swagger).
#
# Uso:
#   export LOJAPP_VERIFY_EMAIL='piloto1+...@loja-exemplo.com'
#   export LOJAPP_VERIFY_PASSWORD='...'
#   export API_BASE='http://localhost:8080'   # opcional
#   ./scripts/verify-api-env.sh
#
# Requisitos: curl; jq OU python3 (para JSON do login e contagens opcionais).

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
API_BASE="${API_BASE%/}"

if [[ -z "${LOJAPP_VERIFY_EMAIL:-}" || -z "${LOJAPP_VERIFY_PASSWORD:-}" ]]; then
  echo "Defina LOJAPP_VERIFY_EMAIL e LOJAPP_VERIFY_PASSWORD (e opcionalmente API_BASE)." >&2
  exit 1
fi

build_login_json() {
  if command -v python3 >/dev/null 2>&1; then
    LOJAPP_VERIFY_EMAIL="$LOJAPP_VERIFY_EMAIL" LOJAPP_VERIFY_PASSWORD="$LOJAPP_VERIFY_PASSWORD" \
      python3 -c "import json,os; print(json.dumps({'email':os.environ['LOJAPP_VERIFY_EMAIL'],'password':os.environ['LOJAPP_VERIFY_PASSWORD']}))"
    return
  fi
  if command -v jq >/dev/null 2>&1; then
    jq -n --arg e "$LOJAPP_VERIFY_EMAIL" --arg p "$LOJAPP_VERIFY_PASSWORD" '{email:$e,password:$p}'
    return
  fi
  echo "Instale python3 ou jq para montar o JSON do login." >&2
  return 1
}

extract_access_token() {
  local json="$1"
  if command -v jq >/dev/null 2>&1; then
    jq -r .accessToken <<<"$json"
    return
  fi
  if command -v python3 >/dev/null 2>&1; then
    python3 -c "import json,sys; print(json.loads(sys.argv[1])['accessToken'])" "$json"
    return
  fi
  echo "Instale jq ou python3 para ler accessToken." >&2
  return 1
}

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

login_payload="$(build_login_json)"

http_login=$(curl -sS -o "$tmp" -w "%{http_code}" -X POST "$API_BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "$login_payload")

login_body="$(cat "$tmp")"
if [[ "$http_login" != "200" ]]; then
  echo "Login falhou (HTTP $http_login). Corpo:" >&2
  echo "$login_body" >&2
  exit 1
fi

TOKEN="$(extract_access_token "$login_body")"
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "Resposta de login sem accessToken:" >&2
  echo "$login_body" >&2
  exit 1
fi

echo "Login OK (JWT obtido)."

http=$(curl -sS -o "$tmp" -w "%{http_code}" "$API_BASE/api/v1/lojapp/brands" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json")
body="$(cat "$tmp")"
if [[ "$http" != "200" ]]; then
  echo "GET /api/v1/lojapp/brands falhou (HTTP $http)." >&2
  echo "$body" >&2
  exit 1
fi
if command -v jq >/dev/null 2>&1; then
  n="$(echo "$body" | jq 'length')"
  echo "GET brands OK — $n marca(s)."
else
  echo "GET brands OK."
fi

http=$(curl -sS -o "$tmp" -w "%{http_code}" \
  "$API_BASE/api/v1/lojapp/products?page=0&size=20&sort=name,asc" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json")
body="$(cat "$tmp")"
if [[ "$http" != "200" ]]; then
  echo "GET /api/v1/lojapp/products falhou (HTTP $http)." >&2
  echo "$body" >&2
  exit 1
fi
if command -v jq >/dev/null 2>&1; then
  n="$(echo "$body" | jq '.totalElements')"
  echo "GET products OK — totalElements=$n (pagina 0)."
else
  echo "GET products OK."
fi

echo "Verificacao concluida: API em $API_BASE responde com esta conta; dados = Postgres deste utilizador."
