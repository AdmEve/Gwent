# Gwent-verse (working title)

A tactical row-based card battler for mobile (Android APK first, distributed via
Cafe Bazaar/Myket for the Iranian market), mechanically inspired by the
row/pass/round-concede tension popularized by *Gwent* — the mechanic itself
isn't owned by anyone — with original game name, UI, card layout, and text.

Five factions so far, mixing original IP with licensed and public-domain
character rosters:

- **Pahlavans** / **Div** — original characters from Ferdowsi's *Shahnameh*
  (Persian mythology); fully original name/art/text, no licensing needed.
- **Marvel** — used under this project's confidential Marvel licensing
  clearance.
- **One Piece** — used under this project's confidential One Piece licensing
  clearance.
- **Greek Myth** — public-domain mythology, no licensing needed.

Card art is hand-drawn in-house even where the character license covers
official art assets.

> Licensing note: which characters/franchises are actually cleared is a
> confidential internal list. Names used in `core/` reflect what's been
> confirmed so far; do not add new franchises or characters to the card
> database without checking first.

## Gameplay

Best-of-three rounds, three rows per side (Melee/Ranged/Siege), highest total
power wins each round, two round wins (or best-of-3 after round 3) wins the
match. Each match deals a shuffled hand from a themed deck (~20 cards);
players draw more cards between rounds, and the board is discarded at the end
of every round — like the game this is inspired by. Card mechanics:

- **Hero** — immune to Scorch, Weather, and Decoy.
- **Horn** — doubles every non-hero unit in its row (including itself).
- **Scorch** — destroys the board-wide highest-power non-hero unit(s).
- **Weather** — caps a row's non-hero units at power 1 until cleared or the
  round ends; affects both players' copies of that row.
- **Clear Weather** — removes all active weather.
- **Decoy** — swaps a friendly non-hero unit back to hand, replacing it on
  the board with a 0-power dummy.
- **Medic** — after entering play, may return one card from your discard
  pile to hand.
- **Spy** — enters play on the *opponent's* board/row instead of your own;
  you draw two cards from your own deck as compensation.

## Project structure

- `core/` — pure-Kotlin game rules engine (cards, board, rounds, scoring,
  abilities, deck/discard/draw) plus a heuristic AI opponent and a terminal
  CLI to play against it. No Android dependency; builds and tests with plain
  Gradle anywhere a JDK is available.
- `android/` — Jetpack Compose Android app (the actual APK target) that
  wraps `core` in a real UI: faction picker, row-based board with weather/
  power/ability indicators, hand, and target-selection flows for Decoy and
  Medic. Requires the Android SDK to configure, so it's only included in the
  Gradle build when one is detected (`ANDROID_HOME` / `ANDROID_SDK_ROOT` set,
  or a `local.properties` file present) — e.g. when opened in Android Studio.

## Running

```bash
# Rules engine tests
./gradlew :core:test

# Play a match against the AI in the terminal
./gradlew :core:run --console=plain -q
```

Open the project root in Android Studio (with an Android SDK installed) to
build and run the `android` app module — this is the primary target (phone
APK).

## Status

Playable prototype: core rules engine (with weather/decoy/medic/spy/horn/
scorch, draw phase, per-round board clearing) and CLI are complete and
tested. The Android app has a full board UI (faction picker, row layout,
card visuals, target-selection dialogs) but placeholder card art (colored
frames, no illustrations yet) and no sound/persistence. Not yet
monetization-ready (no ads/IAP wiring). Only 4 of the eventual character
rosters are filled in — more characters/franchises to be added once cleared.

## Distribution notes for the Iranian market

- Target Cafe Bazaar and Myket, not Google Play/App Store — Iranian users
  can't pay through either due to sanctions.
- Any IAP needs a domestic payment gateway (Shaparak-rail, e.g. ZarinPal/
  IDPay), not Google Play Billing or Apple Pay.
- Keep the game's own name, UI, and card layout/text original — don't reuse
  CD Projekt Red's *Gwent* branding or card text; character names/art follow
  this project's own licensing clearances (see the note above).
