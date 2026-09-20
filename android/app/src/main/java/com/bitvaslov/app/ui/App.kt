package com.bitvaslov.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitvaslov.app.*
import kotlinx.coroutines.delay

private fun encodeAvatar(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = android.graphics.BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return null
        val maxSide = 160f
        val scale = minOf(maxSide / original.width, maxSide / original.height, 1f)
        val scaled = android.graphics.Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        val out = java.io.ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
        android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
    } catch (e: Exception) { null }
}

@Composable
fun App(vm: GameViewModel = viewModel()) {
    val state by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var bottomTab by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { vm.autoConnect() }
    LaunchedEffect(state.toast) { state.toast?.let { snackbar.showSnackbar(it); vm.clearToast() } }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (state.screen == Screen.LOBBY) MainBottomBar(bottomTab) { bottomTab = it; if(it==2) vm.requestFriends() } }
    ) { padding ->
        AppBackground {
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (state.screen) {
                    Screen.CONNECT -> ConnectScreen(state, vm)
                    Screen.LOBBY -> LobbyScreen(state, vm, bottomTab)
                    Screen.CREATEROOM -> CreateRoomScreen(state, vm)
                    Screen.CODEENTRY -> CodeEntryScreen(state, vm)
                    Screen.SETTINGS -> SettingsScreen(state, vm)
                    Screen.ROOM -> RoomScreen(state, vm)
                    Screen.GAME -> GameScreen(state, vm)
                    Screen.RESULT -> ResultScreen(state, vm)
                }
            }
        }
    }

    state.incomingInvite?.let { invite ->
        AlertDialog(
            onDismissRequest = { vm.declineInvite() },
            containerColor = AppColors.Surface,
            title = { Text("Приглашение") },
            text = { Text("${invite.fromName} зовёт в «${invite.roomName}»") },
            confirmButton = { TextButton(onClick = { vm.acceptInvite() }) { Text("ВОЙТИ", color = AppColors.Primary) } },
            dismissButton = { TextButton(onClick = { vm.declineInvite() }) { Text("Позже") } }
        )
    }
}

