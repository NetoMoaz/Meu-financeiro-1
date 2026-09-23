package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.HeaderDarkGreen
import com.example.ui.theme.HeaderDarkGreenEnd
import com.example.ui.theme.GreenLightCardBorder
import com.example.ui.theme.ExpenseRed
import java.io.File
import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double, hide: Boolean = false): String {
    if (hide) return "R$ •••••"
    val ptBr = Locale("pt", "BR")
    val formatter = NumberFormat.getCurrencyInstance(ptBr)
    return formatter.format(amount)
}

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "restaurant", "alimentação", "restaurante" -> Icons.Default.Restaurant
        "shopping_cart", "mercado" -> Icons.Default.ShoppingCart
        "directions_car", "transporte" -> Icons.Default.DirectionsCar
        "home", "casa", "moradia" -> Icons.Default.Home
        "medical_services", "saúde", "saude" -> Icons.Default.MedicalServices
        "sports_esports", "lazer" -> Icons.Default.SportsEsports
        "school", "educação", "educacao" -> Icons.Default.School
        "payments", "salário / renda", "salario" -> Icons.Default.Payments
        "trending_up", "investimentos" -> Icons.Default.TrendingUp
        "credit_card" -> Icons.Default.CreditCard
        "account_balance" -> Icons.Default.AccountBalance
        else -> Icons.Default.Category
    }
}

@Composable
fun UserProfileAvatar(
    photoPath: String?,
    initials: String = "MF",
    size: Dp = 44.dp,
    fontSize: TextUnit = 16.sp,
    borderColor: Color = Color.White.copy(alpha = 0.8f),
    backgroundColor: Color = Color.White.copy(alpha = 0.22f),
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photoFile = remember(photoPath) {
        photoPath?.let { File(it) }?.takeIf { it.exists() }
    }

    if (photoFile != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoFile)
                .crossfade(true)
                .build(),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, borderColor, CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(1.5.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (initials.isNotBlank()) initials else "MF",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun AppTopBar(
    title: String,
    greeting: String? = null,
    isHomeScreen: Boolean = false,
    userName: String = "",
    userPhotoPath: String? = null,
    userInitials: String = "MF",
    onProfileClick: (() -> Unit)? = null,
    monthDisplay: String? = null,
    onPrevMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null,
    hideValues: Boolean,
    onToggleHideValues: () -> Unit
) {
    // Elegant dark green header as specified: "Use verde escuro apenas no cabeçalho e em elementos de destaque."
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(HeaderDarkGreen, HeaderDarkGreenEnd)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isHomeScreen) {
                        // Personalização na Tela Inicial:
                        // Quando houver um nome salvo, exibe de forma organizada:
                        // Olá, [Nome] 👋
                        // Meu Financeiro
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .then(if (onProfileClick != null) Modifier.clickable { onProfileClick() } else Modifier)
                                .testTag("home_header_profile")
                        ) {
                            UserProfileAvatar(
                                photoPath = userPhotoPath,
                                initials = "MF",
                                size = 46.dp,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalArrangement = Arrangement.Center
                            ) {
                                val trimmedName = userName.trim()
                                if (trimmedName.isNotEmpty()) {
                                    Text(
                                        text = "Olá, $trimmedName 👋",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "Meu Financeiro",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    // Se não houver nome cadastrado ou o nome estiver vazio:
                                    // não exibe "Olá,", vírgula, emoji nem espaço reservado.
                                    // Mostra somente "Meu Financeiro", perfeitamente alinhado verticalmente ao lado do avatar.
                                    Text(
                                        text = "Meu Financeiro",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        // Demais telas mantêm os títulos atuais com o mesmo avatar personalizado à esquerda
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .then(if (onProfileClick != null) Modifier.clickable { onProfileClick() } else Modifier)
                                .testTag("header_profile_section")
                        ) {
                            UserProfileAvatar(
                                photoPath = userPhotoPath,
                                initials = "MF",
                                size = 38.dp,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (greeting != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = greeting,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onToggleHideValues,
                        modifier = Modifier
                            .testTag("toggle_privacy_button")
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (hideValues) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (hideValues) "Mostrar valores" else "Ocultar valores",
                            tint = Color.White
                        )
                    }
                }

                if (monthDisplay != null && onPrevMonth != null && onNextMonth != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevMonth,
                            modifier = Modifier
                                .testTag("prev_month_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Mês anterior",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = monthDisplay,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier
                                .testTag("next_month_button")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Próximo mês",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Double,
    hideValues: Boolean,
    icon: ImageVector,
    iconColor: Color,
    valueColor: Color = iconColor,
    containerColor: Color = Color.White,
    borderColor: Color = GreenLightCardBorder,
    deficitText: String? = null
) {
    Card(
        modifier = modifier.border(0.75.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(value, hideValues),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (deficitText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deficitText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = ExpenseRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("deficit_previsto_text")
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun EmptyPlaceholder(
    message: String,
    subMessage: String? = null,
    icon: ImageVector = Icons.Default.Category
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(0.75.dp, GreenLightCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProgressBarCustom(
    progress: Float,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF10B981),
    trackColor: Color = Color(0xFFE2E8F0)
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor)
        )
    }
}
