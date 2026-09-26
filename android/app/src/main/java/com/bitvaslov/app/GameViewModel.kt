package com.bitvaslov.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class GameViewModel : ViewModel() {

    var appContext: Context? = null

    private val _ui = MutableStateFlow(
        UiState(
            soundOn = SettingsStore.soundOn,
            volume = SettingsStore.volume,
            vibrationOn = SettingsStore.vibrationOn,
            vibrationIntensity = SettingsStore.vibrationIntensity,
            notificationsOn = SettingsStore.notificationsOn,
            gameAccent = SettingsStore.gameAccent,
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var client: GameClient? = null
    private var roomListJob: kotlinx.coroutines.Job? = null
    private var connectErrorJob: kotlinx.coroutines.Job? = null
    private var _isActive = true
    private var lastCreateAt = 0L

    // пароль приватной комнаты, которую создали мы: сервер отдаёт его хосту
    // обратно, держим под конкретный roomId, чтобы "Ещё раз" пересоздала такую же
    private var ownedRoomId: String? = null
    private var ownedRoomPassword: String? = null

    init {
        _ui.update {
            it.copy(
                myId = ProfileStore.playerId,
                myName = ProfileStore.name,
                myAvatarId = ProfileStore.avatarId,
                myPhoto = ProfileStore.photo,
                wallet = Wallet(
                    coins = ProfileStore.coins,
                    titleId = ProfileStore.titleId,
                    ownedTitles = ProfileStore.ownedTitles,
                ),
            )
        }
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { setPushToken(it) }
                }
            }
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic("all")
            .addOnCompleteListener { }
    }

    fun autoConnect() {
        val name = ProfileStore.name.ifBlank {
            "Игрок" + ProfileStore.playerId.takeLast(4)
        }
        if (ProfileStore.name.isBlank()) ProfileStore.saveName(name)
        connect(_ui.value.serverUrl, name)
    }

    fun connect(url: String, name: String) {
        _ui.update {
            it.copy(screen = Screen.CONNECT, serverUrl = url, myName = name, error = null)
        }
        client?.disconnect()
        client = GameClient { event, data -> handleEvent(event, data) }
        client?.connect(url)

        roomListJob?.cancel()
        roomListJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                when (_ui.value.screen) {
                    Screen.LOBBY -> client?.refreshRooms()
                    // во время партии и на экране итогов тоже опрашиваем, иначе очки и реконнект не видны
                    Screen.ROOM, Screen.GAME, Screen.RESULT -> client?.requestRoom()
                    else -> Unit
                }
            }
        }
    }

    fun setServerUrl(url: String) = _ui.update { it.copy(serverUrl = url) }

    fun consumeInvite() {
        if (!DeepLink.hasInvite) return
        val roomId = DeepLink.roomId
        DeepLink.clear()
        if (!roomId.isNullOrBlank()) joinRoomById(roomId)
    }

    fun notifyNow() {
        if (DeepLink.hasInvite) consumeInvite()
    }

    fun setMyName(name: String) {
        _ui.update { it.copy(myName = name) }
        ProfileStore.saveName(name)
    }

    fun setAvatar(id: Int) {
        ProfileStore.saveAvatar(id)
        _ui.update { it.copy(myAvatarId = ProfileStore.avatarId) }
        client?.login(_ui.value.myId, _ui.value.myName.trim(), ProfileStore.avatarId, _ui.value.myPhoto)
    }

    fun setPhoto(base64: String) {
        ProfileStore.savePhoto(base64)
        _ui.update { it.copy(myPhoto = base64) }
        client?.login(_ui.value.myId, _ui.value.myName.trim(), _ui.value.myAvatarId, base64)
    }

    fun clearPhoto() = setPhoto("")

    // ─── Монетки, ролики, магазин ─────────────────────────────────────────
    fun requestWallet() {
        client?.requestWallet()
    }

    fun buyTitle(titleId: Int) {
        if (_ui.value.shopBusy) return
        _ui.update { it.copy(shopBusy = true) }
        client?.buyTitle(titleId)
    }

    fun equipTitle(titleId: Int) {
        if (_ui.value.shopBusy) return
        _ui.update { it.copy(shopBusy = true, pendingBuyTitleId = null) }
        client?.equipTitle(titleId)
    }

    /**
     * Ролик как способ заработка. thenBuyTitleId — титул, который не хватило купить:
     * после награды покупка проходит сама, без второго тапа.
     */
    fun watchVideoForCoins(activity: android.app.Activity, thenBuyTitleId: Int? = null) {
        if (_ui.value.shopBusy) return
        val wallet = _ui.value.wallet
        if (client?.isConnected != true) {
            _ui.update { it.copy(toast = "Нет связи с сервером") }
            return
        }
        if (wallet.videosLeft <= 0) {
            _ui.update { it.copy(toast = "Ролики на сегодня уже все — приходи завтра") }
            return
        }
        _ui.update { it.copy(shopBusy = true, pendingBuyTitleId = thenBuyTitleId) }
        AdsManager.showRewarded(
            activity = activity,
            onReward = { client?.claimReward() },
            onUnavailable = {
                _ui.update {
                    it.copy(shopBusy = false, pendingBuyTitleId = null, toast = "Ролик не загрузился, попробуй чуть позже")
                }
            },
        )
    }

    fun requestCreateRoom() = _ui.update { it.copy(screen = Screen.CREATEROOM) }
    fun dismissCreateRoom() = _ui.update { it.copy(screen = Screen.LOBBY) }

    fun requestShop() {
        client?.requestWallet()
        _ui.update { it.copy(screen = Screen.SHOP, error = null, toast = null) }
    }
    fun dismissShop() = _ui.update { it.copy(screen = Screen.LOBBY) }

    fun requestPassword(roomId: String, roomName: String) = _ui.update {
        it.copy(screen = Screen.PASSWORD, pendingRoomId = roomId, pendingRoomName = roomName, passwordFails = 0)
    }
    fun dismissPassword() = _ui.update {
        it.copy(screen = Screen.LOBBY, pendingRoomId = null, pendingRoomName = "", passwordFails = 0)
    }

    fun copyRoomPassword(password: String) {
        if (password.isBlank()) return
        val ctx = appContext ?: return
        val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clip.setPrimaryClip(ClipData.newPlainText("Пароль комнаты", password))
        _ui.update { it.copy(toast = "Пароль скопирован") }
    }

    fun createRoom(
        roomName: String,
        isPrivate: Boolean,
        timer: Int,
        maxPlayers: Int,
        mode: String = "classic",
        minWordLen: Int = 0,
        randomTimer: Boolean = false,
        acceleration: Boolean = false,
        theme: String = "",
        password: String? = null
    ): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastCreateAt < 2000) return false
        lastCreateAt = now
        client?.createRoom(
            _ui.value.myName,
            roomName,
            isPrivate,
            timer,
            maxPlayers,
            _ui.value.myAvatarId,
            _ui.value.myPhoto,
            mode,
            minWordLen,
            randomTimer,
            acceleration,
            theme,
            _ui.value.wallet.titleId,
            password
        )
        return true
    }

    fun setRoomSettings(
        minWordLen: Int? = null,
        randomTimer: Boolean? = null,
        acceleration: Boolean? = null,
        theme: String? = null,
        mode: String? = null
    ) {
        client?.setRoomSettings(minWordLen, randomTimer, acceleration, theme, mode)
    }

    fun joinRoomById(roomId: String) {
        val room = _ui.value.rooms.firstOrNull { it.id == roomId }
        if (room != null && room.isPrivate) {
            requestPassword(room.id, room.name)
            return
        }
        client?.joinRoom(
            _ui.value.myName,
            roomId = roomId,
            avatarId = _ui.value.myAvatarId,
            photo = _ui.value.myPhoto,
            titleId = _ui.value.wallet.titleId,
        )
    }

    fun joinRoomByPassword(password: String) {
        val roomId = _ui.value.pendingRoomId ?: return
        val value = password.trim()
        if (value.isEmpty()) return
        // счётчик не растёт на каждый тап: он увеличивается только по факту ошибки от сервера
        _ui.update { it.copy(error = null, toast = null) }
        client?.joinRoom(
            _ui.value.myName,
            roomId = roomId,
            password = value,
            avatarId = _ui.value.myAvatarId,
            photo = _ui.value.myPhoto,
            titleId = _ui.value.wallet.titleId,
        )
    }

    fun startGame() = client?.startGame()

    /**
     * "Ещё раз" не запускает партию сразу — создаёт новое лобби с теми же
     * настройками и ждёт, пока хост нажмёт «Начать». Иначе второй игрок
     * оказывается в игре, на которую не соглашался.
     */
    fun playAgain() {
        val prev = _ui.value.room
        if (prev == null) {
            leaveRoom()
            return
        }
        val password = if (ownedRoomId == prev.id) ownedRoomPassword else null
        if (prev.isPrivate && password.isNullOrBlank()) {
            // пароль есть только у хоста, пересоздать приватную комнату без
            // него нельзя — отправляем на форму, пусть введёт заново
            leaveRoom()
            _ui.update {
                it.copy(screen = Screen.CREATEROOM, toast = "Придумай пароль для новой комнаты")
            }
            return
        }
        val created = createRoom(
            roomName = prev.name,
            isPrivate = prev.isPrivate,
            timer = prev.timer,
            maxPlayers = prev.maxPlayers,
            mode = prev.mode,
            minWordLen = prev.minWordLen,
            randomTimer = prev.randomTimer,
            acceleration = prev.acceleration,
            theme = prev.theme,
            password = password,
        )
        if (created) {
            _ui.update { it.copy(game = null, winner = null) }
        } else {
            // антиспам не дал создать комнату — экран результата остаётся как был
            _ui.update { it.copy(toast = "Подожди пару секунд и попробуй ещё раз") }
        }
    }

    fun submitWord(word: String) = client?.submitWord(word)
    fun leaveRoom() {
        _ui.update { it.copy(screen = Screen.LOBBY, room = null, game = null, winner = null) }
        client?.leaveRoom()
    }
    fun refreshRooms() = client?.refreshRooms()
    fun loadStats() = client?.requestStats(_ui.value.myName)
    fun toggleSettings() = _ui.update { it.copy(screen = Screen.SETTINGS) }
    fun closeSettings() = _ui.update { it.copy(screen = Screen.LOBBY) }

    fun setSoundOn(on: Boolean) {
        SettingsStore.setSound(on)
        _ui.update { it.copy(soundOn = on) }
        if (on) PlayFeedback.turn()
    }

    fun setVolume(value: Float) {
        SettingsStore.setVolume(value)
        _ui.update { it.copy(volume = value) }
    }

    fun setVibrationOn(on: Boolean) {
        SettingsStore.setVibration(on)
        _ui.update { it.copy(vibrationOn = on) }
    }

    fun setVibrationIntensity(value: Float) {
        SettingsStore.setVibrationIntensity(value)
        _ui.update { it.copy(vibrationIntensity = value) }
    }

    fun setNotificationsOn(on: Boolean) {
        SettingsStore.setNotifications(on)
        _ui.update { it.copy(notificationsOn = on) }
        syncPush()
    }

    fun setGameAccent(color: Int) {
        SettingsStore.setGameAccent(color)
        _ui.update { it.copy(gameAccent = color) }
    }

    fun setActive(active: Boolean) {
        _isActive = active
        client?.setActive(active)
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    fun clearToast() = _ui.update { it.copy(toast = null) }

    fun setPushToken(token: String) {
        PushTokenStore.current = token
        sendPushToken()
    }

    private fun sendPushToken() {
        if (!_ui.value.notificationsOn) return
        val id = _ui.value.myId
        val token = PushTokenStore.current ?: return
        if (id.isBlank()) return
        client?.registerPush(id, token)
    }

    private fun syncPush() {
        val id = _ui.value.myId
        if (id.isBlank()) return
        if (_ui.value.notificationsOn) {
            val token = PushTokenStore.current ?: return
            client?.registerPush(id, token)
        } else {
            client?.unregisterPush(id)
        }
    }

    fun requestFriends() = client?.requestFriends()
    fun searchUser(name: String) = client?.searchUser(name.trim())
    fun clearSearch() = _ui.update { it.copy(searchResults = emptyList()) }
    fun openProfile(id: String) {
        _ui.update { it.copy(profile = null) }
        client?.requestProfile(id)
    }
    fun closeProfile() = _ui.update { it.copy(profile = null) }
    fun notify(msg: String) = _ui.update { it.copy(toast = msg) }
    fun addFriend(id: String) = client?.sendFriendRequest(id)
    fun respondFriend(id: String, accept: Boolean) = client?.respondFriendRequest(id, accept)
    fun cancelFriendRequest(id: String) = client?.cancelFriendRequest(id)
    fun removeFriend(id: String) = client?.removeFriend(id)
    fun inviteFriend(id: String) = client?.inviteFriend(id)

    fun acceptInvite() {
        val invite = _ui.value.incomingInvite ?: return
        _ui.update { it.copy(incomingInvite = null) }
        if (invite.roomId.isNotBlank()) joinRoomById(invite.roomId)
    }

    fun declineInvite() = _ui.update { it.copy(incomingInvite = null) }

    private fun handleEvent(event: String, data: Any?) {
        viewModelScope.launch {
            when (event) {
                "connected" -> {
                    connectErrorJob?.cancel()
                    val wasConnect = _ui.value.screen == Screen.CONNECT
                    _ui.update { s ->
                        s.copy(
                            screen = if (wasConnect) Screen.LOBBY else s.screen,
                            toast = if (wasConnect) "Подключено" else null,
                            error = null,
                            offline = false,
                        )
                    }
                    sendPushToken()
                    client?.setActive(_isActive)
                    client?.login(_ui.value.myId, _ui.value.myName.trim(), _ui.value.myAvatarId, _ui.value.myPhoto)
                    client?.requestFriends()
                    consumeInvite()
                    loadStats()
                }

                "disconnected" -> {
                    _ui.update { s ->
                        s.copy(offline = true, toast = "Соединение потеряно, переподключаюсь...")
                    }
                }

                "connectError" -> {
                    // сеть моргнула — не показываем ошибку сразу, reconnect может пройти
                    val raw = data as? String ?: ""
                    val msg = friendlyNetError(raw)
                    connectErrorJob?.cancel()
                    connectErrorJob = viewModelScope.launch {
                        delay(6000)
                        if (client?.isConnected != true) {
                            _ui.update { s -> s.copy(offline = true, error = msg, toast = msg) }
                        }
                    }
                }

                "connectFailed" -> {
                    connectErrorJob?.cancel()
                    val msg = data as? String ?: "Сервер недоступен"
                    _ui.update { s ->
                        s.copy(
                            offline = true,
                            error = msg,
                            toast = msg,
                            screen = if (s.room == null) Screen.CONNECT else s.screen,
                        )
                    }
                }

                "roomReplaced" -> {
                    _ui.update {
                        it.copy(
                            screen = Screen.LOBBY,
                            room = null,
                            game = null,
                            winner = null,
                            toast = "Подключение обновилось — войди в комнату заново",
                        )
                    }
                }

                "roomGone" -> {
                    _ui.update {
                        it.copy(
                            screen = Screen.LOBBY,
                            room = null,
                            game = null,
                            winner = null,
                            offline = false,
                            toast = "Комната закрылась",
                        )
                    }
                }

                "roomList" -> {
                    val arr = data as? JSONArray ?: return@launch
                    val rooms = buildList {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            runCatching { RoomSummary.fromJson(obj) }.getOrNull()?.let(::add)
                        }
                    }
                    _ui.update { it.copy(rooms = rooms, offline = false) }
                }

                "roomUpdate" -> {
                    val o = data as? JSONObject ?: return@launch
                    val room = runCatching { RoomState.fromJson(o) }.getOrNull() ?: return@launch
                    // хост приватной комнаты получает пароль обратно — запоминаем его,
                    // привязав к id комнаты, чтобы случайно не утечь в чужую
                    if (room.isPrivate && room.hostId == MyIds.current) {
                        ownedRoomId = room.id
                        ownedRoomPassword = room.password
                    } else if (ownedRoomId == room.id) {
                        ownedRoomId = null
                        ownedRoomPassword = null
                    }
                    _ui.update { s ->
                        s.copy(
                            room = room,
                            offline = false,
                            screen = when {
                                room.state == "lobby" && s.screen != Screen.ROOM -> Screen.ROOM
                                else -> s.screen
                            },
                            // game/winner намеренно не трогаем: сервер шлёт roomUpdate и во время партии,
                            // обнуление давало чёрный кадр на экране игры
                            error = null,
                        )
                    }
                }

                "gameStarted" -> {
                    _ui.update { it.copy(screen = Screen.GAME, winner = null, error = null, toast = "Игра началась!") }
                    PlayFeedback.started()
                }

                "gameUpdate" -> {
                    val o = data as? JSONObject ?: return@launch
                    val raw = runCatching { GameState.fromJson(o) }.getOrNull() ?: return@launch
                    val deadlineAt = System.currentTimeMillis() + raw.endIn
                    _ui.update { s ->
                        s.copy(
                            game = raw.copy(endIn = deadlineAt),
                            // экран переключаем только если ещё не в игре, иначе будет чёрный кадр
                            screen = if (s.screen == Screen.GAME || s.screen == Screen.RESULT) s.screen else Screen.GAME,
                            error = null,
                            toast = null,
                        )
                    }
                }

                "gameOver" -> {
                    val o = data as? JSONObject
                    val w = o?.optJSONObject("winner")?.let {
                        WinnerInfo(
                            it.optString("id", ""),
                            it.optString("name", ""),
                            it.optInt("avatarId", 0),
                            it.optInt("titleId", 0),
                            it.optString("photo", ""),
                        )
                    }
                    // сервер кладёт в rewards сколько монет досталось каждому,
                    // само начисление придёт следом в walletState
                    val gained = o?.optJSONObject("rewards")?.optInt(MyIds.current ?: "", 0) ?: 0
                    _ui.update {
                        it.copy(
                            winner = w,
                            screen = Screen.RESULT,
                            toast = if (gained > 0) "🪙 +$gained монет за матч" else it.toast,
                        )
                    }
                    PlayFeedback.result()
                }

                "walletState" -> {
                    val w = (data as? JSONObject)?.let { runCatching { Wallet.fromJson(it) }.getOrNull() }
                        ?: return@launch
                    ProfileStore.saveWallet(w.coins, w.titleId, w.ownedTitles)
                    _ui.update { it.copy(wallet = w, shopBusy = false) }
                }

                "streakBonus" -> {
                    val o = data as? JSONObject
                    val streak = o?.optInt("streak", 0) ?: 0
                    val bonus = o?.optInt("bonus", 0) ?: 0
                    if (bonus > 0) {
                        _ui.update { it.copy(toast = "🔥 Серия $streak дн. — +$bonus монет") }
                    }
                }

                "_ack_reward" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) == true) {
                        val gained = o.optInt("gained", 0)
                        val pending = _ui.value.pendingBuyTitleId
                        if (pending != null) {
                            // ролик посмотрен «в расчёте» покупку — пробуем купить сразу
                            _ui.update { it.copy(pendingBuyTitleId = null) }
                            client?.buyTitle(pending)
                        } else if (gained > 0) {
                            _ui.update { it.copy(shopBusy = false, toast = "🪙 +$gained монет") }
                        }
                    } else {
                        val msg = o?.optString("error", "Не удалось получить монеты") ?: "Не удалось получить монеты"
                        _ui.update { it.copy(shopBusy = false, pendingBuyTitleId = null, toast = msg) }
                    }
                }

                "_ack_buy" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) == true) {
                        val titleId = o.optInt("titleId", 0)
                        val title = TitleCatalog.name(titleId)
                        _ui.update {
                            it.copy(shopBusy = false, pendingBuyTitleId = null, toast = "Титул «$title» надет")
                        }
                    } else {
                        val msg = o?.optString("error", "Покупка не прошла") ?: "Покупка не прошла"
                        _ui.update { it.copy(shopBusy = false, pendingBuyTitleId = null, toast = msg) }
                    }
                }

                "_ack_equip" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) != true) {
                        val msg = o?.optString("error", "Не получилось") ?: "Не получилось"
                        _ui.update { it.copy(shopBusy = false, toast = msg) }
                    }
                }

                "wordAccepted" -> {
                    val o = data as? JSONObject
                    val points = o?.optInt("points", 0) ?: 0
                    val combo = o?.optInt("combo", 1) ?: 1
                    _ui.update {
                        it.copy(error = null, toast = "➤ +$points очков" + if (combo > 1) "  (комбо ×$combo)" else "")
                    }
                    PlayFeedback.turn()
                }

                "wordError" -> {
                    val msg = (data as? JSONObject)?.optString("message", "Ошибка") ?: "Ошибка"
                    _ui.update { it.copy(error = msg, toast = msg) }
                    PlayFeedback.error()
                }

                "playerEliminated" -> {
                    val name = (data as? JSONObject)?.optString("name", "")
                    _ui.update { it.copy(toast = "$name выбыл по таймеру") }
                    PlayFeedback.error()
                }

                "skipTurn" -> {
                    val name = (data as? JSONObject)?.optString("name", "")
                    _ui.update { it.copy(toast = "$name не успел — 0 очков") }
                    PlayFeedback.turn()
                }

                "errorMessage" -> {
                    val msg = (data as? JSONObject)?.optString("message", "Ошибка") ?: "Ошибка"
                    _ui.update { it.copy(error = msg, toast = msg) }
                }

                "_ack_create" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) != true) {
                        _ui.update { it.copy(error = o?.optString("error", "Не удалось создать комнату")) }
                    }
                }

                "_ack_join" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) != true) {
                        val msg = o?.optString("error", "Не удалось войти") ?: "Не удалось войти"
                        _ui.update {
                            // считаем неудачную попытку только здесь, по реальному отказу сервера
                            val inPassword = it.screen == Screen.PASSWORD
                            it.copy(
                                error = msg,
                                toast = msg,
                                passwordFails = if (inPassword) it.passwordFails + 1 else it.passwordFails,
                            )
                        }
                    } else {
                        _ui.update { it.copy(error = null, passwordFails = 0) }
                    }
                }

                "_ack_stats" -> {
                    val o = data as? JSONObject
                    if (o != null) {
                        _ui.update { it.copy(stats = PlayerStats.fromJson(o)) }
                    }
                }

                "_ack_profile" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) == true) {
                        _ui.update { it.copy(profile = UserProfile.fromJson(o)) }
                    } else {
                        _ui.update {
                            it.copy(error = o?.optString("error", "Не удалось открыть профиль") ?: "Не удалось открыть профиль")
                        }
                    }
                }

                "pid" -> {
                    val o = data as? JSONObject
                    val p = o?.optInt("pid", 0) ?: 0
                    if (p > 0) _ui.update { it.copy(myPid = p) }
                }

                "friendsUpdate" -> {
                    val o = data as? JSONObject ?: return@launch
                    val fArr = o.optJSONArray("friends") ?: JSONArray()
                    val inArr = o.optJSONArray("incoming") ?: JSONArray()
                    val outArr = o.optJSONArray("outgoing") ?: JSONArray()
                    val friends = fArr.mapObjects { Friend.fromJson(it) }
                    val incoming = inArr.mapObjects { FriendRef.fromJson(it) }
                    val outgoing = outArr.mapObjects { FriendRef.fromJson(it) }
                    _ui.update {
                        it.copy(
                            friends = friends.filter { f -> f.id.isNotBlank() },
                            incomingRequests = incoming.filter { r -> r.id.isNotBlank() },
                            outgoingRequests = outgoing.filter { r -> r.id.isNotBlank() },
                        )
                    }
                }

                "friendPresence" -> {
                    val o = data as? JSONObject ?: return@launch
                    val friendId = o.optString("id", "")
                    val online = o.optBoolean("online", false)
                    val inGame = o.optBoolean("inGame", false)
                    _ui.update { s ->
                        s.copy(
                            friends = s.friends.map {
                                if (it.id == friendId) it.copy(online = online, inGame = inGame) else it
                            },
                            searchResults = s.searchResults.map {
                                if (it.id == friendId) it.copy(online = online, inGame = inGame) else it
                            },
                        )
                    }
                }

                "userSearchResult" -> {
                    val arr = data as? JSONArray ?: return@launch
                    val list = arr.mapObjects { UserSummary.fromJson(it) }
                    _ui.update { it.copy(searchResults = list) }
                }

                "roomInvite" -> {
                    val o = data as? JSONObject ?: return@launch
                    _ui.update { it.copy(incomingInvite = RoomInvite.fromJson(o)) }
                }

                "_ack_friend" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) == true) {
                        _ui.update { it.copy(searchResults = emptyList()) }
                        client?.requestFriends()
                    } else {
                        _ui.update { it.copy(error = o?.optString("error", "Не получилось") ?: "Не получилось") }
                    }
                }

                "_ack_invite" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) == true) {
                        val online = o.optBoolean("online", false)
                        _ui.update {
                            it.copy(toast = if (online) "Приглашение отправлено" else "Уведомление отправлено")
                        }
                    } else {
                        _ui.update { it.copy(error = o?.optString("error", "Не удалось пригласить") ?: "Не удалось пригласить") }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        roomListJob?.cancel()
        connectErrorJob?.cancel()
        client?.disconnect()
        client = null
        super.onCleared()
    }
}