@Composable
private fun ConnectScreen(state: UiState, vm: GameViewModel) {
    val kb = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Logo(size = 72.dp)
        Spacer(Modifier.height(20.dp))
        AnimatedTitle("БИТВА ", "СЛОВ", "Думай быстро. Пиши первым.")
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(state.myName, vm::setMyName, label = { Text("Имя") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        NeonButton("ИГРАТЬ", { kb?.hide(); vm.connect(state.serverUrl.trim(), state.myName.trim()) }, enabled = state.myName.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        state.error?.let { Text(it, color = AppColors.Danger, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun LobbyScreen(state: UiState, vm: GameViewModel, tab: Int) {
    when (tab) {
        1 -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { AnimatedTitle("", "КОМНАТЫ", "Присоединяйся к игре") }
            roomsItems(state, vm)
        }
        2 -> FriendsTab(state, vm)
        3 -> ProfileTab(state, vm)
        else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Битва слов", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { vm.toggleSettings() }) { Icon(Icons.Filled.Settings, null, tint = Color.White) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatar(state.myName.ifBlank { "И" }, size = 52.dp, avatarId = state.myAvatarId, photo = state.myPhoto)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Привет, ${state.myName}!", style = MaterialTheme.typography.titleLarge)
                        Text("Удачи в бою!", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionButton(Icons.Filled.Add, "Создать", vm::requestCreateRoom, Modifier.weight(1f), AppColors.Primary)
                    ActionButton(Icons.Filled.Lock, "По коду", vm::requestCodeEntry, Modifier.weight(1f), AppColors.Secondary)
                }
            }
            item { Text("Открытые игры", style = MaterialTheme.typography.titleLarge) }
            roomsItems(state, vm)
        }
    }
}

@Composable
private fun FriendsTab(state: UiState, vm: GameViewModel) {
    var query by remember { mutableStateOf("") }
    val kb = LocalSoftwareKeyboardController.current
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AnimatedTitle("", "ДРУЗЬЯ", "Найди своих") }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, {query=it}, label={Text("Поиск")}, singleLine=true, shape=RoundedCornerShape(18.dp), modifier=Modifier.weight(1f))
                IconButton({ if(query.isNotBlank()){ kb?.hide(); vm.searchUser(query) } }) { Icon(Icons.Filled.Search, null, tint=AppColors.Primary) }
            }
        }
        if(state.searchResults.isNotEmpty()){
            item { Text("Результаты поиска") }
            items(state.searchResults) { user ->
                NeonCard(Modifier.fillMaxWidth(), AppColors.Secondary) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(user.name, 44.dp, avatarId=user.avatarId, photo=user.photo)
                        Column(Modifier.weight(1f).padding(start=12.dp)) {
                            Text(user.name, color=AppColors.TextPrimary); Text(if(user.online)"в сети" else "не в сети", style=MaterialTheme.typography.bodySmall, color=if(user.online)AppColors.Primary else AppColors.TextSecondary)
                        }
                        IconButton({ vm.addFriend(user.id) }) { Icon(Icons.Filled.PersonAdd, null, tint=AppColors.Primary) }
                    }
                }
            }
        }
        item { Text("Мои друзья", style = MaterialTheme.typography.titleMedium) }
        items(state.friends) { f ->
            NeonCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatar(f.name, 44.dp, avatarId=f.avatarId, photo=f.photo)
                    Column(Modifier.weight(1f).padding(start=12.dp)) {
                        Text(f.name); Text(if(f.online)"онлайн" else "оффлайн", color=if(f.online)AppColors.Primary else AppColors.TextSecondary, style=MaterialTheme.typography.bodySmall)
                    }
                    if(f.online) IconButton({ vm.inviteFriend(f.id) }) { Icon(Icons.Filled.GroupAdd, null, tint=AppColors.Primary) }
                    IconButton({ vm.removeFriend(f.id) }) { Icon(Icons.Filled.PersonRemove, null, tint=AppColors.TextSecondary) }
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(state: UiState, vm: GameViewModel) {
    var nameDraft by remember { mutableStateOf(state.myName) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) encodeAvatar(context, uri)?.let { vm.setPhoto(it) }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            PlayerAvatar(state.myName, 96.dp, avatarId=state.myAvatarId, photo=state.myPhoto)
            Row(Modifier.padding(top=12.dp), Arrangement.spacedBy(10.dp)) {
                OutlinedButton({ picker.launch("image/*") }) { Text("ФОТО", color=AppColors.Primary) }
                if(state.myPhoto.isNotBlank()) OutlinedButton(vm::clearPhoto) { Text("УБРАТЬ", color=AppColors.Danger) }
            }
            OutlinedTextField(nameDraft, {nameDraft=it}, label={Text("Имя")}, singleLine=true, shape=RoundedCornerShape(18.dp), modifier=Modifier.fillMaxWidth().padding(top=16.dp))
            NeonButton("СОХРАНИТЬ", { vm.setMyName(nameDraft) }, Modifier.fillMaxWidth().padding(top=8.dp))
        }
        item {
            NeonCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
                    Text("Статистика", style=MaterialTheme.typography.titleMedium)
                    Row(Arrangement.spacedBy(8.dp)) {
                        StatChip("Игр: ${state.stats?.games ?: 0}", Modifier.weight(1f)); StatChip("Побед: ${state.stats?.wins ?: 0}", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.roomsItems(state: UiState, vm: GameViewModel) {
    items(state.rooms, key = { it.id }) { room -> RoomCard(room.name, room.players, room.maxPlayers, room.timer, room.mode) { vm.joinRoomById(room.id) } }
    if(state.rooms.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(vertical=24.dp), Alignment.Center) { Text("Пока пусто...", color=AppColors.TextSecondary) } }
}

@Composable
private fun CreateRoomScreen(state: UiState, vm: GameViewModel) {
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("classic") }
    var minL by remember { mutableStateOf(0) }
    var theme by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            IconButton(vm::dismissCreateRoom) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Новая комната", style=MaterialTheme.typography.titleLarge, modifier=Modifier.weight(1f), textAlign=TextAlign.Center)
        }
        OutlinedTextField(name, {name=it}, label={Text("Название")}, singleLine=true, modifier=Modifier.fillMaxWidth())
        Text("Режим игры")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("classic" to "Классика", "blitz" to "Блиц", "marathon" to "Марафон", "teams" to "Команды")) { (k, l) ->
                ModeChip(l, mode == k) { mode = k }
            }
        }
        Text("Усложнения")
        Row(Arrangement.spacedBy(8.dp)) {
            listOf(0, 3, 4, 5).forEach { len -> ModeChip(if(len==0)"любая" else "$len+", minL==len) { minL=len } }
        }
        Text("Тема слов")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("" to "Любая", "food" to "Еда", "animals" to "Животные", "cities" to "Города")) { (k, l) ->
                ModeChip(l, theme == k) { theme = k }
            }
        }
        NeonButton("СОЗДАТЬ", { vm.createRoom(name.ifBlank{"Комната"}, false, 15, 6, mode, minL, false, false, theme) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CodeEntryScreen(state: UiState, vm: GameViewModel) {
    var code by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(200); focus.requestFocus() }
    Column(Modifier.fillMaxSize().padding(20.dp), Arrangement.SpaceBetween, Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) { 
            IconButton(vm::dismissCodeEntry) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Вход по коду", style=MaterialTheme.typography.titleLarge) 
        }
        OutlinedTextField(code, {if(it.length<=4) code=it}, label={Text("4 цифры")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.focusRequester(focus))
        NeonButton("ВОЙТИ", { if(code.length==4) vm.joinRoomByCode(code) }, enabled=code.length==4, modifier=Modifier.fillMaxWidth())
    }
}

@Composable
private fun RoomScreen(state: UiState, vm: GameViewModel) {
    val room = state.room ?: return
    Column(Modifier.fillMaxSize().padding(20.dp), Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) { 
            Text("Комната: ${room.name}", Modifier.weight(1f), style=MaterialTheme.typography.titleLarge)
            OutlinedButton(vm::leaveRoom) { Text("Выйти") } 
        }
        NeonCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Настройки", style=MaterialTheme.typography.titleSmall)
                Text("Режим: ${room.mode} • Мин. длина: ${room.minWordLen} • Тема: ${if(room.theme.isBlank()) "любая" else room.theme}")
            }
        }
        Text("Игроки (${room.players.size}/${room.maxPlayers})")
        LazyColumn(Modifier.weight(1f)) { 
            items(room.players) { p -> 
                Row(Modifier.padding(vertical=4.dp), verticalAlignment=Alignment.CenterVertically) { 
                    PlayerAvatar(p.name, 36.dp, avatarId=p.avatarId, photo=p.photo)
                    Text(p.name, modifier=Modifier.padding(start=12.dp)) 
                } 
            } 
        }
        if(room.isHost) NeonButton("НАЧАТЬ", vm::startGame, enabled=room.players.size>=2, modifier=Modifier.fillMaxWidth())
    }
}

