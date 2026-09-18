package ir.gwent.core.model

/**
 * Original card pool themed on the Shahnameh (Book of Kings), not derived from any
 * existing card game's names, art, or text. Each faction's pool doubles as its deck:
 * players must ration all 12 cards across a best-of-three match.
 */
object CardDatabase {

    val pahlavan: List<Card> = listOf(
        Card("pah-rostam", "Rostam", Faction.PAHLAVAN, Row.MELEE, 10, isHero = true),
        Card("pah-sohrab", "Sohrab", Faction.PAHLAVAN, Row.MELEE, 6),
        Card("pah-bijan", "Bijan", Faction.PAHLAVAN, Row.MELEE, 5),
        Card("pah-giv", "Giv", Faction.PAHLAVAN, Row.MELEE, 4),

        Card("pah-esfandiar", "Esfandiar", Faction.PAHLAVAN, Row.RANGED, 9, isHero = true),
        Card("pah-gordafarid", "Gordafarid", Faction.PAHLAVAN, Row.RANGED, 5),
        Card("pah-arash", "Arash the Archer", Faction.PAHLAVAN, Row.RANGED, 8),
        Card("pah-simurgh", "Simurgh", Faction.PAHLAVAN, Row.RANGED, 4, ability = Ability.SCORCH),

        Card("pah-kaveh", "Kaveh the Blacksmith", Faction.PAHLAVAN, Row.SIEGE, 3, ability = Ability.HORN),
        Card("pah-zal", "Zal", Faction.PAHLAVAN, Row.SIEGE, 6),
        Card("pah-tahmineh", "Tahmineh", Faction.PAHLAVAN, Row.SIEGE, 4),
        Card("pah-manijeh", "Manijeh", Faction.PAHLAVAN, Row.SIEGE, 2),
    )

    val div: List<Card> = listOf(
        Card("div-sepid", "Div-e Sepid", Faction.DIV, Row.MELEE, 10, isHero = true),
        Card("div-akvan", "Akvan Div", Faction.DIV, Row.MELEE, 6),
        Card("div-puladvand", "Puladvand", Faction.DIV, Row.MELEE, 5),
        Card("div-sanjeh", "Sanjeh Div", Faction.DIV, Row.MELEE, 4),

        Card("div-zahhak", "Zahhak", Faction.DIV, Row.RANGED, 9, isHero = true),
        Card("div-arzhang", "Arzhang Div", Faction.DIV, Row.RANGED, 5),
        Card("div-bid", "Bid Div", Faction.DIV, Row.RANGED, 4),
        Card("div-ghoul", "Ghoul", Faction.DIV, Row.RANGED, 4, ability = Ability.SCORCH),

        Card("div-nahang", "Nahang Div", Faction.DIV, Row.SIEGE, 6),
        Card("div-ifrit", "Ifrit", Faction.DIV, Row.SIEGE, 3, ability = Ability.HORN),
        Card("div-karkadann", "Karkadann", Faction.DIV, Row.SIEGE, 4),
        Card("div-shabahang", "Shabahang", Faction.DIV, Row.SIEGE, 2),
    )

    fun deckFor(faction: Faction): List<Card> = when (faction) {
        Faction.PAHLAVAN -> pahlavan
        Faction.DIV -> div
    }
}
