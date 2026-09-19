package ir.gwent.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.CardColor
import ir.gwent.core.model.Faction

/*
 * Palette taken from playgwent.com's own stylesheets, so the game reads as GWENT rather than as a
 * generic dark card game. The two that matter most for play are BoostGreen and DamageRed: in the
 * real game a unit's power number turns green when boosted above base and red when damaged below
 * it, which is how you read the board at a glance.
 *
 * The display faces (GWENT, HalisGR) are proprietary and deliberately NOT bundled. [DisplayFamily]
 * falls back to a system serif, so the UI is correct without them and exact with them.
 */

// --- ground -----------------------------------------------------------------
val Ink = Color(0xFF05070A)
val BoardDeep = Color(0xFF0B0A06)
val BoardMid = Color(0xFF1B160A)          // #1b160a, the site's dark ground
val PanelBackground = Color(0xFF16130C)
val RowSlot = Color(0xFF12100A)
val RowSlotLit = Color(0xFF241D0F)

// --- gold leaf --------------------------------------------------------------
val GoldLight = Color(0xFFFADF7A)         // #fadf7a
val GoldPale = Color(0xFFFFEC88)          // #ffec88
val GoldText = Color(0xFFE2A73E)          // #e2a73e
val GoldDeep = Color(0xFFBF9437)          // #bf9437
val GoldBright = Color(0xFFFEC100)        // #fec100
val GoldMuted = Color(0xFFE7C567)         // #e7c567

// --- state colours ----------------------------------------------------------
val BoostGreen = Color(0xFF18DD91)        // #18dd91 — power above base
val BoostGreenDeep = Color(0xFF00C389)    // #00c389
val DamageRed = Color(0xFFB91818)         // #b91818 — power below base
val Parchment = Color(0xFFD9D8CB)         // #d9d8cb — neutral power text
val ParchmentDim = Color(0xFFD0CFC3)
val MutedText = Color(0xFF8C8672)
val ArmorSteel = Color(0xFFBAC4D0)

// --- metals -----------------------------------------------------------------
val MetalGold = Brush.linearGradient(listOf(GoldDeep, GoldLight, GoldDeep, GoldPale, GoldDeep))
val MetalBronze = Brush.linearGradient(listOf(Color(0xFF4A3520), Color(0xFFB98B54), Color(0xFF4A3520)))

/** Bronze and gold borders are how you tell a card's rarity class at a glance. */
fun frameBrush(color: CardColor): Brush = when (color) {
    CardColor.GOLD -> MetalGold
    CardColor.BRONZE -> MetalBronze
}


// --- board surfaces ---------------------------------------------------------
/*
 * The real game's board is a lit place — a forest floor or a stone path — with warm light in
 * the middle and darkness at the edges. Flat black reads as a debug screen, so the board is
 * built from layered gradients: a warm ground, a vignette, and banded terrain for the rows.
 */

/** The battlefield ground: warm and lit through the centre, falling away at the edges. */
val BoardGround = Brush.verticalGradient(
    0.00f to Color(0xFF0A0906),
    0.18f to Color(0xFF17130B),
    0.50f to Color(0xFF241D10),
    0.82f to Color(0xFF17130B),
    1.00f to Color(0xFF0A0906),
)

/** Darkens the corners so the eye is pulled to the centre of the board. */
val BoardVignette = Brush.radialGradient(
    0.0f to Color.Transparent,
    0.55f to Color(0x00000000),
    1.0f to Color(0xCC000000),
)

/** A row of terrain. The player's own rows sit slightly warmer than the opponent's. */
fun rowBand(mine: Boolean): Brush = if (mine) {
    Brush.verticalGradient(listOf(Color(0xFF241C10), Color(0xFF1A150C), Color(0xFF241C10)))
} else {
    Brush.verticalGradient(listOf(Color(0xFF1E1810), Color(0xFF15110A), Color(0xFF1E1810)))
}

/** A row lit up because the selected card can be dropped on it. */
val RowBandLit = Brush.verticalGradient(
    listOf(Color(0xFF3A2E14), Color(0xFF2A2110), Color(0xFF3A2E14)),
)

/** Stand-in for card art: a lit figure-ground in the faction's colour. */
fun cardArt(faction: Faction): Brush {
    val p = factionPalette(faction)
    return Brush.radialGradient(
        0.0f to p.glow.copy(alpha = 0.34f),
        0.45f to p.accent.copy(alpha = 0.26f),
        1.0f to p.deep.copy(alpha = 0.95f),
    )
}

/** The name plate at the foot of a card. */
val NamePlate = Brush.verticalGradient(listOf(Color(0xCC0A0806), Color(0xF20A0806)))

