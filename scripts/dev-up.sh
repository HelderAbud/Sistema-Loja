#!/usr/bin/env bash
# Sobe Postgres + Redis (docker-compose.yml) para desenvolvimento local.
# Usa o .env na raiz do repo para variáveis do compose (ex.: POSTGRES_PASSWORD).
#
# Uso (na raiz do repositório):
#   chmod +x scripts/dev-up.sh
#   ./scripts/dev-up.sh
#   ./scripts/dev-up.sh --start-api    # também: source .env + mvn spring-boot:run
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

START_API=false
if [[ "${1:-}" == "--start-api" ]]; then
  START_API=true
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Erro: docker não encontrado. Instale Docker Engine + plugin compose." >&2
  exit 1
fi

docker compose up -d db redis

echo "À espera de Postgres em 127.0.0.1:5433 ..."
ok=false
for _ in $(seq 1 90); do
  if timeout 1 bash -c "</dev/tcp/127.0.0.1/5433" 2>/dev/null; then
    ok=true
    break
  fi
  sleep 2
done

if [[ "$ok" != true ]]; then
  echo "Timeout: porta 5433 não abriu. Verifique: docker compose ps" >&2
  exit 1
fi

echo "Postgres acessível na porta 5433."

if [[ "$START_API" != true ]]; then
  echo ""
  echo "Próximo passo (carregar .env e subir API):"
  echo '  set -a && source .env && set +a && mvn -q -DskipTests spring-boot:run'
  echo ""
  echo "Health:"
  echo "  ./scripts/verify-deploy-health.sh"
  exit 0
fi

if [[ ! -f .env ]]; then
  echo "Erro: ficheiro .env não encontrado na raiz do repo." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a
exec mvn -q -DskipTests spring-boot:run
