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
import ir.gwent.core.model.Faction

// Deep, desaturated battlefield tones so gold leaf and faction colors carry the eye.
val Ink = Color(0xFF05070A)
val BoardDeep = Color(0xFF0A0D12)
val BoardMid = Color(0xFF151B26)
val PanelBackground = Color(0xFF161B24)
val RowSlotBackground = Color(0xFF0E121A)
val RowSlotLit = Color(0xFF1A2231)
val CardBackground = Color(0xFF1A202A)
val CardBackgroundHi = Color(0xFF2C3543)

val GoldLight = Color(0xFFF7E6B6)
val GoldText = Color(0xFFE1C67C)
val GoldDeep = Color(0xFF7B5D22)
val HeroGold = Color(0xFFDCB74E)
val MutedText = Color(0xFF8C99AD)
val FrostTint = Color(0xFFAAD0EC)
val WeatherTint = Color(0xFF4A7BA6)
val DangerRed = Color(0xFFCE4F45)

/** Struck-metal look: dark at the edges, bright along the middle. */
val MetalGold = Brush.linearGradient(listOf(GoldDeep, GoldLight, Color(0xFFB08A38), GoldLight, GoldDeep))
val MetalSilver = Brush.linearGradient(listOf(Color(0xFF454E5C), Color(0xFFC3CDDA), Color(0xFF6A7484), Color(0xFF454E5C)))
val MetalBronze = Brush.linearGradient(listOf(Color(0xFF4A3520), Color(0xFFB98B54), Color(0xFF4A3520)))

class FactionPalette(val accent: Color, val deep: Color, val glow: Color)

fun factionPalette(faction: Faction): FactionPalette = when (faction) {
    Faction.PAHLAVAN -> FactionPalette(Color(0xFFD9AE3C), Color(0xFF4A3410), Color(0xFFFFD873))
    Faction.DIV -> FactionPalette(Color(0xFFB03A32), Color(0xFF3D1210), Color(0xFFFF7A6A))
    Faction.MARVEL -> FactionPalette(Color(0xFFE23B3B), Color(0xFF3F0E10), Color(0xFFFF7B7B))
    Faction.ONE_PIECE -> FactionPalette(Color(0xFF2E8BD6), Color(0xFF0E2B45), Color(0xFF6FC2FF))
    Faction.GREEK_MYTH -> FactionPalette(Color(0xFF6C8BEA), Color(0xFF17224A), Color(0xFFA8BEFF))
}

fun factionAccent(faction: Faction): Color = factionPalette(faction).accent

fun factionLabel(faction: Faction): String = when (faction) {
    Faction.PAHLAVAN -> "Pahlavans"
    Faction.DIV -> "Div"
    Faction.MARVEL -> "Marvel"
    Faction.ONE_PIECE -> "One Piece"
    Faction.GREEK_MYTH -> "Greek Myth"
}

fun factionMotto(faction: Faction): String = when (faction) {
    Faction.PAHLAVAN -> "Heroes of the Book of Kings"
    Faction.DIV -> "Demons of Mazandaran"
    Faction.MARVEL -> "Earth's mightiest"
    Faction.ONE_PIECE -> "Pirates of the Grand Line"
    Faction.GREEK_MYTH -> "Gods and heroes of Hellas"
}

private val DeepShadow = Shadow(Color(0xCC000000), Offset(0f, 3f), 10f)

val DisplayTitle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    letterSpacing = 3.sp,
    color = GoldLight,
    shadow = DeepShadow,
)

val SectionTitle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    letterSpacing = 2.sp,
    color = GoldText,
)

val CardName = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    letterSpacing = 0.3.sp,
    color = Color(0xFFEFE3CB),
    shadow = Shadow(Color(0xDD000000), Offset(0f, 1f), 3f),
)

val BannerText = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    letterSpacing = 6.sp,
    color = GoldLight,
    shadow = DeepShadow,
)

@Composable
fun GwentTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = GoldText,
        onPrimary = Ink,
        secondary = HeroGold,
        background = BoardDeep,
        surface = PanelBackground,
        onBackground = Color(0xFFE9EDF4),
        onSurface = Color(0xFFE9EDF4),
        outline = Color(0xFF3A4454),
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
