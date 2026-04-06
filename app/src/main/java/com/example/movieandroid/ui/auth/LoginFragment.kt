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
import com.example.movieandroid.databinding.FragmentLoginBinding
import com.example.movieandroid.util.RefreshBus
import com.example.movieandroid.util.rethrowIfCancellation
import com.example.movieandroid.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private var isRequestInProgress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (appContainer.sessionManager.isLoggedIn()) {
            openMainFlow()
            return
        }

        binding.loginButton.setOnClickListener { login() }
        binding.openRegisterButton.setOnClickListener {
            if (!isRequestInProgress) {
                findNavController().navigate(R.id.registerFragment)
            }
        }
    }

    private fun login() {
        if (isRequestInProgress) return

        val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString()?.trim().orEmpty()
        if (username.isBlank() || password.isBlank()) {
            Snackbar.make(binding.root, R.string.error_fill_credentials, Snackbar.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val response = appContainer.mainRepository.login(username, password)
                val auth = response.body()
                if (!response.isSuccessful) {
                    Snackbar.make(binding.root, response.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                if (auth == null) {
                    Snackbar.make(binding.root, R.string.error_empty_response, Snackbar.LENGTH_LONG).show()
                    return@launch
                }

                appContainer.sessionManager.token = auth.token
                appContainer.sessionManager.userId = auth.userId

                val info = appContainer.mainRepository.getUserInfo()
                if (info.isSuccessful) {
                    val user = info.body()
                    appContainer.sessionManager.username = user?.username
                    appContainer.sessionManager.role = user?.role
                } else {
                    Snackbar.make(binding.root, R.string.error_profile_not_loaded, Snackbar.LENGTH_SHORT).show()
                }

                RefreshBus.requestRefresh()
                Snackbar.make(binding.root, R.string.login_success, Snackbar.LENGTH_SHORT).show()
                openMainFlow()
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
            b.loginButton.isEnabled = !loading
            b.openRegisterButton.isEnabled = !loading
            b.usernameInput.isEnabled = !loading
            b.passwordInput.isEnabled = !loading
            b.loginButton.text = getString(if (loading) R.string.login_in_progress else R.string.login_title)
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


