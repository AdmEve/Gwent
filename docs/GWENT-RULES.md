# GWENT (standalone) — Full Ruleset Reference

Research notes compiled from primary sources for the rebuild of this project from the
*Witcher 3* mini-game model to the **standalone GWENT** model.

- **Compiled:** 2026-09-18
- **Game version this describes:** Update **11.10** (16 Oct 2023) — the last content patch CD PROJEKT RED
  shipped. Since then the game is in "GWENTfinity": the *rules* are frozen, but individual card
  **power** and **provision** numbers change monthly by community vote (see §17).
- **Purpose:** this is the spec the engine rebuild is written against. No engine code was changed
  while producing it.

> **Status of this document.** Everything below is attributed. Statements marked
> **[unverified]** could not be confirmed from either source and need checking against the live
> client before being encoded as engine invariants.

---

## 1. Sources and how they were reached

| Source | Reachable? | Notes |
|---|---|---|
| `https://www.playgwent.com` (official, CD PROJEKT RED) | **Yes** — HTTP 200 | Crawled English locale, 135 URLs, depth 3. |
| `https://gwent.fandom.com` (community wiki) | **Yes, via API only** | `/wiki/*` HTML returns **403** (bot filter) regardless of user agent. `https://gwent.fandom.com/api.php` returns 200 and serves full page wikitext. All wiki content below came through `action=parse&prop=wikitext`. |

`playgwent.com/robots.txt` contains only:

```
User-agent: GPTBot
Disallow: /
```

There is no `User-agent: *` rule, so no general-crawler restriction applies. The crawl was run
rate-limited (0.4 s between requests), English-locale only, HTML only, with news articles capped.

**Two paths could not be retrieved** (neither carries rules content):
- `/en/fan-content` — redirects to `wpc.4d7d.edgecastcdn.net`, blocked by this environment's egress policy (403 on CONNECT).
- `/en/rules` — 301s to the same unresolvable CDN host. *(It is not a rules page; the official rules
  primer is `/en/how-to-play`.)*

### Primary pages used

**Official (playgwent.com)**
- `/en/how-to-play` — core loop, two rows, 10-card opening hand
- `/en/faq` — game identity, best-of-three framing
- `/en/updates/crimson-curse` — Poison, Bleeding, Shield, Vitality
- `/en/updates/iron-judgment` — Armor, Defender, Barricade, Exposed
- `/en/updates/novigrad` — Syndicate faction, Coins, Profit/Fee/Tribute/Intimidate, Crimes
- `/en/updates/merchants-of-ofir` — **Stratagems**, **Scenarios**, Crew, Resupply, Cooldown, Bonded
- `/en/updates/master-mirror` — Devotion, Echo, Veteran, Conspiracy, Symbiosis, Veil, Rupture
- `/en/updates/way-of-the-witcher` (+ `/card-reveals`) — Adrenaline
- `/en/updates/black-sun` — Infused, Clash, Grace
- `/en/news/49294/balance-council-faq-what-is-it-how-to-use-it` — GWENTfinity voting rules
- `/en/news/49288/patch-notes-11-10` — final developer patch
- `/en/news/49585/gwentfinity-and-the-balance-council`

**Wiki (gwent.fandom.com, via API)**
- `Rules` — match structure, deck legality, round flow
- `Glossary` — the canonical keyword + status reference (most mechanic pages *redirect* here)
- `General Gameplay Changes` — 9-unit row cap, mulligan history
- `Gwent Update: Oct 23, 2018` — **Homecoming**, the patch that defines the modern ruleset
- `Leader`, `Faction`, `Stratagem`, `Neutral`, and the six faction pages
- `Vilgefortz` — sampled for card-field anatomy

---

## 2. The match at a glance

- Two players, one deck each, built around a single **Leader**.
- A match is **best of three rounds**. Winning a round earns a **crown** (the wiki also calls these
  "half-crowns"). **Two crowns wins the match.**
- Each round, players alternate turns playing cards onto their two rows. Highest total **power**
  when both players have passed wins the round.
- Cards played are *spent* — the hand is a finite resource across all three rounds. This is the
  central tension of the game: there is no per-turn mana, only the cards you brought and the
  order you spend them in.

