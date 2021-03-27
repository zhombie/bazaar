package kz.zhombie.bazaar.ui.museum

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.alexvasilkov.gestures.animation.ViewPosition
import com.alexvasilkov.gestures.views.GestureFrameLayout
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.media.MediaStoreViewModel
import kz.zhombie.bazaar.ui.model.UIMedia

internal class CinemaDialogFragment : DialogFragment(R.layout.bazaar_fragment_dialog_cinema),
    Player.EventListener {

    companion object {
        private val TAG: String = MuseumDialogFragment::class.java.simpleName

        fun newInstance(uiMedia: UIMedia, startViewPosition: ViewPosition): CinemaDialogFragment {
            val fragment = CinemaDialogFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(BundleKey.UI_MEDIA, uiMedia)
                putSerializable(BundleKey.START_VIEW_POSITION, startViewPosition.pack())
            }
            return fragment
        }
    }

    private object BundleKey {
        const val UI_MEDIA = "ui_media"
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

    private val viewModel: MediaStoreViewModel by activityViewModels()

    private var player: SimpleExoPlayer? = null

    private var uiMedia: UIMedia? = null
    private var startViewPosition: ViewPosition? = null

    private var controllerViewAnimation: ViewPropertyAnimator? = null

    override fun getTheme(): Int {
        return R.style.Bazaar_Dialog_Fullscreen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, theme)

        val arguments = arguments
        require(arguments != null) { "Provide arguments!" }
        uiMedia = arguments.getSerializable(BundleKey.UI_MEDIA) as UIMedia
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
        setupPlayer()
        setupControllerView()

        observeActiveViewPosition()

        val uiMedia = uiMedia
        if (uiMedia != null) {
            player?.setMediaItem(MediaItem.fromUri(uiMedia.media.uri))

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
                    viewModel.onPictureVisibilityChange(uiMedia.media.id, true, 0L)

                    gestureFrameLayout.controller.settings.disableBounds()
                    gestureFrameLayout.positionAnimator.setState(0F, false, false)

                    gestureFrameLayout.postDelayed({ super.dismiss() }, 17L)
                }
            }

            titleView.text = uiMedia.media.displayName

            var subtitle: String? = null
            try {
//                val simpleDateFormat = SimpleDateFormat("dd-mm-yyyy", Locale.ROOT)
//                createdAt = simpleDateFormat.format(uiMedia.media.dateCreated ?: uiMedia.media.dateAdded)
            } catch (e: Exception) {
                if (!uiMedia.media.folderDisplayName.isNullOrBlank()) {
                    subtitle = uiMedia.media.folderDisplayName
                }
            }

            if (subtitle.isNullOrBlank()) {
                subtitleView.visibility = View.GONE
            } else {
                subtitleView.text = subtitle
                subtitleView.visibility = View.VISIBLE
            }
        }

        val viewPosition = startViewPosition
        if (viewPosition != null) {
            gestureFrameLayout.positionAnimator.enter(viewPosition, savedInstanceState == null)
        }

        if (uiMedia != null) {
            gestureFrameLayout.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    gestureFrameLayout.viewTreeObserver.removeOnPreDrawListener(this)
                    viewModel.onPictureVisibilityChange(uiMedia.media.id, false, 17L)
                    return true
                }
            })
            gestureFrameLayout.invalidate()
        }
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

    private fun observeActiveViewPosition() {
        viewModel.getActiveViewPosition().observe(viewLifecycleOwner, { viewPosition ->
            if (gestureFrameLayout.positionAnimator.position > 0f) {
                gestureFrameLayout.positionAnimator.update(viewPosition)
            }
        })
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
        Logger.d(TAG, "onIsPlayingChanged() -> isPlaying: $isPlaying")
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

}