package com.bitvaslov.app.ui

import com.bitvaslov.app.R
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitvaslov.app.Player
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight

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

// Кэш декодированных фото — чтобы не декодировать Base64 на каждом кадре
private val avatarBitmaps = object : android.util.LruCache<String, android.graphics.Bitmap>(48) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.byteCount
}

private fun decodeAvatar(photo: String): android.graphics.Bitmap? {
    if (photo.isBlank()) return null
    avatarBitmaps.get(photo)?.let { return it }
    return try {
        val bytes = android.util.Base64.decode(photo, android.util.Base64.DEFAULT)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) avatarBitmaps.put(photo, bitmap)
        bitmap
    } catch (e: Exception) {
        null
    }
}

// Доминирующий цвет фото — чтобы плашки вокруг авы были её продолжением, а не фиолетовым
fun avatarAccent(photo: String, avatarId: Int): Color {
    if (photo.isBlank()) return avatarColor(avatarId)
    val bmp = decodeAvatar(photo) ?: return avatarColor(avatarId)
    return try {
        val s = 8
        val sm = android.graphics.Bitmap.createScaledBitmap(bmp, s, s, true)
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (x in 0 until s) for (y in 0 until s) {
            val c = sm.getPixel(x, y)
            r += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF; n++
        }
        Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    } catch (e: Exception) {
        avatarColor(avatarId)
    }
}

@Composable
fun PlayerAvatar(
    name: String,
    size: Dp = 40.dp,
    color: Color = AppColors.Primary,
    active: Boolean = true,
    avatarId: Int = -1,
    photo: String = "",
    online: Boolean = false,
) {
    val base = if (avatarId >= 0) avatarColor(avatarId) else color
    val bitmap = remember(photo) { decodeAvatar(photo) }
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
            )
            .then(
                if (online) Modifier.border(3.dp, Color(0xFF2ECC71), CircleShape)
                else Modifier
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
fun RoomCard(
    name: String,
    players: Int,
    maxPlayers: Int,
    timer: Int,
    mode: String = "classic",
    isPrivate: Boolean = false,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF242426)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$players/$maxPlayers",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isPrivate) "Закрытая • $players/$maxPlayers"
                    else "Открытая • ${modeLabel(mode)} • $timer сек",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(10.dp))
            if (isPrivate) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1C1C1F))
                        .border(1.dp, Color(0xFF2A2A2D), RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onJoin() }
                        .height(40.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(R.drawable.ic_lock), null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Пароль",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            } else {
                Button(
                    onClick = onJoin,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1C1C1E),
                    ),
                    contentPadding = PaddingValues(6.dp),
                    modifier = Modifier.width(64.dp).height(44.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Войти", modifier = Modifier.size(22.dp))
                }
            }
        }
        // Тонкий серый разделитель между комнатами
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.dp)
                .background(Color(0xFF2C2C2E))
        )
    }
}

/** Единая подпись режима для всех экранов (раньше были два дубля с разным текстом). */
fun modeLabel(mode: String): String = when (mode) {
    "blitz" -> "Блиц"
    "marathon" -> "Марафон"
    "duel" -> "Дуэль"
    "teams" -> "2 на 2"
    else -> "Классика"
}

private data class NavItem(
    val tab: Int,
    val iconRes: Int,
    val label: String,
    val hasBadge: Boolean = false,
)

@Composable
fun MainBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    useGlass: Boolean = true,
) {
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .then(
                if (useGlass) {
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                blur(9f.dp.toPx())
                            },
                            highlight = { Highlight(alpha = 0.16f) },
                            onDrawSurface = {
                                // Матовое тёмное «стекло» из макета (#121214), слегка прозрачное, чтобы был живёт blur
                                drawRect(Color(0xFF121214).copy(alpha = 0.88f))
                                // Лёгкие светлые края стекла
                                drawRect(
                                    Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.White.copy(alpha = 0.18f),
                                            0.12f to Color.White.copy(alpha = 0f),
                                            0.88f to Color.White.copy(alpha = 0f),
                                            1f to Color.White.copy(alpha = 0.18f),
                                        )
                                    )
                                )
                            },
                        )
                } else {
                    Modifier.clip(shape).background(Color(0xFF121214))
                }
            )
            .clip(shape),
    ) {
        Row(
            Modifier.fillMaxSize()
                .padding(top = 6.dp, start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val navItems = listOf(
                NavItem(2, R.drawable.ic_friends, "Друзья", hasBadge = true),
                NavItem(0, R.drawable.ic_menu, "Меню"),
                NavItem(1, R.drawable.ic_rooms, "Комнаты"),
            )
            navItems.forEach { (tab, iconRes, label, badge) ->
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isActive) Color(0xFF2C2C2E)
                                else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(iconRes),
                                contentDescription = label,
                                modifier = Modifier.size(30.dp),
                            )
                            if (badge) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2E)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = if (isActive) Color.White else Color(0xFF8E8E93),
                                        modifier = Modifier.size(6.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) Color.White else Color(0xFF8E8E93),
                    )
                }
            }
        }
    }
}

