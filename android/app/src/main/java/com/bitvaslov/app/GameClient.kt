package com.bitvaslov.app

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class GameClient(private val onEvent: (String, Any?) -> Unit) {

    private var socket: Socket? = null

    val isConnected: Boolean get() = socket?.connected() == true

    fun connect(serverUrl: String) {
        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = 10
            reconnectionDelay = 1000
            reconnectionDelayMax = 6000
        }
        socket = IO.socket(URI.create(serverUrl), opts)
        register()
        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        MyIds.current = null
    }

    fun createRoom(
        playerName: String,
        name: String,
        isPrivate: Boolean,
        timer: Int,
        maxPlayers: Int,
        avatarId: Int,
        photo: String,
        mode: String = "classic",
        minWordLen: Int = 0,
        randomTimer: Boolean = false,
        acceleration: Boolean = false,
        theme: String = "",
        password: String? = null
    ) {
        val o = JSONObject()
        o.put("playerName", playerName)
        o.put("name", name)
        o.put("isPrivate", isPrivate)
        o.put("timer", timer)
        o.put("maxPlayers", maxPlayers)
        o.put("avatarId", avatarId)
        o.put("mode", mode)
        o.put("minWordLen", minWordLen)
        o.put("randomTimer", randomTimer)
        o.put("acceleration", acceleration)
        o.put("theme", theme)
        if (isPrivate && !password.isNullOrBlank()) o.put("password", password.trim())
        if (photo.isNotBlank()) o.put("photo", photo)
        socket?.emit("createRoom", o, callback("_ack_create"))
    }

    fun setRoomSettings(
        minWordLen: Int? = null,
        randomTimer: Boolean? = null,
        acceleration: Boolean? = null,
        theme: String? = null,
        mode: String? = null
    ) {
        val o = JSONObject()
        minWordLen?.let { o.put("minWordLen", it) }
        randomTimer?.let { o.put("randomTimer", it) }
        acceleration?.let { o.put("acceleration", it) }
        theme?.let { o.put("theme", it) }
        mode?.let { o.put("mode", it) }
        socket?.emit("setRoomSettings", o)
    }

    fun joinRoom(playerName: String, roomId: String? = null, password: String? = null, avatarId: Int = 0, photo: String = "") {
        val o = JSONObject()
        o.put("playerName", playerName)
        o.put("avatarId", avatarId)
        if (photo.isNotBlank()) o.put("photo", photo)
        if (roomId != null) o.put("roomId", roomId)
        if (password != null) o.put("password", password)
        socket?.emit("joinRoom", o, callback("_ack_join"))
    }

    fun startGame() {
        socket?.emit("startGame")
    }

    fun leaveRoom() {
        socket?.emit("leaveRoom")
    }

    fun submitWord(word: String) {
        socket?.emit("submitWord", JSONObject().put("word", word))
    }

    fun refreshRooms() {
        socket?.emit("roomListRequest")
    }

    fun requestRoom() {
        socket?.emit("getRoom")
    }

    fun requestStats(name: String) {
        socket?.emit("statsRequest", JSONObject().put("name", name), callback("_ack_stats"))
    }

    fun registerPush(id: String, token: String) {
        val o = JSONObject()
        o.put("id", id)
        o.put("token", token)
        socket?.emit("registerPush", o)
    }

    fun unregisterPush(id: String) {
        socket?.emit("unregisterPush", JSONObject().put("id", id))
    }

    fun setActive(active: Boolean) {
        socket?.emit("setActive", JSONObject().put("active", active))
    }

    fun login(id: String, name: String, avatarId: Int, photo: String) {
        val o = JSONObject()
        o.put("id", id)
        o.put("name", name)
        o.put("avatarId", avatarId)
        if (photo.isNotBlank()) o.put("photo", photo)
        socket?.emit("login", o)
    }

    fun requestFriends() {
        socket?.emit("friendsRequest")
    }

    fun searchUser(name: String) {
        socket?.emit("userSearch", JSONObject().put("name", name), callback("_ack_search"))
    }

    fun requestProfile(id: String) {
        socket?.emit("profileRequest", JSONObject().put("id", id), callback("_ack_profile"))
    }

    fun sendFriendRequest(id: String) {
        socket?.emit("friendRequest", JSONObject().put("id", id), callback("_ack_friend"))
    }

    fun respondFriendRequest(id: String, accept: Boolean) {
        val o = JSONObject()
        o.put("id", id)
        o.put("accept", accept)
        socket?.emit("friendRespond", o, callback("_ack_friend"))
    }

    fun cancelFriendRequest(id: String) {
        socket?.emit("cancelFriendRequest", JSONObject().put("id", id), callback("_ack_friend"))
    }

    fun removeFriend(id: String) {
        socket?.emit("removeFriend", JSONObject().put("id", id), callback("_ack_friend"))
    }

    fun inviteFriend(id: String) {
        socket?.emit("inviteFriend", JSONObject().put("id", id), callback("_ack_invite"))
    }

    private fun callback(tag: String): io.socket.client.Ack {
        return io.socket.client.Ack { args ->
            onEvent(tag, args.firstOrNull())
        }
    }

    private fun register() {
        val s = socket ?: return

        s.on(Socket.EVENT_DISCONNECT) {
            onEvent("disconnected", null)
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val err = args.firstOrNull()
            // временная неудача: показывать ошибку рано, reconnection ещё может succeed
            onEvent("connectError", (err as? Exception)?.message ?: "Нет связи с сервером")
        }

        s.on("reconnect_failed") {
            onEvent("connectFailed", "Сервер недоступен, не удалось подключиться")
        }

        s.on("connected") { args ->
            val o = args.firstOrNull() as? JSONObject
            MyIds.current = o?.optString("id")
            onEvent("connected", o)
        }

        s.on("roomList") { args ->
            onEvent("roomList", args.firstOrNull() as? JSONArray)
        }

        s.on("roomUpdate") { args ->
            onEvent("roomUpdate", args.firstOrNull() as? JSONObject)
        }

        s.on("gameStarted") {
            onEvent("gameStarted", null)
        }

        s.on("gameUpdate") { args ->
            onEvent("gameUpdate", args.firstOrNull() as? JSONObject)
        }

        s.on("gameOver") { args ->
            onEvent("gameOver", args.firstOrNull() as? JSONObject)
        }

        s.on("wordAccepted") { args ->
            onEvent("wordAccepted", args.firstOrNull())
        }

        s.on("wordError") { args ->
            onEvent("wordError", args.firstOrNull() as? JSONObject)
        }

        s.on("playerEliminated") { args ->
            onEvent("playerEliminated", args.firstOrNull() as? JSONObject)
        }

        s.on("skipTurn") { args ->
            onEvent("skipTurn", args.firstOrNull() as? JSONObject)
        }

        s.on("errorMessage") { args ->
            onEvent("errorMessage", args.firstOrNull() as? JSONObject)
        }

        s.on("friendsUpdate") { args ->
            onEvent("friendsUpdate", args.firstOrNull() as? JSONObject)
        }

        s.on("friendPresence") { args ->
            onEvent("friendPresence", args.firstOrNull() as? JSONObject)
        }

        s.on("roomInvite") { args ->
            onEvent("roomInvite", args.firstOrNull() as? JSONObject)
        }

        s.on("userSearchResult") { args ->
            onEvent("userSearchResult", args.firstOrNull() as? JSONArray)
        }

        s.on("pid") { args ->
            onEvent("pid", args.firstOrNull() as? JSONObject)
        }

        s.on("roomReplaced") {
            onEvent("roomReplaced", null)
        }

        s.on("roomGone") {
            onEvent("roomGone", null)
        }
    }
}
