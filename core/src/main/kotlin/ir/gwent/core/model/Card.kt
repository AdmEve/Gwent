package ir.gwent.core.model

enum class Row { MELEE, RANGED, SIEGE }

enum class Faction { PAHLAVAN, DIV }

enum class Ability {
    NONE,
    /** Doubles the power of every non-hero unit in the row this card is played to. */
    HORN,
    /** Destroys the highest-power non-hero unit(s) on the entire board. */
    SCORCH,
}

data class Card(
    val id: String,
    val name: String,
    val faction: Faction,
    val row: Row,
    val basePower: Int,
    val ability: Ability = Ability.NONE,
    val isHero: Boolean = false,
)