@Composable
fun RoomPasswordCard(password: String, onCopy: () -> Unit) {
    NeonCard(Modifier.fillMaxWidth().clickable(onClick = onCopy), borderColor = Color(0xFF2A2A2D)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Пароль комнаты", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
            Text(
                password,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Text("Нажми, чтобы скопировать", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            Text("Скажи его друзьям, чтобы они вошли", style = MaterialTheme.typography.bodySmall, color = AppColors.TextDisabled)
        }
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
    val fg = if (selected) Color(0xFF0B0B0B) else AppColors.TextPrimary
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color.White else AppColors.SurfaceElevated,
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

private val GlassTint = Color(0xFF121212)
private val PillShape = RoundedCornerShape(percent = 50)

private val GradientPeriodMs = 8000

val GameAccents: List<Color> = listOf(
    Color(0xFF6B33D6),
    Color(0xFF2F6BFF),
    Color(0xFF00BCD4),
    Color(0xFF12B76A),
    Color(0xFFFFB020),
    Color(0xFFFF4D6D),
    Color(0xFFFF3DAE),
    Color(0xFF9AA2B5),
)

private fun accentDeep(accent: Color) = Color(
    red = accent.red * 0.34f,
    green = accent.green * 0.34f,
    blue = accent.blue * 0.34f,
)

private fun accentViolet(accent: Color) = Color(
    red = accent.red * 0.62f,
    green = accent.green * 0.62f,
    blue = accent.blue * 0.62f,
)

private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun waveStops(color: Color, peakAlpha: Float, steps: Int = 40): Array<Pair<Float, Color>> {
    val stops = ArrayList<Pair<Float, Color>>(steps + 1)
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val v = smoothStep(0.14f, 0.44f, t) * (1f - smoothStep(0.56f, 0.86f, t))
        stops.add(t to color.copy(alpha = peakAlpha * v))
    }
    return stops.toTypedArray()
}

@Composable
fun AnimatedGameBackdrop(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gameBackdrop")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GradientPeriodMs, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )
    Canvas(modifier) {
        val h = size.height
        val w = size.width
        val deep = accentDeep(accent)
        val bright = accent
        val violet = accentViolet(accent)
        val neutral = Color(0xFF0A0A0A)
        drawRect(neutral)
        drawRect(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to deep,
                    0.18f to lerp(deep, neutral, 0.35f),
                    0.44f to lerp(deep, neutral, 0.80f),
                    0.66f to neutral,
                    1f to neutral,
                ),
                startY = 0f,
                endY = h * 0.7f,
            )
        )
        val p = h * 1.12f
        var startY = wave * p - p
        while (startY < h) {
            drawRect(
                Brush.verticalGradient(
                    colorStops = waveStops(bright, 0.42f),
                    startY = startY,
                    endY = startY + p,
                )
            )
            startY += p
        }
        drawRect(
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.5f to violet.copy(alpha = 0.12f),
                    1f to Color.Transparent,
                ),
                startX = 0f,
                endX = w,
            )
        )
        drawRect(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(alpha = 0.20f),
                    0.32f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.28f),
                ),
                startY = 0f,
                endY = h,
            )
        )
    }
}

fun Modifier.glassSurface(
    shape: Shape,
    tint: Color = GlassTint,
    alpha: Float = 0.72f,
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.10f),
                tint.copy(alpha = alpha),
                Color.White.copy(alpha = 0.05f),
            )
        )
    )
    .border(1.dp, Color.White.copy(alpha = 0.14f), shape)

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    shape: Shape = PillShape,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .height(height)
            .glassSurface(shape),
        contentAlignment = contentAlignment,
    ) { content() }
}

