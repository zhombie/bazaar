package kz.zhombie.bazaar.ui.presentation

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
import kz.garage.multimedia.store.model.Audio
import kz.garage.window.computeCurrentWindowSize
import kz.zhombie.bazaar.Bazaar
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.event.EventListener
import kz.zhombie.bazaar.api.result.ResultCallback
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.core.media.MediaScanManager
import kz.zhombie.bazaar.ui.components.view.HeaderView
import kz.zhombie.bazaar.ui.components.view.SelectButton
import kz.zhombie.bazaar.ui.model.UIContent
import kz.zhombie.bazaar.ui.model.UIMedia
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
            bundle.putParcelable(BundleKey.SETTINGS, settings)
            fragment.arguments = bundle
            return fragment
        }
    }

    private object BundleKey {
        const val SETTINGS = "settings"
    }

    // UI interface views
    private var rootView: ConstraintLayout? = null
    private var headerView: HeaderView? = null
    private var audioPlayerViewStub: ViewStub? = null
    private var contentView: RecyclerView? = null
    private var audioPlayerViewStubInflatedView: View? = null
    private var playOrPauseButton: MaterialButton? = null
    private var titleView: MaterialTextView? = null
    private var subtitleView: MaterialTextView? = null
    private var closeButton: MaterialButton? = null
    private var selectButton: SelectButton? = null
    private var progressView: LinearLayout? = null
    private var cancelButton: MaterialButton? = null

    // ViewModel
    private var viewModel: MediaStoreViewModel? = null

    // RecyclerView Adapters
    private var foldersAdapterManager: FoldersAdapterManager? = null
    private var visualMediaAdapterManager: VisualMediaAdapterManager? = null
    private var audiosAdapterManager: AudiosAdapterManager? = null
    private var documentsAdapterManager: DocumentsAdapterManager? = null

    // Audio
    private var radio: Radio? = null
    private var currentPlayingAudio: UIContent? = null

    // Variables
    private var expandedHeight: Int = 0
    private var buttonHeight: Int = 0
    private var collapsedMargin: Int = 0

    // Callbacks
    private var eventListener: EventListener? = null
    private var resultCallback: ResultCallback? = null

    // TODO: Handle required permissions on its own
