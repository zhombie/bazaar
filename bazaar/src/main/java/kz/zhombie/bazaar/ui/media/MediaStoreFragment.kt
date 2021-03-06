package kz.zhombie.bazaar.ui.media

import android.app.Dialog
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.buildSpannedString
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexvasilkov.gestures.animation.ViewPosition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.core.Logger
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.ui.museum.MuseumDialogFragment
import kz.zhombie.bazaar.utils.ContentResolverCompat
import kz.zhombie.bazaar.utils.readImage
import kz.zhombie.bazaar.utils.readVideo
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

    private lateinit var titleButton: MaterialButton
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

    override fun getTheme(): Int {
        return R.style.BottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, theme)

        viewModel = ViewModelProvider(this, MediaStoreViewModelFactory())
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
        closeButton = view.findViewById(R.id.closeButton)
        mediaView = view.findViewById(R.id.mediaView)
        selectButton = view.findViewById(R.id.selectButton)
        albumsView = view.findViewById(R.id.albumsView)

        setupHeaderView()
        setupMediaView()
        setupSelectButton()
        setupAlbumsView()

        loadImages()

        viewModel.getSelectedMedia().observe(viewLifecycleOwner, {
            Logger.d(TAG, "getSelectedMedia() -> it.size: ${it.size}")
            setupSelectButton(it.size)
        })

        viewModel.getAllMedia().observe(viewLifecycleOwner, {
            mediaAdapter?.submitList(it)
        })

        viewModel.getAllAlbums().observe(viewLifecycleOwner, {
            albumsAdapter?.submitList(it)
        })
    }

    private fun setupHeaderView() {
        titleButton.text = "Все медиа"

        var isAlbumsDisplayed = false
        titleButton.setOnClickListener {
            if (isAlbumsDisplayed) {
                titleButton.setIconResource(R.drawable.ic_dropdown_up)
                selectButton.visibility = View.INVISIBLE
                albumsView.visibility = View.VISIBLE
            } else {
                titleButton.setIconResource(R.drawable.ic_dropdown_down)
                selectButton.visibility = View.VISIBLE
                albumsView.visibility = View.GONE
            }
            isAlbumsDisplayed = !isAlbumsDisplayed
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
        }
    }

    private fun setupAlbumsView() {
        albumsView.visibility = View.GONE

        albumsAdapter = AlbumsAdapter(Settings.getImageLoader())
        albumsView.adapter = albumsAdapter

        val layoutManager = GridLayoutManager(
            context,
            2,
            GridLayoutManager.VERTICAL,
            false
        )

        albumsView.layoutManager = layoutManager

        albumsView.setHasFixedSize(true)

        albumsView.addItemDecoration(
            SpacingItemDecoration(
                requireContext().resources.getDimensionPixelOffset(R.dimen.album_item_margin_left),
                requireContext().resources.getDimensionPixelOffset(R.dimen.album_item_margin_top),
                requireContext().resources.getDimensionPixelOffset(R.dimen.album_item_margin_right),
                requireContext().resources.getDimensionPixelOffset(R.dimen.album_item_margin_bottom)
            )
        )
    }

    private fun loadImages() {
        Logger.d(TAG, "loadImages()")
        lifecycleScope.launch(Dispatchers.IO) {
            val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = ContentResolverCompat.getProjection(ContentResolverCompat.Type.IMAGE)
            val selection: String? = null
            val selectionArgs: MutableList<String>? = null
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 3000"

            context?.contentResolver
                ?.query(uri, projection, selection, selectionArgs?.toTypedArray(), sortOrder)
                ?.use { cursor ->
                    val data = cursor.mapTo(Image::class.java)
                    viewModel.onMediaLoaded(data)
                }
        }
    }

    private fun <T> Cursor.mapTo(clazz: Class<T>): List<Media> {
        Logger.d(TAG, "$count items to $clazz, ${clazz == Image::class.java}")
        val array = arrayListOf<Media>()
        array.addAll(
            generateSequence { if (moveToNext()) this else null }
                .mapNotNull {
                    when (clazz) {
                        Image::class.java -> it.readImage()
                        Video::class.java -> it.readVideo()
                        else -> null
                    }
                }
        )
        return array
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