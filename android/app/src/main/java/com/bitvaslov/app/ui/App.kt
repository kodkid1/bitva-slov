package com.bitvaslov.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitvaslov.app.*
import com.bitvaslov.app.R
import com.kyant.backdrop.backdrops.LayerBackdrop
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
    val backdrop = rememberLayerBackdrop()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.setActive(true)
                Lifecycle.Event.ON_STOP -> vm.setActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { vm.autoConnect() }
    // приглашение может прийти, когда приложение уже запущено (onNewIntent) —
    // забираем его из DeepLink, как только экран снова активен
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            vm.notifyNow()
        }
    }
    LaunchedEffect(state.toast) { state.toast?.let { snackbar.showSnackbar(it); vm.clearToast() } }
    // ошибка висит на экране и сама исчезает, чтобы не перекрывать интерфейс
    LaunchedEffect(state.error) {
        if (state.error != null) {
            delay(4000)
            vm.clearError()
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        AppBackground {
            Box(Modifier.padding(padding).fillMaxSize()) {
                BackHandler(enabled = state.screen != Screen.CONNECT && state.screen != Screen.LOBBY && state.screen != Screen.GAME) {
                    if (state.screen == Screen.ROOM) vm.leaveRoom() else vm.closeSettings()
                }
                when (state.screen) {
                    Screen.CONNECT, Screen.LOBBY -> LobbyScreen(state, vm, bottomTab, backdrop, onSelectTab = { bottomTab = it })
                    Screen.CREATEROOM -> CreateRoomScreen(state, vm)
                    Screen.PASSWORD -> PasswordScreen(state, vm)
                    Screen.SETTINGS -> SettingsScreen(state, vm)
                    Screen.ROOM -> RoomScreen(state, vm)
                    Screen.GAME -> GameScreen(state, vm)
                    Screen.RESULT -> ResultScreen(state, vm)
                    Screen.SHOP -> ShopScreen(state, vm)
                }
                // раньше UiState.error писался, но нигде не отображался — все ошибки были немыми
                state.error?.let { msg ->
                    ErrorBanner(
                        message = msg,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 16.dp, end = 16.dp),
                    )
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

    // Профиль — оверлей поверх экранов; в магазине он скрывается, иначе перекрыл бы его
    if (state.screen != Screen.SHOP) {
        state.profile?.let { p -> ProfileSheet(state, p, vm, backdrop = backdrop, tab = bottomTab, onSelectTab = { bottomTab = it }) }
    }
}

@Composable
private fun LobbyScreen(state: UiState, vm: GameViewModel, tab: Int, backdrop: LayerBackdrop, onSelectTab: (Int) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B)).layerBackdrop(backdrop)) {
            when (tab) {
                1 -> RoomsTab(state, vm)
                2 -> FriendsTab(state, vm)
                3 -> ProfileTab(state, vm)
                else -> MenuTab(state, vm, onSelectTab)
            }
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            YandexBanner(modifier = Modifier.fillMaxWidth())
            MainBottomBar(
                selected = tab,
                onSelect = { onSelectTab(it); if (it == 2) vm.requestFriends() },
                backdrop = backdrop,
            )
        }
    }
}

/** Тестовый баннер Яндекса: адаптивная высота под конкретный блок. */
@Composable
private fun YandexBanner(modifier: Modifier = Modifier, adUnitId: String = AdsManager.BANNER_UNIT) {
    AndroidView(
        modifier = modifier,
        factory = { ctx -> AdsManager.createBannerView(ctx, adUnitId) },
        onRelease = { view -> view.destroy() },
    )
}

private val TopBarShape = RoundedCornerShape(30.dp)
private val TopBarColor = Color(0xFF111111)

