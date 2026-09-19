package ir.gwent.core.model

/**
 * Deck construction, with the rules enforced as you build rather than checked at the end.
 *
 * This is where GWENT's central decision lives: the provision budget is finite, so every gold
 * bomb is paid for with weaker bronzes elsewhere. The builder therefore answers not just "is
 * this deck legal" but "may I add this card right now, and if not, why" — which is what a UI
 * needs to grey out a card and say something useful about it.
 */
class DeckBuilder(val leader: Leader, stratagem: Card? = null) {

    private val _cards = mutableListOf<Card>()

    var stratagem: Card? = stratagem
        private set

    val cards: List<Card> get() = _cards

    val faction: Faction get() = leader.faction

    /** 150 plus whatever the leader contributes. */
    val provisionLimit: Int get() = BASE_PROVISIONS + leader.provisionBonus

    val provisionsSpent: Int get() = _cards.sumOf { it.provisions }

    val provisionsLeft: Int get() = provisionLimit - provisionsSpent

    val size: Int get() = _cards.size

    /** A deck must reach the minimum size; it may not exceed the provision budget. */
    val isComplete: Boolean get() = size >= MIN_DECK_SIZE && provisionsSpent <= provisionLimit

    fun copiesOf(card: Card): Int = _cards.count { it.id == card.id }

    /** Why [card] cannot be added right now, or null if it can. */
    fun rejectionFor(card: Card): String? = when {
        card.type == CardType.STRATAGEM -> "stratagems are chosen separately"
        !card.faction.playableIn(faction) ->
            "${card.name} belongs to ${card.faction}, not $faction or Neutral"
        copiesOf(card) >= card.color.copyLimit ->
            "already at ${card.color.copyLimit} ${if (card.color == CardColor.GOLD) "copy" else "copies"}"
        card.provisions > provisionsLeft ->
            "costs ${card.provisions}, only $provisionsLeft provisions left"
        else -> null
    }

    fun canAdd(card: Card): Boolean = rejectionFor(card) == null

    /** Add a copy. Returns the reason it was refused, or null on success. */
    fun add(card: Card): String? {
        rejectionFor(card)?.let { return it }
        _cards += card
        return null
    }

    /** Remove one copy. Returns true if a copy was actually removed. */
    fun remove(card: Card): Boolean = _cards.indexOfFirst { it.id == card.id }
        .takeIf { it >= 0 }
        ?.let { _cards.removeAt(it); true }
        ?: false

    fun setStratagem(card: Card?): String? {
        if (card != null && card.type != CardType.STRATAGEM) return "${card.name} is not a stratagem"
        stratagem = card
        return null
    }

    fun clear() = _cards.clear()

    /** Fill the remaining slots with the cheapest legal cards, for a quick playable deck. */
    fun autoComplete(pool: List<Card>) {
        val affordable = pool
            .filter { it.faction.playableIn(faction) && it.type != CardType.STRATAGEM }
            .sortedBy { it.provisions }
        var guard = 0
        while (size < MIN_DECK_SIZE && guard++ < 500) {
            val next = affordable.firstOrNull { canAdd(it) } ?: break
            add(next)
        }
    }

    fun build(): Deck = Deck(leader, stratagem, _cards.toList())

    /** What is stopping this deck from being legal, in words a player can act on. */
    fun problems(): List<String> {
        val out = mutableListOf<String>()
        if (size < MIN_DECK_SIZE) out += "needs ${MIN_DECK_SIZE - size} more card(s)"
        if (provisionsSpent > provisionLimit) out += "over budget by ${provisionsSpent - provisionLimit} provisions"
        return out + build().validate()
    }

    companion object {
        /** Start a builder pre-filled with the generated starter deck, so editing has a base. */
        fun fromStarter(leader: Leader): DeckBuilder {
            val starter = CardDatabase.starterDeck(leader)
            val b = DeckBuilder(leader, starter.stratagem)
            starter.cards.forEach { b.add(it) }
            return b
        }
    }
}
