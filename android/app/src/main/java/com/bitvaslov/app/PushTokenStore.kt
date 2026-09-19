package com.bitvaslov.app

object PushTokenStore {
    @Volatile
    var current: String? = null
}
