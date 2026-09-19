package ir.gwent.core.model

/**
 * The card pool.
 *
 * Provision costs are the balancing lever: a deck may hold 25+ cards but only 150 + leader
 * provisions' worth, so a gold bomb is paid for with weaker bronzes elsewhere. These numbers are
 * the kind the live game re-balances monthly, so they live here as data.
 */
object CardDatabase {

    private fun unit(
        id: String,
        name: String,
        faction: Faction,
        power: Int,
        provisions: Int,
        color: CardColor = CardColor.BRONZE,
        armor: Int = 0,
        tags: Set<Tag> = emptySet(),
        abilities: List<Ability> = emptyList(),
        text: String = "",
    ) = Card(id, name, faction, CardType.UNIT, color, provisions, power, armor, tags, abilities, text = text)

    private fun special(
        id: String,
        name: String,
        faction: Faction,
        provisions: Int,
        color: CardColor = CardColor.BRONZE,
        tags: Set<Tag> = emptySet(),
        effect: Effect,
        text: String = "",
    ) = Card(
        id, name, faction, CardType.SPECIAL, color, provisions, 0, 0, tags,
        listOf(Ability(Trigger.DEPLOY, effect)), text = text,
    )

    // ------------------------------------------------------------- neutral

    val NEUTRALS = listOf(
        unit("neu_militia", "Town Militia", Faction.NEUTRAL, 4, 4, tags = setOf(Tag.HUMAN, Tag.SOLDIER)),
        unit("neu_mercenary", "Hired Blade", Faction.NEUTRAL, 6, 6, tags = setOf(Tag.HUMAN, Tag.SOLDIER)),
        unit(
            "neu_medic", "Field Medic", Faction.NEUTRAL, 4, 6, tags = setOf(Tag.HUMAN),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Heal(3))),
            text = "Deploy: Heal an allied unit by 3.",
        ),
        unit(
            "neu_archer", "Crossbowman", Faction.NEUTRAL, 4, 5, tags = setOf(Tag.HUMAN, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(2), row = Row.RANGED)),
            text = "Deploy (Ranged): Damage an enemy unit by 2.",
        ),
        unit(
            "neu_shieldbearer", "Shieldbearer", Faction.NEUTRAL, 5, 6, armor = 2,
            tags = setOf(Tag.HUMAN, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.SHIELD))),
            text = "Deploy: Gain a Shield.",
        ),
        unit(
            "neu_champion", "Champion of Champions", Faction.NEUTRAL, 10, 11, color = CardColor.GOLD,
            tags = setOf(Tag.HUMAN, Tag.KNIGHT),
            abilities = listOf(Ability(Trigger.ORDER, Effect.Damage(4), charges = 2, cooldown = 1)),
            text = "Order: Damage an enemy unit by 4. Charges: 2.",
        ),
        special(
            "neu_scorch", "Scorch", Faction.NEUTRAL, 8, color = CardColor.GOLD, tags = setOf(Tag.SPELL),
            effect = Effect.Destroy, text = "Destroy the highest-power enemy unit.",
        ),
        special(
            "neu_alzurs_thunder", "Alzur's Thunder", Faction.NEUTRAL, 5, tags = setOf(Tag.SPELL),
            effect = Effect.Damage(5), text = "Damage an enemy unit by 5.",
        ),
        special(
            "neu_swallow", "Swallow", Faction.NEUTRAL, 5, tags = setOf(Tag.ALCHEMY),
            effect = Effect.Apply(Status.VITALITY, 3), text = "Give an allied unit Vitality (3).",
        ),
        unit("neu_scout", "Wandering Scout", Faction.NEUTRAL, 3, 4, tags = setOf(Tag.HUMAN)),
        unit("neu_bear", "Mountain Bear", Faction.NEUTRAL, 7, 8, tags = setOf(Tag.BEAST)),
        unit("neu_smith", "Master Smith", Faction.NEUTRAL, 4, 6, tags = setOf(Tag.HUMAN, Tag.DWARF),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.ARMOR))),
            text = "Deploy: Give an allied unit Armor."),
        unit("neu_witcher", "Path Witcher", Faction.NEUTRAL, 6, 8, tags = setOf(Tag.WITCHER),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(4)))),
        unit("neu_roach", "Roach", Faction.NEUTRAL, 5, 5, tags = setOf(Tag.BEAST)),
    )

    // ------------------------------------------------------------- factions

    val MONSTERS = listOf(
        unit("mon_ghoul", "Ghoul", Faction.MONSTERS, 4, 4, tags = setOf(Tag.NECROPHAGE)),
        unit("mon_harpy", "Harpy", Faction.MONSTERS, 3, 4, tags = setOf(Tag.BEAST)),
        unit(
            "mon_arachas", "Arachas", Faction.MONSTERS, 5, 6, armor = 1, tags = setOf(Tag.INSECTOID),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(2))),
            text = "Deploy: Damage an enemy unit by 2.",
        ),
        unit(
            "mon_werewolf", "Werewolf", Faction.MONSTERS, 7, 8, tags = setOf(Tag.CURSED, Tag.BEAST),
            abilities = listOf(Ability(Trigger.END_OF_TURN, Effect.Boost(1))),
            text = "At the end of your turn, boost self by 1.",
        ),
        unit(
            "mon_vampire", "Katakan", Faction.MONSTERS, 8, 10, color = CardColor.GOLD,
            tags = setOf(Tag.VAMPIRE),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.BLEEDING, 3))),
            text = "Deploy: Give an enemy unit Bleeding (3).",
        ),
        unit("mon_nekker", "Nekker", Faction.MONSTERS, 3, 4, tags = setOf(Tag.OGROID),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Summon("mon_nekker"))),
            text = "Deploy: Summon another Nekker from your deck."),
        unit("mon_foglet", "Foglet", Faction.MONSTERS, 4, 5, tags = setOf(Tag.NECROPHAGE),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(2), row = Row.RANGED)),
            text = "Deploy (Ranged): Damage an enemy unit by 2."),
        unit("mon_griffin", "Griffin", Faction.MONSTERS, 6, 7, tags = setOf(Tag.DRACONID),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Consume)),
            text = "Deploy: Consume an allied unit."),
        unit("mon_ekimmara", "Ekimmara", Faction.MONSTERS, 5, 8, tags = setOf(Tag.VAMPIRE),
            abilities = listOf(Ability(Trigger.END_OF_TURN, Effect.Consume)),
            text = "At the end of your turn, Consume a corpse."),
        unit("mon_fiend", "Fiend", Faction.MONSTERS, 9, 11, color = CardColor.GOLD, armor = 2,
            tags = setOf(Tag.RELICT), text = "A wall of horn and muscle."),
        special("mon_frost", "Biting Frost", Faction.MONSTERS, 6, tags = setOf(Tag.SPELL),
            effect = Effect.Damage(2), text = "Damage the highest enemy unit by 2."),
    )

    val NILFGAARD = listOf(
        unit("nil_soldier", "Nilfgaardian Soldier", Faction.NILFGAARD, 4, 4, tags = setOf(Tag.HUMAN, Tag.SOLDIER)),
        unit("nil_knight", "Impera Brigade", Faction.NILFGAARD, 6, 7, armor = 1, tags = setOf(Tag.HUMAN, Tag.SOLDIER)),
        unit(
            "nil_spy", "Imperial Informant", Faction.NILFGAARD, 5, 6, tags = setOf(Tag.HUMAN),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Draw(1))),
            text = "Deploy: Draw a card.",
        ),
        unit(
            "nil_assassin", "Cantarella", Faction.NILFGAARD, 6, 9, color = CardColor.GOLD,
            tags = setOf(Tag.HUMAN),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.POISON))),
            text = "Deploy: Poison an enemy unit.",
        ),
        special(
            "nil_bribery", "Imperial Diplomacy", Faction.NILFGAARD, 6, tags = setOf(Tag.TACTIC),
            effect = Effect.Draw(2), text = "Draw 2 cards.",
        ),
        unit("nil_archer", "Nauzicaa Sergeant", Faction.NILFGAARD, 4, 5,
            tags = setOf(Tag.HUMAN, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(3), row = Row.RANGED)),
            text = "Deploy (Ranged): Damage an enemy unit by 3."),
        unit("nil_agent", "Vattier de Rideaux", Faction.NILFGAARD, 6, 8,
            tags = setOf(Tag.HUMAN),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.LOCKED))),
            text = "Deploy: Lock an enemy unit."),
        unit("nil_knight2", "Magne Division", Faction.NILFGAARD, 5, 6, armor = 2,
            tags = setOf(Tag.HUMAN, Tag.SOLDIER), text = "Heavy, patient, thorough."),
        unit("nil_emissary", "Ffion var Gaernel", Faction.NILFGAARD, 7, 9, color = CardColor.GOLD,
            tags = setOf(Tag.HUMAN, Tag.KNIGHT),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.SHIELD))),
            text = "Deploy: Give an allied unit a Shield."),
        special("nil_seize", "Coup de Grace", Faction.NILFGAARD, 7, tags = setOf(Tag.TACTIC),
            effect = Effect.Destroy, text = "Destroy a damaged enemy unit."),
    )

    val NORTHERN_REALMS = listOf(
        unit("nor_infantry", "Temerian Infantry", Faction.NORTHERN_REALMS, 4, 4, armor = 1, tags = setOf(Tag.HUMAN, Tag.SOLDIER)),
        unit("nor_trebuchet", "Trebuchet", Faction.NORTHERN_REALMS, 3, 5, tags = setOf(Tag.MACHINE, Tag.SIEGE_ENGINE)),
        unit(
            "nor_knight", "Redanian Knight", Faction.NORTHERN_REALMS, 5, 6, armor = 2,
            tags = setOf(Tag.HUMAN, Tag.KNIGHT, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.END_OF_TURN, Effect.Boost(1))),
            text = "At the end of your turn, boost self by 1.",
        ),
        unit(
            "nor_ballista", "Siege Ballista", Faction.NORTHERN_REALMS, 4, 6,
            tags = setOf(Tag.MACHINE, Tag.SIEGE_ENGINE),
            abilities = listOf(Ability(Trigger.ORDER, Effect.Damage(2), row = Row.RANGED, charges = 3, cooldown = 1)),
            text = "Order (Ranged): Damage an enemy unit by 2. Charges: 3.",
        ),
        unit(
            "nor_vernon", "Vernon Roche", Faction.NORTHERN_REALMS, 7, 10, color = CardColor.GOLD,
            tags = setOf(Tag.HUMAN, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.SHIELD))),
            text = "Deploy: Give an allied unit a Shield.",
        ),
        unit("nor_pikeman", "Kaedweni Pikeman", Faction.NORTHERN_REALMS, 5, 5, armor = 1,
            tags = setOf(Tag.HUMAN, Tag.SOLDIER), text = "Hold the line."),
        unit("nor_priestess", "Priestess of Melitele", Faction.NORTHERN_REALMS, 3, 5,
            tags = setOf(Tag.HUMAN, Tag.MAGE),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Heal(4))),
            text = "Deploy: Heal an allied unit by 4."),
        unit("nor_reaver", "Reaver Scout", Faction.NORTHERN_REALMS, 4, 4,
            tags = setOf(Tag.HUMAN, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Move))),
        unit("nor_siege_tower", "Siege Tower", Faction.NORTHERN_REALMS, 6, 7,
            tags = setOf(Tag.MACHINE, Tag.SIEGE_ENGINE),
            abilities = listOf(Ability(Trigger.END_OF_TURN, Effect.Boost(1))),
            text = "At the end of your turn, boost self by 1."),
        unit("nor_meve", "Meve, White Queen", Faction.NORTHERN_REALMS, 8, 11, color = CardColor.GOLD,
            tags = setOf(Tag.HUMAN, Tag.ARISTOCRAT),
            abilities = listOf(Ability(Trigger.ORDER, Effect.Boost(3), charges = 3, cooldown = 1)),
            text = "Order: Boost an allied unit by 3. Charges: 3."),
        special("nor_mobilise", "Mobilisation", Faction.NORTHERN_REALMS, 6, tags = setOf(Tag.WARFARE),
            effect = Effect.Summon("nor_infantry"), text = "Summon a Temerian Infantry."),
    )

    val SCOIATAEL = listOf(
        unit("sco_commando", "Vrihedd Cadet", Faction.SCOIATAEL, 4, 4, tags = setOf(Tag.ELF, Tag.SOLDIER)),
        unit("sco_dwarf", "Mahakam Defender", Faction.SCOIATAEL, 5, 6, armor = 2, tags = setOf(Tag.DWARF, Tag.SOLDIER)),
        unit(
            "sco_dryad", "Dryad Ranger", Faction.SCOIATAEL, 4, 5, tags = setOf(Tag.DRYAD),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(2), row = Row.RANGED)),
            text = "Deploy (Ranged): Damage an enemy unit by 2.",
        ),
        unit(
            "sco_treant", "Elder Treant", Faction.SCOIATAEL, 6, 7, armor = 3, tags = setOf(Tag.TREANT, Tag.NATURE),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.DEFENDER))),
            text = "Deploy: Gain Defender.",
        ),
        unit(
            "sco_iorveth", "Iorveth", Faction.SCOIATAEL, 7, 10, color = CardColor.GOLD,
            tags = setOf(Tag.ELF, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.ORDER, Effect.Damage(3), charges = 2, cooldown = 1)),
            text = "Order: Damage an enemy unit by 3. Charges: 2.",
        ),
        unit("sco_archer2", "Vrihedd Sappers", Faction.SCOIATAEL, 4, 5, tags = setOf(Tag.ELF),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(2), row = Row.RANGED))),
        unit("sco_dwarf2", "Mahakam Forge", Faction.SCOIATAEL, 5, 6, armor = 3, tags = setOf(Tag.DWARF)),
        unit("sco_hawker", "Hawker Healer", Faction.SCOIATAEL, 3, 5, tags = setOf(Tag.HUMAN),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Heal(4)))),
        unit("sco_treant2", "Yaruga Treant", Faction.SCOIATAEL, 7, 8,
            tags = setOf(Tag.TREANT, Tag.NATURE),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.VEIL)))),
        unit("sco_francesca", "Francesca Findabair", Faction.SCOIATAEL, 7, 10, color = CardColor.GOLD,
            tags = setOf(Tag.ELF, Tag.MAGE),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.RESILIENCE)))),
        special("sco_trap", "Mahakam Ale", Faction.SCOIATAEL, 5, tags = setOf(Tag.ALCHEMY),
            effect = Effect.Boost(5), text = "Boost an allied unit by 5."),
    )

    val SKELLIGE = listOf(
        unit("ske_raider", "Clan Raider", Faction.SKELLIGE, 4, 4, tags = setOf(Tag.HUMAN, Tag.PIRATE)),
        unit("ske_drummond", "Drummond Warrior", Faction.SKELLIGE, 6, 6, tags = setOf(Tag.HUMAN, Tag.SOLDIER)),
        unit(
            "ske_priestess", "Priestess of Freya", Faction.SKELLIGE, 4, 6, tags = setOf(Tag.HUMAN, Tag.MAGE),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.VITALITY, 3))),
            text = "Deploy: Give an allied unit Vitality (3).",
        ),
        unit(
            "ske_berserker", "Berserker", Faction.SKELLIGE, 5, 6, tags = setOf(Tag.HUMAN, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.END_OF_TURN, Effect.Boost(1))),
            text = "At the end of your turn, boost self by 1.",
        ),
        unit(
            "ske_hjalmar", "Hjalmar an Craite", Faction.SKELLIGE, 8, 10, color = CardColor.GOLD,
            tags = setOf(Tag.HUMAN, Tag.SOLDIER),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(5))),
            text = "Deploy: Damage an enemy unit by 5.",
        ),
        unit("ske_shieldmaid", "An Craite Marauder", Faction.SKELLIGE, 5, 5,
            tags = setOf(Tag.HUMAN, Tag.SOLDIER)),
        unit("ske_hound", "Wolf of Skellige", Faction.SKELLIGE, 4, 5, tags = setOf(Tag.BEAST)),
        unit("ske_raider2", "Dimun Corsair", Faction.SKELLIGE, 6, 7, tags = setOf(Tag.HUMAN, Tag.PIRATE)),
        unit("ske_druid", "Skellige Druid", Faction.SKELLIGE, 4, 7, tags = setOf(Tag.HUMAN, Tag.DRUID),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Resurrect)),
            text = "Deploy: Resurrect your strongest fallen unit."),
        unit("ske_cerys", "Cerys an Craite", Faction.SKELLIGE, 7, 10, color = CardColor.GOLD,
            tags = setOf(Tag.HUMAN, Tag.ARISTOCRAT),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.VITALITY, 4)))),
        special("ske_ale", "Ale of the Ancestors", Faction.SKELLIGE, 5, tags = setOf(Tag.ALCHEMY),
            effect = Effect.Resurrect, text = "Resurrect your strongest fallen unit."),
    )

    val SYNDICATE = listOf(
        unit("syn_thug", "Crownsplitter Thug", Faction.SYNDICATE, 4, 4, tags = setOf(Tag.HUMAN, Tag.BANDIT)),
        unit(
            "syn_pickpocket", "Pickpocket", Faction.SYNDICATE, 3, 4, tags = setOf(Tag.HUMAN, Tag.BANDIT),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Profit(3))),
            text = "Deploy: Profit 3.",
        ),
        unit(
            "syn_enforcer", "Tidecloak Enforcer", Faction.SYNDICATE, 6, 7, armor = 1,
            tags = setOf(Tag.HUMAN, Tag.PIRATE),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Damage(3))),
            text = "Deploy: Damage an enemy unit by 3.",
        ),
        unit(
            "syn_moorlehem", "Vincent van Moorlehem", Faction.SYNDICATE, 7, 10, color = CardColor.GOLD,
            tags = setOf(Tag.VAMPIRE, Tag.ARISTOCRAT),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Destroy)),
            text = "Deploy: Destroy an enemy unit with a status.",
        ),
        special(
            "syn_bloodygoodfun", "Bloody Good Fun", Faction.SYNDICATE, 5, tags = setOf(Tag.CRIME),
            effect = Effect.Profit(4), text = "Profit 4.",
        ),
        unit("syn_cutpurse", "Novigrad Cutpurse", Faction.SYNDICATE, 4, 5,
            tags = setOf(Tag.HUMAN, Tag.BANDIT),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Profit(4)))),
        unit("syn_bruiser", "Blindeye Bruiser", Faction.SYNDICATE, 6, 6, armor = 1,
            tags = setOf(Tag.HUMAN, Tag.BANDIT)),
        unit("syn_fisher", "Tidecloak Hideaway", Faction.SYNDICATE, 5, 7,
            tags = setOf(Tag.HUMAN, Tag.PIRATE),
            abilities = listOf(Ability(Trigger.END_OF_TURN, Effect.Profit(1)))),
        unit("syn_whoreson", "Whoreson Junior", Faction.SYNDICATE, 8, 11, color = CardColor.GOLD,
            tags = setOf(Tag.HUMAN, Tag.ARISTOCRAT),
            abilities = listOf(Ability(Trigger.DEPLOY, Effect.Apply(Status.BOUNTY)))),
        special("syn_heist", "Broken Bones", Faction.SYNDICATE, 6, tags = setOf(Tag.CRIME),
            effect = Effect.Damage(6), text = "Damage an enemy unit by 6."),
    )

    /** Stratagems cost no provisions and are chosen separately from the 25-card list. */
    val STRATAGEMS = listOf(
        Card(
            "str_tactical_advantage", "Tactical Advantage", Faction.NEUTRAL, CardType.STRATAGEM,
            CardColor.BRONZE, 0, 0, 0, emptySet(),
            listOf(Ability(Trigger.ORDER, Effect.Boost(4), zeal = true)),
            text = "Order: Boost an allied unit by 4.",
        ),
        Card(
            "str_enchanted_armor", "Enchanted Armor", Faction.NEUTRAL, CardType.STRATAGEM,
            CardColor.BRONZE, 0, 0, 0, emptySet(),
            listOf(Ability(Trigger.ORDER, Effect.Apply(Status.SHIELD), zeal = true)),
            text = "Order: Give a unit a Shield.",
        ),
    )

    val ALL: List<Card> =
        NEUTRALS + MONSTERS + NILFGAARD + NORTHERN_REALMS + SCOIATAEL + SKELLIGE + SYNDICATE

    fun byId(id: String): Card? = ALL.firstOrNull { it.id == id } ?: STRATAGEMS.firstOrNull { it.id == id }

    fun forFaction(faction: Faction): List<Card> =
        ALL.filter { it.faction == faction || it.faction == Faction.NEUTRAL }

    /**
     * Build a legal starter deck for a faction: bronzes doubled up, golds single, filled to the
     * 25-card minimum without exceeding the provision limit.
     */
    fun starterDeck(leader: Leader): Deck {
        val pool = ALL.filter { it.faction == leader.faction } + NEUTRALS
        val limit = BASE_PROVISIONS + leader.provisionBonus
        val cards = mutableListOf<Card>()
        var spent = 0

        // Golds first (one copy each), then bronzes (two copies), cheapest first so the deck fills.
        val golds = pool.filter { it.color == CardColor.GOLD }.sortedBy { it.provisions }
        val bronzes = pool.filter { it.color == CardColor.BRONZE }.sortedBy { it.provisions }

        for (card in golds) {
            if (cards.size >= MIN_DECK_SIZE) break
            if (spent + card.provisions <= limit) { cards += card; spent += card.provisions }
        }
        var i = 0
        while (cards.size < MIN_DECK_SIZE && bronzes.isNotEmpty()) {
            val card = bronzes[i % bronzes.size]
            if (cards.count { it.id == card.id } < CardColor.BRONZE.copyLimit &&
                spent + card.provisions <= limit
            ) {
                cards += card
                spent += card.provisions
            }
            i++
            // Every bronze is either maxed out or unaffordable — stop rather than spin.
            if (i > bronzes.size * CardColor.BRONZE.copyLimit + bronzes.size) break
        }
        return Deck(leader, STRATAGEMS.first(), cards)
    }
}
