#!/usr/bin/env bash
set -euo pipefail
git init || true
git add .
git commit -m "Initial commit - python-cloud-lab-postgres-container scaffold" || true
echo "Committed. Edit infra/backend.tf with OCI details and push to GitHub."
