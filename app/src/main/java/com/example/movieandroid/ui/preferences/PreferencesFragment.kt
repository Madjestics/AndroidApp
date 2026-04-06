package com.example.movieandroid.ui.preferences

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
import com.example.movieandroid.databinding.FragmentPreferencesBinding
import com.example.movieandroid.util.RefreshBus
import com.example.movieandroid.util.rethrowIfCancellation
import com.example.movieandroid.util.toUserMessage
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class PreferencesFragment : Fragment() {
    private var _binding: FragmentPreferencesBinding? = null
    private val binding get() = _binding!!
    private var isSaving = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.savePrefsButton.setOnClickListener { savePreferences() }
        binding.logoutButton.setOnClickListener { logout() }
        setupGenreSync()

        viewLifecycleOwner.lifecycleScope.launch {
            val ok = syncSessionUserInfo(showErrors = true)
            renderAccountInfo()
            if (!ok && appContainer.sessionManager.userId == null) {
                Snackbar.make(binding.root, R.string.login_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_open_login) {
                        findNavController().navigate(R.id.authGateFragment)
                    }
                    .show()
            }
        }
    }

    private fun renderAccountInfo() {
        val username = appContainer.sessionManager.username
        val userId = appContainer.sessionManager.userId
        if (userId == null) {
            binding.accountInfoText.text = getString(R.string.account_info_unknown)
            return
        }
        binding.accountInfoText.text = getString(
            R.string.account_info,
            username?.takeIf { it.isNotBlank() } ?: "user",
            userId.toString()
        )
    }

    private fun savePreferences() {
        if (isSaving) return

        viewLifecycleOwner.lifecycleScope.launch {
            setSaving(true)
            try {
                val userId = resolveUserId()
                if (userId == null) {
                    Snackbar.make(binding.root, R.string.login_required, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_open_login) {
                            findNavController().navigate(R.id.authGateFragment)
                        }
                        .show()
                    return@launch
                }

                val selectedFromChips = binding.genresChipGroup.children()
                    .filter { it.isChecked }
                    .map { it.text.toString() }

                val extra = parseGenresInput(binding.extraGenresInput.text?.toString().orEmpty())

                val genres = (selectedFromChips + extra)
                    .distinctBy { it.lowercase() }

                if (genres.isEmpty()) {
                    Snackbar.make(binding.root, R.string.genres_required, Snackbar.LENGTH_LONG).show()
                    return@launch
                }

                val response = appContainer.prefsRepository.savePreferences(userId, genres)
                if (response.isSuccessful) {
                    Snackbar.make(binding.root, R.string.preferences_saved, Snackbar.LENGTH_LONG).show()
                } else {
                    if (response.code() == 401) {
                        handleUnauthorized()
                    } else {
                        Snackbar.make(binding.root, response.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                Snackbar.make(binding.root, e.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
            } finally {
                setSaving(false)
            }
        }
    }

    private suspend fun syncSessionUserInfo(showErrors: Boolean): Boolean {
        if (!appContainer.sessionManager.isLoggedIn()) return false
        if (appContainer.sessionManager.userId != null && !appContainer.sessionManager.username.isNullOrBlank()) {
            return true
        }

        return try {
            val infoResponse = appContainer.mainRepository.getUserInfo()
            if (!infoResponse.isSuccessful) {
                if (showErrors) {
                    if (infoResponse.code() == 401) {
                        handleUnauthorized()
                    } else {
                        Snackbar.make(binding.root, infoResponse.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                }
                false
            } else {
                val body = infoResponse.body()
                if (body == null) {
                    if (showErrors) {
                        Snackbar.make(binding.root, R.string.error_empty_response, Snackbar.LENGTH_LONG).show()
                    }
                    false
                } else {
                    appContainer.sessionManager.userId = body.id
                    appContainer.sessionManager.username = body.username
                    appContainer.sessionManager.role = body.role
                    true
                }
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            if (showErrors) {
                Snackbar.make(binding.root, e.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
            }
            false
        }
    }

    private suspend fun resolveUserId(): Long? {
        val cached = appContainer.sessionManager.userId
        if (cached != null) return cached
        val synced = syncSessionUserInfo(showErrors = true)
        return if (synced) appContainer.sessionManager.userId else null
    }

    private fun setupGenreSync() {
        binding.genresChipGroup.setOnCheckedStateChangeListener { _, _ ->
            syncGenresInputFromChips()
        }
        syncGenresInputFromChips()
    }

    private fun syncGenresInputFromChips() {
        val selectedFromChips = binding.genresChipGroup.children()
            .filter { it.isChecked }
            .map { it.text.toString().trim() }
            .filter { it.isNotBlank() }
        val standardGenres = binding.genresChipGroup.children()
            .map { it.text.toString().trim() }
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
            .toSet()
        val selectedStandardGenres = selectedFromChips
            .map { it.lowercase() }
            .toSet()

        val typedGenres = parseGenresInput(binding.extraGenresInput.text?.toString().orEmpty())
        val preservedTypedGenres = typedGenres.filter { genre ->
            val normalized = genre.lowercase()
            normalized !in standardGenres || normalized in selectedStandardGenres
        }

        val mergedGenres = (selectedFromChips + preservedTypedGenres)
            .distinctBy { it.lowercase() }
        val updatedText = mergedGenres.joinToString(", ")
        val currentText = binding.extraGenresInput.text?.toString().orEmpty()
        if (currentText != updatedText) {
            binding.extraGenresInput.setText(updatedText)
            binding.extraGenresInput.setSelection(updatedText.length)
        }
    }

    private fun parseGenresInput(value: String): List<String> = value
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    private fun setSaving(saving: Boolean) {
        isSaving = saving
        _binding?.let { b ->
            b.savePrefsButton.isEnabled = !saving
            b.logoutButton.isEnabled = !saving
            b.extraGenresInput.isEnabled = !saving
            b.savePrefsButton.text = getString(if (saving) R.string.save_in_progress else R.string.save)
        }
    }

    private fun handleUnauthorized() {
        appContainer.sessionManager.clear()
        Snackbar.make(binding.root, R.string.error_unauthorized, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_open_login) {
                findNavController().navigate(R.id.authGateFragment)
            }
            .show()
    }

    private fun logout() {
        appContainer.sessionManager.clear()
        RefreshBus.requestRefresh()
        Snackbar.make(binding.root, R.string.toolbar_logout, Snackbar.LENGTH_SHORT).show()
        findNavController().navigate(
            R.id.authGateFragment,
            null,
            navOptions {
                popUpTo(R.id.authGateFragment) { inclusive = true }
                launchSingleTop = true
            }
        )
    }

    private fun ViewGroup.children(): List<Chip> = buildList {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is Chip) add(child)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


