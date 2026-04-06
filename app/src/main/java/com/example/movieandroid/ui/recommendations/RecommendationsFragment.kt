package com.example.movieandroid.ui.recommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movieandroid.appContainer
import com.example.movieandroid.R
import com.example.movieandroid.data.models.MovieDto
import com.example.movieandroid.databinding.FragmentRecommendationsBinding
import com.example.movieandroid.ui.movies.MovieAdapter
import com.example.movieandroid.ui.player.VideoPlayerActivity
import com.example.movieandroid.util.RefreshBus
import com.example.movieandroid.util.rethrowIfCancellation
import com.example.movieandroid.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class RecommendationsFragment : Fragment() {
    private var _binding: FragmentRecommendationsBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        MovieAdapter(
            onDetails = { movie ->
                findNavController().navigate(R.id.movieDetailsFragment, bundleOf("movieId" to movie.id))
            },
            onWatch = { movie ->
                VideoPlayerActivity.start(requireContext(), movie.id, movie.title.orEmpty())
            }
        )
    }

    private var recommendations: List<MovieDto> = emptyList()
    private var watched: List<MovieDto> = emptyList()
    private var showRecommendations = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recommendationsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recommendationsRecycler.adapter = adapter

        binding.recommendationsToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val shouldShowRecommendations = checkedId == R.id.recommendButton
            if (showRecommendations == shouldShowRecommendations) return@addOnButtonCheckedListener

            showRecommendations = shouldShowRecommendations
            renderCurrentList()
        }
        binding.recommendationsToggleGroup.check(R.id.recommendButton)

        loadRecommendationsData()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                RefreshBus.events.collect { loadRecommendationsData() }
            }
        }
    }

    private fun loadRecommendationsData() {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = resolveUserId()
                if (userId == null) {
                    binding.progressBar.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                    Snackbar.make(binding.root, R.string.login_required, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_open_login) {
                            findNavController().navigate(R.id.authGateFragment)
                        }
                        .show()
                    return@launch
                }

                val (recResp, watchedResp) = coroutineScope {
                    val recDeferred = async { appContainer.prefsRepository.getRecommendations(userId) }
                    val watchedDeferred = async { appContainer.prefsRepository.getWatched(userId) }
                    recDeferred.await() to watchedDeferred.await()
                }
                binding.progressBar.visibility = View.GONE

                if (!recResp.isSuccessful) {
                    if (recResp.code() == 401) {
                        handleUnauthorized()
                    } else {
                        Snackbar.make(binding.root, recResp.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }

                if (!watchedResp.isSuccessful) {
                    if (watchedResp.code() == 401) {
                        handleUnauthorized()
                    } else {
                        Snackbar.make(binding.root, watchedResp.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val recommendationsBody = recResp.body()
                val watchedBody = watchedResp.body()

                if (recommendationsBody == null || watchedBody == null) {
                    recommendations = recommendationsBody.orEmpty()
                    watched = watchedBody.orEmpty()
                    renderCurrentList()
                    Snackbar.make(binding.root, R.string.error_empty_response, Snackbar.LENGTH_LONG).show()
                    return@launch
                }

                recommendations = recommendationsBody
                watched = watchedBody
                renderCurrentList()
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, e.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun resolveUserId(): Long? {
        val cached = appContainer.sessionManager.userId
        if (cached != null) return cached
        if (!appContainer.sessionManager.isLoggedIn()) return null

        val infoResponse = appContainer.mainRepository.getUserInfo()
        if (!infoResponse.isSuccessful) return null

        val body = infoResponse.body() ?: return null
        appContainer.sessionManager.userId = body.id
        appContainer.sessionManager.username = body.username
        appContainer.sessionManager.role = body.role
        return body.id
    }

    private fun handleUnauthorized() {
        appContainer.sessionManager.clear()
        Snackbar.make(binding.root, R.string.error_unauthorized, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_open_login) {
                findNavController().navigate(R.id.authGateFragment)
            }
            .show()
    }

    private fun renderCurrentList() {
        val list = if (showRecommendations) recommendations else watched
        val selectedButtonId = if (showRecommendations) R.id.recommendButton else R.id.watchedButton
        if (binding.recommendationsToggleGroup.checkedButtonId != selectedButtonId) {
            binding.recommendationsToggleGroup.check(selectedButtonId)
        }

        adapter.submit(list)
        binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyView.text = getString(
            if (showRecommendations) R.string.empty_recommendations else R.string.empty_watched
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


