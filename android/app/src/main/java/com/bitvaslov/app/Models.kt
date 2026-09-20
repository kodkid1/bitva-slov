package com.bitvaslov.app

import org.json.JSONArray
import org.json.JSONObject

enum class Screen {
    CONNECT,
    LOBBY,
    CREATEROOM,
    CODEENTRY,
    SETTINGS,
    ROOM,
    GAME,
    RESULT,
}

data class Friend(
    val id: String,
    val name: String,
    val avatarId: Int = 0,
    val photo: String = "",
    val online: Boolean = false,
    val inGame: Boolean = false,
) {
    companion object {
        fun fromJson(o: JSONObject) = Friend(
            id = o.optString("id", ""),
            name = o.optString("name", ""),
            avatarId = o.optInt("avatarId", 0),
            photo = o.optString("photo", ""),
            online = o.optBoolean("online", false),
            inGame = o.optBoolean("inGame", false),
        )
    }
}

data class FriendRef(
    val id: String,
    val name: String,
    val avatarId: Int = 0,
    val photo: String = "",
) {
    companion object {
        fun fromJson(o: JSONObject) = FriendRef(
            id = o.optString("id", ""),
            name = o.optString("name", ""),
            avatarId = o.optInt("avatarId", 0),
            photo = o.optString("photo", ""),
        )
    }
}

data class UserSummary(
    val id: String,
    val name: String,
    val avatarId: Int = 0,
    val photo: String = "",
    val online: Boolean = false,
    val inGame: Boolean = false,
    val games: Int = 0,
    val wins: Int = 0,
) {
    companion object {
        fun fromJson(o: JSONObject) = UserSummary(
            id = o.optString("id", ""),
            name = o.optString("name", ""),
            avatarId = o.optInt("avatarId", 0),
            photo = o.optString("photo", ""),
            online = o.optBoolean("online", false),
            inGame = o.optBoolean("inGame", false),
            games = o.optInt("games", 0),
            wins = o.optInt("wins", 0),
        )
    }
}

data class RoomInvite(
    val roomId: String,
    val code: String,
    val roomName: String,
    val fromName: String,
) {
    companion object {
        fun fromJson(o: JSONObject) = RoomInvite(
            roomId = o.optString("roomId", ""),
            code = o.optString("code", ""),
            roomName = o.optString("roomName", ""),
            fromName = o.optString("fromName", ""),
        )
    }
}

object DeepLink {
    var roomId: String? = null
    var code: String? = null

    fun clear() {
        roomId = null
        code = null
    }

    val hasInvite: Boolean get() = !roomId.isNullOrBlank() || !code.isNullOrBlank()
}

data class Player(
    val id: String,
    val name: String,
    val alive: Boolean = true,
    val avatarId: Int = 0,
    val photo: String = "",
    val score: Int = 0,
    val team: Int = 0,
) {
    companion object {
        fun fromJson(o: JSONObject) = Player(
            id = o.optString("id", ""),
            name = o.optString("name", ""),
            alive = o.optBoolean("alive", true),
            avatarId = o.optInt("avatarId", 0),
            photo = o.optString("photo", ""),
            score = o.optInt("score", 0),
            team = o.optInt("team", 0),
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
    val mode: String = "classic",
    val minWordLen: Int = 0,
    val randomTimer: Boolean = false,
    val acceleration: Boolean = false,
    val theme: String = "",
) {
    companion object {
        fun fromJson(o: JSONObject): RoomState {
            val arr = o.optJSONArray("players") ?: JSONArray()
            val players = (0 until arr.length()).map { Player.fromJson(arr.getJSONObject(it)) }
            val s = o.optJSONObject("settings")
            
            // Пытаемся взять настройки из вложенного объекта settings или из корня
            val mode = s?.optString("mode") ?: o.optString("mode", "classic")
            val minWordLen = s?.optInt("minWordLen") ?: o.optInt("minWordLen", 0)
            val randomTimer = s?.optBoolean("randomTimer") ?: o.optBoolean("randomTimer", false)
            val acceleration = s?.optBoolean("acceleration") ?: o.optBoolean("acceleration", false)
            val theme = s?.optString("theme") ?: o.optString("theme", "")

            return RoomState(
                id = o.optString("id", ""),
                name = o.optString("name", ""),
                isPrivate = o.optBoolean("isPrivate", false),
                code = if (o.isNull("code")) null else o.optString("code", ""),
                hostId = o.optString("hostId", ""),
                timer = o.optInt("timer", 15),
                maxPlayers = o.optInt("maxPlayers", 6),
                state = o.optString("state", "lobby"),
                players = players,
                mode = mode,
                minWordLen = minWordLen,
                randomTimer = randomTimer,
                acceleration = acceleration,
                theme = theme,
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
    val mode: String = "classic",
) {
    companion object {
        fun fromJson(o: JSONObject) = RoomSummary(
            id = o.optString("id", ""),
            name = o.optString("name", ""),
            players = o.optInt("players", 0),
            maxPlayers = o.optInt("maxPlayers", 6),
            timer = o.optInt("timer", 15),
            mode = o.optString("mode", "classic"),
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
    val mode: String = "classic",
    val minWordLen: Int = 0,
    val theme: String = "",
    val totalWords: Int = 0,
    val marathonTarget: Int = 0,
    val scores: Map<String, Int> = emptyMap(),
) {
    companion object {
        fun fromJson(o: JSONObject): GameState {
            val arr = o.optJSONArray("usedWords") ?: JSONArray()
            val words = (0 until arr.length()).map { arr.optString(it) }
            val scoresObj = o.optJSONObject("scores") ?: JSONObject()
            val scores = HashMap<String, Int>()
            val keys = scoresObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                scores[k] = scoresObj.optInt(k, 0)
            }
            
            val s = o.optJSONObject("settings")
            val mode = s?.optString("mode") ?: o.optString("mode", "classic")
            val minWordLen = s?.optInt("minWordLen") ?: o.optInt("minWordLen", 0)
            val theme = s?.optString("theme") ?: o.optString("theme", "")

            return GameState(
                requiredLetter = o.optString("requiredLetter", ""),
                lastWord = o.optString("lastWord", ""),
                turnPlayerId = if (o.isNull("turnPlayerId")) null else o.optString("turnPlayerId", ""),
                endIn = o.optLong("endIn", 15000),
                timer = o.optInt("timer", 15),
                usedWords = words,
                finished = o.optString("state", "playing") == "finished",
                mode = mode,
                minWordLen = minWordLen,
                theme = theme,
                totalWords = o.optInt("totalWords", 0),
                marathonTarget = o.optInt("marathonTarget", 0),
                scores = scores,
            )
        }
    }

    val myTurn: Boolean get() = turnPlayerId == MyIds.current
}

data class WinnerInfo(
    val id: String,
    val name: String,
    val avatarId: Int = 0,
    val photo: String = "",
)

data class PlayerStats(
    val games: Int,
    val wins: Int,
    val losses: Int,
    val bestStreak: Int,
    val currentStreak: Int,
) {
    companion object {
        fun fromJson(o: JSONObject) = PlayerStats(
            games = o.optInt("games", 0),
            wins = o.optInt("wins", 0),
            losses = o.optInt("losses", 0),
            bestStreak = o.optInt("bestStreak", 0),
            currentStreak = o.optInt("currentStreak", 0),
        )
    }

    val winRate: Int
        get() = if (games > 0) (wins * 100 / games) else 0
}

object MyIds {
    var current: String? = null
}