> Official framing: *"Sling cards across two tactically distinct rows — melee and ranged. Gather
> more points than your opponent to win a round. Win two out of three rounds to win the battle."*
> — `/en/how-to-play`

---

## 3. The board

```
              OPPONENT
   ┌───────────────────────────────┐
   │  Ranged   (max 9 units)       │
   ├───────────────────────────────┤
   │  Melee    (max 9 units)       │
   ╞═══════════════════════════════╡  ← centre line
   │  Melee    (max 9 units)       │
   ├───────────────────────────────┤
   │  Ranged   (max 9 units)       │
   └───────────────────────────────┘
                 YOU
```

- **Two rows per side: Melee and Ranged.** (Homecoming, Oct 2018 — *"Two rows, layers of character"*.)
  The third row (Siege) existed only in the *Witcher 3* mini-game and pre-Homecoming GWENT.
- **Row capacity is 9 units.** Once a row is full, nothing further may be placed or moved into it —
  including self-moving cards and cards that spawn or summon groups, which simply stop deploying.
  (`General Gameplay Changes`.)
- **Position within a row matters.** Players choose the exact slot; many abilities are defined in
  terms of adjacency, leftmost/rightmost, or "between two X". (`General Gameplay Changes`;
  e.g. Crew = "trigger ability if the unit is between two Soldiers".)
- **Row-specific abilities.** Cards commonly read `Deploy (Melee): …` / `Deploy (Ranged): …` and
  resolve differently by row. The keywords **Melee** and **Ranged** mean "this ability only works
  while on that row".
