# EcoDim

Dims the whole Android screen below the lowest system brightness, including the status bar,
notification shade and navigation bar. A from-scratch take on EcoDimmer, without shake-to-off.

## Features
- Strength from 10% to 90% (capped so the off switch stays findable)
- Optional warm amber tint
- Daily schedule, including overnight windows (e.g. 22:00 to 07:00)
- Quick Settings tile with live strength readout
- Notification with Lighter / Darker / Turn off buttons
- Normal launcher icon
- No motion sensors, no network, no data collection

## Install with Obtainium
In [Obtainium](https://github.com/ImranR98/Obtainium), add `https://github.com/AKolenda/ecodim`,
or use [Add to Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/AKolenda/ecodim).
No other settings are needed.

After installing, open EcoDim and follow the setup card. On Android 13+, sideloaded apps need
App info → ⋮ → Allow restricted settings before the Accessibility switch can be turned on.

## Turning it off
Quick Settings tile, the notification's Turn off button, the app, or (once the EcoDim shortcut is
on in Accessibility settings) holding both volume keys.

## Build
Needs JDK 17 and Android SDK 34.

    ./gradlew assembleDebug

Release APKs are built in a clean container with `scripts/container-build.sh`.

Signed release: pass `-PECODIMMER_STORE_FILE`, `-PECODIMMER_STORE_PASSWORD`,
`-PECODIMMER_KEY_ALIAS` and `-PECODIMMER_KEY_PASSWORD` to `./gradlew assembleRelease`.
