package kz.zhombie.bazaar.core.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes

internal class AudioPlayer constructor(
    private val context: Context,
    private val listener: Listener
) : LifecycleObserver, Player.EventListener {

    private var player: SimpleExoPlayer? = null

    fun create() {
        if (player == null) {
            player = SimpleExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            player?.playWhenReady = true
            player?.pauseAtEndOfMediaItems = true
            player?.addListener(this)
            player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_OFF
            player?.setWakeMode(C.WAKE_MODE_NONE)
            player?.prepare()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        release()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        release()
    }

    fun getAudioSource(): Uri? {
        return player?.currentMediaItem?.playbackProperties?.uri
    }

    fun setAudioSource(uri: Uri) {
        if (player?.isPlaying == true) {
            player?.pause()
        }
        if ((player?.mediaItemCount ?: 0) > 0) {
            player?.clearMediaItems()
        }
        release()
        create()
        player?.setMediaItem(MediaItem.fromUri(uri), true)
    }

    fun playOrPause() {
        if (player?.isPlaying == true) {
            player?.pause()
        } else {
            player?.play()
        }
    }

    fun pause() {
        player?.pause()
    }

    fun release() {
        if ((player?.mediaItemCount ?: 0) > 0) {
            player?.clearMediaItems()
        }
        player?.release()
        player = null
    }

    override fun onPlaybackStateChanged(state: Int) {
        if (state == Player.STATE_ENDED) {
            player?.seekTo(0)

            listener.onEnd()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            listener.onPlay()
        } else {
            listener.onPause()
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        listener.onPlayerError()
    }

    interface Listener {
        fun onPlay()
        fun onPause()
        fun onEnd()
        fun onPlayerError()
    }

}