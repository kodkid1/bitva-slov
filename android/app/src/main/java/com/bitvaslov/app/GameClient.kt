package com.bitvaslov.app

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class GameClient(private val onEvent: (String, Any?) -> Unit) {

    private var socket: Socket? = null
    private var nextRoomId: String? = null

    fun connect(serverUrl: String) {
        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = 10
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

    fun createRoom(playerName: String, name: String, isPrivate: Boolean, timer: Int, maxPlayers: Int) {
        val o = JSONObject()
        o.put("playerName", playerName)
        o.put("name", name)
        o.put("isPrivate", isPrivate)
        o.put("timer", timer)
        o.put("maxPlayers", maxPlayers)
        socket?.emit("createRoom", o, callback("_ack_create"))
    }

    fun joinRoom(playerName: String, roomId: String? = null, code: String? = null) {
        val o = JSONObject()
        o.put("playerName", playerName)
        if (roomId != null) o.put("roomId", roomId)
        if (code != null) o.put("code", code)
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

    fun requestStats(name: String) {
        socket?.emit("statsRequest", JSONObject().put("name", name), callback("_ack_stats"))
    }

    fun registerPush(name: String, token: String) {
        val o = JSONObject()
        o.put("name", name)
        o.put("token", token)
        socket?.emit("registerPush", o)
    }

    fun unregisterPush(name: String) {
        socket?.emit("unregisterPush", JSONObject().put("name", name))
    }

    fun setActive(active: Boolean) {
        socket?.emit("setActive", JSONObject().put("active", active))
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

        s.on("errorMessage") { args ->
            onEvent("errorMessage", args.firstOrNull() as? JSONObject)
        }
    }
}