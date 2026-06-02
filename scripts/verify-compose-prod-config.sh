#!/usr/bin/env bash
# Valida sintaxe do docker-compose.prod.yml sem subir serviços (secrets dummy).
# Uso (na raiz do repositório):
#   ./scripts/verify-compose-prod-config.sh
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "Aviso: docker não encontrado — validação de compose ignorada." >&2
  exit 0
fi

export POSTGRES_PASSWORD="verify-compose-config-dummy-pg-123456"
export LOJAPP_JWT_SECRET="verify-compose-config-dummy-jwt-32chars-min!!"
docker compose -f docker-compose.prod.yml config --quiet
echo "OK: docker-compose.prod.yml válido (expansão de variáveis)."
