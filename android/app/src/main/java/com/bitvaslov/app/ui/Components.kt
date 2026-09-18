package com.bitvaslov.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundGradient = Brush.verticalGradient(
    listOf(AppColors.Background, AppColors.BackgroundSecondary, AppColors.BackgroundGradientEnd)
)

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().background(BackgroundGradient)) {
        content()
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AppColors.Primary,
    height: Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) color else AppColors.ButtonDisabled,
            contentColor = if (enabled) AppColors.ContentDark else AppColors.ButtonDisabledText,
        ),
        modifier = modifier.height(height),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) AppColors.Secondary else AppColors.ButtonDisabled,
            contentColor = if (enabled) Color.White else AppColors.ButtonDisabledText,
        ),
        modifier = modifier.height(56.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AppColors.Border,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, borderColor),
    ) {
        content()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier,
    )
}

@Composable
fun StatChip(text: String, modifier: Modifier = Modifier, color: Color = AppColors.Primary, active: Boolean = true) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) color.copy(alpha = 0.18f) else AppColors.SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) color else AppColors.TextDisabled,
        )
    }
}

@Composable
fun Logo(letter: String = "Б", size: Dp = 72.dp, glow: Boolean = false) {
    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (glow) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
    )
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(AppColors.Primary.copy(alpha = 0.15f))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(size).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(AppColors.Primary, AppColors.PrimaryDark))),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.ExtraBold, color = AppColors.ContentDark)
        }
    }
}

@Composable
fun PlayerAvatar(name: String, size: Dp = 40.dp, color: Color = AppColors.Primary, active: Boolean = true) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) color else AppColors.SurfaceElevated.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            color = if (active) AppColors.ContentDark else AppColors.TextDisabled,
        )
    }
}

@Composable
fun PlayerChip(
    name: String,
    status: String? = null,
    isTurn: Boolean = false,
    isEliminated: Boolean = false,
    isMe: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isEliminated -> AppColors.Danger
        isTurn -> AppColors.Primary
        else -> AppColors.Border
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTurn) AppColors.SurfaceElevated else AppColors.Surface.copy(alpha = 0.9f)
        ),
        border = BorderStroke(if (isTurn) 2.dp else 1.dp, borderColor),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlayerAvatar(
                name,
                size = 36.dp,
                color = if (isTurn) AppColors.Primary else AppColors.Secondary,
                active = isTurn,
            )
            Column {
                Text(
                    name + (if (isMe) " (ты)" else ""),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isEliminated) AppColors.TextDisabled else AppColors.TextPrimary,
                )
                status?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (isEliminated) AppColors.Danger else AppColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun LetterOrb(letter: String, isActive: Boolean = true, size: Dp = 150.dp) {
    val transition = rememberInfiniteTransition()
    val glow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isActive) 1f else 0.6f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
    )
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(AppColors.Secondary.copy(alpha = glow * 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(AppColors.SurfaceElevated.copy(alpha = 0.9f))
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter.ifEmpty { "?" }.uppercase(),
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.Primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TimerBar(remainingMs: Long, totalMs: Long, modifier: Modifier = Modifier) {
    val fraction = (remainingMs.toFloat() / totalMs.coerceAtLeast(1)).coerceIn(0f, 1f)
    val barColor = when {
        fraction > 0.5f -> AppColors.Primary
        fraction > 0.2f -> AppColors.Warning
        else -> AppColors.Danger
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { fraction },
            color = barColor,
            trackColor = AppColors.Border,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).height(10.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "%.1f".format(remainingMs / 1000f).replace(',', '.'),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = barColor,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun RoomCard(
    name: String,
    players: Int,
    maxPlayers: Int,
    timer: Int,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, AppColors.Secondary.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$players/$maxPlayers", fontWeight = FontWeight.Bold, color = AppColors.Secondary)
                    Text("$timer сек", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(maxPlayers) { i ->
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(if (i < players) AppColors.Primary else AppColors.Border)
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onJoin,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Secondary,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("Войти", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.SurfaceElevated,
            contentColor = AppColors.TextPrimary,
        ),
        modifier = modifier.height(56.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun MainBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = AppColors.Surface, contentColor = AppColors.TextSecondary) {
        NavigationBarItem(
            selected = selected == 0,
            onClick = { onSelect(0) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Меню") },
            colors = navBarColors(),
        )
        NavigationBarItem(
            selected = selected == 1,
            onClick = { onSelect(1) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Комнаты") },
            colors = navBarColors(),
        )
        NavigationBarItem(
            selected = selected == 2,
            onClick = { onSelect(2) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Профиль") },
            colors = navBarColors(),
        )
    }
}

@Composable
private fun navBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AppColors.Primary,
    selectedTextColor = AppColors.Primary,
    indicatorColor = AppColors.SurfaceElevated,
    unselectedIconColor = AppColors.TextSecondary,
    unselectedTextColor = AppColors.TextSecondary,
)

@Composable
fun RoomCodeCard(code: String, onCopy: () -> Unit) {
    NeonCard(Modifier.fillMaxWidth().clickable(onClick = onCopy), borderColor = AppColors.Primary) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Код комнаты", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            Text(
                code,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 10.sp,
                color = AppColors.Primary,
            )
            Text("Нажми, чтобы скопировать", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            Text("Поделись кодом с друзьями", style = MaterialTheme.typography.bodySmall, color = AppColors.TextDisabled)
        }
    }
}

@Composable
fun GameInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String = "Введи слово...",
    enabled: Boolean = true,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = AppColors.TextDisabled, style = MaterialTheme.typography.bodyMedium) },
            enabled = enabled,
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Border,
                cursorColor = AppColors.Primary,
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                focusedContainerColor = AppColors.SurfaceElevated,
                unfocusedContainerColor = AppColors.SurfaceElevated,
            ),
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
        )
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onSubmit,
            enabled = enabled && value.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (enabled && value.isNotBlank()) AppColors.Primary else AppColors.ButtonDisabled,
                contentColor = if (enabled && value.isNotBlank()) AppColors.ContentDark else AppColors.ButtonDisabledText,
            ),
            modifier = Modifier.size(44.dp).padding(0.dp),
        ) {
            Text("➤", fontSize = 16.sp)
        }
    }
}

@Composable
fun AnimatedTitle(primary: String, highlight: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(primary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
            Text(highlight, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Primary)
        }
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
    }
}