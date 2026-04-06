package com.example.movieandroid.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navOptions
import androidx.navigation.fragment.findNavController
import com.example.movieandroid.appContainer
import com.example.movieandroid.R
import com.example.movieandroid.databinding.FragmentRegisterBinding
import com.example.movieandroid.util.rethrowIfCancellation
import com.example.movieandroid.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private var isRequestInProgress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (appContainer.sessionManager.isLoggedIn()) {
            openMainFlow()
            return
        }

        binding.registerButton.setOnClickListener { register() }
        binding.openLoginButton.setOnClickListener {
            if (!isRequestInProgress) {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

    private fun register() {
        if (isRequestInProgress) return

        val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString()?.trim().orEmpty()
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()

        if (username.isBlank() || password.isBlank()) {
            Snackbar.make(binding.root, R.string.error_fill_register_fields, Snackbar.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val response = appContainer.mainRepository.register(username, password, email.ifBlank { null })
                if (response.isSuccessful) {
                    Snackbar.make(binding.root, R.string.register_success, Snackbar.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.loginFragment)
                } else {
                    Snackbar.make(binding.root, response.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                Snackbar.make(binding.root, e.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        isRequestInProgress = loading
        _binding?.let { b ->
            b.registerButton.isEnabled = !loading
            b.openLoginButton.isEnabled = !loading
            b.usernameInput.isEnabled = !loading
            b.passwordInput.isEnabled = !loading
            b.emailInput.isEnabled = !loading
            b.registerButton.text = getString(if (loading) R.string.register_in_progress else R.string.register_title)
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


