package kz.zhombie.bazaar.ui.presentation

import android.Manifest
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.event.EventListener
import kz.zhombie.bazaar.api.model.Audio
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.core.media.MediaScanManager
import kz.zhombie.bazaar.ui.components.view.HeaderView
import kz.zhombie.bazaar.ui.components.view.SelectButton
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.ui.model.UIMultimedia
import kz.zhombie.bazaar.ui.presentation.audible.AudiosAdapter
import kz.zhombie.bazaar.ui.presentation.audible.AudiosAdapterManager
import kz.zhombie.bazaar.ui.presentation.audible.AudiosHeaderAdapter
import kz.zhombie.bazaar.ui.presentation.document.DocumentsAdapter
import kz.zhombie.bazaar.ui.presentation.document.DocumentsAdapterManager
import kz.zhombie.bazaar.ui.presentation.document.DocumentsHeaderAdapter
import kz.zhombie.bazaar.ui.presentation.folder.FoldersAdapterManager
import kz.zhombie.bazaar.ui.presentation.visual.VisualMediaAdapter
import kz.zhombie.bazaar.ui.presentation.visual.VisualMediaAdapterManager
import kz.zhombie.bazaar.ui.presentation.visual.VisualMediaHeaderAdapter
import kz.zhombie.bazaar.utils.*
import kz.zhombie.bazaar.utils.contract.GetContentContract
import kz.zhombie.bazaar.utils.contract.GetMultipleContentsContract
import kz.zhombie.bazaar.utils.contract.TakeVideoContract
import kz.zhombie.cinema.CinemaDialogFragment
import kz.zhombie.cinema.model.Movie
import kz.zhombie.museum.MuseumDialogFragment
import kz.zhombie.museum.model.Painting
import kz.zhombie.radio.Radio
import java.io.File
import java.util.*
import kotlin.math.roundToInt

