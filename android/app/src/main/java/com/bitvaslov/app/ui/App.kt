package com.bitvaslov.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitvaslov.app.RoomState
import com.bitvaslov.app.RoomSummary
import com.bitvaslov.app.Screen
import com.bitvaslov.app.UiState
import com.bitvaslov.app.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App(vm: GameViewModel = viewModel()) {
    val state by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            vm.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
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
private fun ScreenTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun ConnectScreen(state: UiState, vm: GameViewModel) {
    val kb = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        ScreenTitle("Битва слов")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = vm::setServerUrl,
            label = { Text("Адрес сервера") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.myName,
            onValueChange = vm::setMyName,
            label = { Text("Твоё имя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                kb?.hide()
                vm.connect(state.serverUrl.trim(), state.myName.trim())
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = state.myName.isNotBlank() && state.serverUrl.isNotBlank(),
        ) {
            Text("Подключиться")
        }
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
                ScreenTitle("Битва слов")
                Spacer(Modifier.weight(1f))
                Text(state.myName, style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            CreateRoomCard(state, vm)
        }

        item { Spacer(Modifier.height(4.dp)) }
        item { Text("Публичные комнаты", style = MaterialTheme.typography.titleMedium) }

        items(state.rooms, key = { it.id }) { room ->
            RoomRow(room) { vm.joinRoomById(room.id) }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Войти по коду", style = MaterialTheme.typography.titleMedium)
                    var code by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Код (4 цифры)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = { if (code.isNotBlank()) vm.joinRoomByCode(code.trim()) }) {
                            Text("Войти")
                        }
                    }
                    OutlinedButton(onClick = vm::refreshRooms, modifier = Modifier.fillMaxWidth()) {
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

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Создать комнату", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { expandedForm = !expandedForm }) {
                    Text(if (expandedForm) "Свернуть" else "Развернуть")
                }
            }
            if (expandedForm) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    Text("Приватная (по коду)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dropdown(
                        label = "Таймер",
                        value = "${timer} сек",
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Создать")
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
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
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
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(room.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${room.players}/${room.maxPlayers} игроков · ${room.timer} сек",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onJoin) { Text("Войти") }
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
            ScreenTitle(room.name)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = vm::leaveRoom) { Text("Выйти") }
        }
        Text(
            "${room.players.size}/${room.maxPlayers} игроков · ход ${room.timer} сек · " +
                if (room.isPrivate) "приватная" else "публичная",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        room.code?.let {
            Text(
                "Код: $it",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
room.players.forEach { p ->
                        val tag = if (p.id == room.hostId) "  (создатель)" else ""
                        Text(p.name + tag, modifier = Modifier.padding(vertical = 4.dp))
                    }
            }
        }
        if (room.isHost) {
            Button(
                onClick = vm::startGame,
                enabled = room.players.size >= 2,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (room.players.size >= 2) "Начать игру" else "Ждём игроков...")
            }
        } else {
            Text("Ждём, пока создатель начнёт игру", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
            Text(room?.name ?: "Игра", style = MaterialTheme.typography.titleMedium)
            Countdown(deadlineAt = game.endIn, totalSeconds = game.timer)

            Text("Нужно слово на букву", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                (game.requiredLetter.ifEmpty { "?" }).uppercase(),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (game.lastWord.isNotEmpty()) {
                Text("Предыдущее слово: ${game.lastWord}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (game.myTurn && !game.finished) {
                var text by remember { mutableStateOf("") }
                val kb = LocalSoftwareKeyboardController.current
                LaunchedEffect(game.lastWord) { text = "" }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Твоё слово") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { kb?.hide(); if (text.isNotBlank()) vm.submitWord(text.trim()) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Отправить")
                }
            } else if (!game.finished) {
                val turnName = room?.players?.firstOrNull { it.id == game.turnPlayerId }?.name ?: "соперник"
                Text("Ход игрока: $turnName", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Игроки", style = MaterialTheme.typography.titleSmall)
                    room?.players?.forEach { p ->
                        Text(
                            (if (p.id == com.bitvaslov.app.MyIds.current) " (это ты)" else "") + " ${p.name}" + if (p.alive) "" else "  — выбыл",
                            color = if (p.alive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                }
            }
        }

        state.winner?.let { winner ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text(if (winner.id == com.bitvaslov.app.MyIds.current) "Ты победил!" else "Победил ${winner.name}") },
                text = { Text("Последний в игре") },
                confirmButton = {
                    Button(onClick = vm::playAgain) { Text("Играть ещё раз") }
                },
                dismissButton = {
                    OutlinedButton(onClick = vm::leaveRoom) { Text("Выйти") }
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
    Column {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        Text(
            "%.1f".format(left / 1000f) + " сек",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}