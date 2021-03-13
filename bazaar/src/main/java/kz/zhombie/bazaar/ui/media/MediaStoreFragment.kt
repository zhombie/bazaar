package kz.zhombie.bazaar.ui.media

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.alexvasilkov.gestures.animation.ViewPosition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.core.media.MediaScanManager
import kz.zhombie.bazaar.ui.components.view.HeaderView
import kz.zhombie.bazaar.ui.components.view.SelectButton
import kz.zhombie.bazaar.ui.media.audible.AudiosAdapter
import kz.zhombie.bazaar.ui.media.audible.AudiosAdapterManager
import kz.zhombie.bazaar.ui.media.folder.FoldersAdapterManager
import kz.zhombie.bazaar.ui.media.visual.VisualMediaAdapter
import kz.zhombie.bazaar.ui.media.visual.VisualMediaAdapterManager
import kz.zhombie.bazaar.ui.media.visual.VisualMediaHeaderAdapter
import kz.zhombie.bazaar.ui.model.UIMultimedia
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.ui.museum.MuseumDialogFragment
import kz.zhombie.bazaar.utils.contract.GetContentContract
import kz.zhombie.bazaar.utils.contract.GetMultipleContentsContract
import kz.zhombie.bazaar.utils.windowHeight
import java.util.*
import kotlin.math.roundToInt

