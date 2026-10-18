#!/bin/sh
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
git -C "$ROOT" config core.hooksPath .githooks
chmod +x "$ROOT/.githooks/pre-push"
echo "Git will now run the fast tests before every push. Undo with: git config --unset core.hooksPath"
