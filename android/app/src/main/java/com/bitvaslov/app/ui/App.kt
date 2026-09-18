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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
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

private val AccentBrush = Brush.horizontalGradient(listOf(Accent, Purple))

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
private fun Logo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(AccentBrush),
            contentAlignment = Alignment.Center,
        ) {
            Text("Б", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "БИТВА СЛОВ",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = Color.White,
        )
        Text(
            "слова на скорость",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = state.myName,
            onValueChange = vm::setMyName,
            label = { Text("Твоё имя") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                kb?.hide()
                vm.connect(state.serverUrl.trim(), state.myName.trim())
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = state.myName.isNotBlank(),
        ) {
            Text("Играть", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Сервер: bitva-slov.onrender.com",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
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
                Column(Modifier.weight(1f)) {
                    Text("Битва слов", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("выбери комнату", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    state.myName,
                    style = MaterialTheme.typography.titleMedium,
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
            RoomRow(room) { vm.joinRoomById(room.id) }
        }

        if (state.rooms.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Пока пусто — создай комнату",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2148)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Войти по коду", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    var code by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Код (4 цифры)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { if (code.isNotBlank()) vm.joinRoomByCode(code.trim()) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Войти", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = vm::refreshRooms, modifier = Modifier.align(Alignment.End)) {
                        Text("Обновить список")
                    }
                }
            }
        }

        state.error?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2148)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { expandedForm = !expandedForm },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (expandedForm) "Свернуть форму" else "＋ Создать комнату", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
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
private fun RoomRow(room: RoomSummary, onJoin: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2148)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    room.name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(room.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${room.players}/${room.maxPlayers} игроков · ${room.timer} сек",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onJoin, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Войти")
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
                Text(
                    "${room.players.size}/${room.maxPlayers} игроков · ход ${room.timer} сек · " + if (room.isPrivate) "приватная" else "публичная",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = vm::leaveRoom) { Text("Выйти") }
        }

        room.code?.let {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2148)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Код для входа", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        it,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        letterSpacing = 8.sp,
                        color = Accent,
                    )
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2148)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Игроки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                room.players.forEach { p ->
                    val tag = if (p.id == room.hostId) "  👑 создатель" else ""
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(Accent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(p.name.firstOrNull()?.uppercase() ?: "?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Accent)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(p.name + tag)
                    }
                }
            }
        }

        if (room.isHost) {
            Button(
                onClick = vm::startGame,
                enabled = room.players.size >= 2,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (room.players.size >= 2) "Начать игру" else "Ждём игроков...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Text("Ждём, когда создатель начнёт игру", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                Column {
                    Text(room?.name ?: "Игра", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val turnName = room?.players?.firstOrNull { it.id == game.turnPlayerId }?.name ?: "?"
                    Text(
                        if (game.finished) "Игра окончена" else (if (game.myTurn) "Твой ход!" else "Ходит: $turnName"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (game.myTurn) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            Countdown(deadlineAt = game.endIn, totalSeconds = game.timer)

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2148)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (game.myTurn) "Придумай слово на букву" else "Ход на букву",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        (game.requiredLetter.ifEmpty { "?" }).uppercase(),
                        fontSize = 88.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Accent,
                    )
                    if (game.lastWord.isNotEmpty()) {
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2148)),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Игра окончена", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Последний в игре:", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Button(
                    onClick = { kb?.hide(); if (text.isNotBlank()) vm.submitWord(text.trim()) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Отправить", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2148)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Игроки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    room?.players?.forEach { p ->
                        val isMe = p.id == MyIds.current
                        val turn = p.id == game.turnPlayerId
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Box(
                                Modifier.size(26.dp).clip(CircleShape).background(
                                    if (turn) Accent else Accent.copy(alpha = 0.2f)
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(p.name.firstOrNull()?.uppercase() ?: "?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (turn) Color.White else Accent)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${p.name}${if (isMe) " (ты)" else ""}${if (!p.alive) "  — выбыл" else ""}",
                                color = if (p.alive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }

        state.winner?.let { winner ->
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        if (winner.id == MyIds.current) "🏆 Ты победил!" else "🏆 Победил «${winner.name}»",
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = { Text("Последний в игре — ты не выбыл!") },
                confirmButton = {
                    Button(onClick = vm::playAgain, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                        Text("Играть ещё раз")
                    }
                },
                dismissButton = {
                    TextButton(onClick = vm::leaveRoom) { Text("Выйти") }
                },
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
    Column {
        LinearProgressIndicator(
            progress = { fraction },
            color = barColor,
            trackColor = Color(0xFF232A56),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).height(10.dp),
        )
        Text(
            "%.1f".format(left / 1000f) + " сек",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            color = barColor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}