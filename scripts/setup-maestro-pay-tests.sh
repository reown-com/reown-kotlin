#!/bin/bash
# Downloads Maestro Pay test flows from WalletConnect/actions repository.
# Usage: ./scripts/setup-maestro-pay-tests.sh [branch]

set -euo pipefail

BRANCH="${1:-main}"
REPO="WalletConnect/actions"
BASE_URL="https://raw.githubusercontent.com/$REPO/$BRANCH/maestro/pay-tests/.maestro"

MAESTRO_DIR=".maestro"
FLOWS_DIR="$MAESTRO_DIR/flows"
SCRIPTS_DIR="$MAESTRO_DIR/scripts"

mkdir -p "$FLOWS_DIR" "$SCRIPTS_DIR"

echo "Downloading Maestro Pay test flows from $REPO@$BRANCH..."

# Main test flows
FLOWS=(
  "pay_single_option_nokyc.yaml"
  "pay_single_option_nokyc_deeplink.yaml"
  "pay_multiple_options_nokyc.yaml"
  "pay_multiple_options_kyc.yaml"
  "pay_cancel_from_review.yaml"
  "pay_cancel_from_kyc.yaml"
  "pay_kyc_back_navigation.yaml"
  "pay_insufficient_funds.yaml"
  "pay_double_scan.yaml"
  "pay_expired_link.yaml"
  "pay_cancelled.yaml"
)

for flow in "${FLOWS[@]}"; do
  echo "  Downloading $flow..."
  curl -sSfL "$BASE_URL/$flow" -o "$MAESTRO_DIR/$flow"
done

# Sub-flows
SUB_FLOWS=(
  "flows/pay_open_and_paste_url.yaml"
  "flows/pay_confirm_and_verify.yaml"
  "flows/pay_open_via_deeplink.yaml"
)

for flow in "${SUB_FLOWS[@]}"; do
  echo "  Downloading $flow..."
  curl -sSfL "$BASE_URL/$flow" -o "$MAESTRO_DIR/$flow"
done

# Scripts
SCRIPTS=(
  "scripts/create-payment.js"
  "scripts/cancel-payment.js"
)

for script in "${SCRIPTS[@]}"; do
  echo "  Downloading $script..."
  curl -sSfL "$BASE_URL/$script" -o "$MAESTRO_DIR/$script"
done

echo "Done! Maestro Pay test flows downloaded to $MAESTRO_DIR/"