@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .glassSurface(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun LetterRing(
    letter: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    accent: Color = AppColors.Secondary,
) {
    val safeFraction = fraction.coerceIn(0f, 1f)
    val ringWidth = 10.dp
    val glowBase = accent
    val glowSoft = accentViolet(accent)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = ringWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            val urgency = 1f - safeFraction
            val glowRadius = this.size.width * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowBase.copy(alpha = 0.06f + 0.34f * urgency),
                        glowSoft.copy(alpha = 0.10f + 0.20f * urgency),
                        Color.Transparent,
                    ),
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(this.size.width / 2f, this.size.height / 2f),
            )
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            if (safeFraction > 0f) {
                val arcBrush = Brush.sweepGradient(
                    listOf(
                        glowBase,
                        lerp(glowBase, Color.White, 0.55f),
                        glowBase,
                    )
                )
                val sweep = 360f * safeFraction
                val cap = androidx.compose.ui.graphics.StrokeCap.Round
                for (glow in intArrayOf(34, 22, 14)) {
                    drawArc(
                        brush = arcBrush,
                        alpha = 0.055f + 0.045f * urgency,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = stroke + glow,
                            cap = cap,
                        ),
                    )
                }
                drawArc(
                    brush = arcBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke + 3f * urgency,
                        cap = cap,
                    ),
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(ringWidth + 7.dp)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter.ifEmpty { "?" }.uppercase(),
                fontSize = (size.value * 0.52f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun OverlappingAvatars(
    players: List<Player>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 24.dp,
    max: Int = 6,
    overlap: Dp = 10.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        players.take(max).forEachIndexed { index, p ->
            Box(
                Modifier
                    .offset(x = -(overlap * index))
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(GlassTint)
                    .padding(1.5.dp)
                    .clip(CircleShape),
            ) {
                PlayerAvatar(
                    name = p.name,
                    size = avatarSize - 3.dp,
                    avatarId = p.avatarId,
                    photo = p.photo,
                )
            }
        }
        if (players.size > max) {
            Box(
                Modifier
                    .offset(x = -(overlap * max))
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+${players.size - max}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun TurnInfoPlate(
    myTurn: Boolean,
    playerName: String,
    themeTitle: String,
    accent: Color = AppColors.Secondary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.09f),
                        GlassTint.copy(alpha = 0.70f),
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (myTurn) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ВАШ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "ХОД",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Ход:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    playerName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Тема",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                ":",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                themeTitle.ifEmpty { "Классика" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun StreakChip(
    streak: Int,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
) {
    GlassPill(modifier.height(height)) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$streak",
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(Modifier.width(9.dp))
            Icon(
                Icons.Filled.Whatshot,
                contentDescription = "Стрик",
                tint = Color.White,
                modifier = Modifier.size(27.dp),
            )
        }
    }
}

@Composable
fun GlassWordInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Введи слово...",
    enabled: Boolean = true,
    onSend: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focus = remember { FocusRequester() }
    Box(
        modifier = modifier
            .height(68.dp)
            .glassSurface(PillShape, alpha = 0.78f)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) { if (enabled) focus.requestFocus() },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                color = Color(0xFF8E8E93),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 22.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(AppColors.Secondary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .focusRequester(focus),
        )
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF3A1A1E))
            .border(1.dp, Color(0xFF7A2A32), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFFF8A8A),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            message,
            color = Color(0xFFFFD9D9),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Компактное табло очков во время партии.
 * Показывает очки, текущего игрока и помечает выбывших — раньше это было видно только в финале.
 */
@Composable
fun ScoreStrip(
    players: List<Player>,
    scores: Map<String, Int>,
    turnPlayerId: String?,
    modifier: Modifier = Modifier,
) {
    if (players.isEmpty()) return
    val ordered = remember(players, scores) {
        players.sortedByDescending { scores[it.id] ?: 0 }
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(ordered, key = { it.id }) { p ->
            val score = scores[p.id] ?: 0
            val isTurn = p.id == turnPlayerId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isTurn) Color(0xFF241C33) else Color(0xFF151517))
                    .border(
                        width = if (isTurn) 1.dp else 0.dp,
                        color = if (isTurn) Color(0xFF6B33D6) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    p.name.take(8).ifBlank { "—" },
                    fontSize = 13.sp,
                    fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Medium,
                    color = if (p.alive) Color.White else Color(0xFF636366),
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "$score",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (p.alive) Color(0xFFB794F6) else Color(0xFF48484A),
                )
                if (!p.alive) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = "Выбыл",
                        tint = Color(0xFF636366),
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}
