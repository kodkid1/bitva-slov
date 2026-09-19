package com.bitvaslov.app

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object ProfileStore {
    private const val PREFS = "bitva_profile"
    private const val KEY_ID = "player_id"
    private const val KEY_NAME = "name"
    private const val KEY_AVATAR = "avatar_id"

    private lateinit var prefs: SharedPreferences

    var playerId: String = ""
        private set
    var name: String = ""
        private set
    var avatarId: Int = 0
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_ID, id).apply()
        }
        playerId = id
        name = prefs.getString(KEY_NAME, "") ?: ""
        avatarId = prefs.getInt(KEY_AVATAR, (playerId.hashCode() and 0x7fffffff) % AVATAR_COUNT)
    }

    fun saveName(value: String) {
        name = value.trim()
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun saveAvatar(id: Int) {
        avatarId = ((id % AVATAR_COUNT) + AVATAR_COUNT) % AVATAR_COUNT
        prefs.edit().putInt(KEY_AVATAR, avatarId).apply()
    }

    val hasProfile: Boolean get() = name.isNotBlank()

    const val AVATAR_COUNT = 12
}
