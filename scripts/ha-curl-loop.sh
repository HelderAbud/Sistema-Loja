#!/usr/bin/env bash
# Loop de health atrás do nginx HA local (Fase E).
# Uso (na raiz do repo, com stack HA no ar):
#   chmod +x scripts/ha-curl-loop.sh
#   ./scripts/ha-curl-loop.sh
#   BASE=http://127.0.0.1:8088 LOOPS=30 ./scripts/ha-curl-loop.sh
#
# Não exige AWS. Se a stack não estiver up, falha cedo.
set -euo pipefail

BASE="${BASE:-http://127.0.0.1:8088}"
PATH_HEALTH="${PATH_HEALTH:-/actuator/health}"
LOOPS="${LOOPS:-20}"
SLEEP_S="${SLEEP_S:-0.5}"

url="${BASE%/}${PATH_HEALTH}"
ok=0
fail=0

echo "HA curl loop → ${url} (${LOOPS} pedidos)"
echo "Esperado: HTTP 200 e header X-Lojapp-Upstream alternando api1/api2 (least_conn)."
echo ""

for i in $(seq 1 "$LOOPS"); do
  # -s silent, -D - dump headers to stdout mixed; use -w for status
  headers="$(mktemp)"
  code="$(curl -sS -o /tmp/lojapp-ha-body.json -D "$headers" -w '%{http_code}' --max-time 15 "$url" || echo "000")"
  upstream="$(grep -i '^X-Lojapp-Upstream:' "$headers" | tr -d '\r' | awk '{print $2}' || true)"
  rm -f "$headers"
  if [[ "$code" == "200" ]]; then
    ok=$((ok + 1))
    echo "[$i] $code upstream=${upstream:-?}"
  else
    fail=$((fail + 1))
    echo "[$i] FAIL code=$code" >&2
  fi
  sleep "$SLEEP_S"
done

echo ""
echo "OK=$ok FAIL=$fail"
if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
