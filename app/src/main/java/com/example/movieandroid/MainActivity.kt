package com.example.movieandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.example.movieandroid.databinding.ActivityMainBinding
import com.example.movieandroid.util.RefreshBus

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val mainDestinations = setOf(
        R.id.moviesFragment,
        R.id.directorsFragment,
        R.id.recommendationsFragment,
        R.id.preferencesFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNavigation.setupWithNavController(navController)
        binding.switchAccountButton.setOnClickListener {
            if (!appContainer.sessionManager.isLoggedIn()) return@setOnClickListener

            appContainer.sessionManager.clear()
            RefreshBus.requestRefresh()
            navController.navigate(
                R.id.authGateFragment,
                null,
                navOptions {
                    popUpTo(R.id.authGateFragment) { inclusive = true }
                    launchSingleTop = true
                }
            )
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isMainDestination = destination.id in mainDestinations
            val loggedIn = appContainer.sessionManager.isLoggedIn()
            val isAuthScreen = destination.id == R.id.authGateFragment ||
                destination.id == R.id.loginFragment ||
                destination.id == R.id.registerFragment

            binding.bottomNavigation.isVisible = isMainDestination && loggedIn
            binding.switchAccountButton.isVisible = loggedIn && !isAuthScreen
            binding.topToolbar.title = destination.label ?: getString(R.string.app_name)

            if (!loggedIn && isMainDestination) {
                navController.navigate(
                    R.id.authGateFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.authGateFragment) { inclusive = false }
                        launchSingleTop = true
                    }
                )
            }
        }
    }
}

