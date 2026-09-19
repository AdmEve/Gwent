package ir.gwent.core.model

/**
 * A leader defines the deck's faction and contributes provisions on top of the base 150.
 *
 * The trade-off is deliberate: a weaker ability carries a larger [provisionBonus], buying stronger
 * cards. These numbers are community-voted in the live game, so they are data here rather than
 * constants baked into the engine.
 */
data class Leader(
    val id: String,
    val name: String,
    val faction: Faction,
    val provisionBonus: Int,
    val ability: Ability,
    val text: String = "",
)

object Leaders {

    val DAGON = Leader(
        id = "dagon",
        name = "Dagon",
        faction = Faction.MONSTERS,
        provisionBonus = 15,
        ability = Ability(
            trigger = Trigger.ORDER,
            effect = Effect.Apply(Status.BLEEDING, duration = 2),
            charges = 2,
            cooldown = 2,
        ),
        text = "Order: Give an enemy unit Bleeding (2). Charges: 2.",
    )

    val EREDIN = Leader(
        id = "eredin",
        name = "Eredin",
        faction = Faction.MONSTERS,
        provisionBonus = 16,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Boost(3), charges = 3, cooldown = 1),
        text = "Order: Boost an allied unit by 3. Charges: 3.",
    )

    val EMHYR = Leader(
        id = "emhyr",
        name = "Emhyr var Emreis",
        faction = Faction.NILFGAARD,
        provisionBonus = 15,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Damage(4), charges = 2, cooldown = 2),
        text = "Order: Damage an enemy unit by 4. Charges: 2.",
    )

    val CALVEIT = Leader(
        id = "calveit",
        name = "John Calveit",
        faction = Faction.NILFGAARD,
        provisionBonus = 16,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Draw(1), charges = 1),
        text = "Order: Draw a card.",
    )

    val FOLTEST = Leader(
        id = "foltest",
        name = "Foltest",
        faction = Faction.NORTHERN_REALMS,
        provisionBonus = 15,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Apply(Status.SHIELD), charges = 3, cooldown = 1),
        text = "Order: Give an allied unit a Shield. Charges: 3.",
    )

    val HENSELT = Leader(
        id = "henselt",
        name = "Henselt",
        faction = Faction.NORTHERN_REALMS,
        provisionBonus = 16,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Boost(2), charges = 4, cooldown = 1),
        text = "Order: Boost an allied unit by 2. Charges: 4.",
    )

    val BROUVER = Leader(
        id = "brouver",
        name = "Brouver Hoog",
        faction = Faction.SCOIATAEL,
        provisionBonus = 15,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Heal(99), charges = 2, cooldown = 2),
        text = "Order: Heal an allied unit. Charges: 2.",
    )

    val EITHNE = Leader(
        id = "eithne",
        name = "Eithné",
        faction = Faction.SCOIATAEL,
        provisionBonus = 16,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Damage(2), charges = 3, cooldown = 1),
        text = "Order: Damage an enemy unit by 2. Charges: 3.",
    )

    val CRACH = Leader(
        id = "crach",
        name = "Crach an Craite",
        faction = Faction.SKELLIGE,
        provisionBonus = 15,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Strengthen(2), charges = 2, cooldown = 2),
        text = "Order: Strengthen an allied unit by 2. Charges: 2.",
    )

    val BRAN = Leader(
        id = "bran",
        name = "King Bran",
        faction = Faction.SKELLIGE,
        provisionBonus = 16,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Apply(Status.VITALITY, duration = 3), charges = 2, cooldown = 1),
        text = "Order: Give an allied unit Vitality (3). Charges: 2.",
    )

    val CLEAVER = Leader(
        id = "cleaver",
        name = "Cleaver",
        faction = Faction.SYNDICATE,
        provisionBonus = 15,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Profit(5), charges = 2, cooldown = 1),
        text = "Order: Gain 5 Coins. Charges: 2.",
    )

    val GUDRUN = Leader(
        id = "gudrun",
        name = "Gudrun Bjornsdottir",
        faction = Faction.SYNDICATE,
        provisionBonus = 16,
        ability = Ability(trigger = Trigger.ORDER, effect = Effect.Profit(9), charges = 1),
        text = "Order: Gain 9 Coins.",
    )

    val ALL = listOf(
        DAGON, EREDIN, EMHYR, CALVEIT, FOLTEST, HENSELT,
        BROUVER, EITHNE, CRACH, BRAN, CLEAVER, GUDRUN,
    )

    fun forFaction(faction: Faction): List<Leader> = ALL.filter { it.faction == faction }

    fun byId(id: String): Leader? = ALL.firstOrNull { it.id == id }

    /** Leaders exist for every faction except Neutral, which has none. */
    val PLAYABLE_FACTIONS: List<Faction> = Faction.entries.filter { it != Faction.NEUTRAL }
}
