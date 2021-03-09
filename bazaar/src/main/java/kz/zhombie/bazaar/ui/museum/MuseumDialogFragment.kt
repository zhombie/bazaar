package kz.zhombie.bazaar.ui.museum

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.alexvasilkov.gestures.animation.ViewPosition
import com.alexvasilkov.gestures.views.GestureImageView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.core.MediaScanManager
import kz.zhombie.bazaar.ui.media.MediaStoreViewModel
import kz.zhombie.bazaar.ui.media.MediaStoreViewModelFactory
import kz.zhombie.bazaar.ui.model.UIMedia

internal class MuseumDialogFragment : DialogFragment(R.layout.fragment_dialog_museum) {

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

    private val viewModel: MediaStoreViewModel by activityViewModels()

    private var uiMedia: UIMedia? = null
    private var startViewPosition: ViewPosition? = null

    override fun getTheme(): Int {
        return R.style.Dialog_Fullscreen
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

    override fun onCancel(dialog: DialogInterface) {
        dismiss()
    }

    override fun dismiss() {
        if (!gestureImageView.positionAnimator.isLeaving) {
            gestureImageView.positionAnimator.exit(true)
        }
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

        viewModel.getActiveViewPosition().observe(viewLifecycleOwner, { viewPosition ->
            if (gestureImageView.positionAnimator.position > 0f) {
                gestureImageView.positionAnimator.update(viewPosition)
            }
        })

        val uiMedia = uiMedia
        if (uiMedia != null) {
            Settings.getImageLoader().loadFullscreenImage(requireContext(), gestureImageView, uiMedia.media.uri)

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
                    viewModel.onVisibilityChange(uiMedia.media.id, true, 0L)

                    gestureImageView.controller.settings.disableBounds()
                    gestureImageView.positionAnimator.setState(0F, false, false)

                    gestureImageView.postDelayed({ super.dismiss() }, 17L)
                }
            }

            titleView.text = uiMedia.media.displayName

            val createdAt: String? = null
            try {
//                val simpleDateFormat = SimpleDateFormat("dd-mm-yyyy", Locale.ROOT)
//                createdAt = simpleDateFormat.format(uiMedia.media.dateCreated ?: uiMedia.media.dateAdded)
            } catch (e: Exception) {
            }

            if (createdAt.isNullOrBlank()) {
                subtitleView.visibility = View.GONE
            } else {
                subtitleView.text = createdAt
                subtitleView.visibility = View.VISIBLE
            }
        }

        val viewPosition = startViewPosition
        if (viewPosition != null) {
            gestureImageView.positionAnimator.enter(viewPosition, savedInstanceState == null)
        }

        if (uiMedia != null) {
            gestureImageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    gestureImageView.viewTreeObserver.removeOnPreDrawListener(this)
                    viewModel.onVisibilityChange(uiMedia.media.id, false, 17L)
                    return true
                }
            })
            gestureImageView.invalidate()
        }
    }

    private fun setupActionBar() {
        val activity = activity
        if (activity is AppCompatActivity) {
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }

    private fun setupGestureImageView() {
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
    }

}