package ir.gwent.core.model

/**
 * Card pools, one per faction/deck. Shahnameh factions are original characters/art/text.
 * Marvel and One Piece characters are used under the studio's confidential licensing
 * clearance for this project; Greek Myth characters are public-domain mythology.
 * Each deck mixes plain row units, a couple of hero units (immune to Horn/Scorch/Decoy/
 * Weather), and one card each of Horn, Scorch, Spy, Medic and Decoy, plus three Weather
 * cards (one per row) and a Clear Weather card.
 */
object CardDatabase {

    val pahlavan: List<Card> = listOf(
        Card("pah-sohrab", "Sohrab", Faction.PAHLAVAN, Row.MELEE, 6),
        Card("pah-bijan", "Bijan", Faction.PAHLAVAN, Row.MELEE, 5),
        Card("pah-giv", "Giv", Faction.PAHLAVAN, Row.MELEE, 4),
        Card("pah-rostam", "Rostam", Faction.PAHLAVAN, Row.MELEE, 10, isHero = true),

        Card("pah-bahram", "Bahram Gur", Faction.PAHLAVAN, Row.RANGED, 4),
        Card("pah-faramarz", "Faramarz", Faction.PAHLAVAN, Row.RANGED, 6),
        Card("pah-arash", "Arash the Archer", Faction.PAHLAVAN, Row.RANGED, 8),
        Card("pah-esfandiar", "Esfandiar", Faction.PAHLAVAN, Row.RANGED, 9, isHero = true),

        Card("pah-tahmineh", "Tahmineh", Faction.PAHLAVAN, Row.SIEGE, 4),
        Card("pah-piran", "Piran", Faction.PAHLAVAN, Row.SIEGE, 5),
        Card("pah-ashkbus", "Ashkbus", Faction.PAHLAVAN, Row.SIEGE, 3),

        Card("pah-kaveh", "Kaveh the Blacksmith", Faction.PAHLAVAN, Row.SIEGE, 3, ability = Ability.HORN),
        Card("pah-simurgh", "Simurgh", Faction.PAHLAVAN, Row.RANGED, 4, ability = Ability.SCORCH),
        Card("pah-gordafarid", "Gordafarid", Faction.PAHLAVAN, Row.RANGED, 5, ability = Ability.SPY),
        Card("pah-zal", "Zal", Faction.PAHLAVAN, Row.SIEGE, 6, ability = Ability.MEDIC),
        Card("pah-manijeh", "Manijeh", Faction.PAHLAVAN, Row.SIEGE, 2, ability = Ability.DECOY),

        Card("pah-blizzard", "Blizzard of Mazandaran", Faction.PAHLAVAN, Row.MELEE, ability = Ability.WEATHER),
        Card("pah-fog", "Fog of Mazandaran", Faction.PAHLAVAN, Row.RANGED, ability = Ability.WEATHER),
        Card("pah-flood", "Flood of the Kashaf River", Faction.PAHLAVAN, Row.SIEGE, ability = Ability.WEATHER),
        Card("pah-sorush", "Sorush's Blessing", Faction.PAHLAVAN, Row.MELEE, ability = Ability.CLEAR_WEATHER),
    )

    val div: List<Card> = listOf(
        Card("div-akvan", "Akvan Div", Faction.DIV, Row.MELEE, 6),
        Card("div-puladvand", "Puladvand", Faction.DIV, Row.MELEE, 5),
        Card("div-sanjeh", "Sanjeh Div", Faction.DIV, Row.MELEE, 4),
        Card("div-sepid", "Div-e Sepid", Faction.DIV, Row.MELEE, 10, isHero = true),

        Card("div-arzhang", "Arzhang Div", Faction.DIV, Row.RANGED, 5),
        Card("div-bid", "Bid Div", Faction.DIV, Row.RANGED, 4),
        Card("div-karkadann", "Karkadann", Faction.DIV, Row.RANGED, 4),
        Card("div-zahhak", "Zahhak", Faction.DIV, Row.RANGED, 9, isHero = true),

        Card("div-nahang", "Nahang Div", Faction.DIV, Row.SIEGE, 6),
        Card("div-shabahang", "Shabahang", Faction.DIV, Row.SIEGE, 2),
        Card("div-ahriman-spawn", "Ahriman's Spawn", Faction.DIV, Row.SIEGE, 3),

        Card("div-ifrit", "Ifrit", Faction.DIV, Row.SIEGE, 3, ability = Ability.HORN),
        Card("div-ghoul", "Ghoul", Faction.DIV, Row.RANGED, 4, ability = Ability.SCORCH),
        Card("div-sork", "Sork Div", Faction.DIV, Row.RANGED, 5, ability = Ability.SPY),
        Card("div-ahriman", "Ahriman", Faction.DIV, Row.SIEGE, 5, ability = Ability.MEDIC),
        Card("div-nasnas", "Nasnas", Faction.DIV, Row.MELEE, 2, ability = Ability.DECOY),

        Card("div-darkness", "Darkness of Mazandaran", Faction.DIV, Row.MELEE, ability = Ability.WEATHER),
        Card("div-plague-wind", "Plague Wind of Ahriman", Faction.DIV, Row.RANGED, ability = Ability.WEATHER),
        Card("div-black-flood", "Black Flood", Faction.DIV, Row.SIEGE, ability = Ability.WEATHER),
        Card("div-dawn", "Dawn of Ohrmazd", Faction.DIV, Row.MELEE, ability = Ability.CLEAR_WEATHER),
    )

