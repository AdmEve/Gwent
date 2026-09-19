package ir.gwent.android.ui

/**
 * The board's geometry, measured off GWENT itself.
 *
 * Every number here was taken from CD PROJEKT RED's own in-game screenshots published on
 * playgwent.com — the source this project treats as authoritative — at 1920x1080, by finding the
 * card rows' edges in the image rather than by eye. They are kept as fractions of the screen so
 * the same proportions hold on a phone.
 *
 * Reference: cdn-l-playgwent.cdprojektred.com/screenshots/CardGameplayEffects-*.jpg
 */
object GwentBoard {

    // --- measured in the 1920x1080 reference ---------------------------------
    private const val REF_H = 1080f

    /** Card rows, far to near: opponent ranged, opponent melee, our melee, our ranged. */
    private val ROW_TOP = floatArrayOf(145f, 303f, 490f, 683f)
    private val ROW_BOTTOM = floatArrayOf(285f, 455f, 660f, 860f)

    /** The top of the hand, which is where the field stops. */
    const val HAND_TOP = 882f / REF_H

    /** The field's first pixel. */
    const val FIELD_TOP = 145f / REF_H

    /** The dividing line between the two sides, midway between the melee rows. */
    const val CENTRE_LINE = 474f / REF_H

    /**
     * Card height per row, far to near: 140, 152, 170, 177 px.
     *
     * This is the game's perspective, and it is not subtle — the row nearest the player is a
     * quarter taller than the row furthest away. A board whose rows are all one size cannot look
     * like GWENT no matter what is drawn on it.
     */
    val ROW_CARD_HEIGHT: List<Float> = (0..3).map { ROW_BOTTOM[it] - ROW_TOP[it] }

    /** The same ladder normalised against the nearest row: 0.791, 0.859, 0.960, 1.000. */
    val ROW_SCALE: List<Float> = ROW_CARD_HEIGHT.map { it / ROW_CARD_HEIGHT[3] }

    /** Row band heights as layout weights, so far rows are shorter on screen as well as smaller. */
    val ROW_WEIGHT: List<Float> = ROW_SCALE

    /**
     * How much narrower a row's band is than the nearest one.
     *
     * Measured card widths run about 113, 122, 128, 133 px far-to-near, a gentler taper than the
     * heights: the cards shrink with distance but the row does not converge as hard as the ground
     * beneath it does.
     */
    val ROW_WIDTH: List<Float> = listOf(0.850f, 0.917f, 0.962f, 1.000f)

    /** Card proportions, from the reference: 133 wide by 177 tall on the nearest row. */
    const val CARD_ASPECT = 133f / 177f

    /**
     * How hard the ground converges toward the horizon.
     *
     * The far edge of the plane is drawn at 1/(1+this) of the near edge's width. Taken from the
     * reference's ground texture rather than from its cards, which foreshorten less.
     */
    const val GROUND_DEPTH = 1.15f
}
