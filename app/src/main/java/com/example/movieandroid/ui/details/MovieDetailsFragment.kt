package com.example.movieandroid.ui.details

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.movieandroid.appContainer
import com.example.movieandroid.R
import com.example.movieandroid.data.models.MovieDto
import com.example.movieandroid.databinding.FragmentMovieDetailsBinding
import com.example.movieandroid.ui.player.VideoPlayerActivity
import com.example.movieandroid.util.FileUtils
import com.example.movieandroid.util.rethrowIfCancellation
import com.example.movieandroid.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class MovieDetailsFragment : Fragment() {
    private var _binding: FragmentMovieDetailsBinding? = null
    private val binding get() = _binding!!

    private var movieId: Long = -1
    private var currentMovie: MovieDto? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadMovie(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        movieId = arguments?.getLong("movieId") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUploadButtonVisibility()
        binding.watchButton.setOnClickListener {
            val title = currentMovie?.title.orEmpty()
            VideoPlayerActivity.start(requireContext(), movieId, title)
        }
        binding.uploadButton.setOnClickListener {
            if (!canUploadMovie()) {
                Snackbar.make(binding.root, R.string.error_upload_forbidden_admin, Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            picker.launch("video/*")
        }
        loadMovie()
        fetchRoleIfNeeded()
    }

    private fun loadMovie() {
        if (movieId <= 0) {
            Snackbar.make(binding.root, R.string.error_invalid_movie_id, Snackbar.LENGTH_LONG).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = appContainer.mainRepository.getMovie(movieId)
                binding.progressBar.visibility = View.GONE
                if (!response.isSuccessful) {
                    if (response.code() == 401) {
                        handleUnauthorized()
                    } else {
                        Snackbar.make(binding.root, response.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    Snackbar.make(binding.root, R.string.error_empty_response, Snackbar.LENGTH_LONG).show()
                    return@launch
                }

                currentMovie = body
                bindMovie(body)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, e.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun bindMovie(movie: MovieDto) = with(binding) {
        titleText.text = movie.title.orEmpty()
        infoText.text = getString(
            R.string.movie_info,
            movie.year?.toString() ?: "-",
            movie.genre ?: "-",
            movie.director ?: "-",
            movie.duration ?: "-"
        )
        descriptionText.text = getString(R.string.movie_description, movie.description ?: "-")
    }

    private fun uploadMovie(uri: Uri) {
        if (!canUploadMovie()) {
            Snackbar.make(binding.root, R.string.error_upload_forbidden_admin, Snackbar.LENGTH_LONG).show()
            return
        }
        if (movieId <= 0) {
            Snackbar.make(binding.root, R.string.error_invalid_movie_id, Snackbar.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val part = withContext(Dispatchers.IO) {
                    val file = FileUtils.copyUriToCache(requireContext(), uri)
                    val requestBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("movie", file.name, requestBody)
                }

                val response = appContainer.mainRepository.uploadMovie(movieId, part)
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Snackbar.make(binding.root, R.string.upload_success, Snackbar.LENGTH_LONG).show()
                    loadMovie()
                } else {
                    if (response.code() == 401) {
                        handleUnauthorized()
                    } else {
                        Snackbar.make(binding.root, response.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, e.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchRoleIfNeeded() {
        if (!appContainer.sessionManager.isLoggedIn()) return
        if (!appContainer.sessionManager.role.isNullOrBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val info = appContainer.mainRepository.getUserInfo()
                if (info.isSuccessful) {
                    val user = info.body()
                    appContainer.sessionManager.userId = user?.id
                    appContainer.sessionManager.username = user?.username
                    appContainer.sessionManager.role = user?.role
                    updateUploadButtonVisibility()
                }
            } catch (_: Exception) {
                // Ignore: role refresh is best-effort and must not block details screen.
            }
        }
    }

    private fun canUploadMovie(): Boolean = appContainer.sessionManager.isAdmin()

    private fun updateUploadButtonVisibility() {
        binding.uploadButton.isVisible = canUploadMovie()
    }

    private fun handleUnauthorized() {
        appContainer.sessionManager.clear()
        Snackbar.make(binding.root, R.string.error_unauthorized, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_open_login) {
                findNavController().navigate(R.id.authGateFragment)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


