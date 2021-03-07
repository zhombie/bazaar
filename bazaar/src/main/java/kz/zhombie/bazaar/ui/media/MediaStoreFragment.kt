package kz.zhombie.bazaar.ui.media

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.alexvasilkov.gestures.animation.ViewPosition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.imageview.ShapeableImageView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.api.ResultCallback
import kz.zhombie.bazaar.core.MediaScanManager
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.components.HeaderView
import kz.zhombie.bazaar.ui.components.SelectButton
import kz.zhombie.bazaar.ui.media.album.AlbumsAdapterManager
import kz.zhombie.bazaar.ui.media.gallery.GalleryAdapter
import kz.zhombie.bazaar.ui.media.gallery.GalleryAdapterManager
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.ui.museum.MuseumDialogFragment
import kz.zhombie.bazaar.utils.windowHeight
import kotlin.math.roundToInt

internal class MediaStoreFragment : BottomSheetDialogFragment(), GalleryAdapter.Callback {

    companion object {
        private val TAG: String = MediaStoreFragment::class.java.simpleName

        fun newInstance(): MediaStoreFragment {
            val fragment = MediaStoreFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private lateinit var headerView: HeaderView
    private lateinit var selectButton: SelectButton

    private lateinit var viewModel: MediaStoreViewModel

    private var albumsAdapterManager: AlbumsAdapterManager? = null
    private var galleryAdapterManager: GalleryAdapterManager? = null

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

                buttonHeight = selectButton.height
                collapsedMargin = peekHeight - buttonHeight
                selectButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topMargin = collapsedMargin
                }

                galleryAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)

                albumsAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)
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

        headerView = view.findViewById(R.id.headerView)
        val galleryView = view.findViewById<RecyclerView>(R.id.galleryView)
        selectButton = view.findViewById(R.id.selectButton)
        val albumsView = view.findViewById<RecyclerView>(R.id.albumsView)

        setupHeaderView()
        setupGalleryView(galleryView)
        setupSelectButton()
        setupAlbumsView(albumsView)

        viewModel.getSelectedMedia().observe(viewLifecycleOwner, { media ->
            Logger.d(TAG, "getSelectedMedia() -> media.size: ${media.size}")
            setupSelectButton(media.size)
        })

        viewModel.getDisplayedMedia().observe(viewLifecycleOwner, { media ->
            galleryAdapterManager?.submitList(media)
        })

        viewModel.getDisplayedAlbums().observe(viewLifecycleOwner, { albums ->
            albumsAdapterManager?.submitList(albums)
        })

        viewModel.getIsAlbumsDisplayed().observe(viewLifecycleOwner, { isAlbumsDisplayed ->
            if (isAlbumsDisplayed) {
                headerView.toggleIcon(true)
                albumsAdapterManager?.show()

                (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                headerView.toggleIcon(false)
                albumsAdapterManager?.hide()

                galleryAdapterManager?.scrollToTop()
            }
        })

        viewModel.getActiveAlbum().observe(viewLifecycleOwner, { album ->
            headerView.setTitle(album.album.displayName)
        })
    }

    override fun onDestroy() {
        albumsAdapterManager?.destroy()
        albumsAdapterManager = null

        galleryAdapterManager?.destroy()
        galleryAdapterManager = null

        selectButton.setOnClickListener(null)

        super.onDestroy()
    }

    private fun setupHeaderView() {
        headerView.setTitle("Все медиа")

        headerView.setOnTitleButtonClickListener {
            viewModel.onHeaderViewTitleClicked()
        }

        headerView.setOnCloseButtonClickListener { dismiss() }
    }

    private fun setupGalleryView(recyclerView: RecyclerView) {
        galleryAdapterManager = GalleryAdapterManager(requireContext(), recyclerView)
        galleryAdapterManager?.create(Settings.getImageLoader(), this)
    }

    private fun setupSelectButton(selectedMediaCount: Int = 0) {
        selectButton.setText(title = "Выбрать", subtitle = "Выбрано $selectedMediaCount файл(-ов)")

        if (!selectButton.hasOnClickListeners()) {
            selectButton.setOnClickListener {
                val selectedMedia = viewModel.getSelectedMedia().value ?: emptyList()
                resultCallback?.onMediaSelected(selectedMedia.map { it.media })
                dismiss()
            }
        }
    }

    private fun setupAlbumsView(recyclerView: RecyclerView) {
        albumsAdapterManager = AlbumsAdapterManager(requireContext(), recyclerView)
        albumsAdapterManager?.hide()
        albumsAdapterManager?.create {
            viewModel.onAlbumClicked(it)
        }
    }

    /**
     * [GalleryAdapter.Callback] implementation
     */

    override fun onImageClicked(imageView: ShapeableImageView, uiMedia: UIMedia) {
        fun onLayoutChange(imageView: ShapeableImageView) {
            val position = ViewPosition.from(imageView)
            viewModel.onLayoutChange(position)
        }

        imageView.viewTreeObserver.addOnGlobalLayoutListener { onLayoutChange(imageView) }

        MuseumDialogFragment.newInstance(uiMedia, ViewPosition.from(imageView))
            .show(childFragmentManager, MuseumDialogFragment::class.java.simpleName)
    }

    override fun onImageCheckboxClicked(uiMedia: UIMedia) {
        viewModel.onImageCheckboxClicked(uiMedia)
    }

    // Calculates height for 90% of fullscreen
    private fun getBottomSheetDialogDefaultHeight(): Int {
        return requireView().windowHeight * 90 / 100
    }

}