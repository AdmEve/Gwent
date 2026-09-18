# Gwent-e Shahnameh (working title)

A tactical row-based card battler for the Iranian mobile market, built around
Persian mythology (Ferdowsi's *Shahnameh*) rather than any existing card
game's names, art, or text. Mechanically it borrows the row/pass/round-concede
tension popularized by *Gwent* — that mechanic itself isn't owned by anyone —
but everything else (name, factions, card art, card text) is original.

Two factions so far: **Pahlavans** (Rostam, Esfandiar, and the heroes of the
Shahnameh) vs. **Div** (Zahhak, Div-e Sepid, and the demons they fought).

## Project structure

- `core/` — pure-Kotlin game rules engine (cards, board, rounds, scoring,
  abilities) plus a simple heuristic AI opponent and a terminal CLI to play
  against it. No Android dependency; builds and tests with plain Gradle
  anywhere a JDK is available.
- `android/` — Jetpack Compose Android app that wraps `core` in a real UI, for
  Cafe Bazaar/Myket distribution. Requires the Android SDK to configure, so
  it's only included in the Gradle build when one is detected (`ANDROID_HOME`
  / `ANDROID_SDK_ROOT` set, or a `local.properties` file present) — e.g. when
  opened in Android Studio.

## Running

```bash
# Rules engine tests
./gradlew :core:test

# Play a match against the AI in the terminal
./gradlew :core:run --console=plain -q
```

Open the project root in Android Studio (with an Android SDK installed) to
build and run the `android` app module.

## Status

Early prototype: core rules engine and CLI are playable end-to-end. The
Android UI is a functional but unstyled scaffold — no real card art, sound,
or persistence yet. Not yet monetization-ready (no ads/IAP wiring).

## Distribution notes for the Iranian market

- Target Cafe Bazaar and Myket, not Google Play/App Store — Iranian users
  can't pay through either due to sanctions.
- Any IAP needs a domestic payment gateway (Shaparak-rail, e.g. ZarinPal/
  IDPay), not Google Play Billing or Apple Pay.
- Keep the game's name, card names, and art fully original — do not reuse
  CD Projekt Red's *Gwent* branding, card names, or artwork.
