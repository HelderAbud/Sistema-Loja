#!/usr/bin/env bash
# Importa todos os *.xml de uma pasta via POST /api/v1/lojapp/nfe/import (automação parcial do Passo 3).
#
# Uso:
#   export LOJAPP_VERIFY_EMAIL=... LOJAPP_VERIFY_PASSWORD=...
#   export API_BASE=http://localhost:8080   # opcional
#   ./scripts/import-nfe-folder.sh /caminho/para/xmls
#
# Ou, sem login (token já obtido):
#   export LOJAPP_JWT='eyJ...'
#   ./scripts/import-nfe-folder.sh /caminho/para/xmls
#
# Requisitos: curl; python3 OU jq para JSON; ficheiros UTF-8.

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
API_BASE="${API_BASE%/}"

DIR="${1:-}"
if [[ -z "$DIR" || ! -d "$DIR" ]]; then
  echo "Uso: $0 <pasta_com_xml>" >&2
  exit 1
fi

wrap_xml_json() {
  local file="$1"
  if command -v python3 >/dev/null 2>&1; then
    python3 -c "import json,sys; print(json.dumps({'rawXml': open(sys.argv[1],encoding='utf-8').read()}))" "$file"
    return
  fi
  echo "python3 é necessário para escapar o XML em JSON." >&2
  return 1
}

get_token() {
  if [[ -n "${LOJAPP_JWT:-}" ]]; then
    echo "$LOJAPP_JWT"
    return
  fi
  if [[ -z "${LOJAPP_VERIFY_EMAIL:-}" || -z "${LOJAPP_VERIFY_PASSWORD:-}" ]]; then
    echo "Defina LOJAPP_JWT ou LOJAPP_VERIFY_EMAIL e LOJAPP_VERIFY_PASSWORD." >&2
    return 1
  fi
  local payload
  if command -v python3 >/dev/null 2>&1; then
    payload="$(LOJAPP_VERIFY_EMAIL="$LOJAPP_VERIFY_EMAIL" LOJAPP_VERIFY_PASSWORD="$LOJAPP_VERIFY_PASSWORD" \
      python3 -c "import json,os; print(json.dumps({'email':os.environ['LOJAPP_VERIFY_EMAIL'],'password':os.environ['LOJAPP_VERIFY_PASSWORD']}))")"
  elif command -v jq >/dev/null 2>&1; then
    payload="$(jq -n --arg e "$LOJAPP_VERIFY_EMAIL" --arg p "$LOJAPP_VERIFY_PASSWORD" '{email:$e,password:$p}')"
  else
    echo "Instale python3 ou jq para o login." >&2
    return 1
  fi
  local tmp
  tmp="$(mktemp)"
  local code
  code="$(curl -sS -o "$tmp" -w "%{http_code}" -X POST "$API_BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" -d "$payload")"
  local body
  body="$(cat "$tmp")"
  rm -f "$tmp"
  if [[ "$code" != "200" ]]; then
    echo "Login falhou HTTP $code: $body" >&2
    return 1
  fi
  if command -v jq >/dev/null 2>&1; then
    jq -r .accessToken <<<"$body"
  elif command -v python3 >/dev/null 2>&1; then
    python3 -c "import json,sys; print(json.loads(sys.argv[1])['accessToken'])" "$body"
  else
    echo "Instale jq ou python3 para ler accessToken." >&2
    return 1
  fi
}

TOKEN="$(get_token)"
shopt -s nullglob
files=("$DIR"/*.xml "$DIR"/*.XML)
if [[ ${#files[@]} -eq 0 ]]; then
  echo "Nenhum .xml em $DIR" >&2
  exit 1
fi

ok=0
fail=0
for f in "${files[@]}"; do
  echo "---- $f"
  body="$(wrap_xml_json "$f")"
  tmp="$(mktemp)"
  code="$(curl -sS -o "$tmp" -w "%{http_code}" -X POST "$API_BASE/api/v1/lojapp/nfe/import" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$body")"
  out="$(cat "$tmp")"
  rm -f "$tmp"
  if [[ "$code" == "200" ]]; then
    echo "OK HTTP $code — $out"
    ok=$((ok + 1))
  else
    echo "FALHA HTTP $code — $out" >&2
    fail=$((fail + 1))
  fi
done

echo "Resumo: $ok OK, $fail falha(s)."
[[ "$fail" -eq 0 ]]