//    private val mandatoryPermissions by lazy {
//        arrayOf(
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//        )
//    }

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

        val configuration = Bazaar.getConfiguration(requireContext())
        Logger.debug(TAG, "configuration: $configuration")

        val imageLoader = Bazaar.getImageLoader(requireContext())
        Logger.debug(TAG, "imageLoader: $imageLoader")

        viewModel = ViewModelProvider(
            this,
            MediaStoreViewModelFactory()
        )[MediaStoreViewModel::class.java]

        val settings = requireNotNull(arguments?.getParcelable<MediaStoreScreen.Settings>(BundleKey.SETTINGS))
        val mediaScanManager = MediaScanManager(requireContext())

        viewModel?.setSettings(settings)
        viewModel?.setMediaScanManager(mediaScanManager)
    }

    // TODO: Try to get rid of hack, which helps to set fixed position.
    //  Reference to: https://github.com/xyzcod2/StickyBottomSheet
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            if (dialog !is BottomSheetDialog) return@setOnShowListener

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

            buttonHeight = selectButton?.height ?: 0
            collapsedMargin = peekHeight - buttonHeight

            selectButton?.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = collapsedMargin
            }

            if (viewModel?.getSettings()?.isVisualMediaMode() == true) {
                visualMediaAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)
            } else if (viewModel?.getSettings()?.mode == Mode.AUDIO) {
                audiosAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)
            }

            foldersAdapterManager?.setPadding(extraPaddingBottom = buttonHeight)
        }

        if (dialog is BottomSheetDialog) {
            dialog.behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    selectButton?.updateLayoutParams<ConstraintLayout.LayoutParams> {
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
                viewModel?.getSettings()?.isVisualMediaMode() == true ->
                    setupVisualMediaView(contentView)
                viewModel?.getSettings()?.mode == Mode.AUDIO ->
                    setupAudiosView(contentView)
                viewModel?.getSettings()?.mode == Mode.DOCUMENT ->
                    setupDocumentsView(contentView)
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

        selectButton?.setOnClickListener(null)

        super.onDestroy()

        eventListener?.onDestroy()
        eventListener = null
    }

    private fun setupHeaderView() {
        headerView?.setTitle(R.string.bazaar_all_media)

        headerView?.setOnTitleButtonClickListener {
            viewModel?.onHeaderViewTitleClicked()
        }

        headerView?.setOnCloseButtonClickListener { dismiss() }
    }

    private fun setupInfoView() {
//        infoView.text = "* Максимально количество файлов для выбора - " + viewModel.getSettings().maxSelectionCount
    }

    private fun setupVisualMediaView(recyclerView: RecyclerView) {
        if (visualMediaAdapterManager == null) {
            visualMediaAdapterManager = VisualMediaAdapterManager(requireContext(), recyclerView)
            visualMediaAdapterManager?.create(
                isCameraEnabled = viewModel?.getSettings()?.isCameraShouldBeAvailable() == true,
                isChooseFromLibraryEnabled = viewModel?.getSettings()?.isLocalMediaSearchAndSelectEnabled == true,
                visualMediaHeaderAdapterCallback = this,
                visualMediaAdapterCallback = this
            )
        }
    }

    private fun setupAudiosView(recyclerView: RecyclerView) {
        if (audiosAdapterManager == null) {
            audiosAdapterManager = AudiosAdapterManager(requireContext(), recyclerView)
            audiosAdapterManager?.create(
                isChooseFromLibraryEnabled = viewModel?.getSettings()?.isLocalMediaSearchAndSelectEnabled == true,
                audiosHeaderAdapterCallback = this,
                audiosAdapterCallback = this
            )
        }
    }

    private fun setupDocumentsView(recyclerView: RecyclerView) {
        if (documentsAdapterManager == null) {
            documentsAdapterManager = DocumentsAdapterManager(requireContext(), recyclerView)
            documentsAdapterManager?.create(
                isChooseFromLibraryEnabled = viewModel?.getSettings()?.isLocalMediaSearchAndSelectEnabled == true,
                documentsHeaderAdapterCallback = this,
                documentsAdapterCallback = this
            )
        }
    }

    private fun setupSelectButton(selectedMediaCount: Int = 0) {
        selectButton?.setText(
            title = getString(R.string.bazaar_select),
            subtitle = if (selectedMediaCount == 0) {
                getString(R.string.bazaar_nothing_selected)
            } else {
                resources.getQuantityString(R.plurals.bazaar_selected_files_count, selectedMediaCount, selectedMediaCount)
            }
        )

        if (selectButton?.hasOnClickListeners() == false) {
            selectButton?.setOnClickListener {
                viewModel?.onSubmitSelectMediaRequested()
            }
        }
    }

    private fun setupFoldersView(recyclerView: RecyclerView) {
        foldersAdapterManager = FoldersAdapterManager(requireContext(), recyclerView)
        foldersAdapterManager?.hide()
        foldersAdapterManager?.create(
            type = if (viewModel?.getSettings()?.mode == Mode.AUDIO) {
                FoldersAdapterManager.Type.LIST
            } else {
                FoldersAdapterManager.Type.GRID
            },
            isCoverEnabled = viewModel?.getSettings()?.isVisualMediaMode() == true
        ) {
            viewModel?.onFolderClicked(it)
        }
    }

    private fun setupProgressView() {
        progressView?.visibility = View.GONE
        cancelButton?.setOnClickListener { viewModel?.onCancelMediaSelectionRequested() }
    }

    private fun observeScreenState() {
        viewModel?.getScreenState()?.observe(viewLifecycleOwner, { state ->
            when (state) {
                MediaStoreScreen.State.IDLE -> {
                    selectButton?.isEnabled = true
                    progressView?.visibility = View.GONE
                }
                MediaStoreScreen.State.LOADING -> {
                    selectButton?.isEnabled = false
                    progressView?.visibility = View.VISIBLE
                }
                else -> {
                    selectButton?.isEnabled = true
                    progressView?.visibility = View.GONE
                }
            }
        })
    }

    private fun observeAction() {
        viewModel?.getAction()?.observe(viewLifecycleOwner, { action ->
            when (action) {
                is MediaStoreScreen.Action.SubmitSelectedMedia -> {
                    resultCallback?.onGalleryMediaResult(action.media)
                    dismiss()
                }
                is MediaStoreScreen.Action.SubmitSelectedContent -> {
                    resultCallback?.onGalleryContentsResult(action.content)
                    dismiss()
                }
                is MediaStoreScreen.Action.ChooseBetweenTakePictureOrVideo -> {
                    MaterialAlertDialogBuilder(requireContext(), R.style.Bazaar_AlertDialogTheme)
                        .setTitle(R.string.bazaar_action_selection)
                        .setSingleChoiceItems(arrayOf(getString(R.string.bazaar_take_picture), getString(R.string.bazaar_take_video)), -1) { dialog, which ->
                            dialog.dismiss()
                            if (which == 0) {
                                viewModel?.onChoiceMadeBetweenTakePictureOrVideo(MediaStoreScreen.Action.TakePicture::class)
                            } else if (which == 1) {
                                viewModel?.onChoiceMadeBetweenTakePictureOrVideo(MediaStoreScreen.Action.TakeVideo::class)
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
                    resultCallback?.onMediaResult(action.image)
                    dismiss()
                }
                // Multiple local media images selection
                is MediaStoreScreen.Action.SelectLocalMediaImages -> {
                    getLocalMediaImages.launch("image/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaImagesResult -> {
                    resultCallback?.onMediaResult(action.images)
                    dismiss()
                }
                // Local media video selection
                is MediaStoreScreen.Action.SelectLocalMediaVideo -> {
                    getLocalMediaVideo.launch("video/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaVideoResult -> {
                    resultCallback?.onMediaResult(action.video)
                    dismiss()
                }
                // Multiple local media videos selection
                is MediaStoreScreen.Action.SelectLocalMediaVideos -> {
                    getLocalMediaVideos.launch("video/*")
                }
                is MediaStoreScreen.Action.SelectedLocalMediaVideosResult -> {
                    resultCallback?.onMediaResult(action.videos)
                    dismiss()
                }
                // Local media image or video selection
                is MediaStoreScreen.Action.SelectLocalMediaImageOrVideo -> {
                    getLocalMediaImageOrVideo.launch(arrayOf("image/*", "video/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaImageOrVideoResult -> {
                    resultCallback?.onMediaResult(action.media)
                    dismiss()
                }
                // Multiple local media images and videos selection
                is MediaStoreScreen.Action.SelectLocalMediaImagesAndVideos -> {
                    getLocalMediaImagesAndVideos.launch(arrayOf("image/*", "video/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaImagesAndVideosResult -> {
                    resultCallback?.onMediaResult(action.media)
                    dismiss()
                }
                // Local media audio selection
                is MediaStoreScreen.Action.SelectLocalMediaAudio -> {
                    getLocalMediaAudio.launch(arrayOf("audio/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaAudio -> {
                    resultCallback?.onMediaResult(action.audio)
                    dismiss()
                }
                // Multiple local media audio selection
                is MediaStoreScreen.Action.SelectLocalMediaAudios -> {
                    getLocalMediaAudios.launch(arrayOf("audio/*"))
                }
                is MediaStoreScreen.Action.SelectedLocalMediaAudios -> {
                    resultCallback?.onContentsResult(action.audios)
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
                    resultCallback?.onContentResult(action.document)
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
                    resultCallback?.onContentsResult(action.documents)
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
        viewModel?.getSelectedMedia()?.observe(viewLifecycleOwner, { media ->
            Logger.debug(TAG, "getSelectedMedia() -> media.size: ${media.size}")
            setupSelectButton(media.size)
        })
    }

    private fun observeDisplayedMedia() {
        viewModel?.getDisplayedMedia()?.observe(viewLifecycleOwner, { uiContents: List<UIContent> ->
            if (viewModel?.getSettings()?.isVisualMediaMode() == true) {
                visualMediaAdapterManager?.submitList(uiContents)
            } else if (viewModel?.getSettings()?.mode == Mode.AUDIO) {
                audiosAdapterManager?.submitList(uiContents)
            }
        })
    }

    private fun observeDisplayedFolders() {
        viewModel?.getDisplayedFolders()?.observe(viewLifecycleOwner, { folders ->
            foldersAdapterManager?.submitList(folders)
        })
    }

    private fun observeIsFoldersDisplayed() {
        viewModel?.getIsFoldersDisplayed()?.observe(viewLifecycleOwner, { isFoldersDisplayed ->
            if (isFoldersDisplayed) {
                headerView?.toggleIcon(true)
                foldersAdapterManager?.show()

                (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                headerView?.toggleIcon(false)
                foldersAdapterManager?.hide()

                if (viewModel?.getSettings()?.isVisualMediaMode() == true) {
                    visualMediaAdapterManager?.scrollToTop()
                } else if (viewModel?.getSettings()?.mode == Mode.AUDIO) {
                    audiosAdapterManager?.scrollToTop()
                }
            }
        })
    }

    private fun observeActiveFolder() {
        viewModel?.getActiveFolder()?.observe(viewLifecycleOwner, { uiFolder ->
            val displayName = uiFolder.getDisplayName(requireContext())
            headerView?.setTitle(displayName)
        })
    }

    /**
     * [VisualMediaHeaderAdapter.Callback] implementation
     */

    override fun onCameraClicked() {
        viewModel?.onCameraShotRequested()
    }

    override fun onChooseFromLibraryClicked() {
        viewModel?.onSelectLocalMediaRequested()
    }

    /**
     * [VisualMediaAdapter.Callback] implementation
     */

    override fun onImageClicked(imageView: ShapeableImageView, uiMedia: UIMedia) {
        val uri = uiMedia.media.uri
        if (uri == null) {
            Toast.makeText(context, R.string.bazaar_error_file_not_found, Toast.LENGTH_SHORT).show()
        } else {
            MuseumDialogFragment.Builder()
                .setPainting(
                    Painting(
                        uri = uri,
                        info = Painting.Info(
                            title = uiMedia.getDisplayTitle(),
                            subtitle = uiMedia.media.folder?.displayName
                        )
                    )
                )
                .setImageView(imageView)
                .setFooterViewEnabled(true)
                .showSafely(childFragmentManager)
        }
    }

    override fun onImageCheckboxClicked(uiMedia: UIMedia) {
        viewModel?.onMediaCheckboxClicked(uiMedia)
    }

    override fun onVideoClicked(imageView: ShapeableImageView, uiMedia: UIMedia) {
        val uri = uiMedia.media.uri
        if (uri == null) {
            Toast.makeText(context, R.string.bazaar_error_file_not_found, Toast.LENGTH_SHORT).show()
        } else {
            CinemaDialogFragment.Builder()
                .setMovie(
                    Movie(
                        uri = uri,
                        info = Movie.Info(
                            title = uiMedia.getDisplayTitle(),
                            subtitle = uiMedia.media.folder?.displayName
                        )
                    )
                )
                .setScreenView(imageView)
                .setFooterViewEnabled(true)
                .showSafely(childFragmentManager)
        }
    }

    override fun onVideoCheckboxClicked(uiMedia: UIMedia) {
        viewModel?.onMediaCheckboxClicked(uiMedia)
    }

    /**
     * [AudiosAdapter.Callback] implementation
     */

    override fun onAudioPlayOrPauseClicked(uiContent: UIContent) {
        if (uiContent.content !is Audio) {
            Toast.makeText(requireContext(), "Cannot perform operation", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = uiContent.content.uri

        if (uri == null) {
            Toast.makeText(requireContext(), "Cannot perform operation", Toast.LENGTH_SHORT).show()
            return
        }

        fun set() {
            titleView?.text = uiContent.getDisplayTitle()

            val folderDisplayName = uiContent.content.folder?.displayName
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
            Logger.debug(TAG, "playOrPause() -> $uiContent")

            if (radio == null) {
                radio = Radio.Builder(requireContext())
                    .create(object : Radio.Listener {
                        override fun onIsPlayingStateChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                Logger.debug(TAG, "onPlay() -> $currentPlayingAudio")

                                playOrPauseButton?.setIconResource(R.drawable.bazaar_ic_pause)

                                val currentPlayingAudio: UIContent? = currentPlayingAudio

                                if (currentPlayingAudio == null) {
                                    // Ignored
                                } else {
                                    audiosAdapterManager?.setPlaying(currentPlayingAudio, isPlaying = true)
                                }
                            } else {
                                Logger.debug(TAG, "onPause() -> $currentPlayingAudio")

                                playOrPauseButton?.setIconResource(R.drawable.bazaar_ic_play)

                                val currentPlayingAudio: UIContent? = currentPlayingAudio

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
            if (radio?.currentSource == uri) {
                radio?.playOrPause()
            } else {
                // The other audio required to play
                val currentPlayingAudio: UIContent? = currentPlayingAudio
                if (currentPlayingAudio == null) {
                    radio?.start(uri)
                } else {
                    radio?.release()
                    audiosAdapterManager?.setPlaying(currentPlayingAudio, isPlaying = false)
                    radio?.start(uri)
                }
                this@MediaStoreFragment.currentPlayingAudio = uiContent
            }
        }

        if (audioPlayerViewStubInflatedView == null) {
            audioPlayerViewStub?.setOnInflateListener { _, inflated ->
                audioPlayerViewStubInflatedView = inflated
                playOrPauseButton = inflated?.findViewById(R.id.playOrPauseButton)
                titleView = inflated?.findViewById(R.id.titleView)
                subtitleView = inflated?.findViewById(R.id.subtitleView)
                closeButton = inflated?.findViewById(R.id.closeButton)

                set()
                playOrPause()
            }

            audioPlayerViewStub?.inflate()
        } else {
            if (audioPlayerViewStubInflatedView?.visibility != View.VISIBLE) {
                audioPlayerViewStubInflatedView?.visibility = View.VISIBLE
            }

            set()
            playOrPause()
        }
    }

    override fun onAudioClicked(uiContent: UIContent) {
        viewModel?.onMediaCheckboxClicked(uiContent)
    }

    /**
     * [DocumentsAdapter.Callback] implementation
     */

    override fun onDocumentIconClicked(uiContent: UIContent) {
        val file = uiContent.content.publicFile?.getFile()
        if (file == null || !file.exists()) {
            return Toast.makeText(context, R.string.bazaar_error_file_not_found, Toast.LENGTH_SHORT).show()
        }
        when (val action = file.open(requireContext())) {
            is OpenFile.Success -> {
                if (!action.tryToOpen(requireContext())) {
                    Toast.makeText(context, R.string.bazaar_error_file_not_found, Toast.LENGTH_SHORT).show()
                }
            }
            is OpenFile.Error -> {
                Toast.makeText(context, R.string.bazaar_error_file_not_found, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDocumentClicked(uiContent: UIContent) {
        viewModel?.onMediaCheckboxClicked(uiContent)
    }

    // Calculates height for 90% of fullscreen
    private fun getBottomSheetDialogDefaultHeight(): Int? {
        val height = activity?.computeCurrentWindowSize()?.height ?: return null
        return height * 90 / 100
    }

    /**
     * [ActivityResultContracts]
     */

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        viewModel?.onPictureTaken(isSuccess)
    }

    private val takeVideo = registerForActivityResult(TakeVideoContract()) { uri ->
        Logger.debug(TAG, "uri: $uri")
        viewModel?.onVideoTaken(uri)
    }

    private val getLocalMediaImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel?.onLocalMediaImageSelected(uri)
    }

    private val getLocalMediaImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        viewModel?.onLocalMediaImagesSelected(uris)
    }

    private val getLocalMediaVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel?.onLocalMediaVideoSelected(uri)
    }

    private val getLocalMediaVideos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        viewModel?.onLocalMediaVideosSelected(uris)
    }

    private val getLocalMediaImageOrVideo = registerForActivityResult(GetContentContract()) { uri ->
        viewModel?.onLocalMediaImageOrVideoSelected(uri)
    }

    private val getLocalMediaImagesAndVideos = registerForActivityResult(GetMultipleContentsContract()) { uris ->
        viewModel?.onLocalMediaImagesAndVideosSelected(uris)
    }

    private val getLocalMediaAudio = registerForActivityResult(GetContentContract()) { uri ->
        viewModel?.onLocalMediaAudioSelected(uri)
    }

    private val getLocalMediaAudios = registerForActivityResult(GetMultipleContentsContract()) { uris ->
        viewModel?.onLocalMediaAudiosSelected(uris)
    }

    private val getLocalDocument = registerForActivityResult(GetContentContract()) { uri ->
        viewModel?.onLocalDocumentSelected(uri)
    }

    private val getLocalDocuments = registerForActivityResult(GetMultipleContentsContract()) { uris ->
        viewModel?.onLocalDocumentsSelected(uris)
    }

}