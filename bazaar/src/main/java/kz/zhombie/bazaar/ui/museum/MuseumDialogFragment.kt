package kz.zhombie.bazaar.ui.museum

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.alexvasilkov.gestures.animation.ViewPosition
import com.alexvasilkov.gestures.views.GestureImageView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.ui.model.UIMedia

internal class MuseumDialogFragment : DialogFragment(R.layout.bazaar_fragment_dialog_museum),
    MuseumListener {

    companion object {
        private val TAG: String = MuseumDialogFragment::class.java.simpleName

        fun newInstance(uiMedia: UIMedia, startViewPosition: ViewPosition): MuseumDialogFragment {
            val fragment = MuseumDialogFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(BundleKey.UI_MEDIA, uiMedia)
                putString(BundleKey.START_VIEW_POSITION, startViewPosition.pack())
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
    private lateinit var gestureImageView: GestureImageView
    private lateinit var footerView: LinearLayout
    private lateinit var titleView: MaterialTextView
    private lateinit var subtitleView: MaterialTextView

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private var uiMedia: UIMedia? = null
    private var startViewPosition: ViewPosition? = null

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
        gestureImageView = view.findViewById(R.id.gestureImageView)
        footerView = view.findViewById(R.id.footerView)
        titleView = view.findViewById(R.id.titleView)
        subtitleView = view.findViewById(R.id.subtitleView)

        setupActionBar()
        setupGestureImageView()

        val uiMedia = uiMedia
        if (uiMedia != null) {
            Settings.getImageLoader()
                .loadFullscreenImage(requireContext(), gestureImageView, uiMedia.media.uri)

            gestureImageView.positionAnimator.addPositionUpdateListener { position, isLeaving ->
                val isFinished = position == 0F && isLeaving

                appBarLayout.alpha = position
                backgroundView.alpha = position
                footerView.alpha = position

                if (isFinished) {
                    appBarLayout.visibility = View.INVISIBLE
                    backgroundView.visibility = View.INVISIBLE
                    footerView.visibility = View.INVISIBLE
                } else {
                    appBarLayout.visibility = View.VISIBLE
                    backgroundView.visibility = View.VISIBLE
                    footerView.visibility = View.VISIBLE
                }

                gestureImageView.visibility = if (isFinished) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }

                if (isFinished) {
                    gestureImageView.controller.settings.disableBounds()
                    gestureImageView.positionAnimator.setState(0F, false, false)

                    gestureImageView.postDelayed({ super.dismiss() }, 17L)
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
            gestureImageView.positionAnimator.enter(viewPosition, savedInstanceState == null)
        }

        gestureImageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                gestureImageView.viewTreeObserver.removeOnPreDrawListener(this)
                callback?.onPictureHide(17L)
                return true
            }
        })
        gestureImageView.invalidate()
    }

    override fun onCancel(dialog: DialogInterface) {
        dismiss()
    }

    override fun dismiss() {
        if (!gestureImageView.positionAnimator.isLeaving) {
            gestureImageView.positionAnimator.exit(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        callback?.onPictureShow(0L)
    }

    override fun onDestroy() {
        super.onDestroy()

        callback?.onDestroy()
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

    private fun setupGestureImageView() {
        // Settings
        gestureImageView.controller.settings
            .setAnimationsDuration(250L)
            .setBoundsType(com.alexvasilkov.gestures.Settings.Bounds.NORMAL)
            .setDoubleTapEnabled(true)
            .setExitEnabled(true)
            .setExitType(com.alexvasilkov.gestures.Settings.ExitType.SCROLL)
            .setFillViewport(true)
            .setFitMethod(com.alexvasilkov.gestures.Settings.Fit.INSIDE)
            .setFlingEnabled(true)
            .setGravity(Gravity.CENTER)
            .setMaxZoom(2.0F)
            .setMinZoom(0F)
            .setPanEnabled(true)
            .setZoomEnabled(true)

        // Click actions
        gestureImageView.setOnClickListener {
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
                footerView.animate()
                    .alpha(0.0F)
                    .setDuration(100L)
                    .withEndAction {
                        footerView.visibility = View.INVISIBLE
                    }
                    .start()
            } else {
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

    /**
     * [MuseumListener] implementation
     */

    override fun onTrackViewPosition(viewPosition: ViewPosition) {
        if (gestureImageView.positionAnimator.position > 0f) {
            gestureImageView.positionAnimator.update(viewPosition)
        }
    }

    interface Callback {
        fun onPictureShow(delay: Long = 0L)
        fun onPictureHide(delay: Long = 0L)

        fun onDestroy()
    }

}