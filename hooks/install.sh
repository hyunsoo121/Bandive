#!/bin/sh
#
# Git 훅 설치 — clone 후 한 번만 실행하면 된다.
# core.hooksPath 를 hooks/ 로 지정하므로 hooks/pre-commit 수정은 즉시 반영된다.

set -e

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

echo "▶ Git 훅 설치 중..."
git config core.hooksPath hooks
chmod +x hooks/pre-commit
echo "✓ core.hooksPath = hooks  (pre-commit 활성화)"
