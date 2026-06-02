#!/usr/bin/env bash
# Verifica 401 JSON em GET protegido sem token e com Bearer inválido (Dia 8).
#
# Uso:
#   ./scripts/verify-auth-errors.sh
#   API_BASE=http://127.0.0.1:8080 ./scripts/verify-auth-errors.sh
#
set -euo pipefail

BASE="${API_BASE:-http://localhost:8080}"
BASE="${BASE%/}"
URL="${BASE}/api/v1/lojapp/products?page=0&size=1"

expect401() {
  local label="$1"
  shift
  local tmp code body
  tmp="$(mktemp)"
  code="$(curl -sS -o "$tmp" -w "%{http_code}" "$@")"
  body="$(cat "$tmp")"
  rm -f "$tmp"
  echo "[$label] HTTP $code"
  echo "$body"
  echo ""
  if [[ "$code" != "401" ]]; then
    echo "Erro: esperado HTTP 401 ($label)" >&2
    exit 1
  fi
  if ! grep -q UNAUTHORIZED <<<"$body"; then
    echo "Aviso: corpo sem UNAUTHORIZED ($label)" >&2
  fi
}

echo "=== Sem Authorization ==="
expect401 "no-auth" "$URL"

echo "=== Bearer inválido ==="
expect401 "bad-bearer" -H "Authorization: Bearer not-a-valid-jwt" "$URL"

echo "OK: respostas 401 coerentes."
