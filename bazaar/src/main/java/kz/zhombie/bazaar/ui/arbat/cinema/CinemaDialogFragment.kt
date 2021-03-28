package kz.zhombie.bazaar.ui.arbat.cinema

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.alexvasilkov.gestures.animation.ViewPosition
import com.alexvasilkov.gestures.views.GestureFrameLayout
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R

class CinemaDialogFragment private constructor() : DialogFragment(R.layout.bazaar_fragment_dialog_cinema),
    CinemaDialogFragmentListener, Player.EventListener {

    companion object {
        private val TAG: String = CinemaDialogFragment::class.java.simpleName

        private fun newInstance(
            uri: Uri,
            title: String,
            subtitle: String? = null,
            startViewPosition: ViewPosition
        ): CinemaDialogFragment {
            val fragment = CinemaDialogFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(BundleKey.URI, uri.toString())
                putSerializable(BundleKey.TITLE, title)
                if (!subtitle.isNullOrBlank()) putSerializable(BundleKey.SUBTITLE, subtitle)
                putSerializable(BundleKey.START_VIEW_POSITION, startViewPosition.pack())
            }
            return fragment
        }
    }

    class Builder {
        private var uri: Uri? = null
        private var title: String? = null
        private var subtitle: String? = null
        private var viewPosition: ViewPosition? = null
        private var screenView: View? = null
        private var callback: Callback? = null

        fun setUri(uri: Uri): Builder {
            this.uri = uri
            return this
        }

        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setSubtitle(subtitle: String?): Builder {
            this.subtitle = subtitle
            return this
        }

        fun setStartViewPosition(view: View): Builder {
            this.viewPosition = ViewPosition.from(view)
            return this
        }

        fun setStartViewPosition(viewPosition: ViewPosition): Builder {
            this.viewPosition = viewPosition
            return this
        }

        fun setScreenView(screenView: View): Builder {
            this.screenView = screenView
            return this
        }

        fun setCallback(callback: Callback): Builder {
            this.callback = callback
            return this
        }

        fun build(): CinemaDialogFragment {
            return newInstance(
                uri = requireNotNull(uri) { "Cinema movie uri is mandatory value" },
                title = requireNotNull(title) { "Cinema movie title is mandatory value" },
                subtitle = subtitle,
                startViewPosition = requireNotNull(viewPosition) {
                    "Cinema movie needs start view position, in order to make smooth transition animation"
                }
            ).apply {
                this@Builder.screenView?.let { setScreenView(it) }

                this@Builder.callback?.let { setCallback(it) }
            }
        }
    }

    private object BundleKey {
        const val URI = "uri"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val START_VIEW_POSITION = "start_view_position"
    }

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var backgroundView: View
    private lateinit var gestureFrameLayout: GestureFrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var controllerView: FrameLayout
    private lateinit var playOrPauseButton: MaterialButton
    private lateinit var footerView: LinearLayout
    private lateinit var titleView: MaterialTextView
    private lateinit var subtitleView: MaterialTextView

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private var isMovieShowCalled: Boolean = false

    private var screenView: View? = null
        set(value) {
            field = value

            if (value != null) {
                if (onGlobalLayoutListener == null) {
                    onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                        onTrackViewPosition(value)
                    }

                    if (value.viewTreeObserver.isAlive) {
                        value.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
                    }
                }
            }
        }

    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var player: SimpleExoPlayer? = null

    private lateinit var uri: Uri
    private lateinit var title: String
    private var subtitle: String? = null
    private lateinit var startViewPosition: ViewPosition

    private var controllerViewAnimation: ViewPropertyAnimator? = null

    override fun getTheme(): Int {
        return R.style.Bazaar_Dialog_Fullscreen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, theme)

        val arguments = arguments
        require(arguments != null) { "Provide arguments!" }
        uri = Uri.parse(requireNotNull(arguments.getString(BundleKey.URI)))
        title = requireNotNull(arguments.getString(BundleKey.TITLE))
        subtitle = arguments.getString(BundleKey.SUBTITLE)
        startViewPosition = ViewPosition.unpack(arguments.getString(BundleKey.START_VIEW_POSITION))
    }

    override fun onResume() {
        val layoutParams: WindowManager.LayoutParams? = dialog?.window?.attributes
        layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams?.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog?.window?.attributes = layoutParams
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appBarLayout = view.findViewById(R.id.appBarLayout)
        toolbar = view.findViewById(R.id.toolbar)
        backgroundView = view.findViewById(R.id.backgroundView)
        gestureFrameLayout = view.findViewById(R.id.gestureFrameLayout)
        playerView = view.findViewById(R.id.playerView)
        controllerView = view.findViewById(R.id.controllerView)
        playOrPauseButton = view.findViewById(R.id.playOrPauseButton)
        footerView = view.findViewById(R.id.footerView)
        titleView = view.findViewById(R.id.titleView)
        subtitleView = view.findViewById(R.id.subtitleView)

        setupActionBar()
        setupBackgroundView()
        setupGestureFrameLayout()
        setupInfo()
        setupPlayer()
        setupControllerView()

        player?.setMediaItem(MediaItem.fromUri(uri))

        gestureFrameLayout.positionAnimator.addPositionUpdateListener { position, isLeaving ->
            val isFinished = position == 0F && isLeaving

            appBarLayout.alpha = position
            backgroundView.alpha = position
            controllerView.alpha = position
            footerView.alpha = position

            if (isLeaving) {
                controllerView.visibility = View.INVISIBLE
            } else {
                controllerView.visibility = View.VISIBLE
            }

            if (isFinished) {
                appBarLayout.visibility = View.INVISIBLE
                backgroundView.visibility = View.INVISIBLE
                footerView.visibility = View.INVISIBLE
            } else {
                appBarLayout.visibility = View.VISIBLE
                backgroundView.visibility = View.VISIBLE
                footerView.visibility = View.VISIBLE
            }

            gestureFrameLayout.visibility = if (isFinished) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

            if (isFinished) {
                if (!isMovieShowCalled) {
                    callback?.onMovieShow(0L)
                    isMovieShowCalled = true
                }

                gestureFrameLayout.controller.settings.disableBounds()
                gestureFrameLayout.positionAnimator.setState(0F, false, false)

                gestureFrameLayout.postDelayed({ super.dismiss() }, 17L)
            }
        }

        gestureFrameLayout.positionAnimator.enter(startViewPosition, savedInstanceState == null)

        gestureFrameLayout.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                gestureFrameLayout.viewTreeObserver.removeOnPreDrawListener(this)
                callback?.onMovieHide(17L)
                return true
            }
        })
        gestureFrameLayout.invalidate()
    }

    override fun onPause() {
        super.onPause()
        playerView.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        playerView.onPause()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isMovieShowCalled) {
            callback?.onMovieShow(0L)
            isMovieShowCalled = true
        }

        if (screenView?.viewTreeObserver?.isAlive == true) {
            screenView?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }

        onGlobalLayoutListener = null
        screenView = null

        callback = null
    }

    private fun setupActionBar() {
        val activity = activity
        if (activity is AppCompatActivity) {
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowTitleEnabled(false)
            toolbar.setNavigationOnClickListener { dismiss() }
        }
    }

    private fun setupBackgroundView() {
        backgroundView.visibility = View.VISIBLE
    }

    private fun setupGestureFrameLayout() {
        // Settings
        gestureFrameLayout.controller.settings
            .setAnimationsDuration(250L)
            .setBoundsType(com.alexvasilkov.gestures.Settings.Bounds.NORMAL)
            .setDoubleTapEnabled(false)
            .setExitEnabled(true)
            .setExitType(com.alexvasilkov.gestures.Settings.ExitType.SCROLL)
            .setFillViewport(true)
            .setFitMethod(com.alexvasilkov.gestures.Settings.Fit.INSIDE)
            .setFlingEnabled(true)
            .setGravity(Gravity.CENTER)
            .setMaxZoom(0F)
            .setMinZoom(0F)
            .setPanEnabled(true)
            .setZoomEnabled(false)

        // Click actions
        gestureFrameLayout.setOnClickListener {
            if (appBarLayout.visibility == View.VISIBLE) {
                appBarLayout.animate()
                    .alpha(0.0F)
                    .setDuration(100L)
                    .withEndAction {
                        appBarLayout.visibility = View.INVISIBLE
                    }
                    .start()
            } else {
                appBarLayout.animate()
                    .alpha(1.0F)
                    .setDuration(100L)
                    .withStartAction {
                        appBarLayout.visibility = View.VISIBLE
                    }
                    .start()
            }

            if (footerView.visibility == View.VISIBLE) {
                controllerViewAnimation?.cancel()
                controllerViewAnimation = null

                controllerView.visibility = View.INVISIBLE

                footerView.animate()
                    .alpha(0.0F)
                    .setDuration(100L)
                    .withEndAction {
                        footerView.visibility = View.INVISIBLE
                    }
                    .start()
            } else {
                controllerViewAnimation?.cancel()
                controllerViewAnimation = null

                controllerView.visibility = View.VISIBLE

                footerView.animate()
                    .alpha(1.0F)
                    .setDuration(100L)
                    .withStartAction {
                        footerView.visibility = View.VISIBLE
                    }
                    .start()
            }
        }
    }

    private fun setupInfo() {
        titleView.text = title

        if (subtitle.isNullOrBlank()) {
            subtitleView.visibility = View.GONE
        } else {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
        }
    }

    private fun setupPlayer() {
        if (player == null) {
            player = SimpleExoPlayer.Builder(requireContext())
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            playerView.player = player
            playerView.setShowPreviousButton(false)
            playerView.setShowNextButton(false)
            playerView.setShowRewindButton(false)
            playerView.setShowRewindButton(false)
            playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            playerView.setShowFastForwardButton(false)
            playerView.setUseSensorRotation(false)
            playerView.useController = false
            playerView.controllerAutoShow = false

            player?.playWhenReady = true
            player?.pauseAtEndOfMediaItems = true
            player?.addListener(this)
            player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_OFF
            player?.setWakeMode(C.WAKE_MODE_NONE)
            player?.prepare()
        }
    }

    private fun setupControllerView() {
        playOrPauseButton.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
            } else {
                player?.play()
            }
        }
    }

    private fun releasePlayer() {
        player?.clearMediaItems()
        player?.release()
        player = null
    }

    /**
     * [Player.EventListener] implementation
     */

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            playOrPauseButton.setIconResource(R.drawable.exo_icon_pause)

            controllerViewAnimation = controllerView.animate()
                .setStartDelay(2500L)
                .withStartAction {
                    controllerView.visibility = View.VISIBLE
                }
                .withEndAction {
                    controllerView.visibility = View.INVISIBLE
                }
            controllerViewAnimation?.start()
        } else {
            playOrPauseButton.setIconResource(R.drawable.exo_icon_play)
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        if (state == Player.STATE_ENDED) {
            player?.seekTo(0)

            appBarLayout.alpha = 1.0F
            appBarLayout.visibility = View.VISIBLE

            controllerView.alpha = 1.0F
            controllerView.visibility = View.VISIBLE

            footerView.alpha = 1.0F
            footerView.visibility = View.VISIBLE
        }
    }

    /**
     * [CinemaDialogFragmentListener] implementation
     */

    override fun onTrackViewPosition(view: View) {
        onTrackViewPosition(ViewPosition.from(view))
    }

    override fun onTrackViewPosition(viewPosition: ViewPosition) {
        if (gestureFrameLayout.positionAnimator.position > 0f) {
            gestureFrameLayout.positionAnimator.update(viewPosition)
        }
    }

    override fun setScreenView(view: View): CinemaDialogFragment {
        this.screenView = view
        return this
    }

    interface Callback {
        fun onMovieShow(delay: Long = 0L)
        fun onMovieHide(delay: Long = 0L)
    }

}