    val marvel: List<Card> = listOf(
        Card("mar-cap-america", "Captain America", Faction.MARVEL, Row.MELEE, 6),
        Card("mar-black-panther", "Black Panther", Faction.MARVEL, Row.MELEE, 5),
        Card("mar-luke-cage", "Luke Cage", Faction.MARVEL, Row.MELEE, 4),
        Card("mar-wolverine", "Wolverine", Faction.MARVEL, Row.MELEE, 10, isHero = true),

        Card("mar-hawkeye", "Hawkeye", Faction.MARVEL, Row.RANGED, 4),
        Card("mar-black-widow", "Black Widow", Faction.MARVEL, Row.RANGED, 5),
        Card("mar-star-lord", "Star-Lord", Faction.MARVEL, Row.RANGED, 6),
        Card("mar-thor", "Thor", Faction.MARVEL, Row.RANGED, 9, isHero = true),

        Card("mar-war-machine", "War Machine", Faction.MARVEL, Row.SIEGE, 5),
        Card("mar-rocket", "Rocket Raccoon", Faction.MARVEL, Row.SIEGE, 3),
        Card("mar-vision", "Vision", Faction.MARVEL, Row.SIEGE, 4),

        Card("mar-iron-man", "Iron Man", Faction.MARVEL, Row.SIEGE, 3, ability = Ability.HORN),
        Card("mar-doctor-strange", "Doctor Strange", Faction.MARVEL, Row.RANGED, 4, ability = Ability.SCORCH),
        Card("mar-loki", "Loki", Faction.MARVEL, Row.RANGED, 5, ability = Ability.SPY),
        Card("mar-scarlet-witch", "Scarlet Witch", Faction.MARVEL, Row.SIEGE, 5, ability = Ability.MEDIC),
        Card("mar-mysterio", "Mysterio", Faction.MARVEL, Row.MELEE, 2, ability = Ability.DECOY),

        Card("mar-storm", "Storm", Faction.MARVEL, Row.MELEE, ability = Ability.WEATHER),
        Card("mar-sandman", "Sandman", Faction.MARVEL, Row.RANGED, ability = Ability.WEATHER),
        Card("mar-hydro-man", "Hydro-Man", Faction.MARVEL, Row.SIEGE, ability = Ability.WEATHER),
        Card("mar-human-torch", "Human Torch", Faction.MARVEL, Row.MELEE, ability = Ability.CLEAR_WEATHER),
    )

