package com.bitvaslov.app

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

    private val _ui = MutableStateFlow(
        UiState(
            soundOn = SettingsStore.soundOn,
            volume = SettingsStore.volume,
            vibrationOn = SettingsStore.vibrationOn,
            vibrationIntensity = SettingsStore.vibrationIntensity,
            notificationsOn = SettingsStore.notificationsOn,
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var client: GameClient? = null
    private var roomListJob: kotlinx.coroutines.Job? = null
    private var _isActive = true
    private var lastCreateAt = 0L
    private var autoStarted = false

    init {
        _ui.update {
            it.copy(
                myId = ProfileStore.playerId,
                myName = ProfileStore.name,
                myAvatarId = ProfileStore.avatarId,
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
        if (autoStarted) return
        autoStarted = true
        if (ProfileStore.hasProfile) {
            connect(_ui.value.serverUrl, ProfileStore.name)
        }
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
                delay(4000)
                if (_ui.value.screen == Screen.LOBBY) client?.refreshRooms()
            }
        }
    }

    fun setServerUrl(url: String) = _ui.update { it.copy(serverUrl = url) }

    fun setMyName(name: String) {
        _ui.update { it.copy(myName = name) }
        ProfileStore.saveName(name)
    }

    fun setAvatar(id: Int) {
        ProfileStore.saveAvatar(id)
        _ui.update { it.copy(myAvatarId = ProfileStore.avatarId) }
        client?.login(_ui.value.myId, _ui.value.myName.trim(), ProfileStore.avatarId)
    }

    fun requestCreateRoom() = _ui.update { it.copy(screen = Screen.CREATEROOM) }
    fun dismissCreateRoom() = _ui.update { it.copy(screen = Screen.LOBBY) }

    fun requestCodeEntry() = _ui.update { it.copy(screen = Screen.CODEENTRY) }
    fun dismissCodeEntry() = _ui.update { it.copy(screen = Screen.LOBBY) }

    fun copyRoomCode(code: String) = _ui.update { it.copy(toast = "Код $code скопирован") }

    fun createRoom(roomName: String, isPrivate: Boolean, timer: Int, maxPlayers: Int) {
        val now = System.currentTimeMillis()
        if (now - lastCreateAt < 2000) return
        lastCreateAt = now
        client?.createRoom(_ui.value.myName, roomName, isPrivate, timer, maxPlayers)
    }

    fun joinRoomById(roomId: String) {
        client?.joinRoom(_ui.value.myName, roomId = roomId)
    }

    fun joinRoomByCode(code: String) {
        client?.joinRoom(_ui.value.myName, code = code)
    }

    fun startGame() = client?.startGame()
    fun playAgain() = client?.startGame()
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
    fun addFriend(id: String) = client?.sendFriendRequest(id)
    fun respondFriend(id: String, accept: Boolean) = client?.respondFriendRequest(id, accept)
    fun removeFriend(id: String) = client?.removeFriend(id)
    fun inviteFriend(id: String) = client?.inviteFriend(id)

    fun acceptInvite() {
        val invite = _ui.value.incomingInvite ?: return
        _ui.update { it.copy(incomingInvite = null) }
        val name = _ui.value.myName.trim()
        if (invite.roomId.isNotBlank()) client?.joinRoom(name, roomId = invite.roomId)
        else if (invite.code.isNotBlank()) client?.joinRoom(name, code = invite.code)
    }

    fun declineInvite() = _ui.update { it.copy(incomingInvite = null) }

    private fun handleEvent(event: String, data: Any?) {
        viewModelScope.launch {
            when (event) {
                "connected" -> {
                    val wasConnect = _ui.value.screen == Screen.CONNECT
                    _ui.update { s ->
                        s.copy(
                            screen = if (wasConnect) Screen.LOBBY else s.screen,
                            toast = if (wasConnect) "Подключено" else null,
                            error = null,
                        )
                    }
                    sendPushToken()
                    client?.setActive(_isActive)
                    client?.login(_ui.value.myId, _ui.value.myName.trim(), _ui.value.myAvatarId)
                    client?.requestFriends()
                    if (DeepLink.hasInvite) {
                        val roomId = DeepLink.roomId
                        val code = DeepLink.code
                        DeepLink.clear()
                        val name = _ui.value.myName.trim()
                        if (!roomId.isNullOrBlank()) client?.joinRoom(name, roomId = roomId)
                        else if (!code.isNullOrBlank()) client?.joinRoom(name, code = code)
                    }
                }

                "disconnected" -> {
                    _ui.update { s ->
                        s.copy(toast = "Соединение потеряно, переподключение...", winner = null)
                    }
                }

                "roomList" -> {
                    val arr = data as? JSONArray ?: return@launch
                    val rooms = (0 until arr.length()).map { RoomSummary.fromJson(arr.getJSONObject(it)) }
                    _ui.update { it.copy(rooms = rooms) }
                }

                "roomUpdate" -> {
                    val o = data as? JSONObject ?: return@launch
                    val room = RoomState.fromJson(o)
                    _ui.update { s ->
                        s.copy(
                            room = room,
                            screen = if (room.state == "lobby") Screen.ROOM else s.screen,
                            game = null,
                            winner = null,
                            error = null,
                        )
                    }
                }

                "gameStarted" -> {
                    _ui.update { it.copy(screen = Screen.GAME, winner = null, error = null, toast = "Игра началась!") }
                }

                "gameUpdate" -> {
                    val o = data as? JSONObject ?: return@launch
                    val raw = GameState.fromJson(o)
                    val deadlineAt = System.currentTimeMillis() + raw.endIn
                    _ui.update { s ->
                        s.copy(
                            game = raw.copy(endIn = deadlineAt),
                            screen = Screen.GAME,
                            error = null,
                            toast = null,
                        )
                    }
                }

                "gameOver" -> {
                    val o = data as? JSONObject
                    val w = o?.optJSONObject("winner")?.let {
                        WinnerInfo(it.optString("id", ""), it.optString("name", ""))
                    }
                    _ui.update {
                        it.copy(winner = w, screen = Screen.RESULT)
                    }
                }

                "wordAccepted" -> {
                    _ui.update { it.copy(error = null, toast = null) }
                }

                "wordError" -> {
                    val msg = (data as? JSONObject)?.optString("message", "Ошибка") ?: "Ошибка"
                    _ui.update { it.copy(error = msg) }
                }

                "playerEliminated" -> {
                    val name = (data as? JSONObject)?.optString("name", "")
                    _ui.update { it.copy(toast = "$name выбыл по таймеру") }
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
                        _ui.update { it.copy(error = o?.optString("error", "Не удалось войти")) }
                    }
                }

                "_ack_stats" -> {
                    val o = data as? JSONObject
                    if (o != null) {
                        _ui.update { it.copy(stats = PlayerStats.fromJson(o)) }
                    }
                }

                "friendsUpdate" -> {
                    val o = data as? JSONObject ?: return@launch
                    val fArr = o.optJSONArray("friends") ?: JSONArray()
                    val inArr = o.optJSONArray("incoming") ?: JSONArray()
                    val outArr = o.optJSONArray("outgoing") ?: JSONArray()
                    val friends = (0 until fArr.length()).map { Friend.fromJson(fArr.getJSONObject(it)) }
                    val incoming = (0 until inArr.length()).map { FriendRef.fromJson(inArr.getJSONObject(it)) }
                    val outgoing = (0 until outArr.length()).map { FriendRef.fromJson(outArr.getJSONObject(it)) }
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
                        s.copy(friends = s.friends.map {
                            if (it.id == friendId) it.copy(online = online, inGame = inGame) else it
                        })
                    }
                }

                "userSearchResult" -> {
                    val arr = data as? JSONArray ?: return@launch
                    val list = (0 until arr.length()).map { UserSummary.fromJson(arr.getJSONObject(it)) }
                    _ui.update { it.copy(searchResults = list) }
                }

                "roomInvite" -> {
                    val o = data as? JSONObject ?: return@launch
                    _ui.update { it.copy(incomingInvite = RoomInvite.fromJson(o)) }
                }

                "_ack_friend" -> {
                    val o = data as? JSONObject
                    if (o?.optBoolean("ok", false) != true) {
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
        client?.disconnect()
        client = null
        super.onCleared()
    }
}

data class UiState(
    val screen: Screen = Screen.CONNECT,
    val serverUrl: String = "https://bitva-slov.onrender.com",
    val myId: String = "",
    val myName: String = "",
    val myAvatarId: Int = 0,
    val rooms: List<RoomSummary> = emptyList(),
    val room: RoomState? = null,
    val game: GameState? = null,
    val winner: WinnerInfo? = null,
    val stats: PlayerStats? = null,
    val friends: List<Friend> = emptyList(),
    val incomingRequests: List<FriendRef> = emptyList(),
    val outgoingRequests: List<FriendRef> = emptyList(),
    val searchResults: List<UserSummary> = emptyList(),
    val incomingInvite: RoomInvite? = null,
    val error: String? = null,
    val toast: String? = null,
    val soundOn: Boolean = true,
    val volume: Float = 0.7f,
    val vibrationOn: Boolean = true,
    val vibrationIntensity: Float = 0.5f,
    val notificationsOn: Boolean = true,
)