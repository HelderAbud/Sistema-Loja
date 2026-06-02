#!/usr/bin/env bash
# Valida GET /actuator/health, /readiness e /liveness (Dia 9 / go-live).
#
# Uso:
#   ./scripts/verify-deploy-health.sh
#   API_BASE=https://api.exemplo.com ./scripts/verify-deploy-health.sh
#
set -euo pipefail

BASE="${API_BASE:-http://localhost:8080}"
BASE="${BASE%/}"

check_path() {
  local label="$1"
  local suffix="$2"
  local url="${BASE}/actuator/health${suffix}"
  local tmp
  tmp="$(mktemp)"
  local code
  code="$(curl -sS -o "$tmp" -w "%{http_code}" "$url")"
  echo "[$label] HTTP $code"
  cat "$tmp"
  echo ""
  if [[ "$code" != "200" ]]; then
    rm -f "$tmp"
    echo "Erro: esperado HTTP 200 para $url" >&2
    exit 1
  fi
  if ! grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' "$tmp"; then
    rm -f "$tmp"
    echo "Erro: resposta sem status UP ($label)" >&2
    exit 1
  fi
  rm -f "$tmp"
}

echo "API: $BASE"
check_path "health" ""
check_path "readiness" "/readiness"
check_path "liveness" "/liveness"
echo "OK: health / readiness / liveness com status UP."
