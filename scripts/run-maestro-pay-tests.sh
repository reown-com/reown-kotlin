#!/bin/bash
# Runs Maestro Pay E2E tests locally.
# Prerequisites: Maestro CLI installed, Android emulator running, wallet app installed.
# Usage: ./scripts/run-maestro-pay-tests.sh

set -euo pipefail

MAESTRO_DIR=".maestro"
ENV_FILE=".env.maestro"
APP_ID="${APP_ID:-com.reown.sample.wallet.internal}"

# Download test flows if not present
if [ ! -f "$MAESTRO_DIR/pay_single_option_nokyc.yaml" ]; then
  echo "Pay test flows not found. Downloading..."
  ./scripts/setup-maestro-pay-tests.sh
fi

# Load secrets from .env.maestro if it exists
ENV_ARGS=""
if [ -f "$ENV_FILE" ]; then
  echo "Loading secrets from $ENV_FILE..."
  while IFS='=' read -r key value; do
    # Skip comments and empty lines
    [[ "$key" =~ ^#.*$ || -z "$key" ]] && continue
    # Remove surrounding quotes from value
    value="${value%\"}"
    value="${value#\"}"
    ENV_ARGS="$ENV_ARGS --env $key=$value"
  done < "$ENV_FILE"
else
  echo "Warning: $ENV_FILE not found. Create it from .env.maestro.example"
  echo "Tests requiring merchant secrets will fail."
fi

echo "Running Maestro Pay tests with APP_ID=$APP_ID..."
maestro test \
  --env APP_ID="$APP_ID" \
  $ENV_ARGS \
  --include-tags pay \
  "$MAESTRO_DIR/"
