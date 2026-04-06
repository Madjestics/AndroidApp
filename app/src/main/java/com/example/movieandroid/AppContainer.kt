package com.example.movieandroid

import android.content.Context
import com.example.movieandroid.data.MainRepository
import com.example.movieandroid.data.PrefsRepository
import com.example.movieandroid.data.SessionManager
import com.example.movieandroid.network.NetworkFactory

class AppContainer(context: Context) {
    val sessionManager: SessionManager = SessionManager(context.applicationContext)
    val mainRepository: MainRepository
    val prefsRepository: PrefsRepository

    init {
        val mainService = NetworkFactory.createMainService(BuildConfig.MAIN_API_BASE_URL, sessionManager)
        val prefsService = NetworkFactory.createPrefsService(BuildConfig.PREFS_API_BASE_URL, sessionManager)
        mainRepository = MainRepository(mainService)
        prefsRepository = PrefsRepository(prefsService)
    }
}

