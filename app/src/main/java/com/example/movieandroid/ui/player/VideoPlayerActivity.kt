package com.example.movieandroid.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.movieandroid.appContainer
import com.example.movieandroid.BuildConfig
import com.example.movieandroid.R
import com.example.movieandroid.databinding.ActivityVideoPlayerBinding
import okhttp3.OkHttpClient

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null

    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val movieId = intent.getLongExtra(EXTRA_MOVIE_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        this.title = if (title.isBlank()) getString(R.string.watch) else title

        if (movieId <= 0L) {
            Toast.makeText(this, R.string.error_invalid_movie_id, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        preparePlayer(movieId)
    }

    @UnstableApi
    private fun preparePlayer(movieId: Long) {
        val token = appContainer.sessionManager.token

        val dataSourceFactory = OkHttpDataSource.Factory(OkHttpClient.Builder().build())
        if (!token.isNullOrBlank()) {
            dataSourceFactory.setDefaultRequestProperties(
                mapOf("Authorization" to "Bearer $token")
            )
        }

        val sourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        val playerInstance = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    binding.playerProgress.visibility = if (playbackState == Player.STATE_BUFFERING) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(
                        this@VideoPlayerActivity,
                        playbackErrorMessage(error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        val uri = BuildConfig.MAIN_API_BASE_URL + "api/movie/watch/$movieId"
        val mediaSource = sourceFactory.createMediaSource(MediaItem.fromUri(uri))
        playerInstance.setMediaSource(mediaSource)
        playerInstance.prepare()
        playerInstance.playWhenReady = true
        player = playerInstance
    }

    private fun playbackErrorMessage(error: PlaybackException): String {
        val details = error.message.orEmpty()
        val base = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> getString(R.string.error_network_unavailable)
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> getString(R.string.error_timeout)
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> getString(R.string.error_server_unavailable)
            else -> getString(R.string.player_error)
        }
        return if (details.isBlank()) base else "$base: $details"
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    companion object {
        private const val EXTRA_MOVIE_ID = "extra_movie_id"
        private const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, movieId: Long, title: String) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
                .putExtra(EXTRA_MOVIE_ID, movieId)
                .putExtra(EXTRA_TITLE, title)
            context.startActivity(intent)
        }
    }
}


