package com.example.movieandroid.ui.directors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movieandroid.appContainer
import com.example.movieandroid.R
import com.example.movieandroid.databinding.FragmentDirectorsBinding
import com.example.movieandroid.util.RefreshBus
import com.example.movieandroid.util.rethrowIfCancellation
import com.example.movieandroid.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DirectorsFragment : Fragment() {
    private var _binding: FragmentDirectorsBinding? = null
    private val binding get() = _binding!!

    private val adapter = DirectorAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.directorsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.directorsRecycler.adapter = adapter
        loadDirectors()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                RefreshBus.events.collect { loadDirectors() }
            }
        }
    }

    private fun loadDirectors() {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = appContainer.mainRepository.getDirectors()
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
                } else {
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


