#!/usr/bin/env bash
set -euo pipefail

# Bootstrap mínimo para Ubuntu (dev local):
# - valida dependências
# - valida variáveis essenciais
# - sobe Postgres/Redis
# - compila backend e frontend

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERRO: comando '$cmd' não encontrado." >&2
    exit 1
  fi
}

load_env_file_if_present() {
  if [[ -f "$ROOT_DIR/.env" ]]; then
    # shellcheck disable=SC1091
    set -a && source "$ROOT_DIR/.env" && set +a
    echo "OK: variáveis carregadas de .env"
  else
    echo "AVISO: .env não encontrado na raiz. Usando variáveis já exportadas no shell."
  fi
}

validate_required_env() {
  if [[ -z "${LOJAPP_JWT_SECRET:-}" ]]; then
    echo "ERRO: LOJAPP_JWT_SECRET não definido." >&2
    echo "Defina no shell ou no arquivo .env (mínimo 32 caracteres)." >&2
    exit 1
  fi
  if (( ${#LOJAPP_JWT_SECRET} < 32 )); then
    echo "ERRO: LOJAPP_JWT_SECRET deve ter pelo menos 32 caracteres." >&2
    exit 1
  fi
}

echo "==> Verificando pré-requisitos..."
require_cmd docker
require_cmd mvn
require_cmd node
require_cmd npm
require_cmd curl
echo "OK: comandos essenciais encontrados."

echo "==> Carregando variáveis..."
load_env_file_if_present
validate_required_env
echo "OK: variáveis essenciais válidas."

echo "==> Subindo infra local (Postgres + Redis)..."
(cd "$ROOT_DIR" && docker compose up -d db redis)

echo "==> Build backend (sem testes)..."
(cd "$ROOT_DIR" && mvn -q clean package -DskipTests)

echo "==> Instalando dependências e build frontend..."
(cd "$FRONTEND_DIR" && npm install && npm run build)

echo
echo "Bootstrap concluído."
echo "Próximos comandos:"
echo "1) Backend:   cd \"$ROOT_DIR\" && mvn spring-boot:run"
echo "2) Frontend:  cd \"$FRONTEND_DIR\" && npm run dev"
echo "3) Health:    curl http://localhost:8000/actuator/health"
echo "4) Smoke API: cd \"$ROOT_DIR\" && ./scripts/smoke-test.sh"
