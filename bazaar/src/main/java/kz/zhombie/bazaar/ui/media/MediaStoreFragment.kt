package kz.zhombie.bazaar.ui.media

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.core.MediaScanManager
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.ui.components.view.HeaderView
import kz.zhombie.bazaar.ui.components.view.SelectButton
import kz.zhombie.bazaar.ui.media.album.AlbumsAdapterManager
import kz.zhombie.bazaar.ui.media.gallery.MediaGalleryAdapter
import kz.zhombie.bazaar.ui.media.gallery.MediaGalleryAdapterManager
import kz.zhombie.bazaar.ui.media.gallery.MediaGalleryHeaderAdapter
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.ui.museum.MuseumDialogFragment
import kz.zhombie.bazaar.utils.windowHeight
import java.util.*
import kotlin.math.roundToInt

internal class MediaStoreFragment : BottomSheetDialogFragment(), MediaGalleryAdapter.Callback, MediaGalleryHeaderAdapter.Callback {

    companion object {
        private val TAG: String = MediaStoreFragment::class.java.simpleName

        fun newInstance(settings: MediaStoreScreen.Settings): MediaStoreFragment {
            val fragment = MediaStoreFragment()
            val bundle = Bundle()
            bundle.putSerializable(BundleKey.SETTINGS, settings)
            fragment.arguments = bundle
            return fragment
        }
    }

    private object BundleKey {
        const val SETTINGS = "settings"
    }

    private lateinit var headerView: HeaderView
    private lateinit var selectButton: SelectButton

    private lateinit var viewModel: MediaStoreViewModel

    private var albumsAdapterManager: AlbumsAdapterManager? = null
    private var mediaGalleryAdapterManager: MediaGalleryAdapterManager? = null

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

        viewModel = ViewModelProvider(this, MediaStoreViewModelFactory())
            .get(MediaStoreViewModel::class.java)

        val settings = arguments?.getSerializable(BundleKey.SETTINGS) as MediaStoreScreen.Settings
        val mediaScanManager = MediaScanManager(requireContext())

        viewModel.setSettings(settings)
        viewModel.setMediaScanManager(mediaScanManager)
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

                mediaGalleryAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)

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
        val mediaGalleryView = view.findViewById<RecyclerView>(R.id.mediaGalleryView)
        selectButton = view.findViewById(R.id.selectButton)
        val albumsView = view.findViewById<RecyclerView>(R.id.albumsView)

        setupHeaderView()
        setupMediaGalleryView(mediaGalleryView)
        setupSelectButton(selectedMediaCount = 0)
        setupAlbumsView(albumsView)

        observeAction()
        observeSelectedMedia()
        observeDisplayedMedia()
        observeDisplayedAlbums()
        observeIsAlbumsDisplayed()
        observeActiveAlbum()
    }

    override fun onDestroy() {
        albumsAdapterManager?.destroy()
        albumsAdapterManager = null

        mediaGalleryAdapterManager?.destroy()
        mediaGalleryAdapterManager = null

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

    private fun setupMediaGalleryView(recyclerView: RecyclerView) {
        mediaGalleryAdapterManager = MediaGalleryAdapterManager(requireContext(), recyclerView)
        mediaGalleryAdapterManager?.create(Settings.getImageLoader(), this, this)
    }

    private fun setupSelectButton(selectedMediaCount: Int = 0) {
        selectButton.setText(title = "Выбрать", subtitle = "Выбрано $selectedMediaCount файл(-ов)")

        if (!selectButton.hasOnClickListeners()) {
            selectButton.setOnClickListener {
                val selectedMedia = viewModel.getSelectedMedia().value ?: emptyList()
                resultCallback?.onMediaGallerySelectResult(selectedMedia.map { it.media })
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

    private fun observeAction() {
        viewModel.getAction().observe(viewLifecycleOwner, { action ->
            when (action) {
                // Take camera picture
                is MediaStoreScreen.Action.TakePicture -> {
                    takePicture.launch(action.input)
                }
                is MediaStoreScreen.Action.TakenPictureResult -> {
                    resultCallback?.onCameraResult(action.image)
                    dismiss()
                }
                // Take camera video
                is MediaStoreScreen.Action.TakeVideo -> {
                    takeVideo.launch(action.input)
                }
                is MediaStoreScreen.Action.TakenVideoResult -> {
                    resultCallback?.onCameraResult(action.video)
                    dismiss()
                }
                // Media gallery image selection
                is MediaStoreScreen.Action.SelectLocalMediaGalleryImage -> {
                    getLocalMediaGalleryImage.launch("image/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaGalleryImageResult -> {
                    resultCallback?.onLocalMediaGalleryResult(action.image)
                    dismiss()
                }
                // Multiple media gallery images selection
                is MediaStoreScreen.Action.SelectLocalMediaGalleryImages -> {
                    getLocalMediaGalleryImages.launch("image/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaGalleryImagesResult -> {
                    resultCallback?.onLocalMediaGalleryResult(action.images)
                    dismiss()
                }
                // Media media gallery video selection
                is MediaStoreScreen.Action.SelectLocalMediaGalleryVideo -> {
                    getLocalMediaGalleryVideo.launch("video/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaGalleryVideoResult -> {
                    resultCallback?.onLocalMediaGalleryResult(action.video)
                    dismiss()
                }
                // Multiple media gallery videos selection
                is MediaStoreScreen.Action.SelectLocalMediaGalleryVideos -> {
                    getLocalMediaGalleryVideos.launch("video/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaGalleryVideosResult -> {
                    resultCallback?.onLocalMediaGalleryResult(action.videos)
                    dismiss()
                }
                else -> {
                }
            }
        })
    }

    private fun observeSelectedMedia() {
        viewModel.getSelectedMedia().observe(viewLifecycleOwner, { media ->
            Logger.d(TAG, "getSelectedMedia() -> media.size: ${media.size}")
            setupSelectButton(media.size)
        })
    }

    private fun observeDisplayedMedia() {
        viewModel.getDisplayedMedia().observe(viewLifecycleOwner, { media ->
            mediaGalleryAdapterManager?.submitList(media)
        })
    }

    private fun observeDisplayedAlbums() {
        viewModel.getDisplayedAlbums().observe(viewLifecycleOwner, { albums ->
            albumsAdapterManager?.submitList(albums)
        })
    }

    private fun observeIsAlbumsDisplayed() {
        viewModel.getIsAlbumsDisplayed().observe(viewLifecycleOwner, { isAlbumsDisplayed ->
            if (isAlbumsDisplayed) {
                headerView.toggleIcon(true)
                albumsAdapterManager?.show()

                (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                headerView.toggleIcon(false)
                albumsAdapterManager?.hide()

                mediaGalleryAdapterManager?.scrollToTop()
            }
        })
    }

    private fun observeActiveAlbum() {
        viewModel.getActiveAlbum().observe(viewLifecycleOwner, { album ->
            headerView.setTitle(album.album.displayName)
        })
    }

    /**
     * [MediaGalleryHeaderAdapter.Callback] implementation
     */

    override fun onCameraClicked() {
        viewModel.onCameraShotRequested()
    }

    override fun onExplorerClicked() {
        viewModel.onSelectMediaGalleryRequested()
    }

    /**
     * [MediaGalleryAdapter.Callback] implementation
     */

    override fun onImageClicked(imageView: ShapeableImageView, uiMedia: UIMedia) {
        fun onLayoutChange(imageView: ShapeableImageView) {
            val position = ViewPosition.from(imageView)
            viewModel.onLayoutChange(position)
        }

        if (imageView.viewTreeObserver.isAlive) {
            imageView.viewTreeObserver.addOnGlobalLayoutListener { onLayoutChange(imageView) }
        }

        MuseumDialogFragment.newInstance(uiMedia, ViewPosition.from(imageView))
            .show(childFragmentManager, MuseumDialogFragment::class.java.simpleName)
    }

    override fun onImageCheckboxClicked(uiMedia: UIMedia) {
        viewModel.onMediaCheckboxClicked(uiMedia)
    }

    override fun onVideoClicked(imageView: ShapeableImageView, uiMedia: UIMedia) {
    }

    override fun onVideoCheckboxClicked(uiMedia: UIMedia) {
        viewModel.onMediaCheckboxClicked(uiMedia)
    }

    // Calculates height for 90% of fullscreen
    private fun getBottomSheetDialogDefaultHeight(): Int {
        return requireView().windowHeight * 90 / 100
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        Logger.d(TAG, "isSuccess: $isSuccess")
        viewModel.onPictureTaken(isSuccess)
    }

    private val takeVideo = registerForActivityResult(ActivityResultContracts.TakeVideo()) { bitmap ->
        Logger.d(TAG, "bitmap: $bitmap")
        viewModel.onVideoTaken(bitmap)
    }

    private val getLocalMediaGalleryImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        Logger.d(TAG, "uri: $uri")
        viewModel.onLocalMediaGalleryImageSelected(uri)
    }

    private val getLocalMediaGalleryImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        Logger.d(TAG, "uris: $uris")
        viewModel.onLocalMediaGalleryImagesSelected(uris)
    }

    private val getLocalMediaGalleryVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        Logger.d(TAG, "uri: $uri")
        viewModel.onLocalMediaGalleryVideoSelected(uri)
    }

    private val getLocalMediaGalleryVideos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        Logger.d(TAG, "uris: $uris")
        viewModel.onLocalMediaGalleryVideosSelected(uris)
    }

}