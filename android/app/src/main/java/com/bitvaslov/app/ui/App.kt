package com.bitvaslov.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitvaslov.app.GameViewModel
import com.bitvaslov.app.MyIds
import com.bitvaslov.app.RoomState
import com.bitvaslov.app.RoomSummary
import com.bitvaslov.app.Screen
import com.bitvaslov.app.UiState
import kotlinx.coroutines.delay

private val Background = Brush.verticalGradient(
    listOf(Color(0xFF0A0C20), Color(0xFF151A3A), Color(0xFF1A1040))
)

private val CardColor = Color(0xFF1C2148)
private val CardBorder = Color(0xFF2E3363)
private val AccentBrush = Brush.horizontalGradient(listOf(Accent, Purple))
private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun App(vm: GameViewModel = viewModel()) {
    val state by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            vm.clearToast()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier.padding(padding).fillMaxSize().background(Background)
        ) {
            when (state.screen) {
                Screen.CONNECT -> ConnectScreen(state, vm)
                Screen.LOBBY -> LobbyScreen(state, vm)
                Screen.ROOM -> RoomScreen(state, vm)
                Screen.GAME -> GameScreen(state, vm)
            }
        }
    }
}

@Composable
private fun GlowButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Accent else Color(0xFF2E3363),
            contentColor = if (enabled) Color(0xFF01231A) else Color(0xFF9AA0D0),
        ),
        modifier = modifier
            .shadow(if (enabled) 12.dp else 0.dp, RoundedCornerShape(16.dp), clip = false)
            .height(54.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun Logo(size: Int = 72, letter: String = "Б") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(size.dp)
                .shadow(20.dp, CircleShape)
                .clip(CircleShape)
                .background(AccentBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, fontSize = (size * 0.55f).sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF01231A))
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color = Accent) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun RoomCard(room: RoomSummary, onJoin: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(room.name.firstOrNull()?.uppercase() ?: "?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(room.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("${room.players}/$room.maxPlayers ☺")
                    StatChip("${room.timer} сек")
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onJoin,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color(0xFF01231A),
                ),
            ) {
                Text("Войти", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlayerRow(p: com.bitvaslov.app.Player, turn: Boolean = false, alive: Boolean = true) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp)
                .clip(CircleShape)
                .background(if (turn) Accent else Accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                p.name.firstOrNull()?.uppercase() ?: "?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (turn) Color(0xFF01231A) else Accent,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            p.name,
            fontWeight = if (turn) FontWeight.Bold else FontWeight.Normal,
            color = if (alive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectScreen(state: UiState, vm: GameViewModel) {
    val kb = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo()
        Spacer(Modifier.height(16.dp))
        Text(
            "БИТВА СЛОВ",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
            color = Color.White,
        )
        Text(
            "придумай слово за секунды",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = state.myName,
            onValueChange = vm::setMyName,
            label = { Text("Твоё имя") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        GlowButton(
            text = "Играть",
            onClick = { kb?.hide(); vm.connect(state.serverUrl.trim(), state.myName.trim()) },
            enabled = state.myName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun LobbyScreen(state: UiState, vm: GameViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Logo(size = 44)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Битва слов", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("выбери комнату", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    state.myName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent,
                )
            }
        }

        item { CreateRoomCard(state, vm) }

        item {
            Text("Комнаты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(state.rooms, key = { it.id }) { room ->
            RoomCard(room) { vm.joinRoomById(room.id) }
        }

        if (state.rooms.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🃏", fontSize = 34.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Пока пусто — создай комнату", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = CardColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Войти по коду", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    var code by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Код (4 цифры)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GlowButton(
                        text = "Войти",
                        onClick = { if (code.isNotBlank()) vm.joinRoomByCode(code.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = vm::refreshRooms, modifier = Modifier.align(Alignment.End)) {
                        Text("Обновить список")
                    }
                }
            }
        }

        state.error?.let {
            item { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun CreateRoomCard(state: UiState, vm: GameViewModel) {
    var expandedForm by remember { mutableStateOf(false) }
    var roomName by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(15) }
    var maxP by remember { mutableStateOf(6) }

    Card(
        Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlowButton(
                text = if (expandedForm) "Свернуть форму" else "＋ Создать комнату",
                onClick = { expandedForm = !expandedForm },
                modifier = Modifier.fillMaxWidth(),
            )
            if (expandedForm) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Название (необязательно)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    Text("Приватная — вход по коду")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dropdown(
                        label = "Таймер",
                        value = "$timer сек",
                        items = listOf("5", "10", "15", "20", "30"),
                        onSelect = { timer = it.toInt() },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Dropdown(
                        label = "Игроков",
                        value = "$maxP",
                        items = listOf("2", "3", "4", "5", "6"),
                        onSelect = { maxP = it.toInt() },
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(
                    onClick = {
                        vm.createRoom(
                            roomName = roomName.trim().ifBlank { "Комната" },
                            isPrivate = isPrivate,
                            timer = timer,
                            maxPlayers = maxP,
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF01231A)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Создать и войти", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Dropdown(
    label: String,
    value: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("$label: $value")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = {
                    onSelect(it)
                    open = false
                })
            }
        }
    }
}

@Composable
private fun RoomScreen(state: UiState, vm: GameViewModel) {
    val room = state.room ?: RoomState("", "", false, null, "", 15, 6, "lobby", emptyList())
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(room.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    StatChip("${room.players.size}/$room.maxPlayers ☺")
                    StatChip("${room.timer} сек")
                    StatChip(if (room.isPrivate) "🔒 чужой код" else "публичная", color = Purple)
                }
            }
            OutlinedButton(onClick = vm::leaveRoom) { Text("Выйти") }
        }

        room.code?.let {
            Card(
                Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = CardColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Код для входа", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        it,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp,
                        letterSpacing = 10.sp,
                        color = Accent,
                    )
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = CardColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Игроки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                room.players.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(p.name.firstOrNull()?.uppercase() ?: "?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(p.name + if (p.id == room.hostId) "  👑" else "")
                    }
                }
            }
        }

        if (room.isHost) {
            GlowButton(
                text = if (room.players.size >= 2) "Начать игру" else "Ждём игроков...",
                onClick = vm::startGame,
                enabled = room.players.size >= 2,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                "Ждём, когда создатель начнёт игру",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun GameScreen(state: UiState, vm: GameViewModel) {
    val game = state.game ?: return
    val room = state.room

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(room?.name ?: "Игра", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val turnName = room?.players?.firstOrNull { it.id == game.turnPlayerId }?.name ?: "?"
                    Text(
                        if (game.finished) "Игра окончена" else (if (game.myTurn) "Твой ход!" else "Ходит: $turnName"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (game.myTurn) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (game.finished) {
                    StatChip("завершена")
                } else if (game.myTurn) {
                    StatChip("твой ход", color = Purple)
                }
            }

            Countdown(deadlineAt = game.endIn, totalSeconds = game.timer)

            Card(
                Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = CardColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (game.myTurn) "Придумай слово на букву" else "Слово на букву",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.size(120.dp)
                            .shadow(18.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (game.requiredLetter.ifEmpty { "?" }).uppercase(),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Accent,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                    if (game.lastWord.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "после слова: «${game.lastWord}»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (game.finished) {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Игра окончена", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Смотри, кто остался на поле:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (game.myTurn) {
                var text by remember { mutableStateOf("") }
                val kb = LocalSoftwareKeyboardController.current
                LaunchedEffect(game.lastWord) { text = "" }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Твоё слово") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                GlowButton(
                    text = "Отправить",
                    onClick = { kb?.hide(); if (text.isNotBlank()) vm.submitWord(text.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Card(
                Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = CardColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Игроки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    room?.players?.forEach { p ->
                        val isMe = p.id == MyIds.current
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Box(
                                Modifier.size(30.dp).clip(CircleShape).background(
                                    if (p.id == game.turnPlayerId) Accent else Accent.copy(alpha = 0.15f)
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    p.name.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (p.id == game.turnPlayerId) Color(0xFF01231A) else Accent,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${p.name}${if (isMe) " (ты)" else ""}${if (!p.alive) "  ✖" else ""}",
                                color = if (p.alive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (p.id == game.turnPlayerId && p.alive) {
                                Spacer(Modifier.width(8.dp))
                                StatChip("ходит", color = Accent)
                            }
                        }
                    }
                }
            }
        }

        state.winner?.let { winner ->
            AlertDialog(
                onDismissRequest = {},
                containerColor = CardColor,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        if (winner.id == MyIds.current) "🏆 Ты победил!" else "🏆 Победил «${winner.name}»",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Остался один в игре — круто!", textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        GlowButton(
                            text = "Играть ещё раз",
                            onClick = vm::playAgain,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = vm::leaveRoom, modifier = Modifier.fillMaxWidth()) {
                            Text("Выйти")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {},
            )
        }
    }
}

@Composable
private fun Countdown(deadlineAt: Long, totalSeconds: Int) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(deadlineAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(100)
        }
    }
    val left = (deadlineAt - now).coerceAtLeast(0)
    val total = (totalSeconds * 1000).coerceAtLeast(1)
    val fraction = (left.toFloat() / total).coerceIn(0f, 1f)
    val barColor = when {
        fraction > 0.5f -> Accent
        fraction > 0.25f -> Color(0xFFF5B83D)
        else -> Danger
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { fraction },
            color = barColor,
            trackColor = Color(0xFF232A56),
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).height(10.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "%.0f".format(left / 1000f) + "с",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = barColor,
        )
    }
}