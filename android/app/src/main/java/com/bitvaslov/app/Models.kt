package com.bitvaslov.app

import org.json.JSONArray
import org.json.JSONObject

enum class Screen {
    CONNECT,
    LOBBY,
    ROOM,
    GAME,
}

data class Player(
    val id: String,
    val name: String,
    val alive: Boolean = true,
) {
    companion object {
        fun fromJson(o: JSONObject) = Player(
            id = o.optString("id", ""),
            name = o.optString("name", ""),
            alive = o.optBoolean("alive", true),
        )
    }
}

data class RoomState(
    val id: String,
    val name: String,
    val isPrivate: Boolean,
    val code: String?,
    val hostId: String,
    val timer: Int,
    val maxPlayers: Int,
    val state: String,
    val players: List<Player>,
) {
    companion object {
        fun fromJson(o: JSONObject): RoomState {
            val arr = o.optJSONArray("players") ?: JSONArray()
            val players = (0 until arr.length()).map { Player.fromJson(arr.getJSONObject(it)) }
            return RoomState(
                id = o.optString("id", ""),
                name = o.optString("name", ""),
                isPrivate = o.optBoolean("isPrivate", false),
                code = if (o.isNull("code")) null else o.optString("code", null),
                hostId = o.optString("hostId", ""),
                timer = o.optInt("timer", 15),
                maxPlayers = o.optInt("maxPlayers", 6),
                state = o.optString("state", "lobby"),
                players = players,
            )
        }
    }

    val isHost: Boolean get() = hostId == MyIds.current
}

data class RoomSummary(
    val id: String,
    val name: String,
    val players: Int,
    val maxPlayers: Int,
    val timer: Int,
) {
    companion object {
        fun fromJson(o: JSONObject) = RoomSummary(
            id = o.optString("id", ""),
            name = o.optString("name", ""),
            players = o.optInt("players", 0),
            maxPlayers = o.optInt("maxPlayers", 6),
            timer = o.optInt("timer", 15),
        )
    }
}

data class GameState(
    val requiredLetter: String,
    val lastWord: String,
    val turnPlayerId: String?,
    val endIn: Long,
    val timer: Int,
    val usedWords: List<String>,
    val finished: Boolean,
) {
    companion object {
        fun fromJson(o: JSONObject): GameState {
            val arr = o.optJSONArray("usedWords") ?: JSONArray()
            val words = (0 until arr.length()).map { arr.optString(it) }
            return GameState(
                requiredLetter = o.optString("requiredLetter", ""),
                lastWord = o.optString("lastWord", ""),
                turnPlayerId = if (o.isNull("turnPlayerId")) null else o.optString("turnPlayerId", null),
                endIn = o.optLong("endIn", 15000),
                timer = o.optInt("timer", 15),
                usedWords = words,
                finished = o.optString("state", "playing") == "finished",
            )
        }
    }

    val myTurn: Boolean get() = turnPlayerId == MyIds.current
}

data class WinnerInfo(
    val id: String,
    val name: String,
)

object MyIds {
    var current: String? = null
}