internal class MediaStoreFragment : BottomSheetDialogFragment(),
    VisualMediaHeaderAdapter.Callback,
    VisualMediaAdapter.Callback,
    AudiosHeaderAdapter.Callback,
    AudiosAdapter.Callback,
    DocumentsHeaderAdapter.Callback,
    DocumentsAdapter.Callback {

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

    // UI interface views
    private lateinit var rootView: ConstraintLayout
    private lateinit var headerView: HeaderView
    private lateinit var audioPlayerViewStub: ViewStub
    private var contentView: RecyclerView? = null
    private var audioPlayerViewStubInflatedView: View? = null
    private var playOrPauseButton: MaterialButton? = null
    private var titleView: MaterialTextView? = null
    private var subtitleView: MaterialTextView? = null
    private var closeButton: MaterialButton? = null
    private lateinit var selectButton: SelectButton
    private lateinit var progressView: LinearLayout
    private lateinit var cancelButton: MaterialButton

    // ViewModel
    private lateinit var viewModel: MediaStoreViewModel

    // RecyclerView Adapters
    private var foldersAdapterManager: FoldersAdapterManager? = null
    private var visualMediaAdapterManager: VisualMediaAdapterManager? = null
    private var audiosAdapterManager: AudiosAdapterManager? = null
    private var documentsAdapterManager: DocumentsAdapterManager? = null

    // Audio
    private var radio: Radio? = null
    private var currentPlayingAudio: UIMultimedia? = null

    // Variables
    private var expandedHeight: Int = 0
    private var buttonHeight: Int = 0
    private var collapsedMargin: Int = 0

    // Callbacks
    private var eventListener: EventListener? = null
    private var resultCallback: ResultCallback? = null

    // TODO: Handle required permissions on its own
    private val mandatoryPermissions by lazy {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun setEventListener(eventListener: EventListener) {
        this.eventListener = eventListener
    }

    fun setResultCallback(resultCallback: ResultCallback) {
        this.resultCallback = resultCallback
    }

    override fun getTheme(): Int = R.style.Bazaar_BottomSheetDialog

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
            if (dialog !is BottomSheetDialog) return@setOnShowListener

            dialog.setOnShowListener {
                val bottomSheet = dialog.findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
                bottomSheet.updateLayoutParams<ViewGroup.LayoutParams> {
                    getBottomSheetDialogDefaultHeight()?.let {
                        height = it
                    }
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
                } else if (viewModel.getSettings().mode == Mode.AUDIO){
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
    ): View? = inflater.inflate(R.layout.bazaar_fragment_media_store, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view.findViewById(R.id.rootView)
        headerView = view.findViewById(R.id.headerView)
        audioPlayerViewStub = view.findViewById(R.id.audioPlayerViewStub)
        contentView = view.findViewById(R.id.contentView)
        selectButton = view.findViewById(R.id.selectButton)
        val foldersView = view.findViewById<RecyclerView>(R.id.foldersView)
        progressView = view.findViewById(R.id.progressView)
        cancelButton = view.findViewById(R.id.cancelButton)

        setupHeaderView()
        setupInfoView()

        contentView?.let { contentView ->
            when {
                viewModel.getSettings().isVisualMediaMode() -> setupVisualMediaView(contentView)
                viewModel.getSettings().mode == Mode.AUDIO -> setupAudiosView(contentView)
                viewModel.getSettings().mode == Mode.DOCUMENT -> setupDocumentsView(contentView)
            }
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
        radio?.release()
        radio?.let { lifecycle.removeObserver(it) }
        radio = null

        foldersAdapterManager?.destroy()
        foldersAdapterManager = null

        visualMediaAdapterManager?.destroy()
        visualMediaAdapterManager = null

        audiosAdapterManager?.destroy()
        audiosAdapterManager = null

        documentsAdapterManager?.destroy()
        documentsAdapterManager = null

        selectButton.setOnClickListener(null)

        super.onDestroy()

        eventListener?.onDestroy()
        eventListener = null
    }

    private fun setupHeaderView() {
        headerView.setTitle(R.string.bazaar_all_media)

        headerView.setOnTitleButtonClickListener {
            viewModel.onHeaderViewTitleClicked()
        }

        headerView.setOnCloseButtonClickListener { dismiss() }
    }

    private fun setupInfoView() {
//        infoView.text = "* Максимально количество файлов для выбора - " + viewModel.getSettings().maxSelectionCount
    }

    private fun setupVisualMediaView(recyclerView: RecyclerView) {
        if (visualMediaAdapterManager == null) {
            visualMediaAdapterManager = VisualMediaAdapterManager(requireContext(), recyclerView)
            visualMediaAdapterManager?.create(
                imageLoader = Settings.getImageLoader(),
                isCameraEnabled = viewModel.getSettings().isCameraShouldBeAvailable(),
                isChooseFromLibraryEnabled = viewModel.getSettings().isLocalMediaSearchAndSelectEnabled,
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
                isChooseFromLibraryEnabled = viewModel.getSettings().isLocalMediaSearchAndSelectEnabled,
                audiosHeaderAdapterCallback = this,
                audiosAdapterCallback = this
            )
        }
    }

    private fun setupDocumentsView(recyclerView: RecyclerView) {
        if (documentsAdapterManager == null) {
            documentsAdapterManager = DocumentsAdapterManager(requireContext(), recyclerView)
            documentsAdapterManager?.create(
                isChooseFromLibraryEnabled = viewModel.getSettings().isLocalMediaSearchAndSelectEnabled,
                documentsHeaderAdapterCallback = this,
                documentsAdapterCallback = this
            )
        }
    }

    private fun setupSelectButton(selectedMediaCount: Int = 0) {
        selectButton.setText(
            title = getString(R.string.bazaar_select),
            subtitle = if (selectedMediaCount == 0) {
                getString(R.string.bazaar_nothing_selected)
            } else {
                resources.getQuantityString(R.plurals.bazaar_selected_files_count, selectedMediaCount, selectedMediaCount)
            }
        )

        if (!selectButton.hasOnClickListeners()) {
            selectButton.setOnClickListener {
                viewModel.onSubmitSelectMediaRequested()
            }
        }
    }

    private fun setupFoldersView(recyclerView: RecyclerView) {
        foldersAdapterManager = FoldersAdapterManager(requireContext(), recyclerView)
        foldersAdapterManager?.hide()
        foldersAdapterManager?.create(
            type = if (viewModel.getSettings().mode == Mode.AUDIO) {
                FoldersAdapterManager.Type.LIST
            } else {
                FoldersAdapterManager.Type.GRID
            },
            isCoverEnabled = viewModel.getSettings().isVisualMediaMode()
        ) {
            viewModel.onFolderClicked(it)
        }
    }

    private fun setupProgressView() {
        progressView.visibility = View.GONE
        cancelButton.setOnClickListener { viewModel.onCancelMediaSelectionRequested() }
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
                    MaterialAlertDialogBuilder(requireContext(), R.style.Bazaar_AlertDialogTheme)
                        .setTitle(R.string.bazaar_action_selection)
                        .setSingleChoiceItems(arrayOf(getString(R.string.bazaar_take_picture), getString(R.string.bazaar_take_video)), -1) { dialog, which ->
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
                    getLocalMediaImageOrVideo.launch(arrayOf("image/*", "video/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaImageOrVideoResult -> {
                    resultCallback?.onLocalMediaStoreResult(action.media)
                    dismiss()
                }
                // Multiple local media images and videos selection
                is MediaStoreScreen.Action.SelectLocalMediaImagesAndVideos -> {
                    getLocalMediaImagesAndVideos.launch(arrayOf("image/*", "video/*"))
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
                    resultCallback?.onMultimediaLocalMediaStoreResult(action.audio)
                    dismiss()
                }
                // Multiple local media audio selection
                is MediaStoreScreen.Action.SelectLocalMediaAudios -> {
                    getLocalMediaAudios.launch(arrayOf("audio/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaAudios -> {
                    resultCallback?.onMultimediaLocalMediaStoreResult(action.audios)
                    dismiss()
                }
                // Local media document selection
                is MediaStoreScreen.Action.SelectLocalDocument -> {
                    getLocalDocument.launch(
                        arrayOf(
                            "text/*",
                            "application/pdf",
                            "application/excel",
                            "application/vnd.ms-excel",
                            "application/x-excel",
                            "application/x-msexcel",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                }
                is MediaStoreScreen.Action.SelectedLocalDocument -> {
                    resultCallback?.onMultimediaLocalMediaStoreResult(action.document)
                    dismiss()
                }
                // Multiple local media document selection
                is MediaStoreScreen.Action.SelectLocalDocuments -> {
                    getLocalDocuments.launch(
                        arrayOf(
                            "text/*",
                            "application/pdf",
                            "application/excel",
                            "application/vnd.ms-excel",
                            "application/x-excel",
                            "application/x-msexcel",
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                }
                is MediaStoreScreen.Action.SelectedLocalDocuments -> {
                    resultCallback?.onMultimediaLocalMediaStoreResult(action.documents)
                    dismiss()
                }
                // Empty value
                is MediaStoreScreen.Action.Empty -> {
                    Toast.makeText(context, R.string.bazaar_error_media_selection, Toast.LENGTH_SHORT).show()
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
            } else if (viewModel.getSettings().mode == Mode.AUDIO) {
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
                } else if (viewModel.getSettings().mode == Mode.AUDIO) {
                    audiosAdapterManager?.scrollToTop()
                }
            }
        })
    }

    private fun observeActiveFolder() {
        viewModel.getActiveFolder().observe(viewLifecycleOwner, { uiFolder ->
            val displayName = uiFolder.getDisplayName(requireContext())
            headerView.setTitle(displayName)
        })
    }

    /**
     * [VisualMediaHeaderAdapter.Callback] implementation
     */

    override fun onCameraClicked() {
        viewModel.onCameraShotRequested()
    }

    override fun onChooseFromLibraryClicked() {
        viewModel.onSelectLocalMediaRequested()
    }

    /**
     * [VisualMediaAdapter.Callback] implementation
     */

    override fun onImageClicked(imageView: ShapeableImageView, uiMedia: UIMedia) {
        MuseumDialogFragment.Builder()
            .setPaintingLoader(Settings.getImageLoader())
            .setPainting(
                Painting(
                    uri = uiMedia.media.uri,
                    info = Painting.Info(
                        title = uiMedia.getDisplayTitle(),
                        subtitle = uiMedia.media.folderDisplayName
                    )
                )
            )
            .setImageView(imageView)
            .setFooterViewEnabled(true)
            .showSafely(childFragmentManager)
    }

    override fun onImageCheckboxClicked(uiMedia: UIMedia) {
        viewModel.onMediaCheckboxClicked(uiMedia)
    }

    override fun onVideoClicked(imageView: ShapeableImageView, uiMedia: UIMedia) {
        CinemaDialogFragment.Builder()
            .setMovie(
                Movie(
                    uri = uiMedia.media.uri,
                    info = Movie.Info(
                        title = uiMedia.getDisplayTitle(),
                        subtitle = uiMedia.media.folderDisplayName
                    )
                )
            )
            .setScreenView(imageView)
            .setFooterViewEnabled(true)
            .showSafely(childFragmentManager)
    }

    override fun onVideoCheckboxClicked(uiMedia: UIMedia) {
        viewModel.onMediaCheckboxClicked(uiMedia)
    }

    /**
     * [AudiosAdapter.Callback] implementation
     */

    override fun onAudioPlayOrPauseClicked(uiMultimedia: UIMultimedia) {
        if (uiMultimedia.multimedia !is Audio) return

        fun set() {
            titleView?.text = uiMultimedia.getDisplayTitle()

            val folderDisplayName = uiMultimedia.multimedia.folderDisplayName
            if (folderDisplayName.isNullOrBlank()) {
                subtitleView?.text = null
                subtitleView?.visibility = View.GONE
            } else {
                subtitleView?.text = folderDisplayName
                subtitleView?.visibility = View.VISIBLE
            }

            playOrPauseButton?.setOnClickListener { radio?.playOrPause() }

            closeButton?.setOnClickListener {
                radio?.release()

                val currentPlayingAudio = currentPlayingAudio
                if (currentPlayingAudio == null) {
                    // Ignored
                } else {
                    audiosAdapterManager?.setPlaying(currentPlayingAudio, isPlaying = false)
                    this.currentPlayingAudio = null
                }

                audioPlayerViewStubInflatedView?.visibility = View.GONE

                radio?.let { radio ->
                    viewLifecycleOwner.lifecycle.removeObserver(radio)
                }

                radio = null
            }

            audioPlayerViewStubInflatedView?.setOnClickListener {
                val currentPlayingAudio = currentPlayingAudio
                if (currentPlayingAudio == null) {
                    return@setOnClickListener
                } else {
                    audiosAdapterManager?.smoothScrollTo(currentPlayingAudio)
                }
            }
        }

        fun playOrPause() {
            Logger.d(TAG, "playOrPause() -> $uiMultimedia")

            if (radio == null) {
                radio = Radio.Builder(requireContext())
                    .create(object : Radio.Listener {
                        override fun onIsPlayingStateChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                Logger.d(TAG, "onPlay() -> $currentPlayingAudio")

                                playOrPauseButton?.setIconResource(R.drawable.bazaar_ic_pause)

                                val currentPlayingAudio: UIMultimedia? = currentPlayingAudio

                                if (currentPlayingAudio == null) {
                                    // Ignored
                                } else {
                                    audiosAdapterManager?.setPlaying(currentPlayingAudio, isPlaying = true)
                                }
                            } else {
                                Logger.d(TAG, "onPause() -> $currentPlayingAudio")

                                playOrPauseButton?.setIconResource(R.drawable.bazaar_ic_play)

                                val currentPlayingAudio: UIMultimedia? = currentPlayingAudio

                                if (currentPlayingAudio == null) {
                                    // Ignored
                                } else {
                                    audiosAdapterManager?.setPlaying(currentPlayingAudio, isPlaying = false)
                                }
                            }
                        }

                        override fun onPlaybackStateChanged(state: Radio.PlaybackState) {
                        }

                        override fun onPlaybackPositionChanged(position: Long) {
                        }

                        override fun onPlayerError(cause: Throwable?) {
                            Toast.makeText(context, R.string.bazaar_error_player, Toast.LENGTH_SHORT).show()
                        }
                    })
                    .also { viewLifecycleOwner.lifecycle.addObserver(it) }
            }

            // The same audio required to play
            if (radio?.currentSource == uiMultimedia.multimedia.uri) {
                radio?.playOrPause()
            } else {
                // The other audio required to play
                val currentPlayingAudio: UIMultimedia? = currentPlayingAudio
                if (currentPlayingAudio == null) {
                    radio?.start(uiMultimedia.multimedia.uri)
                } else {
                    radio?.release()
                    audiosAdapterManager?.setPlaying(currentPlayingAudio, isPlaying = false)
                    radio?.start(uiMultimedia.multimedia.uri)
                }
                this@MediaStoreFragment.currentPlayingAudio = uiMultimedia
            }
        }

        if (audioPlayerViewStubInflatedView == null) {
            audioPlayerViewStub.setOnInflateListener { _, inflated ->
                audioPlayerViewStubInflatedView = inflated
                playOrPauseButton = inflated?.findViewById(R.id.playOrPauseButton)
                titleView = inflated?.findViewById(R.id.titleView)
                subtitleView = inflated?.findViewById(R.id.subtitleView)
                closeButton = inflated?.findViewById(R.id.closeButton)

                set()
                playOrPause()
            }

            audioPlayerViewStub.inflate()
        } else {
            if (audioPlayerViewStubInflatedView?.visibility != View.VISIBLE) {
                audioPlayerViewStubInflatedView?.visibility = View.VISIBLE
            }

            set()
            playOrPause()
        }
    }

    override fun onAudioClicked(uiMultimedia: UIMultimedia) {
        viewModel.onMediaCheckboxClicked(uiMultimedia)
    }

    /**
     * [DocumentsAdapter.Callback] implementation
     */

    override fun onDocumentIconClicked(uiMultimedia: UIMultimedia) {
        val path = uiMultimedia.multimedia.path
        if (path.isNullOrBlank()) {
            return Toast.makeText(context, R.string.bazaar_error_file_not_found, Toast.LENGTH_SHORT).show()
        }
        val file = File(path)
        when (val action = file.open(requireContext())) {
            is OpenFileAction.Success -> {
                if (!action.tryToLaunch(requireContext())) {
                    Toast.makeText(context, R.string.bazaar_error_file_not_found, Toast.LENGTH_SHORT).show()
                }
            }
            is OpenFileAction.Error -> {
                Toast.makeText(context, R.string.bazaar_error_file_not_found, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDocumentClicked(uiMultimedia: UIMultimedia) {
        viewModel.onMediaCheckboxClicked(uiMultimedia)
    }

    // Calculates height for 90% of fullscreen
    private fun getBottomSheetDialogDefaultHeight(): Int? {
        return view?.let { it.windowHeight * 90 / 100 }
    }

    /**
     * [ActivityResultContracts]
     */

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        viewModel.onPictureTaken(isSuccess)
    }

    private val takeVideo = registerForActivityResult(TakeVideoContract()) { uri ->
        Logger.d(TAG, "uri: $uri")
        viewModel.onVideoTaken(uri)
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

    private val getLocalDocument = registerForActivityResult(GetContentContract()) { uri ->
        viewModel.onLocalDocumentSelected(uri)
    }

    private val getLocalDocuments = registerForActivityResult(GetMultipleContentsContract()) { uris ->
        viewModel.onLocalDocumentsSelected(uris)
    }

}