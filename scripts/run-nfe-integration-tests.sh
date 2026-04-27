#!/usr/bin/env bash
# Passo 5 (opcional): corre só NfeImportStockIntegrationTest se o Docker estiver acessível.
#
# Uso (na raiz do repo ou qualquer pasta):
#   ./scripts/run-nfe-integration-tests.sh
#
# Se Docker não estiver disponível, termina com código 1 e mensagem clara (útil em CI).

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker (CLI) não encontrado no PATH. Instale Docker Engine e tente de novo." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker não está acessível (docker info falhou). Inicie o serviço ou adicione o utilizador ao grupo docker." >&2
  exit 1
fi

echo "Docker OK. A correr NfeImportStockIntegrationTest..."
mvn -q test -Dtest=NfeImportStockIntegrationTest