private inline fun <T> JSONArray.mapObjects(factory: (JSONObject) -> T): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        runCatching { factory(obj) }.getOrNull()?.let(out::add)
    }
    return out
}

/** Технические тексты socket.io/okhttp заменяем на понятные. */
private fun friendlyNetError(raw: String): String {
    val v = raw.lowercase()
    return when {
        v.isBlank() -> "Нет связи с сервером"
        v.contains("timeout") -> "Сервер не отвечает"
        v.contains("host") || v.contains("resolve") || v.contains("unknownhost") -> "Не найден сервер"
        v.contains("refused") -> "Сервер отказал в подключении"
        v.contains("ssl") || v.contains("certificate") || v.contains("handshake") -> "Проблема с защищённым соединением"
        v.contains("network") || v.contains("unreachable") || v.contains("offline") -> "Нет интернета"
        else -> "Нет связи с сервером"
    }
}

data class UiState(
    val screen: Screen = Screen.CONNECT,
    val serverUrl: String = "https://bitva-slov.onrender.com",
    val offline: Boolean = false,
    val myId: String = "",
    val myPid: Int = 0,
    val myName: String = "",
    val myAvatarId: Int = 0,
    val myPhoto: String = "",
    val rooms: List<RoomSummary> = emptyList(),
    val pendingRoomId: String? = null,
    val pendingRoomName: String = "",
    val passwordFails: Int = 0,
    val room: RoomState? = null,
    val game: GameState? = null,
    val winner: WinnerInfo? = null,
    val stats: PlayerStats? = null,
    val friends: List<Friend> = emptyList(),
    val incomingRequests: List<FriendRef> = emptyList(),
    val outgoingRequests: List<FriendRef> = emptyList(),
    val searchResults: List<UserSummary> = emptyList(),
    val profile: UserProfile? = null,
    val incomingInvite: RoomInvite? = null,
    val error: String? = null,
    val toast: String? = null,
    val soundOn: Boolean = true,
    val volume: Float = 0.7f,
    val vibrationOn: Boolean = true,
    val vibrationIntensity: Float = 0.5f,
    val notificationsOn: Boolean = true,
    val gameAccent: Int = SettingsStore.DEFAULT_ACCENT,
    val wallet: Wallet = Wallet(),
    val shopBusy: Boolean = false,
    val pendingBuyTitleId: Int? = null,
)