# Wallet Sample

Reference wallet implementation for WalletConnect and Reown protocols.

## E2E Tests (Maestro)

Maestro-based E2E tests for the WalletConnect Pay flow.

### Prerequisites

- [Maestro CLI](https://maestro.mobile.dev/) installed (`curl -Ls "https://get.maestro.mobile.dev" | bash`)
- Android emulator running
- WalletConnect Pay merchant credentials

### Setup

1. **Download test flows** from the [WalletConnect/actions](https://github.com/WalletConnect/actions) repo:

   ```bash
   ./scripts/setup-maestro-pay-tests.sh
   ```

2. **Configure merchant credentials** by copying the example env file and filling in the values:

   ```bash
   cp .env.maestro.example .env.maestro
   ```

   Required variables (get these from the WalletConnect Pay dashboard):
   - `WPAY_CUSTOMER_KEY_SINGLE_NOKYC` / `WPAY_MERCHANT_ID_SINGLE_NOKYC`
   - `WPAY_CUSTOMER_KEY_MULTI_NOKYC` / `WPAY_MERCHANT_ID_MULTI_NOKYC`
   - `WPAY_CUSTOMER_KEY_MULTI_KYC` / `WPAY_MERCHANT_ID_MULTI_KYC`

3. **Build and install the wallet app** with test mode enabled:

   ```bash
   ENABLE_TEST_MODE=true ./gradlew :sample:wallet:assembleDebug
   adb install sample/wallet/build/outputs/apk/debug/*.apk
   ```

   > **Note:** `ENABLE_TEST_MODE=true` enables a URL input field in the scanner screen so Maestro can paste payment URLs (since there's no camera on the emulator). This is `false` by default, so the field is hidden in all regular builds including Firebase distribution.

### Running tests

Run all Pay E2E tests:

```bash
APP_ID=com.reown.sample.wallet.debug ./scripts/run-maestro-pay-tests.sh
```

Run a specific test:

```bash
maestro test --env APP_ID=com.reown.sample.wallet.debug .maestro/pay_single_option_nokyc.yaml
```

### Available test flows

| Test | Description |
|------|-------------|
| `pay_single_option_nokyc.yaml` | Single payment option, no KYC |
| `pay_single_option_nokyc_deeplink.yaml` | Single option via deep link |
| `pay_multiple_options_nokyc.yaml` | Multiple options, no KYC |
| `pay_multiple_options_kyc.yaml` | Multiple options with KYC/IC |
| `pay_cancel_from_review.yaml` | Cancel payment from review screen |
| `pay_cancel_from_kyc.yaml` | Cancel payment from KYC screen |
| `pay_kyc_back_navigation.yaml` | Back button navigation in KYC |
| `pay_insufficient_funds.yaml` | Insufficient funds error |
| `pay_double_scan.yaml` | Scanning same payment twice |
| `pay_expired_link.yaml` | Expired payment link |
| `pay_cancelled.yaml` | Cancelled payment |