@Composable
private fun RoomsTab(state: UiState, vm: GameViewModel) {
    var query by remember { mutableStateOf("") }
    val kb = LocalSoftwareKeyboardController.current
    val q = query.trim().lowercase()

    val rooms = state.rooms
        .filter { it.name.lowercase().contains(q) }
        .sortedByDescending { it.createdAt }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        ) {
            RoomsTopBar()
            Spacer(Modifier.height(14.dp))
            // Плашка поиска, стиль как в меню
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF111111))
                    .padding(start = 24.dp, end = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) Text("Поиск по названию", color = Color(0xFF8E8E93), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                inner()
                            }
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                            .clickable { kb?.hide() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(painterResource(R.drawable.ic_search), null, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // Бокс со списком комнат: открытые и закрытые вместе
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(Color(0xFF161618))
                    .padding(top = 16.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 130.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    if (rooms.isNotEmpty()) {
                        items(rooms, key = { it.id }) { room ->
                            RoomCard(
                                room.name,
                                room.players,
                                room.maxPlayers,
                                room.timer,
                                room.mode,
                                isPrivate = room.isPrivate,
                                onJoin = { vm.joinRoomById(room.id) },
                            )
                        }
                    } else {
                        item { Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), Alignment.Center) { Text("Пока пусто...", color = AppColors.TextSecondary) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomsTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(TopBarShape)
            .background(TopBarColor),
    ) {
        Text(
            "Комнаты",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
        )
    }
}

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
                IconButton(onClick = onSettings) { Image(painterResource(R.drawable.ic_settings), null, modifier = Modifier.size(22.dp)) }
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
                        ) { vm.requestCreateRoom() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Создать комнату", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                    "Комнаты",
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
            onProfile = { if (state.myId.isNotBlank()) vm.openProfile(state.myId) },
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
    val button = Color(0xFF1C1C1E)
    val offlineGray = Color(0xFF9AA2B5)
    val green = Color(0xFF2ECC71)
    val redDark = Color(0xFFA93226)

    // Онлайн друзья сверху, офлайн ниже
    val sortedFriends = remember(state.friends) {
        state.friends.sortedByDescending { it.online }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // --- Плашка поиска: как плашка «Меню» (60dp, радиус 30, фон #111111) ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF111111))
                    .padding(start = 24.dp, end = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) Text("Поиск", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
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
                        Image(
                            painterResource(R.drawable.ic_search),
                            contentDescription = "Искать",
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
                    PlayerAvatar(user.name, 48.dp, avatarId = user.avatarId, photo = user.photo, online = user.online)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(user.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        PlayerTitleBadge(user.titleId)
                        Spacer(Modifier.height(2.dp))
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
                    Image(painterResource(R.drawable.ic_add), contentDescription = "Добавить", modifier = Modifier.size(24.dp))
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
                            PlayerAvatar(f.name, 48.dp, avatarId = f.avatarId, photo = f.photo, online = f.online)
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Text(f.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                PlayerTitleBadge(f.titleId)
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
                                    Image(painterResource(R.drawable.ic_add), contentDescription = "Пригласить", modifier = Modifier.size(22.dp))
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
                                    Image(painterResource(R.drawable.ic_forward), contentDescription = "Профиль", modifier = Modifier.size(22.dp))
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
                                    PlayerTitleBadge(r.titleId)
                                    Spacer(Modifier.height(2.dp))
                                    Text("Игр: ${r.games} • Побед: ${r.wins}", fontSize = 14.sp, color = offlineGray)
                                }
                                // стеклянные кнопки: крестик и плюс раздельно
                                GlassIconButton(
                                    onClick = { vm.respondFriend(r.id, false) },
                                    tint = redDark,
                                    alpha = 0.50f,
                                ) {
                                    Image(
                                        painterResource(R.drawable.ic_close),
                                        contentDescription = "Отклонить",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                GlassIconButton(
                                    onClick = { vm.respondFriend(r.id, true) },
                                    tint = green,
                                    alpha = 0.42f,
                                ) {
                                    Image(
                                        painterResource(R.drawable.ic_add),
                                        contentDescription = "Принять",
                                        modifier = Modifier.size(20.dp),
                                    )
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
                                    PlayerTitleBadge(r.titleId)
                                    Spacer(Modifier.height(2.dp))
                                    Text("Игр: ${r.games} • Побед: ${r.wins}", fontSize = 14.sp, color = offlineGray)
                                }
                                // только крестик, растянутый в овал
                                GlassIconButton(
                                    onClick = { vm.cancelFriendRequest(r.id) },
                                    width = 86.dp,
                                    height = 46.dp,
                                    shape = RoundedCornerShape(percent = 50),
                                    tint = redDark,
                                    alpha = 0.50f,
                                ) {
                                    Image(
                                        painterResource(R.drawable.ic_close),
                                        contentDescription = "Отменить",
                                        modifier = Modifier.size(20.dp),
                                    )
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
private fun ProfileSheet(state: UiState, p: UserProfile, vm: GameViewModel, backdrop: LayerBackdrop, tab: Int, onSelectTab: (Int) -> Unit) {
    val context = LocalContext.current
    val plaque = Color(0xFF161618)
    val plaqueBtn = Color(0xFF1C1C1E)
    val gray = Color(0xFF8E8E93)
    val green = Color(0xFF2ECC71)
    val red = Color(0xFFA93226)
    val white = Color.White

    fun copyId() {
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("id", p.id))
        vm.notify("Айди скопирован")
    }

    fun invite() {
        vm.inviteFriend(p.id)
        vm.notify("Приглашение отправлено")
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Градиент от цвета аватара сверху вниз до 1/3 экрана
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.33f)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to avatarAccent(p.photo, p.avatarId),
                            0.35f to avatarAccent(p.photo, p.avatarId).copy(alpha = 0.4f),
                            1f to Color.Black.copy(alpha = 0f),
                        )
                    )
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .clickable(enabled = false) { }
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            // Аватарка выше середины
            PlayerAvatar(p.name, 104.dp, avatarId = p.avatarId, photo = p.photo, online = p.online)
            Spacer(Modifier.height(14.dp))
            // Имя — большими белыми
            Text(p.name, color = white, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (p.titleId != 0) {
                Spacer(Modifier.height(6.dp))
                PlayerTitleBadge(p.titleId, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            // id: сразу после ника
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("id: ", color = gray, fontSize = 14.sp)
                Text(if (p.pid > 0) "${p.pid}" else shortId(p.id), color = white, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Image(painterResource(R.drawable.ic_copy), null, modifier = Modifier.size(14.dp).clickable { copyId() })
            }
            Spacer(Modifier.height(6.dp))
            // Статус
            Text(
                when {
                    p.isMe -> "это ты"
                    p.inGame -> "в игре"
                    p.online -> "в сети"
                    else -> "не в сети"
                },
                color = when {
                    p.isMe -> gray
                    p.inGame -> Color(0xFFFFB300)
                    p.online -> green
                    else -> gray
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            if (p.isFriend) {
                // Серая круглая кнопка «Пригласить в комнату»
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(if (p.online || p.inGame) plaqueBtn else Color(0xFF2C2C2E))
                        .clickable(enabled = p.online || p.inGame, onClick = { invite() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Пригласить в комнату", color = if (p.online || p.inGame) white else gray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
            } else if (!p.isMe && p.incoming) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(green)
                        .clickable { vm.respondFriend(p.id, true) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Принять заявку", color = white, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
            } else if (!p.isMe && p.outgoing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Заявка отправлена", color = gray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
            } else if (!p.isMe) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(plaqueBtn)
                        .clickable { vm.addFriend(p.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Добавить в друзья", color = white, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
            }
            // Статистика
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(plaque)
                    .padding(vertical = 14.dp, horizontal = 12.dp),
            ) {
                Column {
                    Text("Статистика", color = white, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCell("Игр", "${p.games}", Modifier.weight(1f))
                        StatCell("Побед", "${p.wins}", Modifier.weight(1f))
                        StatCell("Поражений", "${p.losses}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCell("Процент побед", "${p.winRate}%", Modifier.weight(1f))
                        StatCell("Лучшая серия", "${p.bestStreak}", Modifier.weight(2f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Кнопка удаления — в потоке, после статистики
            if (p.isFriend) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(Color(0xFF3A1A17))
                        .clickable { vm.removeFriend(p.id); vm.closeProfile() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Удалить из друзей", color = white, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
            }
            // Кошелёк и магазин титулов — только в своём профиле
            if (p.isMe) {
                WalletCard(state, vm)
                Spacer(Modifier.height(12.dp))
            }
            // Тестовая реклама — только в своём профиле
            if (p.isMe) {
                Spacer(Modifier.height(12.dp))
                AdsButton(
                    text = "Проверить рекламу",
                    onClick = {
                        val a = context as? android.app.Activity
                        if (a != null) {
                            AdsManager.showInterstitial(a)
                        }
                    },
                )
            }
        }

        // Кнопка назад — слева сверху
        Box(
            modifier = Modifier
                .padding(start = 20.dp, top = 20.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(plaqueBtn)
                .clickable { vm.closeProfile() },
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_back), contentDescription = "Назад", modifier = Modifier.size(22.dp))
        }

        // Хотбар — тот же, что в меню (стекло), но без блюра (чёрный фон)
        MainBottomBar(
            selected = tab,
            onSelect = { vm.closeProfile(); onSelectTab(it); if (it == 2) vm.requestFriends() },
            backdrop = backdrop,
            useGlass = false,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
private fun ProfileTab(state: UiState, vm: GameViewModel) {
    var nameDraft by remember { mutableStateOf(state.myName) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) encodeAvatar(context, uri)?.let { vm.setPhoto(it) }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            PlayerAvatar(state.myName, 96.dp, avatarId=state.myAvatarId, photo=state.myPhoto)
            Row(Modifier.padding(top=12.dp), Arrangement.spacedBy(10.dp)) {
                OutlinedButton({ picker.launch("image/*") }) { Text("ФОТО", color=AppColors.Primary) }
                if(state.myPhoto.isNotBlank()) OutlinedButton(vm::clearPhoto) { Text("УБРАТЬ", color=AppColors.Danger) }
            }
            // выбор аватара из палитры — раньше аватар нельзя было поменять
            Text(
                "АВАТАР",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
            )
            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(ProfileStore.AVATAR_COUNT) { id ->
                    val selected = state.myAvatarId == id
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(avatarColor(id))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) Color.White else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable { vm.setAvatar(id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
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
                        Text(if (state.myPid > 0) state.myPid.toString() else state.myId, color=AppColors.TextSecondary, fontSize=14.sp, modifier=Modifier.weight(1f))
                        NeonButton("КОПИРОВАТЬ", {
                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("id", if (state.myPid > 0) state.myPid.toString() else state.myId))
                            vm.notify("Айди скопирован")
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletCard(state: UiState, vm: GameViewModel) {
    val wallet = state.wallet
    val next = TitleCatalog.nextAffordable(wallet.coins)
    Column(
        Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(26.dp), alpha = 0.66f)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinDot(34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${wallet.coins}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
                Text(
                    "МОНЕТ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextSecondary,
                    letterSpacing = 1.5.sp,
                )
            }
            if (wallet.streak > 0) {
                StatChip("серия ${wallet.streak}", color = AppColors.Warning)
            }
        }

        if (wallet.titleId != 0) {
            PlayerTitleBadge(wallet.titleId, fontSize = 12.sp)
        }

        if (next != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(
                        "До «${next.name}»",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "ещё ${next.price - wallet.coins}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                ThinProgress(
                    value = (wallet.coins.toFloat() / next.price).coerceIn(0f, 1f),
                    color = AppColors.Warning,
                )
            }
        }

        GlassActionButton("МАГАЗИН ТИТУЛОВ", { vm.requestShop() }, Modifier.fillMaxWidth())
    }
}

/** Тонкая полоска прогресса — используется в кошельке и магазине. */
@Composable
private fun ThinProgress(value: Float, color: Color = AppColors.Primary) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(value)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
private fun ShopScreen(state: UiState, vm: GameViewModel) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val wallet = state.wallet
    val busy = state.shopBusy
    val canWatch = activity != null && wallet.videosLeft > 0 && !busy

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        Column(Modifier.fillMaxSize()) {
            // Шапка — как в комнате и настройках
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(TopBarShape)
                    .background(TopBarColor),
            ) {
                Row(
                    Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassCircleButton(onClick = { vm.dismissShop() }, size = 44.dp) {
                        Image(painterResource(R.drawable.ic_back), null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "МАГАЗИН",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    CoinDot(20.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${wallet.coins}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(16.dp))
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .glassSurface(RoundedCornerShape(26.dp), alpha = 0.66f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Заработать",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                        Text(
                            "Ролик — 10 монет",
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                    Text(
                        "осталось ${wallet.videosLeft} из 20",
                        color = if (wallet.videosLeft > 0) Color.White else AppColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                ThinProgress(
                    value = wallet.videosLeft / 20f,
                    color = if (wallet.videosLeft > 0) AppColors.Warning else Color.White.copy(alpha = 0.25f),
                )
                GlassActionButton(
                    text = if (wallet.videosLeft > 0) "СМОТРЕТЬ РОЛИК" else "ЛИМИТ НА СЕГОДНЯ",
                    onClick = { if (canWatch) vm.watchVideoForCoins(activity) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                "ТИТУЛЫ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 10.dp),
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(TitleCatalog.all.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { def ->
                            TitleCard(
                                def = def,
                                wallet = wallet,
                                busy = busy,
                                canEarn = canWatch,
                                modifier = Modifier.weight(1f),
                                onBuy = {
                                    // хватает денег — покупаем сразу, иначе смотрим ролик
                                    if (wallet.coins >= def.price) vm.buyTitle(def.id) else if (canWatch) vm.watchVideoForCoins(activity, def.id)
                                },
                                onEquip = { vm.equipTitle(def.id) },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleCard(
    def: TitleDef,
    wallet: Wallet,
    busy: Boolean,
    canEarn: Boolean,
    modifier: Modifier = Modifier,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
) {
    val owned = wallet.owns(def.id)
    val equipped = wallet.titleId == def.id
    val canBuy = wallet.coins >= def.price
    Column(
        modifier
            .glassSurface(
                RoundedCornerShape(22.dp),
                tint = if (equipped) AppColors.Warning.copy(alpha = 0.16f) else Color.Transparent,
                alpha = if (equipped) 0.85f else 0.60f,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            def.name,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (equipped) AppColors.Warning else Color.White,
            maxLines = 2,
            minLines = 2,
        )
        Spacer(Modifier.weight(1f))
        when {
            equipped -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AppColors.Warning),
                )
                Spacer(Modifier.width(6.dp))
                Text("НАДЕТ", color = AppColors.Warning, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            owned -> GlassMiniButton("НАДЕТЬ", onEquip, !busy)
            canBuy -> GlassMiniButton("КУПИТЬ · ${def.price}", onBuy, !busy, filled = true)
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinDot(14.dp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${def.price}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                ThinProgress((wallet.coins.toFloat() / def.price).coerceIn(0f, 1f), color = AppColors.Warning)
                // не хватает монет — тот же ролик, что и в шапке
                GlassMiniButton("СМОТРЕТЬ +10", onBuy, canEarn && !busy)
            }
        }
    }
}

/** Компактная стеклянная кнопка для карточек магазина. */
@Composable
private fun GlassMiniButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    filled: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.06f)
                    filled -> AppColors.Primary
                    else -> Color.White.copy(alpha = 0.10f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            color = if (enabled) Color.White else AppColors.TextDisabled,
        )
    }
}

private fun LazyListScope.roomsItems(state: UiState, vm: GameViewModel) {
    items(state.rooms, key = { it.id }) { room ->
        RoomCard(
            room.name,
            room.players,
            room.maxPlayers,
            room.timer,
            room.mode,
            isPrivate = room.isPrivate,
            onJoin = { vm.joinRoomById(room.id) },
        )
    }
    if(state.rooms.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(vertical=24.dp), Alignment.Center) { Text("Пока пусто...", color=AppColors.TextSecondary) } }
}

@Composable
private fun CreateRoomScreen(state: UiState, vm: GameViewModel) {
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("classic") }
    var minL by remember { mutableStateOf(0) }
    var theme by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(6) }
    var timer by remember { mutableStateOf(15) }
    var randomTimer by remember { mutableStateOf(false) }
    var acceleration by remember { mutableStateOf(false) }
    var isPrivate by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(true) }

    val block = Color(0xFF161618)
    val divider = Color(0xFF2C2C2E)
    val accent = Color.White
    val gray = Color(0xFF8E8E93)

    // Лимиты зависят от режима
    val maxPlayers = if (mode == "duel") 2 else if (mode == "teams") 6 else 6
    val maxTimer = if (mode == "blitz") 8 else 120
    val safePlayers = players.coerceIn(2, maxPlayers)
    val safeTimer = timer.coerceIn(5, maxTimer)
    if (maxPlayers == 2) players = 2

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        ) {
            // Плашка заголовка, стиль как в меню
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(TopBarShape)
                    .background(TopBarColor),
            ) {
                Row(
                    Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = vm::dismissCreateRoom) {
                        Image(painterResource(R.drawable.ic_back), null, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        "Новая комната",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            // Настройки комнаты на тёмной плашке
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(block)
                    .padding(top = 16.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp),
                ) {
                    // Название комнаты
                    item {
                        Text("Название", modifier = Modifier.padding(top = 4.dp, bottom = 10.dp), color = Color(0xFF9AA2B5), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            BasicTextField(
                                value = name,
                                onValueChange = { if (it.length <= 30) name = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                        if (name.isEmpty()) Text("Комната", color = gray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                        inner()
                                    }
                                },
                            )
                        }
                    }
                    item { SettingsDivider(divider) }
                    item { SectionLabel("Режим игры") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(listOf("classic" to "Классика", "blitz" to "Блиц", "marathon" to "Марафон", "teams" to "2v2", "duel" to "Дуэль")) { (k, l) ->
                                ModeChip(l, mode == k, onClick = { mode = k })
                            }
                        }
                    }
                    item { SettingsDivider(divider) }

                    // Игроки
                    item { SectionLabel("Игроки") }
                    item {
                        SettingsPickRow(label = "${safePlayers} чел.", max = maxPlayers, value = safePlayers, accent = accent)
                    }
                    if (maxPlayers > 2) {
                        item {
                            Slider(
                                value = safePlayers.toFloat(),
                                onValueChange = { players = it.toInt() },
                                valueRange = 2f..maxPlayers.toFloat(),
                                steps = (maxPlayers - 3).coerceAtLeast(0),
                                colors = SliderDefaults.colors(
                                    thumbColor = accent,
                                    activeTrackColor = accent,
                                    inactiveTrackColor = Color(0xFF2C2C2E),
                                ),
                            )
                        }
                    }
                    item { SettingsDivider(divider) }

                    // Время на ход
                    item { SectionLabel("Время на ход") }
                    item {
                        SettingsPickRow(label = "${safeTimer} сек", max = maxTimer, value = safeTimer, accent = accent)
                    }
                    item {
                        Slider(
                            value = safeTimer.toFloat(),
                            onValueChange = { timer = it.toInt() },
                            valueRange = 5f..maxTimer.toFloat(),
                            steps = ((maxTimer - 5) / 5 - 1).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = Color(0xFF2C2C2E),
                            ),
                        )
                    }
                    item { SettingsDivider(divider) }

                    // Усложнение
                    item { SectionLabel("Минимальная длина слова") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(listOf(0, 3, 4, 5, 6)) { len ->
                                ModeChip(if (len == 0) "Любая" else "$len+", minL == len, onClick = { minL = len })
                            }
                        }
                    }
                    item { SettingsDivider(divider) }

                    // Тема слов
                    item { SectionLabel("Тема слов") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(listOf("" to "Любая", "food" to "Еда", "animals" to "Животные", "cities" to "Города", "plants" to "Растения", "countries" to "Страны")) { (k, l) ->
                                ModeChip(l, theme == k, onClick = { theme = k })
                            }
                        }
                    }
                    item { SettingsDivider(divider) }

                    // Быстрые опции
                    item { SettingsToggleRow("Случайное время", "Время на ход меняется случайно", randomTimer, { randomTimer = it }) }
                    item { SettingsDivider(divider) }
                    item { SettingsToggleRow("Ускорение", "Время сокращается с каждым словом", acceleration, { acceleration = it }) }
                    item { SettingsDivider(divider) }
                    item { SettingsToggleRow("Закрытая комната", "Вход только по паролю", isPrivate, { isPrivate = it }) }
                    if (isPrivate) {
                        item { SettingsDivider(divider) }
                        item {
                            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                Text(
                                    "Пароль комнаты",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                                Text(
                                    "Любые буквы или цифры. Его введут те, кто заходит к тебе",
                                    fontSize = 13.sp,
                                    color = gray,
                                )
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFF1C1C1F))
                                        .border(1.5.dp, Color(0xFF2C2C2E), RoundedCornerShape(18.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    BasicTextField(
                                        value = password,
                                        onValueChange = { if (it.length <= 40) password = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        ),
                                        singleLine = true,
                                        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                                        cursorBrush = SolidColor(AppColors.Primary),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (password.isEmpty()) {
                                        Text(
                                            "Например, «мяч»",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF5A5A5F),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (hidden) "Показать" else "Скрыть",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { hidden = !hidden }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                    item { SettingsDivider(divider) }
                }
            }
        }
        // Кнопка создания внизу поверх плашки
        val canCreate = !isPrivate || password.isNotBlank()
        Button(
            onClick = {
                vm.createRoom(
                    name.ifBlank { "Комната" },
                    isPrivate,
                    safeTimer,
                    safePlayers,
                    mode,
                    minL,
                    randomTimer,
                    acceleration,
                    theme,
                    password.trim().takeIf { isPrivate },
                )
            },
            enabled = canCreate,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF0B0B0B),
                disabledContainerColor = Color(0xFF2C2C2E),
                disabledContentColor = Color(0xFF6E6E73),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(58.dp)
                .align(Alignment.BottomCenter),
        ) {
            Text(
                if (isPrivate && password.isBlank()) "ВВЕДИ ПАРОЛЬ" else "СОЗДАТЬ",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun SettingsPickRow(label: String, max: Int, value: Int, accent: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            "до $max",
            fontSize = 14.sp,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PasswordScreen(state: UiState, vm: GameViewModel) {
    var password by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(true) }
    val locked = state.passwordFails >= 5
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(TopBarShape)
                    .background(TopBarColor),
            ) {
                Row(
                    Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = vm::dismissPassword) {
                        Image(painterResource(R.drawable.ic_back), null, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        "Вход в комнату",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(Color(0xFF161618))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0x1FFFB020))
                        .border(1.5.dp, Color(0x59FFB020), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.ic_lock), null, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    state.pendingRoomName.ifBlank { "Закрытая комната" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.passwordFails > 0 && !locked) "Неверный пароль, попробуй ещё"
                    else if (locked) "Слишком много попыток. Попроси пароль у создателя"
                    else "Введи пароль, который задал создатель комнаты",
                    fontSize = 15.sp,
                    color = if (locked) AppColors.Primary else Color(0xFF8E8E93),
                )
                Spacer(Modifier.height(28.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C1F))
                        .border(1.5.dp, Color(0xFF2C2C2E), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = password,
                        onValueChange = { if (it.length <= 40) password = it },
                        enabled = !locked,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                        singleLine = true,
                        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                        cursorBrush = SolidColor(AppColors.Primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (password.isEmpty() && !locked) {
                        Text(
                            "Пароль",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF5A5A5F),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (hidden) "Показать" else "Скрыть",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { hidden = !hidden }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }
        Button(
            onClick = { vm.joinRoomByPassword(password) },
            enabled = password.isNotBlank() && !locked,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF0B0B0B),
                disabledContainerColor = Color(0xFF2C2C2E),
                disabledContentColor = Color(0xFF6E6E73),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(58.dp)
                .align(Alignment.BottomCenter),
        ) {
            Text("ВОЙТИ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun RoomScreen(state: UiState, vm: GameViewModel) {
    val room = state.room ?: return
    val accent = Color(0xFF6B33D6)
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(TopBarShape)
                    .background(TopBarColor),
            ) {
                Row(
                    Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = vm::leaveRoom) {
                        Image(painterResource(R.drawable.ic_back), null, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        room.name.ifBlank { "Комната" },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    if (room.isPrivate) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1C1C1F))
                                .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(50))
                                .height(36.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(painterResource(R.drawable.ic_lock), null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Закрытая", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5C5C7))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.offline) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2A2118))
                        .border(1.dp, Color(0xFF4A3A22), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Нет связи с сервером — переподключаюсь…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE8C89A),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Color(0xFF161618))
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Игроки",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${room.players.size} из ${room.maxPlayers}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93),
                        )
                    }
                }
                items(room.players, key = { it.id }) { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1C1C1F))
                            .border(1.dp, Color(0xFF2A2A2D), RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerAvatar(p.name, 44.dp, avatarId = p.avatarId, photo = p.photo)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(
                                p.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            PlayerTitleBadge(p.titleId)
                            Text(
                                if (p.id == room.hostId) "Создатель" else "Игрок",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8E8E93),
                            )
                        }
                        if (p.id == room.hostId) {
                            Text(
                                "ХОЗЯИН",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x336B33D6))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
                if (room.isPrivate && room.password.isNotBlank()) {
                    item {
                        Column {
                            Text(
                                "Пароль комнаты",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF8E8E93),
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                            )
                            RoomPasswordCard(room.password) { vm.copyRoomPassword(room.password) }
                        }
                    }
                }
                item {
                    Text(
                        "Настройки",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    )
                }
                item { RoomSettingRow("Режим", modeLabel(room.mode)) }
                item { RoomSettingRow("Время на ход", "${room.turnSecondsOrTimer()} сек") }
                item { RoomSettingRow("Минимальная длина", if (room.minWordLen > 0) "${room.minWordLen}+ букв" else "любая") }
                item { RoomSettingRow("Тема слов", gameThemeTitle(room.theme).ifBlank { "Любая" }) }
                item { RoomSettingRow("Случайное время", if (room.randomTimer) "включено" else "выключено") }
                item { RoomSettingRow("Ускорение", if (room.acceleration) "включено" else "выключено") }
                item {
                    Column(Modifier.padding(top = 14.dp)) {
                        Button(
                            onClick = vm::startGame,
                            enabled = room.isHost && room.players.size >= 2,
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0B0B0B),
                                disabledContainerColor = Color(0xFF2C2C2E),
                                disabledContentColor = Color(0xFF6E6E73),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                        ) {
                            Text(
                                when {
                                    !room.isHost -> "ЖДЁМ СОЗДАТЕЛЯ"
                                    room.players.size < 2 -> "НУЖЕН ВТОРОЙ ИГРОК"
                                    else -> "НАЧАТЬ ИГРУ"
                                },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = vm::leaveRoom,
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC5C5C7)),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2D)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                        ) {
                            Text("ВЕРНУТЬСЯ В ЛОББИ", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun RoomState.turnSecondsOrTimer(): Int = timer

@Composable
private fun RoomSettingRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1F))
            .border(1.dp, Color(0xFF2A2A2D), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            modifier = Modifier.weight(1f),
        )
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun gameThemeTitle(theme: String): String = when (theme) {
    "food" -> "Еда"
    "animals" -> "Животные"
    "cities" -> "Города"
    "plants" -> "Растения"
    "countries" -> "Страны"
    else -> ""
}

@Composable
private fun GameScreen(state: UiState, vm: GameViewModel) {
    val game = state.game ?: return
    val players = state.room?.players ?: emptyList()
    var text by remember { mutableStateOf("") }

    fun send() {
        val w = text.trim()
        if (w.isNotEmpty()) {
            vm.submitWord(w)
            text = ""
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B)),
    ) {
        AnimatedGameBackdrop(accent = Color(state.gameAccent), modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(onClick = { vm.leaveRoom() }, size = 56.dp) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Домой",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                GlassPill(Modifier.weight(1f), height = 56.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${players.size} чел",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(12.dp))
                        OverlappingAvatars(players)
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.width(12.dp))
                StreakChip(game.totalWords, height = 56.dp)
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TurnInfoPlate(
                    myTurn = game.myTurn,
                    playerName = players.firstOrNull { it.id == game.turnPlayerId }?.name ?: "",
                    themeTitle = gameThemeTitle(game.theme),
                    accent = Color(state.gameAccent),
                )
            }
            Spacer(Modifier.weight(1f))
            GameTurnArea(
                letter = game.requiredLetter,
                endIn = game.endIn,
                turnSeconds = game.turnSeconds.takeIf { it > 0 } ?: game.timer,
                accent = Color(state.gameAccent),
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassWordInput(
                    value = text,
                    onValueChange = { text = it },
                    enabled = game.myTurn,
                    onSend = { send() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                GlassCircleButton(
                    onClick = { send() },
                    size = 68.dp,
                    enabled = game.myTurn && text.isNotBlank(),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Отправить",
                        tint = if (game.myTurn && text.isNotBlank()) Color.White else Color(0xFF636366),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTurnArea(letter: String, endIn: Long, turnSeconds: Int, accent: Color) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endIn) {
        while (true) {
            now = System.currentTimeMillis()
            delay(100)
        }
    }
    val remainingMs = (endIn - now).coerceAtLeast(0L)
    val totalMs = turnSeconds.toLong() * 1000L
    val fraction = if (totalMs > 0L) remainingMs.toFloat() / totalMs else 0f
    val secondsLeft = kotlin.math.ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LetterRing(letter, fraction, size = 248.dp, accent = accent)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "$secondsLeft",
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultScreen(state: UiState, vm: GameViewModel) {
    val winner = state.winner ?: WinnerInfo("", "Никто")
    val accent = Color(state.gameAccent)
    val scores = state.game?.scores.orEmpty()
    val players = state.room?.players.orEmpty()
    val standings = remember(players, scores) {
        players.sortedWith(
            compareByDescending<Player> { scores[it.id] ?: it.score }.thenBy { it.name },
        )
    }
    val myPlace = remember(standings, state.myId) {
        standings.indexOfFirst { it.id == state.myId }.takeIf { it >= 0 }?.plus(1)
    }
    val iWon = state.myId.isNotBlank() && winner.id == state.myId
    val totalWords = state.game?.totalWords ?: 0

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        AnimatedGameBackdrop(accent = accent, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.45f))

            Text(
                text = if (iWon) "ТЫ ПОБЕДИЛ" else "ПОБЕДА",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (iWon) "Это твой ход победил" else "Победитель игры",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(30.dp))
            WinnerPlate(winner = winner, isMe = iWon)

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VictoryStat(
                    label = "СЛОВ В ИГРЕ",
                    value = "$totalWords",
                    modifier = Modifier.weight(1f),
                )
                VictoryStat(
                    label = "МОЁ МЕСТО",
                    value = myPlace?.let { "$it из ${standings.size}" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(28.dp))
            GlassActionButton(
                text = "В ЛОББИ",
                onClick = vm::leaveRoom,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Стеклянная плашка с аватаром и ником победителя. */
@Composable
private fun WinnerPlate(winner: WinnerInfo, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(winner.name, 60.dp, avatarId = winner.avatarId, photo = winner.photo)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                winner.name.ifBlank { "Никто" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            PlayerTitleBadge(winner.titleId, fontSize = 13.sp)
            Text(
                if (isMe) "Это ты" else "Победитель",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isMe) Color.White else AppColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}

/** Крупная стеклянная карточка статистики на всю ширину. */
@Composable
private fun VictoryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(100.dp)
            .glassSurface(RoundedCornerShape(24.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(62.dp)
            .glassSurface(
                shape = RoundedCornerShape(22.dp),
                alpha = 0.62f,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 0.5.sp,
        )
    }
}
@Composable
private fun SettingsScreen(state: UiState, vm: GameViewModel) {
    val block = Color(0xFF161618)
    val divider = Color(0xFF2C2C2E)
    val accent = Color.White

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        ) {
            // Плашка заголовка, стиль как в меню
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(TopBarShape)
                    .background(TopBarColor),
            ) {
                Row(
                    Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = vm::closeSettings) {
                        Image(painterResource(R.drawable.ic_back), null, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        "Настройки",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            // Настройки в списке на тёмной плашке, по контенту
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(36.dp))
                    .background(block)
                    .padding(top = 16.dp, bottom = 24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    SectionLabel("Звук")
                    SettingsToggleRow("Звук", "Звуковые эффекты в игре", state.soundOn, vm::setSoundOn)
                    SettingsDivider(divider)
                    SettingsSliderRow("Громкость", state.volume, vm::setVolume, enabled = state.soundOn, accent)
                    SectionLabel("Вибрация")
                    SettingsToggleRow("Вибрация", "Отклик при действиях", state.vibrationOn, vm::setVibrationOn)
                    SettingsDivider(divider)
                    SettingsSliderRow("Сила вибрации", state.vibrationIntensity, vm::setVibrationIntensity, enabled = state.vibrationOn, accent)
                    SectionLabel("Уведомления")
                    SettingsToggleRow("Уведомления", "Пуш о ходе и приглашениях", state.notificationsOn, vm::setNotificationsOn)
                    SectionLabel("Кастомизация")
                    AccentPickerRow(state.gameAccent, vm::setGameAccent)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPickerRow(current: Int, onPick: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            "Цвет градиента игры",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Меняет фон и свечение вокруг буквы",
            fontSize = 13.sp,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameAccents.forEach { accent ->
                val argb = accent.toArgb()
                val selected = argb == current
                Box(
                    Modifier
                        .size(if (selected) 46.dp else 40.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.22f),
                            shape = CircleShape,
                        )
                        .clickable { onPick(argb) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        color = Color(0xFF9AA2B5),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsDivider(color: Color) {
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(color))
}

@Composable
private fun SettingsToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 13.sp, color = Color(0xFF8E8E93))
        }
        SettingsToggle(checked, onCheckedChange)
    }
}

@Composable
private fun SettingsToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val anim by animateFloatAsState(if (checked) 1f else 0f, tween(200), label = "toggle")
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) Color.White else Color(0xFF3A3A3C))
            .clickable(onClick = { onCheckedChange(!checked) }),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(24.dp)
                .offset(x = 22.dp * anim)
                .clip(CircleShape)
                .background(if (checked) Color(0xFF0B0B0B) else Color.White),
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    accent: Color,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.White else Color(0xFF8E8E93),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color(0xFF2C2C2E),
                disabledThumbColor = Color(0xFF5A5A5E),
                disabledActiveTrackColor = Color(0xFF3A3A3C),
                disabledInactiveTrackColor = Color(0xFF2C2C2E),
            ),
        )
    }
}

@Composable
private fun AdsButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
