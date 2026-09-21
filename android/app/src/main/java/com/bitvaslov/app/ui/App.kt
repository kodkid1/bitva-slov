package com.bitvaslov.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
                    Screen.CONNECT, Screen.LOBBY -> LobbyScreen(state, vm, bottomTab, onSelectTab = { bottomTab = it })
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

    state.profile?.let { p -> ProfileSheet(p, vm) }
}

@Composable
private fun LobbyScreen(state: UiState, vm: GameViewModel, tab: Int, onSelectTab: (Int) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        val backdrop = rememberLayerBackdrop()
        Box(Modifier.fillMaxSize().background(AppColors.Background).layerBackdrop(backdrop)) {
            when (tab) {
                1 -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    item { AnimatedTitle("", "КОМНАТЫ", "Присоединяйся к игре") }
                    roomsItems(state, vm)
                }
                2 -> FriendsTab(state, vm)
                3 -> ProfileTab(state, vm)
                else -> MenuTab(state, vm, onSelectTab)
            }
        }
        MainBottomBar(
            selected = tab,
            onSelect = { onSelectTab(it); if (it == 2) vm.requestFriends() },
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private val TopBarShape = RoundedCornerShape(30.dp)
private val TopBarColor = Color(0xFF111111)

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
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
        )
        val avatarAccent = avatarAccent(myPhoto, myAvatarId)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black,
                            0.15f to Color.Black,
                            0.55f to avatarAccent.copy(alpha = 0.5f),
                            1f to avatarAccent,
                        )
                    )
                ),
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
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 96.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF141416))
                        .border(1.5.dp, Color(0xFF2A2A2D), RoundedCornerShape(26.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { vm.requestCodeEntry() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Войти по коду", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF131316))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { vm.requestCreateRoom() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Создать комнату", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5C5C7))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(Color(0xFF161618))
                    .padding(top = 16.dp),
            ) {
                Text(
                    "Открытые комнаты",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(1.dp)
                        .background(Color(0xFF2C2C2E))
                )
                Spacer(Modifier.height(6.dp))
                val roomsListState = rememberLazyListState()
                var roomsScrolling by remember { mutableStateOf(false) }
                LaunchedEffect(roomsListState) {
                    snapshotFlow { roomsListState.isScrollInProgress }.collect { roomsScrolling = it }
                }
                val fadeAlpha by animateFloatAsState(
                    targetValue = if (roomsScrolling) 1f else 0f,
                    animationSpec = tween(300),
                    label = "roomsFade",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    LazyColumn(
                        state = roomsListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 130.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        roomsItems(state, vm)
                    }
                    // Фейд-фильтр снизу списка — появляется только во время скролла
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .align(Alignment.BottomCenter)
                            .alpha(fadeAlpha)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF161618).copy(alpha = 0f),
                                        Color(0xFF161618).copy(alpha = 0.9f),
                                        Color(0xFF161618),
                                    )
                                )
                            ),
                    )
                    // Фейд-фильтр сверху списка — проявляется при скролле вверх
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.TopCenter)
                            .alpha(fadeAlpha)
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF161618),
                                        Color(0xFF161618).copy(alpha = 0.9f),
                                        Color(0xFF161618).copy(alpha = 0f),
                                    )
                                )
                            ),
                    )
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
    val blockBg = Color(0xFF161618)
    val button = Color(0xFF141416)
    val offlineGray = Color(0xFF9AA2B5)
    val green = Color(0xFF2ECC71)
    val red = Color(0xFFE74C3C)
    val redDark = Color(0xFFA93226)

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
                    .background(blockBg),
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
                    // Круглая кнопка поиска
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(button)
                            .clickable {
                                kb?.hide()
                                if (query.isNotBlank()) vm.searchUser(query)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Искать",
                            tint = Color.White,
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
                        .background(blockBg)
                        .clickable { vm.openProfile(user.id) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerAvatar(user.name, 48.dp, avatarId = user.avatarId, photo = user.photo)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(user.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
Text(
                        if (user.online) "В сети" else "Не в сети",
                        fontSize = 15.sp,
                        color = if (user.online) Color.White else offlineGray,
                    )
                }
                // Кнопка добавления
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(button)
                        .clickable { vm.addFriend(user.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить", tint = Color.White, modifier = Modifier.size(26.dp))
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
                    .background(blockBg)
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    "Мои друзья",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                if (sortedFriends.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), Alignment.Center) {
                        Text("Пока пусто...", color = offlineGray, fontSize = 16.sp)
                    }
                } else {
                    sortedFriends.forEach { f ->
                        // Строка друга: фото | имя/статус | одна плашка [плюс | стрелка вправо]
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
                                    if (f.online) "В сети" else "Офлайн",
                                    fontSize = 15.sp,
                                    color = if (f.online) Color.White else offlineGray,
                                )
                            }
                            // Единая плашка без контура: плюс (пригласить) | стрелка (профиль)
                            Row(
                                modifier = Modifier.height(50.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(26.dp, 0.dp, 0.dp, 26.dp))
                                        .background(if (f.online) button else button.copy(alpha = 0.55f))
                                        .clickable(enabled = f.online) { vm.inviteFriend(f.id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Пригласить", tint = if (f.online) Color.White else offlineGray, modifier = Modifier.size(26.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(0.dp, 26.dp, 26.dp, 0.dp))
                                        .background(button)
                                        .clickable { vm.openProfile(f.id) },
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
                    .background(blockBg)
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    "Запросы",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                val hasIncoming = state.incomingRequests.isNotEmpty()
                val hasOutgoing = state.outgoingRequests.isNotEmpty()
                if (!hasIncoming && !hasOutgoing) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), Alignment.Center) {
                        Text("Пока пусто...", color = offlineGray, fontSize = 16.sp)
                    }
                } else {
                    if (hasIncoming) {
                        Text("Мне", modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 2.dp), color = Color(0xFF9AA2B5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                                // Одна плашка с градиентом от красного (отклонить) к зелёному (принять)
                                Row(
                                    modifier = Modifier
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(Brush.horizontalGradient(listOf(red, green))),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .fillMaxHeight()
                                            .clickable { vm.respondFriend(r.id, false) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Отклонить", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .fillMaxHeight()
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
                        Text("От меня", modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 2.dp), color = Color(0xFF9AA2B5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                                // Только крестик — отменить заявку, тёмно-красный (ширина как градиентная плашка)
                                Box(
                                    modifier = Modifier
                                        .width(104.dp)
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(redDark)
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
private fun ProfileSheet(p: UserProfile, vm: GameViewModel) {
    val context = LocalContext.current
    val plaque = Color(0xFF161618)
    val plaqueBtn = Color(0xFF1C1C1E)
    val gray = Color(0xFF8E8E93)
    val green = Color(0xFF2ECC71)
    val red = Color(0xFFE74C3C)
    val white = Color.White

    fun copyId() {
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("id", p.id))
        vm.notify("Айди скопирован")
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).clickable { vm.closeProfile() }, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(plaque)
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerAvatar(p.name, 96.dp, avatarId = p.avatarId, photo = p.photo)
            Spacer(Modifier.height(12.dp))
            Text(p.name, color = white, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    p.isMe -> "это ты"
                    p.inGame -> "в игре"
                    p.online -> "в сети"
                    else -> "офлайн"
                },
                color = if (p.isMe) gray else if (p.inGame) Color(0xFFFFB300) else if (p.online) green else gray,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            // Айди
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(plaqueBtn)
                    .clickable { copyId() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("айди: ", color = gray, fontSize = 14.sp)
                Text(shortId(p.id), color = white, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ContentCopy, null, tint = gray, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(16.dp))
            // Статистика
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCell("Игр", "${p.games}", Modifier.weight(1f))
                StatCell("Побед", "${p.wins}", Modifier.weight(1f))
                StatCell("↑", "${p.winRate}%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            // Действия
            if (p.isMe) {
                // ничего
            } else if (p.isFriend) {
                ProfileButton("Удалить из друзей", red, { vm.removeFriend(p.id); vm.closeProfile() })
            } else if (p.incoming) {
                ProfileButton("Принять заявку", green, { vm.respondFriend(p.id, true) })
            } else if (p.outgoing) {
                Text("Заявка отправлена", color = gray, fontSize = 15.sp)
            } else {
                ProfileButton("Добавить в друзья", plaqueBtn, { vm.addFriend(p.id) })
            }
        }
    }
}

private fun shortId(id: String): String = id.take(8) + "…"

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color(0xFF8E8E93), fontSize = 12.sp)
    }
}

@Composable
private fun ProfileButton(label: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
        if (state.myId.isNotBlank()) item {
            NeonCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
                    Text("Твой айди", style=MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.myId, color=AppColors.TextSecondary, fontSize=14.sp, modifier=Modifier.weight(1f))
                        NeonButton("КОПИРОВАТЬ", {
                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("id", state.myId))
                            vm.notify("Айди скопирован")
                        })
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
