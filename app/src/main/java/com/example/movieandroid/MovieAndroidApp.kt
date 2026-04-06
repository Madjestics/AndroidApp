package com.example.movieandroid

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MovieAndroidApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MovieAndroidApp).container

val Fragment.appContainer: AppContainer
    get() = requireContext().appContainer

val AppCompatActivity.appContainer: AppContainer
    get() = applicationContext.appContainer

