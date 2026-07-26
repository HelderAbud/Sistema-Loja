#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/frontend"
export LOJAPP_CAPTURE_PILOTO_DIA12=1
export LOJAPP_SCREENSHOT_EMAIL="${LOJAPP_SCREENSHOT_EMAIL:-piloto-dia12@lojapp.demo}"
export LOJAPP_SCREENSHOT_PASSWORD="$(cat /tmp/lojapp-dia12-pass)"
npx playwright test --config=playwright.piloto-dia12.config.ts
ls -la "$ROOT/docs/screenshots/piloto/"*.png