internal class MediaStoreFragment : BottomSheetDialogFragment(),
    VisualMediaAdapter.Callback,
    VisualMediaHeaderAdapter.Callback, AudiosAdapter.Callback {

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
    private lateinit var progressView: FrameLayout

    private lateinit var viewModel: MediaStoreViewModel

    private var foldersAdapterManager: FoldersAdapterManager? = null
    private var visualMediaAdapterManager: VisualMediaAdapterManager? = null
    private var audiosAdapterManager: AudiosAdapterManager? = null

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
                val bottomSheet = dialog.findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
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

                if (viewModel.getSettings().isVisualMediaMode()) {
                    visualMediaAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)
                } else if (viewModel.getSettings().isAudibleMediaMode()){
                    audiosAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)
                }

                foldersAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)
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
        val contentView = view.findViewById<RecyclerView>(R.id.contentView)
        selectButton = view.findViewById(R.id.selectButton)
        val foldersView = view.findViewById<RecyclerView>(R.id.foldersView)
        progressView = view.findViewById(R.id.progressView)

        setupHeaderView()

        if (viewModel.getSettings().isVisualMediaMode()) {
            setupVisualMediaView(contentView)
        } else if (viewModel.getSettings().isAudibleMediaMode()) {
            setupAudiosView(contentView)
        }

        setupSelectButton(selectedMediaCount = 0)
        setupFoldersView(foldersView)
        setupProgressView()

        observeScreenState()
        observeAction()
        observeSelectedMedia()
        observeDisplayedMedia()
        observeDisplayedFolders()
        observeIsFoldersDisplayed()
        observeActiveFolder()
    }

    override fun onDestroy() {
        foldersAdapterManager?.destroy()
        foldersAdapterManager = null

        visualMediaAdapterManager?.destroy()
        visualMediaAdapterManager = null

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

    private fun setupVisualMediaView(recyclerView: RecyclerView) {
        if (visualMediaAdapterManager == null) {
            visualMediaAdapterManager = VisualMediaAdapterManager(requireContext(), recyclerView)
            visualMediaAdapterManager?.create(
                imageLoader = Settings.getImageLoader(),
                isCameraEnabled = viewModel.getSettings().isCameraShouldBeAvailable(),
                isExplorerEnabled = viewModel.getSettings().isLocalMediaSearchAndSelectEnabled,
                visualMediaHeaderAdapterCallback = this,
                visualMediaAdapterCallback = this
            )
        }
    }

    private fun setupAudiosView(recyclerView: RecyclerView) {
        if (audiosAdapterManager == null) {
            audiosAdapterManager = AudiosAdapterManager(requireContext(), recyclerView)
            audiosAdapterManager?.create(
                imageLoader = Settings.getImageLoader(),
                callback = this
            )
        }
    }

    private fun setupSelectButton(selectedMediaCount: Int = 0) {
        selectButton.setText(title = "Выбрать", subtitle = "Выбрано $selectedMediaCount файл(-ов)")

        if (!selectButton.hasOnClickListeners()) {
            selectButton.setOnClickListener {
                viewModel.onSubmitSelectMediaRequested()
            }
        }
    }

    private fun setupFoldersView(recyclerView: RecyclerView) {
        foldersAdapterManager = FoldersAdapterManager(requireContext(), recyclerView)
        foldersAdapterManager?.hide()
        foldersAdapterManager?.create {
            viewModel.onFolderClicked(it)
        }
    }

    private fun setupProgressView() {
        progressView.visibility = View.GONE
    }

    private fun observeScreenState() {
        viewModel.getScreenState().observe(viewLifecycleOwner, { state ->
            when (state) {
                MediaStoreScreen.State.LOADING -> {
                    selectButton.isEnabled = false
                    progressView.visibility = View.VISIBLE
                }
                MediaStoreScreen.State.CONTENT -> {
                    selectButton.isEnabled = true
                    progressView.visibility = View.GONE
                }
                else -> {
                    selectButton.isEnabled = true
                    progressView.visibility = View.GONE
                }
            }
        })
    }

    private fun observeAction() {
        viewModel.getAction().observe(viewLifecycleOwner, { action ->
            when (action) {
                is MediaStoreScreen.Action.SubmitSelectedMedia -> {
                    resultCallback?.onMediaGallerySelectResult(action.media)
                    dismiss()
                }
                is MediaStoreScreen.Action.SubmitSelectedMultimedia -> {
                    resultCallback?.onMultimediaGallerySelectResult(action.multimedia)
                    dismiss()
                }
                is MediaStoreScreen.Action.ChooseBetweenTakePictureOrVideo -> {
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                        .setTitle("Выбор действия")
                        .setSingleChoiceItems(arrayOf(getString(R.string.take_picture), getString(R.string.take_video)), -1) { dialog, which ->
                            dialog.dismiss()
                            if (which == 0) {
                                viewModel.onChoiceMadeBetweenTakePictureOrVideo(MediaStoreScreen.Action.TakePicture::class)
                            } else if (which == 1) {
                                viewModel.onChoiceMadeBetweenTakePictureOrVideo(MediaStoreScreen.Action.TakeVideo::class)
                            }
                        }
                        .show()
                }
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
                // Local media image selection
                is MediaStoreScreen.Action.SelectLocalMediaImage -> {
                    getLocalMediaImage.launch("image/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaImageResult -> {
                    resultCallback?.onLocalMediaStoreResult(action.image)
                    dismiss()
                }
                // Multiple local media images selection
                is MediaStoreScreen.Action.SelectLocalMediaImages -> {
                    getLocalMediaImages.launch("image/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaImagesResult -> {
                    resultCallback?.onLocalMediaStoreResult(action.images)
                    dismiss()
                }
                // Local media video selection
                is MediaStoreScreen.Action.SelectLocalMediaVideo -> {
                    getLocalMediaVideo.launch("video/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaVideoResult -> {
                    resultCallback?.onLocalMediaStoreResult(action.video)
                    dismiss()
                }
                // Multiple local media videos selection
                is MediaStoreScreen.Action.SelectLocalMediaVideos -> {
                    getLocalMediaVideos.launch("video/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaVideosResult -> {
                    resultCallback?.onLocalMediaStoreResult(action.videos)
                    dismiss()
                }
                // Local media image or video selection
                is MediaStoreScreen.Action.SelectLocalMediaImageOrVideo -> {
                    getLocalMediaImageOrVideo.launch(arrayOf("image/*, video/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaImageOrVideoResult -> {
                    resultCallback?.onLocalMediaStoreResult(action.media)
                    dismiss()
                }
                // Multiple local media images and videos selection
                is MediaStoreScreen.Action.SelectLocalMediaImagesAndVideos -> {
                    getLocalMediaImagesAndVideos.launch(arrayOf("image/*, video/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaImagesAndVideosResult -> {
                    resultCallback?.onLocalMediaStoreResult(action.media)
                    dismiss()
                }
                // Local media audio selection
                is MediaStoreScreen.Action.SelectLocalMediaAudio -> {
                    getLocalMediaAudio.launch(arrayOf("audio/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaAudio -> {
                    resultCallback?.onLocalMediaStoreResult(action.audio)
                    dismiss()
                }
                // Empty value
                is MediaStoreScreen.Action.Empty -> {
                    Toast.makeText(context, "Произошла ошибка при выборе медиафайлов", Toast.LENGTH_SHORT).show()
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
        viewModel.getDisplayedMedia().observe(viewLifecycleOwner, { uiMultimedia: List<UIMultimedia> ->
            if (viewModel.getSettings().isVisualMediaMode()) {
                visualMediaAdapterManager?.submitList(uiMultimedia)
            } else if (viewModel.getSettings().isAudibleMediaMode()) {
                audiosAdapterManager?.submitList(uiMultimedia)
            }
        })
    }

    private fun observeDisplayedFolders() {
        viewModel.getDisplayedFolders().observe(viewLifecycleOwner, { folders ->
            foldersAdapterManager?.submitList(folders)
        })
    }

    private fun observeIsFoldersDisplayed() {
        viewModel.getIsFoldersDisplayed().observe(viewLifecycleOwner, { isFoldersDisplayed ->
            if (isFoldersDisplayed) {
                headerView.toggleIcon(true)
                foldersAdapterManager?.show()

                (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                headerView.toggleIcon(false)
                foldersAdapterManager?.hide()

                if (viewModel.getSettings().isVisualMediaMode()) {
                    visualMediaAdapterManager?.scrollToTop()
                } else if (viewModel.getSettings().isAudibleMediaMode()) {
                    audiosAdapterManager?.scrollToTop()
                }
            }
        })
    }

    private fun observeActiveFolder() {
        viewModel.getActiveFolder().observe(viewLifecycleOwner, { uiFolder ->
            headerView.setTitle(uiFolder.folder.displayName)
        })
    }

    /**
     * [VisualMediaHeaderAdapter.Callback] implementation
     */

    override fun onCameraClicked() {
        viewModel.onCameraShotRequested()
    }

    override fun onExplorerClicked() {
        viewModel.onSelectLocalMediaRequested()
    }

    /**
     * [VisualMediaAdapter.Callback] implementation
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

    /**
     * [AudiosAdapter.Callback] implementation
     */

    override fun onAudioClicked(uiMultimedia: UIMultimedia) {
        viewModel.onMediaCheckboxClicked(uiMultimedia)
    }

    // Calculates height for 90% of fullscreen
    private fun getBottomSheetDialogDefaultHeight(): Int {
        return requireView().windowHeight * 90 / 100
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        viewModel.onPictureTaken(isSuccess)
    }

    private val takeVideo = registerForActivityResult(ActivityResultContracts.TakeVideo()) { bitmap ->
        Logger.d(TAG, "bitmap: $bitmap")
        viewModel.onVideoTaken(bitmap)
    }

    private val getLocalMediaImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.onLocalMediaImageSelected(uri)
    }

    private val getLocalMediaImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        viewModel.onLocalMediaImagesSelected(uris)
    }

    private val getLocalMediaVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.onLocalMediaVideoSelected(uri)
    }

    private val getLocalMediaVideos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        viewModel.onLocalMediaVideosSelected(uris)
    }

    private val getLocalMediaImageOrVideo = registerForActivityResult(GetContentContract()) { uri ->
        viewModel.onLocalMediaImageOrVideoSelected(uri)
    }

    private val getLocalMediaImagesAndVideos = registerForActivityResult(GetMultipleContentsContract()) { uris ->
        viewModel.onLocalMediaImagesAndVideosSelected(uris)
    }

    private val getLocalMediaAudio = registerForActivityResult(GetContentContract()) { uri ->
        viewModel.onLocalMediaAudioSelected(uri)
    }

    private val getLocalMediaAudios = registerForActivityResult(GetMultipleContentsContract()) { uris ->
        viewModel.onLocalMediaAudiosSelected(uris)
    }

}