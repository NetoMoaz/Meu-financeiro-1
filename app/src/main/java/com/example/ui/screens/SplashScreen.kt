package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.MontserratFontFamily
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Smoothly animate loading progress over 2.2 seconds
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
        )
        delay(200)
        onFinish()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAF8))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinish
            )
            .testTag("splash_screen")
    ) {
        val screenHeight = maxHeight
        val isCompact = screenHeight < 720.dp
        val isVeryCompact = screenHeight < 600.dp

        // Responsive sizes
        val logoSize = when {
            isVeryCompact -> 110.dp
            isCompact -> 135.dp
            else -> 165.dp
        }
        val phraseFontSize = if (isVeryCompact) 17.sp else 20.sp
        val titleMeuSize = if (isVeryCompact) 26.sp else 32.sp
        val titleFinanceiroSize = if (isVeryCompact) 30.sp else 38.sp
        val centerSpacing = if (isVeryCompact) 8.dp else if (isCompact) 14.dp else 20.dp

        // Dynamic background decorative waves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. TOP-LEFT WAVES
            val topLeftPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.58f, 0f)
                cubicTo(
                    w * 0.45f, h * 0.07f,
                    w * 0.18f, h * 0.11f,
                    0f, h * 0.15f
                )
                close()
            }
            drawPath(
                path = topLeftPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03603E),
                        Color(0xFF088055)
                    ),
                    startY = 0f,
                    endY = h * 0.15f
                )
            )

            // Secondary softer green wave at top-left
            val topLeftAccent = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.72f, 0f)
                cubicTo(
                    w * 0.52f, h * 0.10f,
                    w * 0.22f, h * 0.15f,
                    0f, h * 0.20f
                )
                close()
            }
            drawPath(
                path = topLeftAccent,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x3510B981),
                        Color(0x10059669)
                    )
                )
            )

            // 2. TOP-RIGHT GENTLE WAVE
            val topRightPath = Path().apply {
                moveTo(w * 0.72f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.11f)
                cubicTo(
                    w * 0.90f, h * 0.09f,
                    w * 0.80f, h * 0.04f,
                    w * 0.72f, 0f
                )
                close()
            }
            drawPath(
                path = topRightPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x35059669), Color(0x1810B981))
                )
            )

            // 3. BOTTOM FLOWING WAVES:
            // Calculate wave height dynamically: at least 180dp to guarantee pillars fit perfectly
            val minFooterPx = 180.dp.toPx()
            val footerPx = (h * 0.26f).coerceAtLeast(minFooterPx)
            val waveStartY = h - footerPx

            // Back wave: soft emerald highlight
            val bottomWaveBack = Path().apply {
                moveTo(0f, waveStartY + 15f)
                cubicTo(
                    w * 0.32f, waveStartY - 35f,
                    w * 0.68f, waveStartY + 35f,
                    w, waveStartY - 25f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = bottomWaveBack,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10B981),
                        Color(0xFF059669)
                    ),
                    startY = waveStartY - 35f,
                    endY = h
                )
            )

            // Front wave: deep rich emerald gradient forming the 4-pillars footer backdrop
            val bottomWaveFront = Path().apply {
                moveTo(0f, waveStartY)
                cubicTo(
                    w * 0.35f, waveStartY + 25f,
                    w * 0.65f, waveStartY + 30f,
                    w, waveStartY - 10f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = bottomWaveFront,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF04754A),
                        Color(0xFF02472F),
                        Color(0xFF012D1D)
                    ),
                    startY = waveStartY - 10f,
                    endY = h
                )
            )
        }

        // Foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Left and right motivational phrases
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = if (isVeryCompact) 4.dp else 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left phrase: "Planeje / Controle / Economize / Realize"
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Planeje\nControle\nEconomize\nRealize",
                        fontFamily = CaveatFontFamily,
                        fontSize = phraseFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF155A3C),
                        lineHeight = if (isVeryCompact) 19.sp else 22.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Canvas(modifier = Modifier.size(width = 54.dp, height = 8.dp)) {
                        val path = Path().apply {
                            moveTo(0f, 4f)
                            quadraticBezierTo(size.width * 0.5f, 8f, size.width, 2f)
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF10B981),
                            style = Stroke(width = 2.5f)
                        )
                    }
                }

                // Right phrase: "Disciplina hoje, / conquistas amanhã!"
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Disciplina\nhoje,\nconquistas\namanhã!",
                        fontFamily = CaveatFontFamily,
                        fontSize = phraseFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF064E3B),
                        textAlign = TextAlign.End,
                        lineHeight = if (isVeryCompact) 19.sp else 22.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Canvas(modifier = Modifier.size(width = 62.dp, height = 8.dp)) {
                        val path = Path().apply {
                            moveTo(0f, 6f)
                            quadraticBezierTo(size.width * 0.5f, 2f, size.width, 7f)
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF10B981),
                            style = Stroke(width = 2.5f)
                        )
                    }
                }
            }

            // CENTER SECTION: App Logo, Name, Tagline & Animated Progress
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .widthIn(max = 500.dp)
                    .padding(horizontal = 24.dp)
            ) {
                // Official 3D Tile Logo with rounded corners and drop shadow
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color(0x33000000),
                            spotColor = Color(0x4002472F)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF04754A)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_icon_asset),
                        contentDescription = "Logo Meu Financeiro",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(centerSpacing))

                // Brand Name: "Meu" centered above "Financeiro"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Meu",
                        fontFamily = MontserratFontFamily,
                        fontSize = titleMeuSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF008751),
                        lineHeight = titleMeuSize
                    )
                    Text(
                        text = "Financeiro",
                        fontFamily = MontserratFontFamily,
                        fontSize = titleFinanceiroSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF09291D),
                        lineHeight = titleFinanceiroSize
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Green Accent Bar
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF10B981))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Brand Tagline
                Text(
                    text = "SEU DINHEIRO\nEM BOAS MÃOS",
                    fontFamily = MontserratFontFamily,
                    fontSize = if (isVeryCompact) 11.sp else 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF065F46),
                    textAlign = TextAlign.Center,
                    letterSpacing = 3.sp,
                    lineHeight = if (isVeryCompact) 15.sp else 17.sp
                )

                Spacer(modifier = Modifier.height(centerSpacing))

                // Loading Progress Bar
                val progressValue = animProgress.value
                Box(
                    modifier = Modifier
                        .width(if (isVeryCompact) 180.dp else 210.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressValue)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF047857),
                                        Color(0xFF10B981)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Carregando...",
                    fontFamily = MontserratFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4B5563)
                )
            }

            // BOTTOM SECTION: 4 Brand Pillars sitting comfortably inside the Emerald Wave Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = if (isVeryCompact) 12.dp else 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillarItem(
                        iconRes = R.drawable.ic_splash_chart,
                        line1 = "MAIS",
                        line2 = "CONTROLE",
                        modifier = Modifier.weight(1f)
                    )

                    PillarDivider()

                    PillarItem(
                        iconRes = R.drawable.ic_splash_shield,
                        line1 = "MAIS",
                        line2 = "TRANQUILIDADE",
                        modifier = Modifier.weight(1f)
                    )

                    PillarDivider()

                    PillarItem(
                        iconRes = R.drawable.ic_splash_target,
                        line1 = "MAIS",
                        line2 = "CONQUISTAS",
                        modifier = Modifier.weight(1f)
                    )

                    PillarDivider()

                    PillarItem(
                        iconRes = R.drawable.ic_splash_sprout,
                        line1 = "UM AMANHÃ",
                        line2 = "MELHOR",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PillarItem(
    iconRes: Int,
    line1: String,
    line2: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = "$line1 $line2",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "$line1\n$line2",
            color = Color.White,
            fontFamily = MontserratFontFamily,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun PillarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(Color(0x38FFFFFF))
    )
}
