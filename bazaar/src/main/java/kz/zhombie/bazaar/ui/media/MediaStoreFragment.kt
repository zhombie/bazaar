package kz.zhombie.bazaar.ui.media

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.buildSpannedString
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexvasilkov.gestures.animation.ViewPosition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.api.ResultCallback
import kz.zhombie.bazaar.core.MediaScanManager
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.ui.museum.MuseumDialogFragment
import kotlin.math.roundToInt

internal class MediaStoreFragment : BottomSheetDialogFragment(), MediaAdapter.Callback {

    companion object {
        private val TAG: String = MediaStoreFragment::class.java.simpleName

        fun newInstance(): MediaStoreFragment {
            val fragment = MediaStoreFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private lateinit var titleButton: LinearLayout
    private lateinit var titleView: MaterialTextView
    private lateinit var iconView: ShapeableImageView
    private lateinit var closeButton: MaterialButton
    private lateinit var mediaView: RecyclerView
    private lateinit var selectButton: MaterialButton
    private lateinit var albumsView: RecyclerView

    private lateinit var viewModel: MediaStoreViewModel

    private var mediaHeaderAdapter: MediaHeaderAdapter? = null
    private var mediaAdapter: MediaAdapter? = null
    private var concatAdapter: ConcatAdapter? = null

    private var albumsAdapter: AlbumsAdapter? = null

    private var expandedHeight: Int = 0
    private var buttonHeight: Int = 0
    private var collapsedMargin: Int = 0

    private var resultCallback: ResultCallback? = null

    fun setResultCallback(resultCallback: ResultCallback) {
        this.resultCallback = resultCallback
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, theme)

        val mediaScanManager = MediaScanManager(requireContext())

        viewModel = ViewModelProvider(this, MediaStoreViewModelFactory(mediaScanManager))
            .get(MediaStoreViewModel::class.java)
    }

    // TODO: Try to get rid of hack, which helps to set fixed position.
    //  Reference to: https://github.com/xyzcod2/StickyBottomSheet
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            if (dialog is BottomSheetDialog) {
                val bottomSheet = dialog.findViewById<ViewGroup>(R.id.design_bottom_sheet) ?: return@setOnShowListener
                bottomSheet.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = getBottomSheetDialogDefaultHeight()
                }

                expandedHeight = bottomSheet.layoutParams.height
                val peekHeight = (expandedHeight / 1.3F).roundToInt()

                BottomSheetBehavior.from(bottomSheet).apply {
                    state = BottomSheetBehavior.STATE_COLLAPSED
                    setPeekHeight(peekHeight, false)
                    skipCollapsed = false
                    isHideable = true
                }

                buttonHeight = selectButton.height + 0
                collapsedMargin = peekHeight - buttonHeight
                selectButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topMargin = collapsedMargin
                }

                mediaView.setPadding(
                    mediaView.paddingLeft,
                    mediaView.paddingTop,
                    mediaView.paddingRight,
                    mediaView.paddingBottom + buttonHeight
                )