    val onePiece: List<Card> = listOf(
        Card("op-zoro", "Roronoa Zoro", Faction.ONE_PIECE, Row.MELEE, 7),
        Card("op-sanji", "Sanji", Faction.ONE_PIECE, Row.MELEE, 6),
        Card("op-jinbe", "Jinbe", Faction.ONE_PIECE, Row.MELEE, 5),
        Card("op-luffy", "Monkey D. Luffy", Faction.ONE_PIECE, Row.MELEE, 10, isHero = true),

        Card("op-usopp", "Usopp", Faction.ONE_PIECE, Row.RANGED, 4),
        Card("op-killer", "Killer", Faction.ONE_PIECE, Row.RANGED, 5),
        Card("op-law", "Trafalgar Law", Faction.ONE_PIECE, Row.RANGED, 6),
        Card("op-shanks", "Shanks", Faction.ONE_PIECE, Row.RANGED, 9, isHero = true),

        Card("op-franky", "Franky", Faction.ONE_PIECE, Row.SIEGE, 5),
        Card("op-brook", "Brook", Faction.ONE_PIECE, Row.SIEGE, 3),
        Card("op-kid", "Eustass Kid", Faction.ONE_PIECE, Row.SIEGE, 4),

        Card("op-whitebeard", "Whitebeard", Faction.ONE_PIECE, Row.SIEGE, 4, ability = Ability.HORN),
        Card("op-akainu", "Akainu", Faction.ONE_PIECE, Row.RANGED, 5, ability = Ability.SCORCH),
        Card("op-robin", "Nico Robin", Faction.ONE_PIECE, Row.RANGED, 5, ability = Ability.SPY),
        Card("op-chopper", "Tony Tony Chopper", Faction.ONE_PIECE, Row.MELEE, 3, ability = Ability.MEDIC),
        Card("op-buggy", "Buggy", Faction.ONE_PIECE, Row.MELEE, 2, ability = Ability.DECOY),

        Card("op-nami", "Nami", Faction.ONE_PIECE, Row.MELEE, ability = Ability.WEATHER),
        Card("op-eneru", "Eneru", Faction.ONE_PIECE, Row.RANGED, ability = Ability.WEATHER),
        Card("op-crocodile", "Crocodile", Faction.ONE_PIECE, Row.SIEGE, ability = Ability.WEATHER),
        Card("op-nika", "Sun God Nika", Faction.ONE_PIECE, Row.MELEE, ability = Ability.CLEAR_WEATHER),
    )

    val greekMyth: List<Card> = listOf(
        Card("grk-ajax", "Ajax", Faction.GREEK_MYTH, Row.MELEE, 6),
        Card("grk-hector", "Hector", Faction.GREEK_MYTH, Row.MELEE, 6),
        Card("grk-menelaus", "Menelaus", Faction.GREEK_MYTH, Row.MELEE, 5),
        Card("grk-achilles", "Achilles", Faction.GREEK_MYTH, Row.MELEE, 10, isHero = true),

        Card("grk-paris", "Paris", Faction.GREEK_MYTH, Row.RANGED, 4),
        Card("grk-atalanta", "Atalanta", Faction.GREEK_MYTH, Row.RANGED, 5),
        Card("grk-diomedes", "Diomedes", Faction.GREEK_MYTH, Row.RANGED, 6),
        Card("grk-apollo", "Apollo", Faction.GREEK_MYTH, Row.RANGED, 9, isHero = true),

        Card("grk-perseus", "Perseus", Faction.GREEK_MYTH, Row.SIEGE, 5),
        Card("grk-bellerophon", "Bellerophon", Faction.GREEK_MYTH, Row.SIEGE, 4),
        Card("grk-jason", "Jason", Faction.GREEK_MYTH, Row.SIEGE, 3),

        Card("grk-zeus", "Zeus", Faction.GREEK_MYTH, Row.SIEGE, 4, ability = Ability.HORN),
        Card("grk-medusa", "Medusa", Faction.GREEK_MYTH, Row.RANGED, 4, ability = Ability.SCORCH),
        Card("grk-hermes", "Hermes", Faction.GREEK_MYTH, Row.RANGED, 5, ability = Ability.SPY),
        Card("grk-asclepius", "Asclepius", Faction.GREEK_MYTH, Row.SIEGE, 5, ability = Ability.MEDIC),
        Card("grk-odysseus", "Odysseus", Faction.GREEK_MYTH, Row.MELEE, 3, ability = Ability.DECOY),

        Card("grk-boreas", "Boreas", Faction.GREEK_MYTH, Row.MELEE, ability = Ability.WEATHER),
        Card("grk-demeter", "Demeter", Faction.GREEK_MYTH, Row.RANGED, ability = Ability.WEATHER),
        Card("grk-poseidon", "Poseidon", Faction.GREEK_MYTH, Row.SIEGE, ability = Ability.WEATHER),
        Card("grk-helios", "Helios", Faction.GREEK_MYTH, Row.MELEE, ability = Ability.CLEAR_WEATHER),
    )

    fun deckFor(faction: Faction): List<Card> = when (faction) {
        Faction.PAHLAVAN -> pahlavan
        Faction.DIV -> div
        Faction.MARVEL -> marvel
        Faction.ONE_PIECE -> onePiece
        Faction.GREEK_MYTH -> greekMyth
    }
}
