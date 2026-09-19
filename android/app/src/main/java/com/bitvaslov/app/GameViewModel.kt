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
    fun setMyName(name: String) = _ui.update { it.copy(myName = name) }

    fun requestCreateRoom() = _ui.update { it.copy(screen = Screen.CREATEROOM) }
    fun dismissCreateRoom() = _ui.update { it.copy(screen = Screen.LOBBY) }

    fun requestCodeEntry() = _ui.update { it.copy(screen = Screen.CODEENTRY) }
    fun dismissCodeEntry() = _ui.update { it.copy(screen = Screen.LOBBY) }

    fun copyRoomCode(code: String) = _ui.update { it.copy(toast = "Код $code скопирован") }

    fun createRoom(roomName: String, isPrivate: Boolean, timer: Int, maxPlayers: Int) {
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
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    fun clearToast() = _ui.update { it.copy(toast = null) }

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
    val myName: String = "",
    val rooms: List<RoomSummary> = emptyList(),
    val room: RoomState? = null,
    val game: GameState? = null,
    val winner: WinnerInfo? = null,
    val stats: PlayerStats? = null,
    val error: String? = null,
    val toast: String? = null,
    val soundOn: Boolean = true,
    val volume: Float = 0.7f,
    val vibrationOn: Boolean = true,
    val vibrationIntensity: Float = 0.5f,
    val notificationsOn: Boolean = true,
)