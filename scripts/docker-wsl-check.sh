#!/usr/bin/env bash
# Verifica se o Docker responde sem sudo (permissões + daemon).
# Não altera o sistema; em falha, aponta para docs/docker-wsl-ubuntu.md
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOC_REL="docs/docker-wsl-ubuntu.md"
DOC_ABS="$ROOT/$DOC_REL"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERRO: 'docker' não está no PATH. Instala o Docker Engine e tenta de novo."
  echo "Guia: $DOC_ABS"
  exit 1
fi

if docker info >/dev/null 2>&1; then
  echo "OK: Docker acessível (sem sudo)."
  docker ps
  exit 0
fi

echo "Docker não respondeu (permissão negada, daemon parado ou outro erro)."
echo "Troubleshooting: $DOC_REL"
echo "Caminho completo: $DOC_ABS"
exit 1