class FactionPalette(val accent: Color, val deep: Color, val glow: Color)

fun factionPalette(faction: Faction): FactionPalette = when (faction) {
    Faction.NEUTRAL -> FactionPalette(Color(0xFFBFB39A), Color(0xFF2A2419), Color(0xFFE6DCC6))
    Faction.MONSTERS -> FactionPalette(Color(0xFF8E2F24), Color(0xFF2B0F0B), Color(0xFFD9695A))
    Faction.NILFGAARD -> FactionPalette(Color(0xFFC9A227), Color(0xFF1A1710), Color(0xFFF2D879))
    Faction.NORTHERN_REALMS -> FactionPalette(Color(0xFF3B7BC4), Color(0xFF0E1E33), Color(0xFF86BEF5))
    Faction.SCOIATAEL -> FactionPalette(Color(0xFF4E8C3A), Color(0xFF13260F), Color(0xFF93D477))
    Faction.SKELLIGE -> FactionPalette(Color(0xFF2E6F8E), Color(0xFF0C2029), Color(0xFF79C4E0))
    Faction.SYNDICATE -> FactionPalette(Color(0xFF7C4A9E), Color(0xFF22102C), Color(0xFFC08FE0))
}

fun factionAccent(faction: Faction): Color = factionPalette(faction).accent

fun factionLabel(faction: Faction): String = when (faction) {
    Faction.NEUTRAL -> "Neutral"
    Faction.MONSTERS -> "Monsters"
    Faction.NILFGAARD -> "Nilfgaard"
    Faction.NORTHERN_REALMS -> "Northern Realms"
    Faction.SCOIATAEL -> "Scoia'tael"
    Faction.SKELLIGE -> "Skellige"
    Faction.SYNDICATE -> "Syndicate"
}

fun factionMotto(faction: Faction): String = when (faction) {
    Faction.NEUTRAL -> "Sellswords and wanderers"
    Faction.MONSTERS -> "They attack in hordes, and consume their own kin"
    Faction.NILFGAARD -> "Diplomacy, subterfuge, and the long knife"
    Faction.NORTHERN_REALMS -> "Numbers, armour, and engines of war"
    Faction.SCOIATAEL -> "Ambushes, traps, and guerilla support"
    Faction.SKELLIGE -> "Death is a door they walk back through"
    Faction.SYNDICATE -> "No crime too hideous, for the right coin"
}

/**
 * The display face. GWENT-ExtraBold is proprietary and not bundled; drop it in at
 * `res/font/gwent_extrabold.ttf` and swap this to `FontFamily(Font(R.font.gwent_extrabold))`
 * for exact fidelity.
 */
val DisplayFamily = FontFamily.Serif

/** Power numbers are condensed in the real game, which is why they read at small sizes. */
val NumeralFamily = FontFamily.SansSerif

private val DeepShadow = Shadow(Color(0xCC000000), Offset(0f, 3f), 10f)

val DisplayTitle = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    letterSpacing = 4.sp,
    color = GoldLight,
    shadow = DeepShadow,
)

val SectionTitle = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    letterSpacing = 2.sp,
    color = GoldText,
)

val CardName = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 9.sp,
    letterSpacing = 0.2.sp,
    color = Color(0xFFEFE3CB),
    shadow = Shadow(Color(0xDD000000), Offset(0f, 1f), 3f),
)

/** The number in the corner gem. Colour is applied per-unit by power state. */
val PowerNumeral = TextStyle(
    fontFamily = NumeralFamily,
    fontWeight = FontWeight.Black,
    fontSize = 15.sp,
    color = Parchment,
    shadow = Shadow(Color(0xFF000000), Offset(0f, 1f), 2f),
)

val ScoreNumeral = TextStyle(
    fontFamily = NumeralFamily,
    fontWeight = FontWeight.Black,
    fontSize = 22.sp,
    color = Parchment,
    shadow = DeepShadow,
)

val BannerText = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    letterSpacing = 8.sp,
    color = GoldLight,
    shadow = DeepShadow,
)

val BodyText = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 12.sp,
    color = ParchmentDim,
)

/** Power text colour follows the live game: green above base, red below, parchment at base. */
fun powerColor(current: Int, base: Int): Color = when {
    current > base -> BoostGreen
    current < base -> DamageRed
    else -> Parchment
}

@Composable
fun GwentTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = GoldText,
        onPrimary = Ink,
        secondary = GoldBright,
        background = BoardDeep,
        surface = PanelBackground,
        onBackground = Parchment,
        onSurface = Parchment,
        outline = GoldDeep,
        error = DamageRed,
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
