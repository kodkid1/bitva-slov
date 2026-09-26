package com.bitvaslov.app

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object ProfileStore {
    private const val PREFS = "bitva_profile"
    private const val KEY_ID = "player_id"
    private const val KEY_NAME = "name"
    private const val KEY_AVATAR = "avatar_id"
    private const val KEY_PHOTO = "photo"
    private const val KEY_COINS = "coins"
    private const val KEY_TITLE = "title_id"
    private const val KEY_TITLES = "owned_titles"

    private lateinit var prefs: SharedPreferences

    var playerId: String = ""
        private set
    var name: String = ""
        private set
    var avatarId: Int = 0
        private set
    var photo: String = ""
        private set

    /** Кэш кошелька: сервер — источник истины, но чтобы профиль не пустовал оффлайн. */
    var coins: Int = 0
        private set
    var titleId: Int = 0
        private set
    var ownedTitles: Set<Int> = setOf(0)
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
        photo = prefs.getString(KEY_PHOTO, "") ?: ""
        coins = prefs.getInt(KEY_COINS, 0)
        titleId = prefs.getInt(KEY_TITLE, 0)
        ownedTitles = prefs.getString(KEY_TITLES, "")?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            ?.plus(0)
            ?: setOf(0)
    }

    fun saveName(value: String) {
        name = value.trim()
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun saveAvatar(id: Int) {
        avatarId = ((id % AVATAR_COUNT) + AVATAR_COUNT) % AVATAR_COUNT
        prefs.edit().putInt(KEY_AVATAR, avatarId).apply()
    }

    fun savePhoto(base64: String) {
        photo = base64
        prefs.edit().putString(KEY_PHOTO, base64).apply()
    }

    fun saveWallet(coins: Int, titleId: Int, ownedTitles: Set<Int>) {
        this.coins = coins
        this.titleId = titleId
        this.ownedTitles = ownedTitles
        prefs.edit()
            .putInt(KEY_COINS, coins)
            .putInt(KEY_TITLE, titleId)
            .putString(KEY_TITLES, ownedTitles.joinToString(","))
            .apply()
    }

    val hasProfile: Boolean get() = name.isNotBlank()

    const val AVATAR_COUNT = 12
}
