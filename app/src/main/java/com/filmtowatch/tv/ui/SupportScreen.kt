package com.filmtowatch.tv.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.filmtowatch.tv.BuildConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

// Who made this app and how to support it. Forks: replace these values with your own.
private object About {
    const val APP = "Film to Watch"
    const val AUTHOR = "@0xProbabillity"
    const val X = "x.com/0xProbabillity"
    const val SOURCE = "github.com/0xProbabillity/film-to-watch-tv"
    const val INTRO = "Film to Watch is free and has no ads. If it helped you pick tonight's movie, a tip in crypto helps it grow. " +
        "Scan a code with your phone's wallet, and send each coin only over the network named on its card."
}

private data class Donation(val title: String, val network: String, val address: String)

// Each QR encodes the plain address, which every wallet understands.
private val DONATIONS = listOf(
    Donation("Bitcoin", "BTC · native SegWit address", "bc1q07hfz95crdalcrnywjavdpc4pdvllct83as5re"),
    Donation("Ethereum & co.", "Same address on Ethereum, Base, BNB Chain, Arbitrum", "0x4ed3aF428e938a0CC8e60df86D714B4a7F0928F0"),
    Donation("TON", "Toncoin and USDT on TON · no memo needed", "UQBCDKDp11tBIwTon4ItzTPcQgNGcaqTM6Gn31KwSN7qWdmM"),
    Donation("TRON", "TRX and USDT (TRC-20)", "TWtWLJp2Hc6242RynhU1B4Uujru3tppb3B"),
)

@Composable
fun SupportScreen(vm: AppViewModel) {
    val backFocus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize()) {
        BlurredBackdrop(vm.genreArt.values.firstOrNull(), dim = 0.82f)
        Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 30.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillButton("Back", onClick = vm::back, icon = Icons.AutoMirrored.Filled.ArrowBack, modifier = Modifier.focusRequester(backFocus))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = Palette.Pink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${About.APP} ${BuildConfig.VERSION_NAME} · made by ${About.AUTHOR}", style = MaterialTheme.typography.titleSmall)
                    }
                    Text("${About.X}  ·  ${About.SOURCE}", style = MaterialTheme.typography.labelMedium, color = Palette.Secondary)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Support the author", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(About.INTRO, style = MaterialTheme.typography.bodyMedium, color = Palette.Secondary)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                DONATIONS.forEach { DonationCard(it, Modifier.weight(1f)) }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }
}

@Composable
private fun DonationCard(donation: Donation, modifier: Modifier) {
    val qr = remember(donation.address) { qrBitmap(donation.address) }
    // Focusable so the remote can move across the cards; nothing happens on click
    Surface(
        onClick = {},
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.07f),
            focusedContainerColor = Color.White.copy(alpha = 0.16f),
            contentColor = Color.White,
            focusedContentColor = Color.White,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(donation.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                donation.network,
                style = MaterialTheme.typography.labelMedium,
                color = Palette.Secondary,
                textAlign = TextAlign.Center,
                minLines = 3,
                maxLines = 3,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .size(132.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(8.dp),
            ) {
                Image(qr, contentDescription = "QR code of the ${donation.title} address", filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(12.dp))
            Text(
                donation.address,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                minLines = 3,
            )
        }
    }
}

private fun qrBitmap(text: String): ImageBitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
    val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap.asImageBitmap()
}
