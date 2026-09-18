package com.bitvaslov.app.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitvaslov.app.GameViewModel
import com.bitvaslov.app.MyIds
import com.bitvaslov.app.Screen
import com.bitvaslov.app.UiState
import com.bitvaslov.app.WinnerInfo
import kotlinx.coroutines.delay

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
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        AppBackground {
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (state.screen) {
                    Screen.CONNECT -> ConnectScreen(state, vm)
                    Screen.LOBBY -> LobbyScreen(state, vm)
                    Screen.ROOM -> RoomScreen(state, vm)
                    Screen.GAME -> GameScreen(state, vm)
                    Screen.RESULT -> ResultScreen(state, vm)
                }

                if (state.showCreateRoom) {
                    CreateRoomDialog(state, vm)
                }
                if (state.showCodeEntry) {
                    CodeEntryDialog(vm)
                }
            }
        }
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
        Logo(size = 72.dp)
        Spacer(Modifier.height(20.dp))
        AnimatedTitle(primary = "БИТВА ", highlight = "СЛОВ", subtitle = "Думай быстро. Пиши первым.")
        Spacer(Modifier.height(32.dp))
        androidx.compose.material3.OutlinedTextField(
            value = state.myName,
            onValueChange = vm::setMyName,
            label = { Text("Как тебя называть?") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        NeonButton(
            text = "ИГРАТЬ",
            onClick = { kb?.hide(); vm.connect(state.serverUrl.trim(), state.myName.trim()) },
            enabled = state.myName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = AppColors.Danger)
        }
    }
}

@Composable
private fun LobbyScreen(state: UiState, vm: GameViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Logo(size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("БИТВА СЛОВ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Привет, ${state.myName}. Найди соперников", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NeonButton("СОЗДАТЬ КОМНАТУ", onClick = vm::requestCreateRoom, modifier = Modifier.fillMaxWidth())
                SecondaryButton("ВОЙТИ ПО КОДУ", onClick = vm::requestCodeEntry, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Text("Открытые комнаты", style = MaterialTheme.typography.headlineSmall)
        }

        items(state.rooms, key = { it.id }) { room ->
            RoomCard(
                name = room.name,
                players = room.players,
                maxPlayers = room.maxPlayers,
                timer = room.timer,
                onJoin = { vm.joinRoomById(room.id) },
            )
        }

        if (state.rooms.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("Пока пусто — создай комнату", color = AppColors.TextSecondary)
                }
            }
        }

        state.error?.let {
            item { Text(it, color = AppColors.Danger) }
        }
    }
}

@Composable
private fun CreateRoomDialog(state: UiState, vm: GameViewModel) {
    var roomName by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(15) }
    var maxP by remember { mutableStateOf(6) }

    AlertDialog(
        onDismissRequest = vm::dismissCreateRoom,
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Создание комнаты", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Название комнаты") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.Text("Приватность", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    Text(if (isPrivate) "Приватная (по коду)" else "Открытая (в списке)")
                }
                androidx.compose.material3.Text("Время на ход", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20, 30).forEach { t ->
                        androidx.compose.material3.FilterChip(
                            selected = timer == t,
                            onClick = { timer = t },
                            label = { Text("$t") },
                        )
                    }
                }
                androidx.compose.material3.Text("Игроков", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 5, 6).forEach { p ->
                        androidx.compose.material3.FilterChip(
                            selected = maxP == p,
                            onClick = { maxP = p },
                            label = { Text("$p") },
                        )
                    }
                }
                state.error?.let { Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            NeonButton(
                "СОЗДАТЬ КОМНАТУ",
                onClick = {
                    vm.createRoom(roomName.trim().ifBlank { "Комната" }, isPrivate, timer, maxP)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = {
            TextButton(onClick = vm::dismissCreateRoom, modifier = Modifier.fillMaxWidth()) {
                Text("Отмена", color = AppColors.TextSecondary)
            }
        },
    )
}

@Composable
private fun CodeEntryDialog(vm: GameViewModel) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = vm::dismissCodeEntry,
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Войти по коду", style = MaterialTheme.typography.headlineSmall) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter { c -> c.isDigit() }.take(4) },
                placeholder = { Text("4827") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium,
            )
        },
        confirmButton = {
            NeonButton("ВОЙТИ", onClick = {
                vm.dismissCodeEntry()
                if (code.isNotBlank()) vm.joinRoomByCode(code.trim())
            }, modifier = Modifier.fillMaxWidth())
        },
        dismissButton = {
            TextButton(onClick = vm::dismissCodeEntry, modifier = Modifier.fillMaxWidth()) {
                Text("Отмена", color = AppColors.TextSecondary)
            }
        },
    )
}