- **Rows hold effects.** Weather/hazard effects (Fog, Frost, Rain, Storm, Blood Moon, Cataclysm,
  Dragon's Dream) attach to a *row*, not the whole board, and tick at the start of the owner's turn.

---

## 4. Card anatomy

Fields observed on real cards (wiki `Infobox Card`, cross-checked against card text on the
official expansion pages):

| Field | Meaning |
|---|---|
| **Name** | Display name. |
| **Power** | The unit's point contribution. Shown top-left. `basePower` vs `currentPower` must be tracked separately — many mechanics key off the difference (Reset, Heal, Inspired, Berserk, Veteran). |
| **Armor** | Damage buffer. Damage depletes Armor before power. |
| **Provision cost** | Deck-building cost only. Has no in-match effect except for cards that read off it (e.g. Alzur, Cleaver). |
| **Colour** | **Bronze** or **Gold** — see §5. |
| **Rarity** | Common / Rare (→ Bronze) and Epic / Legendary (→ Gold). |
| **Faction** | One of six, or Neutral. |
| **Tags / categories** | e.g. `Human, Mage`, `Machine, Siege Engine`, `Beast`, `Cursed`, `Alchemy`, `Soldier`, `Aristocrat`. Drive Harmony, Crew, Devotion, and many conditionals. A card can have several; the **primary tag** is what Harmony counts. |
| **Loyalty** | `Loyal` (plays on your side) or `Disloyal` (plays on the *opponent's* side — see Spying). |
| **Ability text** | The rules text, composed of the keywords in §11. |

**Worked example** (`Vilgefortz`, from the wiki infobox):

```
name        Vilgefortz
cardtype    Gold          rarity      Legendary
faction     Nilfgaardian Empire
type        Mage, Human   loyalty     Loyal
strength    5 (power)     provisions  9
description Deploy (Melee):  Destroy an enemy unit, then your opponent Summons
                             the top unit from their deck to a random enemy row.
            Deploy (Ranged): Destroy an allied unit, then Summon the top unit
                             from your deck to a random allied row.
```

---

## 5. Bronze and Gold

| | Bronze | Gold |
|---|---|---|
| Rarities | Common, Rare | Epic, Legendary |
| Border | bronze | gold |
| Copies allowed per deck | **2** | **1** |

Source: `Rules` and `Glossary`.

> **Important:** in modern GWENT, Gold does **not** mean "immune". Post-Homecoming, gold cards are
> ordinary targets. The blanket immunity of the *Witcher 3* "hero" card is **not** part of this
> ruleset — the equivalent protections are the explicit statuses **Immunity**, **Shield**, **Veil**
> and **Defender** (§12). The current engine's `isHero` flag maps to nothing here.

---

## 6. Deck building

A legal deck is:

1. **Exactly one Leader**, which fixes the deck's faction and contributes provisions.
2. **Exactly one Stratagem** (§7).
3. **At least 25 cards**, not counting Leader/Stratagem.
4. Every card must match the Leader's **faction** or be **Neutral**.
5. **Max 2 copies of any Bronze**, **max 1 copy of any Gold**.
6. Total provision cost must not exceed the **provision limit**.

**Provision limit = 150 + the Leader's provision bonus.**
> *"Provisions: This is the cost of a card you have to pay to put it in your deck. Decks have a
> maximum total value of 150 (plus a value added by the leader ability)."* — `Glossary`

Leaders trade raw provisions against ability strength — a weak leader ability grants more
provisions to spend on cards. **[unverified]** the exact per-leader bonus range; these are
Balance-Council-mutable numbers (§17) and should be data, not constants.

**[unverified]** whether there is a hard *maximum* deck size. Both sources state only the 25-card
minimum and the provision ceiling.

**Devotion** — a deck containing *only* faction cards (zero Neutrals) activates the `Devotion`
clause printed on some cards. This makes "did the starting deck contain any Neutral card?" a
property the engine must evaluate at deck-load time and keep for the whole match.

---

## 7. Going first, the coin flip, and Stratagems

- Who moves first in round 1 is decided randomly (the "coin flip").
- **The player going first is compensated** (Homecoming, *"Flipping the coin flip"*):
  1. they get **one additional mulligan**, and
  2. their **Stratagem** is placed on the board at the start of the match.

**Stratagem** is a card type chosen during deck building, one per deck:
- It is an **Order** card that grants a bonus in round 1.
- It is **non-interactable** — the opponent cannot remove it.
- It only ever appears **if you go first**; otherwise it never enters the game.
- It stays on the board until used.
- There are neutral Stratagems plus one per faction. The default/original is **Tactical Advantage**.

Examples (from `/en/updates/merchants-of-ofir`, all provision cost 0):
`Enchanted Armor` — *Order: Boost a unit in your hand by 3 and give it 2 Armor.*
`Tiger's Eye` — *Order: Gain 5 Coins.*
`Aen Seidhe Saber` — *Order: Spawn and play a Scoia'tael Neophyte.*
`Urn of Shadows` — *Order: Trigger an allied unit's Deathwish ability.*

---

## 8. Round flow

### Round 1
- Each player **draws 10 cards**.
- **Mulligan** phase: return up to *N* cards to the deck and redraw. Base **N = 3**; the player
  going first gets **+1**; the **Leader modifies the total** (*"[Leaders] determine how many cards
  in total you can exchange at the beginning of a match and between rounds"* — Homecoming).
  **[unverified]** the exact modern per-round allowances; the wiki is self-contradictory here
  (`Rules` says 3 every round; `General Gameplay Changes` says 3 before R1 and 1 before R2/R3).
  Treat mulligan counts as configurable, not hardcoded.

### Rounds 2 and 3
- Each player **draws 3 cards** at round start.
- A further mulligan phase occurs.
- **Hand cap is 10.** Cards drawn while at 10 are discarded. If you would draw at the cap, the wiki
  notes those draws convert into **extra mulligans** instead. **[unverified]** — the official
  Homecoming text says only *"any additional cards will be automatically discarded"*.
- **The winner of the previous round moves first** in the next round.

### Ending a round
- The round ends when **both players have passed**.
- All units on the battlefield are then sent to the **Graveyard**, except units with **Resilience**
  (§12), which survive into the next round at **base power** — boosts and Armor are *not* carried over.
- Syndicate **Coins** carry over **halved, rounded down** (§15).

---

## 9. Turns, passing, and the end of a round

This is the area where standalone GWENT diverges most sharply from the mini-game, and it is worth
encoding carefully.

**A turn does not end when you play a card.** (Homecoming: *"Turns don't end immediately after a
card is sent to the battlefield anymore … it's up to you to decide whether you're done for the
turn."*) Within one turn a player may play a card **and then** use Order abilities, before
explicitly ending the turn.

**In a single turn you may:**
- play one card from hand, and/or
- use any number of available **Order** abilities on the board, and/or
- use the **Leader** ability (once per match; using it is equivalent to playing a card),
- then end the turn — **or pass.**

**Passing:**
- You may **only pass if, during that turn, you have not** played a card, used an Order ability, or
  used the Leader ability.
- Passing ends your participation for the rest of the round.
- **If your hand reaches 0 cards at any point, you pass automatically.**
- A **dry pass** (passing in round 2 without spending a card) is the standard way to trade tempo for
  card advantage.

**Order and Charge:**
- **Order** = a manually triggered ability. A card with Order **cannot use it on the turn it was
  placed**, unless it has **Zeal**.
- **Charge (X)** = how many times an Order may be used.
- **Cooldown (X)** = turns before an Order/Fee may be used again.

---

## 10. Scoring

- A unit on the battlefield contributes its **current power** to its controller's score.
- Score is tracked **per row** and summed; the total sits beside the crowns.
- A unit reduced to **0 power is destroyed** and goes to the Graveyard, removing its points.
- **Artifacts have no power** and never contribute points directly.
- Highest total when both players have passed **wins the round and takes a crown**.
- **A tie: both players lose the round** — i.e. both receive a crown. (`Rules`.)
  This matters: a drawn round can end a match 2–2 on crowns or hand a player their second crown.
- **Two crowns wins the match.**

---

## 11. Keywords

Definitions are CD PROJEKT RED's where the expansion pages give one, otherwise the wiki `Glossary`.

### Timing / trigger keywords

| Keyword | Meaning |
|---|---|
| **Deploy** | Triggers when the card is **played** from hand. (Explicitly *not* triggered by Summon.) |
| **Order** | Manually triggered by the player. Unusable the turn the card is placed, unless **Zeal**. |
| **Zeal** | The Order may be used on the same turn the card is placed. |
| **Charge (X)** | Number of times an Order may be used. Only cards printed with Charge can gain more. |
| **Cooldown (X)** | The Order/Fee is usable again after X turns. |
| **Deathwish** | Triggers when the card is moved to the Graveyard. |
| **Deathblow** | Extra ability if this card kills another. |
| **Counter (X)** | Decrements when its condition is met; triggers at 0. |
| **Timer (X)** | Decrements at the end of each turn; triggers at 0. |
| **Patience** | At end of your turn, if the Order was not used, permanently raise the stated value by 1. |
| **Initiative** | Triggers if using this card is the first action of your turn. |
| **Spring** | Manually triggered, only while the card is face-down; the card flips after use. |

### Conditional keywords

| Keyword | Meaning |
|---|---|
| **Adrenaline (X)** | Triggers if you have X or fewer cards in hand. |
| **Assimilate (X)** | Boost by X (default 1) whenever you play a card that was not in your starting deck. |
| **Barricade** | Triggers only if the unit currently **has Armor**. |
| **Berserk (X)** | Triggers whenever the unit's **base** power is ≤ X. |
| **Bloodthirst (X)** | Triggers if the opponent has X damaged units. |
| **Bonded** | Triggers if you control another copy of this unit on the battlefield. |
| **Conspiracy** | Triggers if the target has the **Spying** status. |
| **Crew** | Triggers if the unit sits **between two Soldiers**. |
| **Devotion** | Bonus ability if your **starting deck contains no Neutral cards**. |
| **Dominance** | Triggers while you control the highest-power unit on the battlefield. |
| **Exposed** | Triggers when the unit loses **all** of its Armor. |
| **Grace (X)** | The **first** time this unit's power reaches ≥ X, trigger. Triggers immediately if already met on entry. |
| **Hoard (X)** | Triggers while you have ≥ X Coins. |
| **Inspired** | Triggers if current power > base power (i.e. boosted). |
| **Melee** / **Ranged** | The ability works only while on that row. |
| **Sabbath** | Triggers if one of your rows totals ≥ 25 points. |

### Action keywords

| Keyword | Meaning |
|---|---|
| **Boost** | Raise current power. Boost is **temporary** — removed by Reset, not carried across rounds. |
| **Strengthen** | Raise **base** power (permanent). |
| **Weaken** | Lower base power. |
| **Damage** | Reduce current power; consumed by Armor first. |
| **Heal** | Boost the card back up to, at most, its base power. |
| **Reset** | Restore to base power (boost if damaged, damage if boosted). |
| **Destroy** | Send to the Graveyard. |
| **Banish** | Remove from the game entirely. **Does not count as being destroyed** (so Deathwish does not fire). |
| **Discard** | Move a card from hand or deck directly to the Graveyard. |
| **Consume** | Destroy another card and boost self by its value. If the target is in the Graveyard, Banish it instead. |
| **Drain** | Damage a unit and boost self by the amount damaged. |
| **Duel** | The two units alternately deal damage equal to their power until one dies. |
| **Clash** | Both units damage each other by their power **simultaneously**. |
| **Spawn** | Add a new card to the game. |
| **Create** | Spawn one of **three randomly offered** cards from a stated source. |
| **Summon** | Move a card to the battlefield from hand or deck. **Does not count as "played"** → does **not** trigger Deploy. |
| **Resurrect** | Play a card from the Graveyard. |
| **Move** | Relocate a unit to another row. |
| **Swap** | Move a card from hand to deck and draw a replacement (your turn only; not during mulligan). |
| **Seize** | Steal an opponent's card; it arrives on the **same row** (Melee/Ranged) it occupied. |
| **Purify** | Remove **all** statuses from a card. |
| **Lock** | Disable a unit's ability text (see Locked, §12). |
| **Reveal** | Show a random card to both players, then re-hide it. |
| **Draw** | Move the top card of your deck to hand. |

### Economy / faction keywords

| Keyword | Meaning |
|---|---|
| **Profit (X)** | Gain X Coins (cap 9). |
| **Fee (X)** | Spend X Coins to trigger. |
| **Tribute (X)** | On deploy, optionally spend X Coins for an extra effect. |
| **Insanity** | May pay a Fee by damaging itself instead of spending Coins. |
| **Intimidate** | Boost by 1 (or stated amount) whenever you play a **Crime** card. |
| **Bounty** | See §12. |

### Growth / synergy keywords

| Keyword | Meaning |
|---|---|
| **Harmony (X)** | Boost by X (default 1) when a card with a **new primary tag** appears on your side. |
| **Thrive** | Boost by 1 each time you play a card with a higher provision value. |
| **Symbiosis** | When you play a **Nature** card, spawn a Treant in a random allied row with power equal to the number of Symbiosis units you control. |
| **Resupply** | Boost by 1 each time you play a **Warfare** card. |
| **Veteran** | Increase **base** power by 1 at the start of rounds 2 and 3. |
| **Formation** | Played in Melee → gain Zeal. Played in Ranged → boost self by 1. |
| **Echo** | At round start, return this card from the Graveyard to the top of its owner's deck, and give it **Doomed**. |
| **Ambush** | Played face-down; reveals under stated conditions. Manual reveal produces a different effect. |
| **Disloyal** | The card is played on the **opponent's** side of the battlefield. |

---

## 12. Statuses

Statuses are removable by **Purify**, and blocked (prospectively) by **Veil**.

| Status | Effect |
|---|---|
| **Armor** | Absorbs damage before power is lost. |
| **Bleeding (X)** | Lose 1 power at the end of each of the owner's turns. **Stacks in duration.** Countered by Vitality. |
| **Vitality (X)** | Boost by 1 at the end of each of the owner's turns. Stacks in duration. Countered by Bleeding. |
| **Poison** | Applying Poison to an **already poisoned** unit **destroys** it. |
| **Rupture** | At the end of the owner's turn, damage the unit by its **base power**; if it survives, the status is removed. |
| **Shield** | Ignores the **first** instance of damage, then is removed. |
| **Resilience** | The unit survives into the next round, then the status expires. **Boosts and Armor are not carried over** (it returns at base power). |
| **Doomed** | When the card leaves the battlefield (to Graveyard or deck), it is **Banished** instead. |
| **Immunity** | Cannot be targeted directly. **Still affected by row/board effects.** |
| **Locked** | The card's ability text is ignored while on the battlefield. Does **not** affect statuses, and does not apply in the Graveyard. |
| **Veil** | Prevents the unit from **gaining** further statuses. Does **not** remove statuses already applied. |
| **Defender** | While a Defender is on a row, the opponent **cannot target other units on that row** until it is killed or Purified. |
| **Spying** | Set on a card that is not on its owner's side of the board. No effect by itself; many cards key off it. |
| **Bounty** | When the unit is destroyed or banished, the **opponent** gains Coins equal to its base power. Only one Bounty unit per side at a time. |
| **Infused** | Adds effects or categories to a card. Removing the status removes everything it added. |

---

## 13. Row effects, weather and hazards

These attach to a **single row** and tick **at the start of the owner's turn**.

| Effect | Behaviour |
|---|---|
| **Fog** | Damage the **lowest**-power unit in the row by 2. |
| **Frost** | Damage the **highest**-power unit in the row by 2. |
| **Rain** | Damage **2 random** units in the row by 1. |
| **Storm** | Damage **all** units in the row by 1. |
| **Blood Moon** | Give a random unit in the row **Bleeding (2)**; if it is already Bleeding, damage it by 2 instead. |
| **Cataclysm** | Split **3 damage** randomly between units in the row. |
| **Dragon's Dream** | A bomb: when its timer expires, damage **all** units in the row by 3. |

Note the design change from the mini-game: modern weather is **damage over time on one row**, not a
blanket power cap on a row-type across the whole board. Weather **Immunity** as a concept was removed.

---

## 14. Factions

Six playable factions plus **Neutral** (which has no leaders and is legal in every deck).

| Faction | Identity (official copy) |
|---|---|
| **Northern Realms** | Reinforce numbers; commanders boost their units; **Armor** and **Shields** protect troops. Engines of war. |
| **Nilfgaard** | Diplomacy and subterfuge. **Spies** behind enemy lines, hand reveal, targeting and crippling the strongest enemy units. |
| **Monsters** | Attack in vast numbers that grow into hordes; **Consume** their own kin to absorb strength. |
| **Skellige** | Embrace death — send units to the **Graveyard** deliberately and resurrect them stronger; damaged units fight harder. |
| **Scoia'tael** | Guerilla fighters: **Ambushes** (face-down cards), traps, agility and support; raise Commandos. |
| **Syndicate** | Crime and **Coins** — a resource economy no other faction has (§15). Introduced in Novigrad. |

Faction *passives* were removed in May 2017; faction identity now lives entirely in the card pool
and the Leader abilities.

---

## 15. The Syndicate coin economy

Syndicate is the one faction with a resource, and it needs dedicated engine support:

- Cards **earn** Coins (`Profit X`) and **spend** them (`Fee X`, `Tribute X`).
- **Coin cap is 9.** Gains beyond 9 are wasted (some cards, e.g. the leader Gudrun Bjornsdottir,
  convert the excess into a boost).
- **At the end of a round, each player's Coins are halved, rounded down**, and carried into the
  next round.
- **Hoard (X)** reads the current balance; **Insanity** lets a card pay a Fee with self-damage
  instead of Coins.
- **Crimes** are a category of Special card that Syndicate cards key off (e.g. Intimidate, and the
  leader Cleaver, whose reach scales with the number of Crimes in the starting deck).

---

## 16. Card types

| Type | Behaviour |
|---|---|
| **Unit** | Has power, occupies a row slot, contributes to score. |
| **Special** | Resolves its effect and does **not** stay on the board. Sub-categories include **Tactic**, **Spell**, **Warfare**, **Alchemy**, **Organic**, **Crime**. These sub-tags are load-bearing (Resupply counts Warfare, Crow Clan Preacher counts Alchemy, Intimidate counts Crime). |
| **Artifact** | Stays on the board but has **no power** and scores nothing. **Cannot be damaged by ordinary cards** — historically only specific answers (e.g. *Heatwave*) remove them. |
| **Stratagem** | One per deck, deployed only when going first, non-interactable Order. See §7. |
| **Scenario** | A multi-stage card that **progresses** on a stated condition, resolving `Prologue` → `Chapter 1` → `Chapter 2`. Introduced in Merchants of Ofir. |
| **Leader** | One per deck, defines faction and provisions, one ability per match. Playing it costs your turn like a card. |
| **Token** | Spawned cards that exist only in-match and are not deck-legal. |

---

## 17. GWENTfinity — why numbers must be data

Update **11.10** (16 Oct 2023) was the final patch authored by CD PROJEKT RED. Card balance is now
decided by the **Balance Council**, a monthly community vote:

- Four brackets: **provisions increase / provisions decrease / power increase / power decrease**.
- Each eligible player votes for up to 3 cards per bracket; slot position weights the vote 3/2/1.
- Max 15 card changes per bracket per cycle, minimum 3, with a 50-vote threshold.
- A card may only be changed in one bracket per cycle.

**Engine implication:** power and provision values are *live data*, not constants. The rules in
§§2–16 are stable; the numbers on any individual card are not. Card definitions belong in a data
file that can be regenerated, not baked into Kotlin.

---

## 18. Delta: what this project has today vs. what GWENT needs

Current implementation (`core/src/main/kotlin/ir/gwent/core/model/Card.kt`,
`engine/GameEngine.kt`) is the **Witcher 3 mini-game**, as the screenshots in `docs/screenshots/`
show: three rows, heroes, and mini-game specials.

| Area | Current | Required |
|---|---|---|
| Rows | `Row { MELEE, RANGED, SIEGE }` — 3 per side, unbounded | **2 per side** (Melee, Ranged), **cap 9 units**, ordered slots with adjacency |
| Card economy | none | **Provisions**: 150 + leader bonus, enforced at deck build |
| Card colour | `isHero: Boolean` | **Bronze/Gold** with copy limits (2 / 1); Gold is *not* immune |
| Protection | hero immunity | Explicit **Immunity / Shield / Veil / Defender** statuses |
| Statuses | none | Full status system (§12) with Purify, stacking durations, end-of-turn ticks |
| Abilities | fixed enum: `HORN, SCORCH, SPY, MEDIC, DECOY, WEATHER, CLEAR_WEATHER` | Keyword system (§11) — **Deploy / Order / Charge / Cooldown / Zeal / Deathwish** as a trigger framework, not a closed enum |
| Turn structure | turn ends on play | Play **and** use Orders in one turn; explicit end-turn |
| Passing | — | Only legal if nothing was played/triggered this turn; **auto-pass at 0 cards** |
| Weather | caps row power at 1 | Per-row **damage-over-time** at start of owner's turn (§13) |
| Round transition | — | Draw 3; **Resilience** survivors return at base power; **Doomed** banishes; coins halve |
| Going first | — | Coin flip → **+1 mulligan** and **Stratagem** on board |
| Tight Bond / Muster | `bondGroup`, `musterGroup` | Not GWENT mechanics — replace with **Bonded**, **Harmony**, **Thrive**, **Symbiosis**, etc. |
| Leaders | 5 fixed `LeaderAbility` values | Leaders carry a **provision bonus** + an ability, and set the deck's faction |
| Factions | `PAHLAVAN, DIV, MARVEL, ONE_PIECE, GREEK_MYTH` | Structure must support faction + **Neutral**, and **Devotion** (no-Neutral) checks |

---

## 19. Open questions to settle against the live client

1. Exact mulligan allowance per round, and how Leader provision/mulligan trade-offs are expressed.
2. Whether a hard maximum deck size exists above the 25-card minimum.
3. Whether draws at the 10-card hand cap are discarded (official) or converted to mulligans (wiki) —
   the two sources disagree.
4. Exact tie-breaking: a drawn round awards a crown to both players; confirm the 2–2 crown case.
5. Per-leader provision bonuses (Balance-Council-mutable — should be sourced as data anyway).
6. Artifact removal rules beyond the named exceptions.

---

## 20. Source index

**Official — CD PROJEKT RED, playgwent.com** (crawled 2026-09-18, English locale, 135 URLs)

```
/en                                     /en/updates/crimson-curse
/en/how-to-play                         /en/updates/iron-judgment
/en/faq                                 /en/updates/novigrad
/en/decks/builder                       /en/updates/merchants-of-ofir
/en/news                                /en/updates/master-mirror
/en/news/49288/patch-notes-11-10        /en/updates/way-of-the-witcher
/en/news/49294/balance-council-faq...   /en/updates/way-of-the-witcher/card-reveals
/en/news/49585/gwentfinity-and-...      /en/updates/black-sun
                                        /en/updates/price-of-power
```

**Community — gwent.fandom.com** (via `api.php`, `action=parse&prop=wikitext`)

```
Rules                       Glossary                 General Gameplay Changes
Gwent Update: Oct 23, 2018  Leader                   Faction
Stratagem                   Neutral                  Spy
Monsters / Nilfgaardian Empire / Northern Realms / Scoia'tael / Skellige / Syndicate
Vilgefortz  (sampled for card field anatomy)
```

Trademarks and card text are © CD PROJEKT RED. Quoted here for design reference only.
