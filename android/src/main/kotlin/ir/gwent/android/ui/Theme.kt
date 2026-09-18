package ir.gwent.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ir.gwent.core.model.Faction

/** Deep battlefield backdrop so faction accent colors pop, phone-friendly (no light-mode glare). */
val BoardBackground = Color(0xFF0E1116)
val PanelBackground = Color(0xFF171B22)
val CardBackground = Color(0xFF20252E)
val RowSlotBackground = Color(0xFF141821)
val GoldText = Color(0xFFE8C77A)
val MutedText = Color(0xFF8B93A3)
val WeatherTint = Color(0xFF3E6B8A)
val HeroGold = Color(0xFFCBA135)

fun factionAccent(faction: Faction): Color = when (faction) {
    Faction.PAHLAVAN -> Color(0xFFC9A227) // sun-baked gold
    Faction.DIV -> Color(0xFF8E2A2A) // demon red
    Faction.MARVEL -> Color(0xFFD62828) // classic hero red
    Faction.ONE_PIECE -> Color(0xFF1F6FB2) // straw-hat blue
    Faction.GREEK_MYTH -> Color(0xFF5C7AEA) // aegean blue-violet
}

fun factionLabel(faction: Faction): String = when (faction) {
    Faction.PAHLAVAN -> "Pahlavans"
    Faction.DIV -> "Div"
    Faction.MARVEL -> "Marvel"
    Faction.ONE_PIECE -> "One Piece"
    Faction.GREEK_MYTH -> "Greek Myth"
}

@Composable
fun GwentTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = GoldText,
        secondary = HeroGold,
        background = BoardBackground,
        surface = PanelBackground,
        onBackground = Color(0xFFE7E9EE),
        onSurface = Color(0xFFE7E9EE),
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