@Composable
private fun RoomScreen(state: UiState, vm: GameViewModel) {
    val room = state.room ?: return
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Комната", style = MaterialTheme.typography.headlineSmall)
                Text(room.name, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            }
            OutlinedButton(onClick = vm::leaveRoom, shape = RoundedCornerShape(18.dp)) { Text("Выйти") }
        }

        room.code?.let { code ->
            RoomCodeCard(code, onCopy = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(code))
                vm.copyRoomCode(code)
            })
        }

        NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Border) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Игроки (${room.players.size}/${room.maxPlayers})", style = MaterialTheme.typography.titleMedium)
                room.players.forEach { p ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(p.name, size = 36.dp, color = if (p.id == room.hostId) AppColors.Primary else AppColors.Secondary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            p.name + (if (p.id == room.hostId) "  (хост)" else ""),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }

        if (room.isHost) {
            NeonButton(
                text = if (room.players.size >= 2) "НАЧАТЬ БИТВУ" else "ОЖИДАНИЕ ИГРОКОВ",
                onClick = vm::startGame,
                enabled = room.players.size >= 2,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                "Ждём, когда создатель начнёт игру",
                textAlign = TextAlign.Center,
                color = AppColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { Text(it, color = AppColors.Danger) }
    }
}

@Composable
private fun GameScreen(state: UiState, vm: GameViewModel) {
    val game = state.game ?: return
    val room = state.room

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Игра", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (game.finished) "Игра окончена" else (if (game.myTurn) "Твой ход!" else "Ходит: ${room?.players?.firstOrNull { it.id == game.turnPlayerId }?.name ?: "..."}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (game.myTurn) AppColors.Primary else AppColors.TextSecondary,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            room?.players?.sortedByDescending { it.id == game.turnPlayerId }?.take(4)?.forEach { p ->
                PlayerChip(
                    name = p.name,
                    status = when {
                        !p.alive -> "Выбыл"
                        p.id == game.turnPlayerId -> if (p.id == MyIds.current) "Твой ход" else "Ход игрока"
                        else -> null
                    },
                    isTurn = p.id == game.turnPlayerId,
                    isEliminated = !p.alive,
                    isMe = p.id == MyIds.current,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LetterOrb(letter = game.requiredLetter, isActive = game.myTurn)
                Spacer(Modifier.height(16.dp))
                var now by remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(game) {
                    while (true) {
                        now = System.currentTimeMillis()
                        delay(100)
                    }
                }
                TimerBar(
                    remainingMs = (game.endIn - now).coerceAtLeast(0),
                    totalMs = game.timer.toLong() * 1000,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!game.finished && game.myTurn) {
            var text by remember { mutableStateOf("") }
            val kb = LocalSoftwareKeyboardController.current
            LaunchedEffect(game.lastWord) { text = "" }

            Column {
                GameInput(
                    value = text,
                    onValueChange = { text = it },
                    onSubmit = { kb?.hide(); if (text.isNotBlank()) vm.submitWord(text.trim()) },
                    isError = state.error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(state: UiState, vm: GameViewModel) {
    val winner = state.winner ?: WinnerInfo("", "")
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ПОБЕДИТЕЛЬ", style = MaterialTheme.typography.titleMedium, letterSpacing = 4.sp, color = AppColors.TextSecondary)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(110.dp).clip(RoundedCornerShape(50)).background(AppColors.Secondary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            PlayerAvatar(winner.name, size = 96.dp, color = AppColors.Primary)
        }
        Spacer(Modifier.height(20.dp))
        Text(winner.name, style = MaterialTheme.typography.headlineMedium, color = AppColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        Text("Поздравляем! Остался один в игре", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        Spacer(Modifier.height(32.dp))
        NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Border) {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("${state.game?.usedWords?.size ?: 0} слов")
                StatChip("${state.game?.timer ?: 0} сек ход")
                StatChip("битва", color = AppColors.Secondary)
            }
        }
        Spacer(Modifier.height(32.dp))
        NeonButton("ЕЩЁ РАЗ", vm::playAgain, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton("В ЛОББИ", vm::leaveRoom, modifier = Modifier.fillMaxWidth())
    }
}