# March (Market Watch) for Android

March is a Persian/English crypto market watcher. The name is short for **Market Watch**.

## What is included

- Live market prices, USDT/Toman, watchlist, economic-release reminders and per-coin price rules.
- Price alerts can ring the phone like a wake-up alarm. March first tries the device's own Clock app with a one-second timer, so the phone uses its alarm stream and the user must stop it. If Android blocks opening Clock from the background, March's full-screen alarm fallback rings instead.
- The Alerts tab has a **Clock app alarm** switch, a test button and links for the permissions Android may require: notifications, exact alarms, full-screen notifications and display-over-other-apps. On Xiaomi, enable the relevant background pop-up permission too.
- The Alerts tab also has **App update**. It checks the latest GitHub Release, downloads the APK, and opens Android's installer. The first update may ask for permission to install unknown apps. A matching application ID and stable signing key are required for seamless updates.

## Build and release

The workflow at `.github/workflows/build-apk.yml` builds an APK and AAB on every push to `main`, assigns a monotonically increasing version (`2.2.<run number>`), and publishes a GitHub Release with both files. The in-app updater reads the latest release and selects the APK asset.

The GitHub connection used to write this change could not edit workflow files directly, so `ci/build-and-release.yml` is a ready-to-copy workflow. Copy it to `.github/workflows/build-apk.yml` once, then commit it. It needs the repository's **Actions: Read and write permissions**.

For production, replace the fallback CI key with GitHub Actions secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. The checked-in `ci/march-ci.jks.b64` is only a stable development fallback so repeated CI releases can update over one another. Do not use it for Google Play or a public store.

## Data sources

CoinGecko supplies the ranked market list and history. Binance WebSocket supplies fast quotes, with Nobitex fallbacks for live prices and USDT/Toman. Access from some regions may require a proxy or backend.

## Install

Download the APK from the latest [GitHub Release](https://github.com/MobitSolidity/march/releases/latest). Android may require enabling **Install unknown apps** for March.
