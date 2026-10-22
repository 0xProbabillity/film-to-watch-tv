package com.filmtowatch.tv.ui

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.filmtowatch.tv.R
import java.util.Locale

/** Apple tvOS-like dark palette. */
object Palette {
    val Background = Color(0xFF07070A)
    val Glass = Color.White.copy(alpha = 0.12f)
    val GlassStrong = Color.White.copy(alpha = 0.2f)
    val GlassBorder = Color.White.copy(alpha = 0.14f)
    val Secondary = Color.White.copy(alpha = 0.64f)
    val Tertiary = Color.White.copy(alpha = 0.42f)
    val Pink = Color(0xFFFF375F)
    val Green = Color(0xFF30D158)
    val Red = Color(0xFFFF453A)
    val Imdb = Color(0xFFF5C518)
    val Tomato = Color(0xFFFA320A)
    val Metacritic = Color(0xFF66CC33)
}

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.inter(tracking: Double, weight: FontWeight) =
    copy(fontFamily = Inter, letterSpacing = tracking.sp, fontWeight = weight)

private val AppTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.inter(-1.6, FontWeight.Bold),
        displayMedium = t.displayMedium.inter(-1.2, FontWeight.Bold),
        displaySmall = t.displaySmall.inter(-0.9, FontWeight.Bold),
        headlineLarge = t.headlineLarge.inter(-0.7, FontWeight.Bold),
        headlineMedium = t.headlineMedium.inter(-0.5, FontWeight.Bold),
        headlineSmall = t.headlineSmall.inter(-0.4, FontWeight.SemiBold),
        titleLarge = t.titleLarge.inter(-0.3, FontWeight.SemiBold),
        titleMedium = t.titleMedium.inter(-0.1, FontWeight.SemiBold),
        titleSmall = t.titleSmall.inter(0.0, FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.inter(-0.1, FontWeight.Normal),
        bodyMedium = t.bodyMedium.inter(0.0, FontWeight.Normal),
        bodySmall = t.bodySmall.inter(0.0, FontWeight.Normal),
        labelLarge = t.labelLarge.inter(0.0, FontWeight.Medium),
        labelMedium = t.labelMedium.inter(0.2, FontWeight.SemiBold),
        labelSmall = t.labelSmall.inter(0.4, FontWeight.SemiBold),
    )
}

@Composable
fun FilmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Palette.Background,
            onBackground = Color.White,
            surface = Color(0xFF1C1C1E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2C2C2E),
            onSurfaceVariant = Palette.Secondary,
        ),
        typography = AppTypography,
    ) {
        // tv-material only sets the content color inside a Surface; make plain Text white everywhere
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}

/** tvOS-style capsule button: frosted when idle, solid white with black text when focused. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    large: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interaction,
        scale = ButtonDefaults.scale(focusedScale = 1.07f),
        colors = ButtonDefaults.colors(
            containerColor = Palette.Glass,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
            pressedContainerColor = Color.White.copy(alpha = 0.8f),
            pressedContentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContentColor = Color.White.copy(alpha = 0.3f),
        ),
        contentPadding = if (large) PaddingValues(horizontal = 32.dp, vertical = 16.dp) else PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (!focused && iconTint != null) iconTint else LocalContentColor.current,
                modifier = Modifier.size(if (large) 24.dp else 20.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
    }
}

/** Translucent rounded panel with a hairline border. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(20.dp), color: Color = Palette.Glass): Modifier =
    this.background(color, shape).border(BorderStroke(1.dp, Palette.GlassBorder), shape)

/**
 * Soft, blurred full-screen backdrop. Android 11 TV sticks have no blur effect, so we decode a tiny
 * version of the image and let it scale up, which gives the same soft look for free.
 */
@Composable
fun BlurredBackdrop(url: String?, modifier: Modifier = Modifier, dim: Float = 0.5f) {
    val context = LocalContext.current
    Box(modifier.fillMaxSize()) {
        Crossfade(targetState = url, animationSpec = tween(600), label = "backdrop") { target ->
            if (target != null) {
                AsyncImage(
                    model = remember(target) { ImageRequest.Builder(context).data(target).size(64, 36).build() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(24.dp) else Modifier),
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f), radius = 1400f)),
        )
    }
}

/** IMDb-style rating chip. */
@Composable
fun ImdbBadge(rating: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "IMDb",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            modifier = Modifier.background(Palette.Imdb, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(rating, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/** Small outlined badge, like Apple's "PG-13" / "4K" chips. */
@Composable
fun OutlineBadge(text: String, modifier: Modifier = Modifier, color: Color = Palette.Secondary) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .border(BorderStroke(1.dp, color.copy(alpha = 0.6f)), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun ScoreBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(6.dp))
        Text("$label $value", style = MaterialTheme.typography.titleSmall)
    }
}

fun formatRating(value: Double): String = String.format(Locale.US, "%.1f", value)