                albumsView.setPadding(
                    albumsView.paddingLeft,
                    albumsView.paddingTop,
                    albumsView.paddingRight,
                    albumsView.paddingBottom + buttonHeight
                )
            }
        }

        if (dialog is BottomSheetDialog) {
            dialog.behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    selectButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        topMargin = if (slideOffset > 0) {
                            (((expandedHeight - buttonHeight) - collapsedMargin) * slideOffset + collapsedMargin).roundToInt()
                        } else {
                            collapsedMargin
                        }
                    }
                }
            })
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media_store, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleButton = view.findViewById(R.id.titleButton)
        titleView = view.findViewById(R.id.titleView)
        iconView = view.findViewById(R.id.iconView)
        closeButton = view.findViewById(R.id.closeButton)
        mediaView = view.findViewById(R.id.mediaView)
        selectButton = view.findViewById(R.id.selectButton)
        albumsView = view.findViewById(R.id.albumsView)

        setupHeaderView()
        setupMediaView()
        setupSelectButton()
        setupAlbumsView()

        viewModel.getSelectedMedia().observe(viewLifecycleOwner, { media ->
            Logger.d(TAG, "getSelectedMedia() -> media.size: ${media.size}")
            setupSelectButton(media.size)
        })

        viewModel.getDisplayedMedia().observe(viewLifecycleOwner, { media ->
            mediaAdapter?.submitList(media)
        })

        viewModel.getDisplayedAlbums().observe(viewLifecycleOwner, { albums ->
            albumsAdapter?.submitList(albums)
        })

        viewModel.getIsAlbumsDisplayed().observe(viewLifecycleOwner, { isAlbumsDisplayed ->
            if (isAlbumsDisplayed) {
                iconView.setImageResource(R.drawable.ic_dropdown_up)
//                selectButton.visibility = View.INVISIBLE
                albumsView.visibility = View.VISIBLE

                (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                iconView.setImageResource(R.drawable.ic_dropdown_down)
//                selectButton.visibility = View.VISIBLE
                albumsView.visibility = View.GONE

                mediaView.scrollToPosition(0)
            }
        })

        viewModel.getActiveAlbum().observe(viewLifecycleOwner, { album ->
            titleView.text = album.album.displayName
        })
    }

    private fun setupHeaderView() {
        titleView.text = "Все медиа"

        titleButton.setOnClickListener {
            viewModel.onHeaderViewTitleClicked()
        }

        closeButton.setOnClickListener { dismiss() }
    }

    private fun setupMediaView() {
        Logger.d(TAG, "setupRecyclerView()")

        mediaHeaderAdapter = MediaHeaderAdapter()
        mediaAdapter = MediaAdapter(Settings.getImageLoader(), this)
        concatAdapter = ConcatAdapter(mediaHeaderAdapter, mediaAdapter)
        mediaView.adapter = concatAdapter

        val layoutManager = GridLayoutManager(
            context,
            3,
            GridLayoutManager.VERTICAL,
            false
        )

        mediaView.layoutManager = layoutManager

        mediaView.setHasFixedSize(true)

        mediaView.itemAnimator = null

        mediaView.addItemDecoration(
            SpacingItemDecoration(
                requireContext().resources.getDimensionPixelOffset(R.dimen.media_item_margin_left),
                requireContext().resources.getDimensionPixelOffset(R.dimen.media_item_margin_top),
                requireContext().resources.getDimensionPixelOffset(R.dimen.media_item_margin_right),
                requireContext().resources.getDimensionPixelOffset(R.dimen.media_item_margin_bottom)
            )
        )
    }

    private fun setupSelectButton(selectedMediaCount: Int = 0) {
        selectButton.text = buildSpannedString {
            val title = "Выбрать"
            val subtitle = "Выбрано $selectedMediaCount файл(-ов)"

            append(title + "\n" + subtitle)

            setSpan(ForegroundColorSpan(Color.parseColor("#333333")), 0, title.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, title.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            setSpan(RelativeSizeSpan(0.7F), title.length, title.length + subtitle.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(Color.parseColor("#8290A0")), title.length, title.length + subtitle.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.NORMAL), title.length, subtitle.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        selectButton.setOnClickListener {
            val selectedMedia = viewModel.getSelectedMedia().value ?: emptyList()
            resultCallback?.onMediaSelected(selectedMedia.map { it.media })
            dismiss()
        }
    }

    private fun setupAlbumsView() {
        albumsView.visibility = View.GONE

        albumsAdapter = AlbumsAdapter(Settings.getImageLoader()) {
            viewModel.onAlbumClicked(it)
        }

        albumsView.adapter = albumsAdapter

        val layoutManager = GridLayoutManager(
            context,
            2,
            GridLayoutManager.VERTICAL,
            false
        )

        albumsView.layoutManager = layoutManager

        albumsView.setHasFixedSize(true)

        albumsView.itemAnimator = null

        albumsView.addItemDecoration(
            SpacingItemDecoration(
                requireContext().resources.getDimensionPixelOffset(R.dimen.album_item_margin_left),
                requireContext().resources.getDimensionPixelOffset(R.dimen.album_item_margin_top),
                requireContext().resources.getDimensionPixelOffset(R.dimen.album_item_margin_right),
                requireContext().resources.getDimensionPixelOffset(R.dimen.album_item_margin_bottom)
            )
        )
    }

    override fun onImageClicked(imageView: ShapeableImageView, uiMedia: UIMedia) {
        imageView.viewTreeObserver.addOnGlobalLayoutListener { onLayoutChange(imageView) }

        MuseumDialogFragment.newInstance(uiMedia, ViewPosition.from(imageView))
            .show(childFragmentManager, MuseumDialogFragment::class.java.simpleName)
    }

    override fun onImageCheckboxClicked(uiMedia: UIMedia) {
        viewModel.onImageCheckboxClicked(uiMedia)
    }

    private fun onLayoutChange(imageView: ShapeableImageView) {
        val position = ViewPosition.from(imageView)
        viewModel.onLayoutChange(position)
    }

    // Calculates height for 90% of fullscreen
    private fun getBottomSheetDialogDefaultHeight(): Int {
        return getWindowHeight() * 90 / 100
    }

    // Calculates window height for fullscreen use
    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        ViewCompat.getDisplay(requireView())?.getRealMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

}