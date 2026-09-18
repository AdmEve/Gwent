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

Three rows per side (Melee/Ranged/Siege). Highest total power wins the round.
Each army starts with two gems: losing a round costs a gem, a tied round costs
both players one, and running out of gems loses the match — so a match runs at
most three rounds and can legitimately end in a draw.

A coin toss decides who opens. Each player draws ten cards at the start and
**that hand has to last the whole match** — there is no draw between rounds, so
conceding a round you cannot win and saving the cards is a real play. The board
is discarded at the end of every round. Card mechanics:

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
- **Tight Bond** — copies of the same unit in one row multiply each other:
  two are worth double each, three are worth triple each.
- **Muster** — playing one card calls every other copy of its group out of
  your deck and hand at once.

Before the match each player may swap up to two cards. Each army also has a
**leader** with a once-per-match ability (it costs your turn, like playing a
card) and a passive **trait**: draw on a won round, hold a unit over between
rounds, win level rounds, always open the match, or recover a card from the
graveyard in round three.

## Project structure

- `core/` — pure-Kotlin game rules engine (cards, board, rounds, scoring,
  abilities, deck/discard/draw) plus a heuristic AI opponent and a terminal
  CLI to play against it. No Android dependency; builds and tests with plain
  Gradle anywhere a JDK is available. Its test suite plays ~1,500 complete
  simulated matches across every faction pairing, asserting that the AI never
  picks a move the engine rejects — the UI drives the AI in a loop on the main
  thread, so such a move is a frozen app.
- `android/` — Jetpack Compose Android app (the actual APK target) that
  wraps `core` in a real UI: faction picker, opening-hand redraw, row-based
  board with weather/power/ability indicators, hand, leader ability, and
  target-selection flows for Decoy and Medic. Requires the Android SDK to configure, so it's only included in the
  Gradle build when one is detected (`ANDROID_HOME` / `ANDROID_SDK_ROOT` set,
  or a `local.properties` file present) — e.g. when opened in Android Studio.

## Card art

Card illustrations are looked up by convention, so art can be added without
touching code: a card with id `mar-iron-man` uses the drawable
`card_mar_iron_man`. Drop files into `android/src/main/res/drawable/` (webp or
png, portrait, roughly 2:3) and they replace the placeholder monogram panel
automatically. Anything without art keeps the placeholder.

## Debugging without a device

The app stores the stack trace of any uncaught exception and shows it on the
next launch, so a crash can be read and reported rather than just vanishing.
Compose smoke tests run under Robolectric in CI (`:android:testDebugUnitTest`)
to catch a screen that throws or a turn loop that never terminates.

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