@Composable
private fun GameScreen(state: UiState, vm: GameViewModel) {
    val game = state.game ?: return
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), Arrangement.spacedBy(14.dp), Alignment.CenterHorizontally) {
        Text("Игра: ${game.mode}", style=MaterialTheme.typography.titleLarge)
        Text(if(game.myTurn) "ТВОЙ ХОД!" else "Жди соперника...", color=if(game.myTurn) AppColors.Primary else AppColors.TextSecondary)
        Row(Arrangement.spacedBy(6.dp)) {
            if(game.minWordLen > 0) StatChip("≥${game.minWordLen} букв", color=AppColors.Warning)
            if(game.theme.isNotBlank()) StatChip("Тема: ${game.theme}", color=AppColors.Secondary)
        }
        Box(Modifier.weight(1f), Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text(game.requiredLetter.uppercase(), fontSize=64.sp, fontWeight=FontWeight.Bold, color=AppColors.Primary)
                Text("Последнее: ${game.lastWord.uppercase()}", style=MaterialTheme.typography.bodyLarge)
            }
        }
        if(game.myTurn) {
            OutlinedTextField(text, {text=it}, placeholder={Text("Слово...")}, modifier=Modifier.fillMaxWidth(), singleLine=true)
            NeonButton("ОТПРАВИТЬ", { if(text.isNotBlank()){ vm.submitWord(text); text="" } }, Modifier.fillMaxWidth().padding(top=8.dp))
        }
        state.error?.let { Text(it, color=AppColors.Danger) }
    }
}

@Composable
private fun ResultScreen(state: UiState, vm: GameViewModel) {
    val w = state.winner ?: WinnerInfo("", "Никто")
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("ПОБЕДИТЕЛЬ", style = MaterialTheme.typography.titleLarge, color = AppColors.Primary)
        Spacer(Modifier.height(20.dp))
        PlayerAvatar(w.name, 120.dp, avatarId = w.avatarId, photo = w.photo)
        Text(w.name, style = MaterialTheme.typography.headlineMedium)
        NeonButton("ЕЩЁ РАЗ", vm::playAgain, Modifier.fillMaxWidth().padding(top=40.dp))
        OutlinedButton(vm::leaveRoom, Modifier.fillMaxWidth().padding(top=12.dp)) { Text("В ЛОББИ") }
    }
}

@Composable
private fun SettingsScreen(state: UiState, vm: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp), Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) { 
            IconButton(vm::closeSettings) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Настройки", style=MaterialTheme.typography.titleLarge) 
        }
        SettingsBlock("Звук") { SettingsToggleRow("Включить звук", state.soundOn, vm::setSoundOn) }
        SettingsBlock("Вибрация") { SettingsToggleRow("Включить вибрацию", state.vibrationOn, vm::setVibrationOn) }
    }
}

@Composable
private fun SettingsBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
        Column(Modifier.padding(16.dp)) { 
            Text(title, style=MaterialTheme.typography.titleSmall, color=AppColors.TextSecondary)
            content() 
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) { 
        Text(label)
        Spacer(Modifier.weight(1f))
        Switch(checked, onCheckedChange) 
    }
}
