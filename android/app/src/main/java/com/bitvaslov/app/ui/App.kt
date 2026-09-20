package com.bitvaslov.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
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
        val maxSide = 512f
        val scale = minOf(maxSide / original.width, maxSide / original.height, 1f)
        val scaled = android.graphics.Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        val out = java.io.ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
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
    ) { padding ->
        AppBackground {
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (state.screen) {
                    Screen.CONNECT -> ConnectScreen(state, vm)
                    Screen.LOBBY -> LobbyScreen(state, vm, bottomTab, onSelectTab = { bottomTab = it })
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
private fun LobbyScreen(state: UiState, vm: GameViewModel, tab: Int, onSelectTab: (Int) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        when (tab) {
            1 -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { AnimatedTitle("", "КОМНАТЫ", "Присоединяйся к игре") }
                roomsItems(state, vm)
            }
            2 -> FriendsTab(state, vm)
            3 -> ProfileTab(state, vm)
            else -> MenuTab(state, vm, onSelectTab)
        }
        MainBottomBar(
            selected = tab,
            onSelect = { onSelectTab(it); if (it == 2) vm.requestFriends() },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private val TopBarShape = RoundedCornerShape(30.dp)
private val TopBarColor = Color(0xFF21242B)
private val TopBarInnerColor = Color(0xFF191C22)

@Composable
private fun TopBar(
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    myName: String = "",
    myAvatarId: Int = 0,
    myPhoto: String = "",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(60.dp)
            .clip(TopBarShape)
            .background(TopBarColor),
    ) {
        Text(
            "Меню",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(TopBarInnerColor),
        ) {
            Row(Modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfile),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayerAvatar(myName, 42.dp, avatarId = myAvatarId, photo = myPhoto)
                }
            }
        }
    }
}

@Composable
private fun MenuTab(state: UiState, vm: GameViewModel, onSelectTab: (Int) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 96.dp, bottom = 130.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { vm.requestCodeEntry() },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A0C10),
                        contentColor = Color.White,
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3A4050)),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                        .height(72.dp),
                ) {
                    Text("Войти по коду", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = { vm.requestCreateRoom() },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D0F14),
                            contentColor = Color(0xFFB4BCCC),
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(64.dp),
                    ) {
                        Text("Создать комнату", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0A0C10))
                    .padding(top = 14.dp, bottom = 10.dp),
            ) {
                Text(
                    "Открытые комнаты",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(2.dp)
                        .background(Color(0xFF2E323C))
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    roomsItems(state, vm)
                }
            }
        }

        TopBar(
            onProfile = { onSelectTab(3) },
            onSettings = { vm.toggleSettings() },
            myName = state.myName,
            myAvatarId = state.myAvatarId,
            myPhoto = state.myPhoto,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun FriendsTab(state: UiState, vm: GameViewModel) {
    var query by remember { mutableStateOf("") }
    val kb = LocalSoftwareKeyboardController.current
    // Цвета как в меню
    val plaque = Color(0xFF21242B)
    val circle = Color(0xFF3A4050)
    val plusLighter = Color(0xFF9AA2B5)
    val offlineGray = Color(0xFF9AA2B5)
    val divider = Color(0xFF2E323C)
    val green = Color(0xFF2ECC71)
    val red = Color(0xFFE74C3C)
    val plaqueDark = Color(0xFF0D0F14)

    // Онлайн друзья сверху, офлайн ниже
    val sortedFriends = remember(state.friends) {
        state.friends.sortedByDescending { it.online }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // --- Плашка поиска: полукруглые концы, как в меню по размеру ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(plaque),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.padding(start = 22.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) Text("Поиск", color = Color(0xFFE4E8F0), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                                inner()
                            }
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    // Серый кружок с плюсиком чуть светлее
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(circle)
                            .clickable {
                                kb?.hide()
                                if (query.isNotBlank()) vm.searchUser(query)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Искать",
                            tint = plusLighter,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }

        // --- Результаты поиска ---
        if (state.searchResults.isNotEmpty()) {
            item { Text("Результаты поиска", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(state.searchResults, key = { it.id }) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(plaque)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerAvatar(user.name, 48.dp, avatarId = user.avatarId, photo = user.photo)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(user.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (user.online) "в сети" else "не в сети",
                            fontSize = 15.sp,
                            color = if (user.online) Color.White else offlineGray,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(circle)
                            .clickable { vm.addFriend(user.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Добавить", tint = plusLighter, modifier = Modifier.size(26.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(2.dp)) }
        }

        // --- Блок «Мои друзья» ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(plaque)
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    "Мои друзья",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(2.dp)
                        .background(divider)
                )
                Spacer(Modifier.height(10.dp))
                if (sortedFriends.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), Alignment.Center) {
                        Text("Пока пусто...", color = offlineGray, fontSize = 16.sp)
                    }
                } else {
                    sortedFriends.forEach { f ->
                        // Строка друга: фото | имя/статус | плашка [плюс | стрелка вправо]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlayerAvatar(f.name, 48.dp, avatarId = f.avatarId, photo = f.photo)
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Text(f.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (f.online) "в сети" else "офлайн",
                                    fontSize = 15.sp,
                                    color = if (f.online) Color.White else offlineGray,
                                )
                            }
                            // Две отдельные плашки: плюс (пригласить) | стрелка вправо (профиль), со спейсером
                            // Плюс: слева полукруг, справа стена. Стрелка: зеркально.
                            Row(
                                modifier = Modifier.height(48.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(24.dp, 0.dp, 0.dp, 24.dp))
                                        .background(if (f.online) plaqueDark else plaqueDark.copy(alpha = 0.55f))
                                        .border(1.dp, if (f.online) circle else circle.copy(alpha = 0.45f), RoundedCornerShape(24.dp, 0.dp, 0.dp, 24.dp))
                                        .clickable(enabled = f.online) { vm.inviteFriend(f.id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Пригласить", tint = if (f.online) Color.White else offlineGray.copy(alpha = 0.55f), modifier = Modifier.size(26.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(0.dp, 24.dp, 24.dp, 0.dp))
                                        .background(plaqueDark)
                                        .border(1.dp, circle, RoundedCornerShape(0.dp, 24.dp, 24.dp, 0.dp))
                                        .clickable { /* профиль позже */ },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Профиль", tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Блок «Запросы» — заголовок внутри бокса ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(plaque)
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    "Запросы",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(2.dp)
                        .background(divider)
                )
                Spacer(Modifier.height(6.dp))
                val hasIncoming = state.incomingRequests.isNotEmpty()
                val hasOutgoing = state.outgoingRequests.isNotEmpty()
                if (!hasIncoming && !hasOutgoing) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), Alignment.Center) {
                        Text("Пока пусто...", color = offlineGray, fontSize = 16.sp)
                    }
                } else {
                    if (hasIncoming) {
                        Text("Мне", modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 2.dp), color = Color(0xFF9AA2B5), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        state.incomingRequests.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlayerAvatar(r.name, 48.dp, avatarId = r.avatarId, photo = r.photo)
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text(r.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(2.dp))
                                    Text("Игр: ${r.games} • Побед: ${r.wins}", fontSize = 14.sp, color = offlineGray)
                                }
                                // Две отдельные плашки: красный крестик (отклонить) | зелёная галочка (принять), со спейсером
                                // Крестик: слева полукруг, справа стена. Галочка: зеркально.
                                Row(
                                    modifier = Modifier.height(48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(24.dp, 0.dp, 0.dp, 24.dp))
                                            .background(red)
                                            .clickable { vm.respondFriend(r.id, false) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Отклонить", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(0.dp, 24.dp, 24.dp, 0.dp))
                                            .background(green)
                                            .clickable { vm.respondFriend(r.id, true) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = "Принять", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (hasOutgoing) {
                        Text("От меня", modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 2.dp), color = Color(0xFF9AA2B5), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        state.outgoingRequests.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlayerAvatar(r.name, 48.dp, avatarId = r.avatarId, photo = r.photo)
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text(r.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(2.dp))
                                    Text("Игр: ${r.games} • Побед: ${r.wins}", fontSize = 14.sp, color = offlineGray)
                                }
                                // Только крестик — отменить заявку (ширина как две плашки со спейсером у друзей)
                                Box(
                                    modifier = Modifier
                                        .width(104.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(red)
                                        .clickable { vm.cancelFriendRequest(r.id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Отменить", tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }
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
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip("Игр: ${state.stats?.games ?: 0}", Modifier.weight(1f)); StatChip("Побед: ${state.stats?.wins ?: 0}", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.roomsItems(state: UiState, vm: GameViewModel) {
    items(state.rooms, key = { it.id }) { room -> RoomCard(room.name, room.players, room.maxPlayers, room.timer, room.mode, onJoin = { vm.joinRoomById(room.id) }) }
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
                ModeChip(l, mode == k, onClick = { mode = k })
            }
        }
        Text("Усложнения")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 3, 4, 5).forEach { len -> ModeChip(if(len==0)"любая" else "$len+", minL==len, onClick = { minL=len }) }
        }
        Text("Тема слов")
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(listOf("" to "Любая", "food" to "Еда", "animals" to "Животные", "cities" to "Города")) { (k, l) ->
                ModeChip(l, theme == k, onClick = { theme = k })
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
