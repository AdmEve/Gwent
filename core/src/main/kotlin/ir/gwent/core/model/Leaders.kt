package ir.gwent.core.model

/** One leader and one passive trait per army. */
object Leaders {

    private val all: Map<Faction, Leader> = mapOf(
        Faction.PAHLAVAN to Leader(
            id = "lead-pah",
            name = "Kay Khosrow",
            faction = Faction.PAHLAVAN,
            ability = LeaderAbility.CLEAR_ALL_WEATHER,
            description = "Clear all weather from the battlefield.",
        ),
        Faction.DIV to Leader(
            id = "lead-div",
            name = "Zahhak the Serpent King",
            faction = Faction.DIV,
            ability = LeaderAbility.SCORCH_ENEMY_STRONGEST,
            description = "Destroy the enemy's strongest unit.",
        ),
        Faction.MARVEL to Leader(
            id = "lead-mar",
            name = "Nick Fury",
            faction = Faction.MARVEL,
            ability = LeaderAbility.DRAW_CARD,
            description = "Draw a card from your deck.",
        ),
        Faction.ONE_PIECE to Leader(
            id = "lead-op",
            name = "Gol D. Roger",
            faction = Faction.ONE_PIECE,
            ability = LeaderAbility.HORN_STRONGEST_ROW,
            description = "Double the power of your strongest row.",
        ),
        Faction.GREEK_MYTH to Leader(
            id = "lead-grk",
            name = "Zeus Cloudgatherer",
            faction = Faction.GREEK_MYTH,
            ability = LeaderAbility.WEATHER_ENEMY_STRONGEST_ROW,
            description = "Call a storm down on the enemy's strongest row.",
        ),
    )

    private val traits: Map<Faction, FactionTrait> = mapOf(
        Faction.PAHLAVAN to FactionTrait.DRAW_ON_ROUND_WIN,
        Faction.DIV to FactionTrait.KEEP_RANDOM_UNIT,
        Faction.MARVEL to FactionTrait.WIN_TIES,
        Faction.ONE_PIECE to FactionTrait.ALWAYS_OPENS,
        Faction.GREEK_MYTH to FactionTrait.RECOVER_AT_ROUND_THREE,
    )

    fun forFaction(faction: Faction): Leader = all.getValue(faction)

    fun traitFor(faction: Faction): FactionTrait = traits.getValue(faction)

    fun describeTrait(faction: Faction): String = when (traitFor(faction)) {
        FactionTrait.DRAW_ON_ROUND_WIN -> "Draw a card after winning a round."
        FactionTrait.KEEP_RANDOM_UNIT -> "Keep one random unit on the board between rounds."
        FactionTrait.WIN_TIES -> "Win rounds that end level."
        FactionTrait.ALWAYS_OPENS -> "Always makes the opening move."
        FactionTrait.RECOVER_AT_ROUND_THREE -> "Recover a random card from the graveyard in round three."
    }
}
