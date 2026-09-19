package com.bitvaslov.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.bitvaslov.app.Screen
import com.bitvaslov.app.UiState
import com.bitvaslov.app.WinnerInfo
import kotlinx.coroutines.delay

@Composable
fun App(vm: GameViewModel = viewModel()) {
    val state by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var bottomTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { vm.autoConnect() }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> vm.setActive(true)
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> vm.setActive(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            vm.clearToast()
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.screen == Screen.LOBBY) {
                MainBottomBar(selected = bottomTab, onSelect = {
                    bottomTab = it
                    if (it == 2) vm.requestFriends()
                })
            }
        },
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
            title = { Text("Приглашение в комнату") },
            text = { Text("${invite.fromName} зовёт в «${invite.roomName}»") },
            confirmButton = {
                TextButton(onClick = { vm.acceptInvite() }) {
                    Text("ВОЙТИ", color = AppColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.declineInvite() }) {
                    Text("Позже", color = AppColors.TextSecondary)
                }
            },
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
private fun LobbyScreen(state: UiState, vm: GameViewModel, tab: Int) {
    when (tab) {
        1 -> RoomsTab(state, vm)
        2 -> FriendsTab(state, vm)
        3 -> ProfileTab(state, vm)
        else -> MenuTab(state, vm)
    }
}

@Composable
private fun MenuTab(state: UiState, vm: GameViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Битва слов",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.toggleSettings() }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = Color.White)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatar(state.myName.ifBlank { "И" }, size = 52.dp, color = AppColors.Secondary, avatarId = state.myAvatarId)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Привет, ${state.myName.ifBlank { "Друг" }}!", style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
                    Text("Найдите соперников и начинайте игру", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton(
                    icon = Icons.Filled.Add,
                    text = "Создать комнату",
                    onClick = vm::requestCreateRoom,
                    modifier = Modifier.weight(1f),
                    color = AppColors.Primary,
                )
                ActionButton(
                    icon = Icons.Filled.Lock,
                    text = "Войти по коду",
                    onClick = vm::requestCodeEntry,
                    modifier = Modifier.weight(1f),
                    color = AppColors.Secondary,
                )
            }
        }

item {
            Text("Открытые комнаты", style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
        }

        roomsItems(state, vm)
    }
}

@Composable
private fun RoomsTab(state: UiState, vm: GameViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AnimatedTitle(primary = "", highlight = "КОМНАТЫ", subtitle = "Присоединяйся к игре")
        }
        roomsItems(state, vm)
    }
}

