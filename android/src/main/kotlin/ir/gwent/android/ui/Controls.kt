package ir.gwent.android.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.gwent.core.model.Row as GwentRow

/** A struck-metal control. Nothing on this board should look like a system widget. */
@Composable
fun CarvedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val face = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0xFF1B2029), Color(0xFF13171E)))
        primary -> Brush.verticalGradient(listOf(Color(0xFF4A3B1B), Color(0xFF241C0C)))
        else -> Brush.verticalGradient(listOf(Color(0xFF2B323F), Color(0xFF181D25)))
    }
    val edge = when {
        !enabled -> Brush.linearGradient(listOf(Color(0xFF2A303A), Color(0xFF2A303A)))
        primary -> MetalGold
        else -> MetalSilver
    }
    val label = when {
        !enabled -> Color(0xFF5B6675)
        primary -> GoldLight
        else -> Color(0xFFD4DBE6)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(face)
            .border(1.dp, edge, RoundedCornerShape(6.dp))
            .ornateCorners(tint = label.copy(alpha = 0.35f), inset = 2f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = label,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
        )
    }
}

/** A visible stack of cards rather than a number on a label. */
@Composable
fun CardPile(count: Int, label: String, width: Dp = 24.dp, height: Dp = 34.dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            if (count > 0) {
                val layers = count.coerceAtMost(3)
                repeat(layers) { i ->
                    CardBack(
                        width = width,
                        height = height,
                        modifier = Modifier.offset(x = (i * 1.5f).dp, y = -(i * 1.5f).dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(width = width, height = height)
                        .clip(RoundedCornerShape((width.value * 0.1f).dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0xFF2A3140), RoundedCornerShape((width.value * 0.1f).dp)),
                )
            }
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .offset(x = 9.dp, y = 11.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF2B323F), Color(0xFF12161D))))
                    .border(1.dp, MetalSilver, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$count", color = GoldText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(label, color = MutedText, fontSize = 7.sp, letterSpacing = 0.8.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

/** The carved shield at the head of a row, holding that row's score. */
@Composable
fun RowBanner(row: GwentRow, total: Int, weathered: Boolean, modifier: Modifier = Modifier) {
    val shown by animateIntAsState(targetValue = total, animationSpec = tween(360), label = "rowTotal")
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val shield = Path().apply {
                moveTo(w * 0.12f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.58f)
                quadraticBezierTo(w * 0.5f, h * 0.98f, w * 0.12f, h * 0.58f)
                close()
            }
            drawPath(
                shield,
                brush = Brush.verticalGradient(
                    if (weathered) listOf(Color(0xFF24394B), Color(0xFF101922))
                    else listOf(Color(0xFF2C3341), Color(0xFF12161D))
                ),
            )
            drawPath(shield, color = if (weathered) FrostTint.copy(alpha = 0.7f) else GoldDeep, style = Stroke(width = w * 0.045f))
            drawRowMark(
                row = row,
                centre = Offset(w * 0.5f, h * 0.26f),
                radius = w * 0.15f,
                tint = if (weathered) FrostTint else Color(0xFF8E9AAC),
            )
        }
        Text(
            text = shown.toString(),
            color = if (weathered) FrostTint else GoldText,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.offset(y = 4.dp),
        )
    }
}

/** The army's total, the biggest number on the board. */
@Composable
fun ArmyGem(total: Int, leading: Boolean, modifier: Modifier = Modifier) {
    val shown by animateIntAsState(targetValue = total, animationSpec = tween(420), label = "armyTotal")
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                if (leading) Brush.radialGradient(listOf(Color(0xFFFFEFC0), Color(0xFFB4841E)))
                else Brush.radialGradient(listOf(Color(0xFF39424F), Color(0xFF171C24)))
            )
            .border(2.dp, if (leading) MetalGold else MetalSilver, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = shown.toString(),
            color = if (leading) Color(0xFF14181F) else GoldText,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

/** Leader portrait, sat beside the board as in the real game. */
@Composable
fun LeaderBadge(
    name: String,
    accent: Color,
    used: Boolean,
    ready: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(width = 42.dp, height = 56.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = if (used) 0.25f else 0.8f), Color(0xFF0D1016))))
            .border(1.5.dp, if (ready) MetalGold else MetalSilver, RoundedCornerShape(5.dp))
            .then(if (onClick != null && ready) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * 0.42f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), center = c, radius = size.width * 0.7f),
                radius = size.width * 0.7f,
                center = c,
            )
            // A simple crowned silhouette.
            drawCircle(Color(0xFF0C0E13).copy(alpha = 0.75f), radius = size.width * 0.20f, center = c)
            val path = Path().apply {
                moveTo(size.width * 0.26f, size.height * 0.78f)
                quadraticBezierTo(size.width * 0.5f, size.height * 0.5f, size.width * 0.74f, size.height * 0.78f)
                close()
            }
            drawPath(path, Color(0xFF0C0E13).copy(alpha = 0.75f))
        }
        Text(
            text = name.first().toString(),
            color = if (used) Color(0xFF5B6675) else GoldLight,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.offset(y = (-4).dp),
        )
        if (ready) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(GoldLight),
            )
        }
    }
}

/** Two gems per army; lose both and the match is over. */
@Composable
fun Gems(lives: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        repeat(total) { index ->
            RoundPip(won = index < lives, modifier = Modifier.padding(end = 3.dp))
        }
    }
}

/** A thin lit strip used to separate the two armies. */
@Composable
fun CentreLine(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(10.dp)) {
        val midY = size.height / 2f
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, GoldDeep, GoldLight, GoldDeep, Color.Transparent)
            ),
            topLeft = Offset(0f, midY - 0.7f),
            size = androidx.compose.ui.geometry.Size(size.width, 1.4f),
        )
        val r = size.height * 0.36f
        val cx = size.width / 2f
        val diamond = Path().apply {
            moveTo(cx, midY - r)
            lineTo(cx + r, midY)
            lineTo(cx, midY + r)
            lineTo(cx - r, midY)
            close()
        }
        drawPath(diamond, GoldLight)
    }
}
