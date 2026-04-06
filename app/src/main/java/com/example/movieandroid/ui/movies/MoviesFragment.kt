package com.example.movieandroid.ui.movies

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
import com.example.movieandroid.databinding.FragmentMoviesBinding
import com.example.movieandroid.ui.player.VideoPlayerActivity
import com.example.movieandroid.util.RefreshBus
import com.example.movieandroid.util.rethrowIfCancellation
import com.example.movieandroid.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MoviesFragment : Fragment() {
    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        MovieAdapter(
            onDetails = { movie ->
                findNavController().navigate(
                    R.id.movieDetailsFragment,
                    bundleOf("movieId" to movie.id)
                )
            },
            onWatch = { movie ->
                VideoPlayerActivity.start(requireContext(), movie.id, movie.title.orEmpty())
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.moviesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.moviesRecycler.adapter = adapter
        loadMovies()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                RefreshBus.events.collect { loadMovies() }
            }
        }
    }

    private fun loadMovies() {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = appContainer.mainRepository.getMovies()
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val list = response.body()
                    if (list == null) {
                        adapter.submit(emptyList())
                        binding.emptyView.visibility = View.VISIBLE
                        Snackbar.make(binding.root, R.string.error_empty_response, Snackbar.LENGTH_LONG).show()
                    } else {
                        adapter.submit(list)
                        binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                    return@launch
                }

                if (response.code() == 401) {
                    appContainer.sessionManager.clear()
                    Snackbar.make(binding.root, R.string.error_unauthorized, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_open_login) {
                            findNavController().navigate(R.id.authGateFragment)
                        }
                        .show()
                } else {
                    Snackbar.make(binding.root, response.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, e.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


