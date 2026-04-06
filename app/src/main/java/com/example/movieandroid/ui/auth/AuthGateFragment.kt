package com.example.movieandroid.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.navOptions
import androidx.navigation.fragment.findNavController
import com.example.movieandroid.appContainer
import com.example.movieandroid.R
import com.example.movieandroid.databinding.FragmentAuthGateBinding

class AuthGateFragment : Fragment() {
    private var _binding: FragmentAuthGateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthGateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (appContainer.sessionManager.isLoggedIn()) {
            openMainFlow()
            return
        }

        binding.openLoginButton.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        binding.openRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun openMainFlow() {
        findNavController().navigate(
            R.id.moviesFragment,
            null,
            navOptions {
                popUpTo(R.id.authGateFragment) { inclusive = true }
                launchSingleTop = true
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


