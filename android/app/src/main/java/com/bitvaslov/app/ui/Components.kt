package com.bitvaslov.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideogameAsset
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundGradient = Brush.verticalGradient(
    listOf(AppColors.Background, AppColors.Background, AppColors.Background)
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
    contentColor: Color = AppColors.ContentDark,
    height: Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) color else AppColors.ButtonDisabled,
            contentColor = if (enabled) contentColor else AppColors.ButtonDisabledText,
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

val AvatarColors = listOf(
    Color(0xFF7C4DFF),
    Color(0xFF00E5FF),
    Color(0xFFFF3D00),
    Color(0xFF00E676),
    Color(0xFFFFD600),
    Color(0xFFFF4081),
    Color(0xFF2979FF),
    Color(0xFFFF9100),
    Color(0xFF00BFA5),
    Color(0xFFD500F9),
    Color(0xFF64DD17),
    Color(0xFFF50057),
)

fun avatarColor(id: Int): Color =
    AvatarColors[((id % AvatarColors.size) + AvatarColors.size) % AvatarColors.size]

@Composable
fun PlayerAvatar(
    name: String,
    size: Dp = 40.dp,
    color: Color = AppColors.Primary,
    active: Boolean = true,
    avatarId: Int = -1,
    photo: String = "",
) {
    val base = if (avatarId >= 0) avatarColor(avatarId) else color
    val bitmap = remember(photo) {
        if (photo.isBlank()) null
        else try {
            val bytes = android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                when {
                    bitmap != null -> Color.Black
                    active -> base
                    else -> AppColors.SurfaceElevated.copy(alpha = 0.6f)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Bold,
                color = if (active) AppColors.ContentDark else AppColors.TextDisabled,
            )
        }
    }
}

@Composable
fun PlayerChip(
    name: String,
    status: String? = null,
    isTurn: Boolean = false,
    isEliminated: Boolean = false,
    isMe: Boolean = false,
    avatarId: Int = -1,
    photo: String = "",
    score: Int? = null,
    team: Int = 0,
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
                avatarId = avatarId,
                photo = photo,
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name + (if (isMe) " (ты)" else ""),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isEliminated) AppColors.TextDisabled else AppColors.TextPrimary,
                    )
                    if (team != 0) {
                        Text(
                            " T${team + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Secondary,
                        )
                    }
                }
                status?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (isEliminated) AppColors.Danger else AppColors.TextSecondary)
                }
            }
            score?.let {
                Text(
                    "$it",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.Primary,
                )
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
    mode: String = "classic",
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101318)),
        border = BorderStroke(1.dp, Color(0xFF2E323C)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF21242B))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$players/$maxPlayers",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${modeLabel(mode)} • Время на ход $timer секунд",
                    fontSize = 13.sp,
                    color = Color(0xFF9AA2B5),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onJoin,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Войти", modifier = Modifier.size(24.dp))
            }
        }
    }
}

private fun modeLabel(mode: String): String = when (mode) {
    "blitz" -> "Блиц"
    "marathon" -> "Марафон"
    "teams" -> "2v2"
    "duel" -> "Дуэль"
    else -> "Классика"
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Primary,
) {
    val fg = if (color == AppColors.Primary) AppColors.ContentDark else Color.White
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = fg,
        ),
        modifier = modifier.height(88.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun MainBottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(shape)
            .background(Color(0xFF0B0C10)),
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                Triple(2, Icons.Filled.Person, "Друзья"),
                Triple(0, Icons.Filled.Home, "Меню"),
                Triple(1, Icons.Filled.VideogameAsset, "Комнаты"),
            ).forEach { (tab, icon, label) ->
                val isActive = selected == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(tab) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) Color(0xFF26292F) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) Color.White else Color(0xFF9AA2B5),
                    )
                }
            }
        }
    }
}

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

@Composable
fun OptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fg = if (selected) AppColors.ContentDark else AppColors.TextPrimary
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AppColors.Primary else AppColors.SurfaceElevated,
            contentColor = fg,
        ),
        modifier = modifier.height(56.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun NumberTile(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fg = if (selected) AppColors.ContentDark else AppColors.TextPrimary
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AppColors.Primary else AppColors.SurfaceElevated,
            contentColor = fg,
            disabledContainerColor = AppColors.SurfaceElevated.copy(alpha = 0.5f),
            disabledContentColor = AppColors.TextSecondary.copy(alpha = 0.7f),
        ),
        modifier = modifier.height(52.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text("$value", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fg = if (selected) AppColors.ContentDark else AppColors.TextPrimary
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AppColors.Primary else AppColors.SurfaceElevated,
            contentColor = fg,
            disabledContainerColor = AppColors.SurfaceElevated.copy(alpha = 0.5f),
            disabledContentColor = AppColors.TextSecondary.copy(alpha = 0.6f),
        ),
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}