@Composable
private fun FriendsTab(state: UiState, vm: GameViewModel) {
    LaunchedEffect(Unit) { vm.requestFriends() }
    var query by remember { mutableStateOf("") }
    val kb = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { AnimatedTitle(primary = "", highlight = "ДРУЗЬЯ", subtitle = "Найди игрока по имени") }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Имя игрока") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = {
                        if (query.isNotBlank()) {
                            kb?.hide()
                            vm.searchUser(query)
                        }
                    },
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Найти", tint = AppColors.Primary)
                }
            }
        }

        if (state.searchResults.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Найденные игроки", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.clearSearch(); query = "" }) {
                        Text("Сбросить", color = AppColors.TextSecondary)
                    }
                }
            }
            items(state.searchResults, key = { "s_${it.id}" }) { user ->
                NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Secondary) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(user.name, size = 44.dp, avatarId = user.avatarId)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.name, color = AppColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${if (user.online) "в сети" else "не в сети"} • игр: ${user.games}, побед: ${user.wins}",
                                color = if (user.online) AppColors.Primary else AppColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = { vm.addFriend(user.id) }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "Добавить", tint = AppColors.Primary)
                        }
                    }
                }
            }
        }

        if (state.incomingRequests.isNotEmpty()) {
            item { Text("Заявки в друзья", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary) }
            items(state.incomingRequests, key = { "in_${it.id}" }) { req ->
                NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Primary) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(req.name, size = 40.dp, avatarId = req.avatarId)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            req.name,
                            modifier = Modifier.weight(1f),
                            color = AppColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { vm.respondFriend(req.id, true) }) {
                            Icon(Icons.Filled.Check, contentDescription = "Принять", tint = AppColors.Primary)
                        }
                        IconButton(onClick = { vm.respondFriend(req.id, false) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Отклонить", tint = AppColors.Danger)
                        }
                    }
                }
            }
        }

        if (state.outgoingRequests.isNotEmpty()) {
            item { Text("Отправленные заявки", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary) }
            items(state.outgoingRequests, key = { "out_${it.id}" }) { req ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatar(req.name, size = 28.dp, avatarId = req.avatarId)
                    Spacer(Modifier.width(10.dp))
                    Text("${req.name} — ждём подтверждения", color = AppColors.TextSecondary)
                }
            }
        }

        item { Text("Мои друзья", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary) }

        if (state.friends.isEmpty()) {
            item { Text("Пока никого. Найди друга по имени выше.", color = AppColors.TextSecondary) }
        } else {
            items(state.friends, key = { "f_${it.id}" }) { friend ->
                NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Border) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(
                            friend.name,
                            size = 44.dp,
                            color = if (friend.online) AppColors.Primary else AppColors.Border,
                            avatarId = friend.avatarId,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                friend.name,
                                color = AppColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            val status = when {
                                friend.inGame -> "в игре"
                                friend.online -> "в сети"
                                else -> "не в сети"
                            }
                            val statusColor = when {
                                friend.inGame -> AppColors.Warning
                                friend.online -> AppColors.Primary
                                else -> AppColors.TextSecondary
                            }
                            Text(status, color = statusColor, style = MaterialTheme.typography.bodySmall)
                        }
                        if (friend.online) {
                            IconButton(onClick = { vm.inviteFriend(friend.id) }) {
                                Icon(Icons.Filled.GroupAdd, contentDescription = "Пригласить", tint = AppColors.Primary)
                            }
                        }
                        IconButton(onClick = { vm.removeFriend(friend.id) }) {
                            Icon(Icons.Filled.PersonRemove, contentDescription = "Удалить", tint = AppColors.TextSecondary)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfileTab(state: UiState, vm: GameViewModel) {
    LaunchedEffect(Unit) { vm.loadStats() }
    val s = state.stats
    var editing by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(state.myName) }
    val kb = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PlayerAvatar(state.myName.ifBlank { "И" }, size = 96.dp, avatarId = state.myAvatarId)
                Spacer(Modifier.height(12.dp))
                if (editing) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        label = { Text("Имя") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { editing = false }) { Text("Отмена") }
                        NeonButton(
                            text = "СОХРАНИТЬ",
                            onClick = {
                                kb?.hide()
                                vm.setMyName(nameDraft)
                                editing = false
                            },
                            enabled = nameDraft.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.myName.ifBlank { "Игрок" }, style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { nameDraft = state.myName; editing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Изменить имя", tint = AppColors.Primary)
                        }
                    }
                }
            }
        }

        item {
            NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Border) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Аватар", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                    Text("Выбери цвет — по нему тебя отличат тёзки", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    AvatarPicker(selected = state.myAvatarId, onSelect = vm::setAvatar)
                }
            }
        }

        item {
            NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Border) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Статистика", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatChip("Игр: ${s?.games ?: 0}", modifier = Modifier.weight(1f), active = true)
                        StatChip("Побед: ${s?.wins ?: 0}", modifier = Modifier.weight(1f), active = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatChip("Винрейт: ${s?.winRate ?: 0}%", modifier = Modifier.weight(1f), active = true)
                        StatChip("Поражений: ${s?.losses ?: 0}", modifier = Modifier.weight(1f), active = true)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Победная серия", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatChip("Текущий: ${s?.currentStreak ?: 0}", modifier = Modifier.weight(1f), color = AppColors.Secondary)
                        StatChip("Рекорд: ${s?.bestStreak ?: 0}", modifier = Modifier.weight(1f), color = AppColors.Warning)
                    }
                }
            }
        }

        item {
            Text("Счётчики обновляются после игры", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun AvatarPicker(selected: Int, onSelect: (Int) -> Unit) {
    val rows = 3
    val perRow = 4
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (c in 0 until perRow) {
                    val id = r * perRow + c
                    val isSelected = id == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(54.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(avatarColor(id))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape,
                            )
                            .clickable { onSelect(id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (id + 1).toString(),
                            color = AppColors.ContentDark,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.roomsItems(state: UiState, vm: GameViewModel) {
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

@Composable
private fun CreateRoomScreen(state: UiState, vm: GameViewModel) {
    var roomName by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(15) }
    var maxP by remember { mutableStateOf(6) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::dismissCreateRoom) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
            Text(
                "Создание комнаты",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(48.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Название комнаты", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Например: Быстрые слова") },
                    leadingIcon = {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = AppColors.Secondary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Border,
                        cursorColor = AppColors.Primary,
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary,
                        focusedContainerColor = AppColors.Surface,
                        unfocusedContainerColor = AppColors.Surface,
                    ),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Приватность", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OptionCard(
                        icon = Icons.Filled.Public,
                        label = "Открытая",
                        selected = !isPrivate,
                        onClick = { isPrivate = false },
                        modifier = Modifier.weight(1f),
                    )
                    OptionCard(
                        icon = Icons.Filled.Lock,
                        label = "Приватная",
                        selected = isPrivate,
                        onClick = { isPrivate = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Время на ход", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(5, 10, 15, 20, 25, 30).forEach { t ->
                        NumberTile(
                            value = t,
                            selected = timer == t,
                            onClick = { timer = t },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Количество игроков", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(2, 3, 4, 5, 6).forEach { p ->
                        NumberTile(
                            value = p,
                            selected = maxP == p,
                            onClick = { maxP = p },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        state.error?.let { err ->
            Text(err, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
        }

        NeonButton(
            "СОЗДАТЬ КОМНАТУ",
            onClick = {
                vm.createRoom(roomName.trim().ifBlank { "Комната" }, isPrivate, timer, maxP)
            },
            color = AppColors.Primary,
            contentColor = AppColors.ContentDark,
            modifier = Modifier.fillMaxWidth().height(60.dp),
        )
    }
}

@Composable
private fun SettingsScreen(state: UiState, vm: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::closeSettings) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
            Text(
                "Настройки",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(48.dp))
        }

        SettingsBlock(title = "Звук") {
            SettingsToggleRow(
                label = "Звук",
                checked = state.soundOn,
                onCheckedChange = vm::setSoundOn,
            )
            Spacer(Modifier.height(12.dp))
            SettingsSliderRow(
                label = "Громкость",
                value = state.volume,
                onValueChange = vm::setVolume,
                enabled = state.soundOn,
            )
        }

        SettingsBlock(title = "Вибрация") {
            SettingsToggleRow(
                label = "Вибрация",
                checked = state.vibrationOn,
                onCheckedChange = vm::setVibrationOn,
            )
            Spacer(Modifier.height(12.dp))
            SettingsSliderRow(
                label = "Интенсивность",
                value = state.vibrationIntensity,
                onValueChange = vm::setVibrationIntensity,
                enabled = state.vibrationOn,
            )
        }

        SettingsBlock(title = "Уведомления") {
            SettingsToggleRow(
                label = "Уведомления",
                checked = state.notificationsOn,
                onCheckedChange = vm::setNotificationsOn,
            )
        }
    }
}

@Composable
private fun SettingsBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppColors.ContentDark,
                checkedTrackColor = AppColors.Primary,
                uncheckedThumbColor = AppColors.TextSecondary,
                uncheckedTrackColor = AppColors.SurfaceElevated,
                uncheckedBorderColor = AppColors.Border,
            ),
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = AppColors.Primary,
                activeTrackColor = AppColors.Primary,
                inactiveTrackColor = AppColors.SurfaceElevated,
                disabledThumbColor = AppColors.TextDisabled,
                disabledActiveTrackColor = AppColors.SurfaceElevated,
                disabledInactiveTrackColor = AppColors.SurfaceElevated,
            ),
        )
    }
}

@Composable
private fun CodeEntryScreen(state: UiState, vm: GameViewModel) {
    var code by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::dismissCodeEntry) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
            Text(
                "Войти в комнату",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { focusRequester.requestFocus() },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    border = BorderStroke(1.5.dp, AppColors.Secondary.copy(alpha = 0.6f)),
                ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                            .background(AppColors.SurfaceElevated),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = AppColors.Secondary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) { i ->
                            Box(
                                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                                    .background(AppColors.SurfaceElevated)
                                    .border(1.5.dp, AppColors.Secondary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (code.length > i) {
                                    Text(
                                        code[i].toString(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppColors.TextPrimary,
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Введите код, чтобы зайти в комнату к друзьям!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.Secondary,
                    )
                }
                }

                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter { c -> c.isDigit() }.take(4) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.size(1.dp).alpha(0f).focusRequester(focusRequester),
                    )
                }
            }

            state.error?.let { err ->
                Text(err, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            }
        }

        NeonButton(
            "ВОЙТИ",
            onClick = {
                if (code.length == 4) {
                    vm.dismissCodeEntry()
                    vm.joinRoomByCode(code.trim())
                }
            },
            enabled = code.length == 4,
            color = AppColors.Secondary,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth().height(60.dp),
        )
    }
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

        val onlineFriends = state.friends.filter { it.online }
        NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Border) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Пригласить друзей", style = MaterialTheme.typography.titleMedium)
                if (onlineFriends.isEmpty()) {
                    Text(
                        "Никого нет в сети",
                        color = AppColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    onlineFriends.forEach { friend ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(friend.name, size = 32.dp, color = AppColors.Primary, avatarId = friend.avatarId)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                friend.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            IconButton(onClick = { vm.inviteFriend(friend.id) }) {
                                Icon(Icons.Filled.GroupAdd, contentDescription = "Пригласить", tint = AppColors.Primary)
                            }
                        }
                    }
                }
            }
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

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                NeonCard(Modifier.fillMaxWidth(), borderColor = AppColors.Border) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Последнее слово", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                        Text(
                            game.lastWord.ifBlank { "—" }.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = AppColors.TextPrimary,
                        )
                    }
                }
                LetterOrb(letter = game.requiredLetter, isActive = game.myTurn)
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

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GameInput(
                    value = text,
                    onValueChange = { text = it.take(20) },
                    onSubmit = { kb?.hide(); if (text.isNotBlank()) vm.submitWord(text.trim()) },
                    isError = state.error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let {
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