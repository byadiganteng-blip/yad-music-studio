package com.yad.musicstudio

import android.app.Application
import android.content.Context
import android.util.Log

/**
 * YadMusicApp — Application class untuk init Logger + CrashHandler.
 */
class YadMusicApp : Application() {

    companion object {
        private const val TAG = "YadMusicApp"

        @Volatile
        private var INSTANCE: YadMusicApp? = null

        fun get(): YadMusicApp? = INSTANCE
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // Init Logger
        Logger.init(this)
        Logger.section("APPLICATION START")
        Logger.i(TAG, "YadMusicApp created")
        Logger.i(TAG, "Package: $packageName")

        // Init CrashHandler
        CrashHandler.init(this)

        Log.i(TAG, "Initialization complete")
    }
}
