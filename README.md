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

## Turning it off
Quick Settings tile, the notification's Turn off button, the app, or (once the EcoDim shortcut is
on in Accessibility settings) holding both volume keys.

## Build
Needs JDK 17 and Android SDK 34.

    ./gradlew assembleDebug

Signed release: pass `-PECODIMMER_STORE_FILE`, `-PECODIMMER_STORE_PASSWORD`,
`-PECODIMMER_KEY_ALIAS` and `-PECODIMMER_KEY_PASSWORD` to `./gradlew assembleRelease`.
