package kz.zhombie.bazaar.ui.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.model.*
import kz.zhombie.bazaar.core.cache.Cache
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.core.media.MediaScanManager
import kz.zhombie.bazaar.ui.model.*
import kz.zhombie.bazaar.ui.model.UIFolder
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.ui.model.UIMultimedia
import kz.zhombie.bazaar.ui.model.onlyImages
import kotlin.reflect.KClass

internal class MediaStoreViewModel : ViewModel() {

    companion object {
        private val TAG: String = MediaStoreViewModel::class.java.simpleName
    }

    private lateinit var settings: MediaStoreScreen.Settings
    private lateinit var mediaScanManager: MediaScanManager

    private val allMedia = mutableListOf<Multimedia>()

    private val screenState by lazy { MutableLiveData<MediaStoreScreen.State>() }
    fun getScreenState(): LiveData<MediaStoreScreen.State> = screenState

    private val action by lazy { MutableLiveData<MediaStoreScreen.Action>() }
    fun getAction(): LiveData<MediaStoreScreen.Action> = action

    private val selectedMedia by lazy { MutableLiveData<List<UIMultimedia>>() }
    fun getSelectedMedia(): LiveData<List<UIMultimedia>> = selectedMedia

    private val displayedMedia by lazy { MutableLiveData<List<UIMultimedia>>() }
    fun getDisplayedMedia(): LiveData<List<UIMultimedia>> = displayedMedia

    private val displayedFolders by lazy { MutableLiveData<List<UIFolder>>() }
    fun getDisplayedFolders(): LiveData<List<UIFolder>> = displayedFolders

    private val isFoldersDisplayed by lazy { MutableLiveData(false) }
    fun getIsFoldersDisplayed(): LiveData<Boolean> = isFoldersDisplayed

    private val activeFolder by lazy { MutableLiveData<UIFolder>() }
    fun getActiveFolder(): LiveData<UIFolder> = activeFolder

    private var takePictureInput: Image? = null
    private var takeVideoInput: Video? = null

    private var selectionJob: Job? = null

    init {
        Logger.d(TAG, "created")
    }

    fun getSettings(): MediaStoreScreen.Settings {
        return settings
    }

    fun setSettings(settings: MediaStoreScreen.Settings) {
        this.settings = settings
    }

    fun setMediaScanManager(mediaScanManager: MediaScanManager) {
        if (!this::mediaScanManager.isInitialized) {
            this.mediaScanManager = mediaScanManager

            onStart()
        }
    }

    private fun onStart() {
        if (allMedia.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val localData = when (settings.mode) {
                    Mode.IMAGE -> {
                        val images = try {
                            Cache.getInstance().getMedia()?.filterIsInstance<Image>()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        if (images.isNullOrEmpty()) {
                            mediaScanManager.loadLocalMediaImages(Dispatchers.IO)
                        } else {
                            images
                        }
                    }
                    Mode.VIDEO -> {
                        val videos = try {
                            Cache.getInstance().getMedia()?.filterIsInstance<Video>()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        if (videos.isNullOrEmpty()) {
                            mediaScanManager.loadLocalMediaVideos(Dispatchers.IO)
                        } else {
                            videos
                        }
                    }
                    Mode.IMAGE_AND_VIDEO -> {
                        val imagesAndVideos = try {
                            Cache.getInstance().getMedia()?.filter { it is Image || it is Video }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        if (imagesAndVideos.isNullOrEmpty()) {
                            mediaScanManager.loadLocalMediaImagesAndVideos(Dispatchers.IO)
                        } else {
                            imagesAndVideos
                        }
                    }
                    Mode.AUDIO -> {
                        val audios = try {
                            Cache.getInstance().getMultimedia()?.filterIsInstance<Audio>()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        if (audios.isNullOrEmpty()) {
                            mediaScanManager.loadLocalMediaAudios(Dispatchers.IO)
                        } else {
                            audios
                        }
                    }
                    Mode.DOCUMENT -> {
                        onSelectLocalMediaRequested()
                        null
                    }
                }
                if (localData != null) {
                    onLocalDataLoaded(localData)
                }
            }
        }
    }

    private fun onLocalDataLoaded(multimedia: List<Multimedia>) {
        viewModelScope.launch(Dispatchers.IO) {
            Logger.d(TAG, "onLocalDataLoaded() -> multimedia.size: ${multimedia.size}")

            allMedia.addAll(multimedia)

            val uiMultimedia: List<UIMultimedia> = multimedia.mapNotNull {
                when (it) {
                    is Media ->
                        UIMedia(it, isSelectable = true, isSelected = false, isVisible = true)
                    is Audio ->
                        UIMultimedia(it, isSelectable = true, isSelected = false, isVisible = true)
                    else ->
                        null
                }
            }

            val defaultFolder = when (settings.mode) {
                Mode.IMAGE -> uiMultimedia.onlyImages()
                Mode.VIDEO -> uiMultimedia.onlyVideos()
                Mode.IMAGE_AND_VIDEO -> uiMultimedia.onlyMedia()
                Mode.AUDIO -> uiMultimedia.onlyAudios()
                Mode.DOCUMENT -> uiMultimedia.onlyDocuments()
            }

            activeFolder.postValue(defaultFolder)
            displayedMedia.postValue(uiMultimedia)

            val folders = uiMultimedia.convert(multimedia)
                .distinctBy { it.folder.id }
                .sortedBy { it.folder.displayName }
                .toMutableList()

            folders.add(0, defaultFolder)

            displayedFolders.postValue(folders)
        }
    }

    fun onMediaCheckboxClicked(uiMultimedia: UIMultimedia) {
        viewModelScope.launch(Dispatchers.IO) {
            Logger.d(TAG, "onMediaCheckboxClicked() -> uiMultimedia: $uiMultimedia")

            // Selected
            var selectedMediaSize = 0
            with(selectedMedia.value?.toMutableList() ?: mutableListOf()) {
                val index = indexOfFirst {
                    if (uiMultimedia is UIMedia) {
                        if (it is UIMedia) {
                            it.media.id == uiMultimedia.media.id
                        } else {
                            false
                        }
                    } else {
                        it.multimedia.id == uiMultimedia.multimedia.id
                    }
                }
                Logger.d(TAG, "onMediaCheckboxClicked() -> [SELECTED] index: $index")
                if (index > -1) {
                    removeAt(index)
                } else {
                    add(uiMultimedia)
                }
                selectedMediaSize = size
                Logger.d(TAG, "onMediaCheckboxClicked() -> [SELECTED] selectedMediaSize: $selectedMediaSize")
                selectedMedia.postValue(this)
            }

            // All
            with(displayedMedia.value?.toMutableList() ?: mutableListOf()) {
                indexOfFirst {
                    if (uiMultimedia is UIMedia) {
                        if (it is UIMedia) {
                            it.media.id == uiMultimedia.media.id
                        } else {
                            false
                        }
                    } else {
                        it.multimedia.id == uiMultimedia.multimedia.id
                    }
                }
                    .takeIf { index -> index > -1 }
                    ?.let { index ->
                        Logger.d(TAG, "onMediaCheckboxClicked() -> [ALL] index: $index")

                        this[index] = this[index].copy(isSelected = !this[index].isSelected)

                        if (selectedMediaSize >= settings.maxSelectionCount) {
                            forEachIndexed { eachIndex, eachUIMedia ->
                                if (eachUIMedia.isSelected) {
                                    // Ignored
                                } else {
                                    this[eachIndex] = this[eachIndex].copy(isSelectable = false)
                                }
                            }
                        } else {
                            if (any { !it.isSelectable }) {
                                forEachIndexed { eachIndex, _ ->
                                    this[eachIndex] = this[eachIndex].copy(isSelectable = true)
                                }
                            }
                        }

                        displayedMedia.postValue(this)
                    }

            }
        }
    }

    fun onPreviewPictureVisibilityChange(id: Long, isVisible: Boolean, delayDuration: Long) {
        Logger.d(TAG, "onPreviewPictureVisibilityChange() -> id: $id, isVisible: $isVisible")
        viewModelScope.launch(Dispatchers.IO) {
            delay(delayDuration)

            with(displayedMedia.value?.toMutableList() ?: mutableListOf()) {
                indexOfFirst {
                    if (it is UIMedia) {
                        id == it.media.id
                    } else {
                        id == it.multimedia.id
                    }
                }
                    .takeIf { index -> index > -1 }
                    ?.let { index ->
                        this[index] = this[index].copy(isVisible = isVisible)
                        displayedMedia.postValue(this)
                    }
            }
        }
    }

    fun onHeaderViewTitleClicked() {
        isFoldersDisplayed.postValue(!(isFoldersDisplayed.value ?: false))
    }

    fun onFolderClicked(uiFolder: UIFolder) {
        viewModelScope.launch(Dispatchers.IO) {
            Logger.d(TAG, "onFolderClicked() -> uiFolder: ${uiFolder.folder.displayName}")

            val folderUiMedia = allMedia
                .filter {
                    if (uiFolder.folder.id == UIFolder.ALL_MEDIA_ID) {
                        true
                    } else {
                        it.folderId == uiFolder.folder.id
                    }
                }
                .mapNotNull {
                    when (it) {
                        is Media ->
                            UIMedia(it, isSelectable = true, isSelected = false, isVisible = true)
                        is Audio ->
                            UIMultimedia(it, isSelectable = true, isSelected = false, isVisible = true)
                        else ->
                            null
                    }
                }
                .toMutableList()

            val selectedMedia = selectedMedia.value ?: emptyList()
            selectedMedia.forEach { eachSelectedMedia ->
                val index = folderUiMedia.indexOfFirst {
                    if (it is UIMedia) {
                        it.media.id == eachSelectedMedia.multimedia.id
                    } else {
                        it.multimedia.id == eachSelectedMedia.multimedia.id
                    }
                }
                if (index > -1) {
                    folderUiMedia[index] = folderUiMedia[index].copy(isSelected = true)
                }
            }

            if (selectedMedia.size >= settings.maxSelectionCount) {
                folderUiMedia.forEachIndexed { eachIndex, eachFolderUIMedia ->
                    if (eachFolderUIMedia.isSelected) {
                        // Ignored
                    } else {
                        folderUiMedia[eachIndex] = folderUiMedia[eachIndex].copy(isSelectable = false)
                    }
                }
            } else {
                if (folderUiMedia.any { !it.isSelectable }) {
                    folderUiMedia.forEachIndexed { eachIndex, _ ->
                        folderUiMedia[eachIndex] = folderUiMedia[eachIndex].copy(isSelectable = true)
                    }
                }
            }

            activeFolder.postValue(uiFolder)
            displayedMedia.postValue(folderUiMedia)

            isFoldersDisplayed.postValue(false)
        }
    }

    fun onCameraShotRequested() {
        Logger.d(TAG, "onCameraShotRequested()")
        if (settings.cameraSettings.isAnyCameraActionEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                // TODO: ActivityResultContracts.TakeVideo returns null,
                //  that's why after fix add an ability to record a video by camera
//                if (settings.mode == Mode.IMAGE) {
//                    val takePictureInput = mediaScanManager.createCameraPictureInputTempFile()
//                    this@MediaStoreViewModel.takePictureInput = takePictureInput
//                    Logger.d(TAG, "takePictureInput: $takePictureInput")
//                    if (takePictureInput != null) {
//                        action.postValue(MediaStoreScreen.Action.TakePicture(takePictureInput.uri))
//                    }
//                } else if (settings.mode == Mode.VIDEO) {
//                    val takeVideoInput = mediaScanManager.createCameraVideoInputTempFile()
//                    this@MediaStoreViewModel.takeVideoInput = takeVideoInput
//                    Logger.d(TAG, "takeVideoInput: $takeVideoInput")
//                    if (takeVideoInput != null) {
//                        action.postValue(MediaStoreScreen.Action.TakeVideo(takeVideoInput.uri))
//                    }
//                } else if (settings.mode == Mode.IMAGE_AND_VIDEO) {
//                    action.postValue(MediaStoreScreen.Action.ChooseBetweenTakePictureOrVideo)
//                }
                val takePictureInput = mediaScanManager.createCameraPictureInputTempFile(Dispatchers.IO)
                this@MediaStoreViewModel.takePictureInput = takePictureInput
                Logger.d(TAG, "takePictureInput: $takePictureInput")
                if (takePictureInput != null) {
                    action.postValue(MediaStoreScreen.Action.TakePicture(takePictureInput.uri))
                }
            }
        }
    }

    fun onChoiceMadeBetweenTakePictureOrVideo(kClass: KClass<*>) {
        if (settings.cameraSettings.isAnyCameraActionEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                if (kClass == MediaStoreScreen.Action.TakePicture::class) {
                    val takePictureInput = mediaScanManager.createCameraPictureInputTempFile()
                    this@MediaStoreViewModel.takePictureInput = takePictureInput
                    Logger.d(TAG, "takePictureInput: $takePictureInput")
                    if (takePictureInput != null) {
                        action.postValue(MediaStoreScreen.Action.TakePicture(takePictureInput.uri))
                    }
                } else if (kClass == MediaStoreScreen.Action.TakeVideo::class) {
                    val takeVideoInput = mediaScanManager.createCameraVideoInputTempFile()
                    this@MediaStoreViewModel.takeVideoInput = takeVideoInput
                    Logger.d(TAG, "takeVideoInput: $takeVideoInput")
                    if (takeVideoInput != null) {
                        action.postValue(MediaStoreScreen.Action.TakeVideo(takeVideoInput.uri))
                    }
                }
            }
        }
    }

    fun onSelectLocalMediaRequested() {
        Logger.d(TAG, "onSelectLocalMediaRequested()")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                when (settings.mode) {
                    Mode.IMAGE -> {
                        if (settings.maxSelectionCount == 1) {
                            action.postValue(MediaStoreScreen.Action.SelectLocalMediaImage)
                        } else {
                            action.postValue(MediaStoreScreen.Action.SelectLocalMediaImages)
                        }
                    }
                    Mode.VIDEO -> {
                        if (settings.maxSelectionCount == 1) {
                            action.postValue(MediaStoreScreen.Action.SelectLocalMediaVideo)
                        } else {
                            action.postValue(MediaStoreScreen.Action.SelectLocalMediaVideos)
                        }
                    }
                    Mode.IMAGE_AND_VIDEO -> {
                        if (settings.maxSelectionCount == 1) {
                            action.postValue(MediaStoreScreen.Action.SelectLocalMediaImageOrVideo)
                        } else {
                            action.postValue(MediaStoreScreen.Action.SelectLocalMediaImagesAndVideos)
                        }
                    }
                    Mode.AUDIO -> {
                        if (settings.maxSelectionCount == 1) {
                            action.postValue(MediaStoreScreen.Action.SelectLocalMediaAudio)
                        } else {
                            action.postValue(MediaStoreScreen.Action.SelectLocalMediaAudios)
                        }
                    }
                    Mode.DOCUMENT -> {
                        if (settings.maxSelectionCount == 1) {
                            action.postValue(MediaStoreScreen.Action.SelectLocalDocument)
                        } else {
                            action.postValue(MediaStoreScreen.Action.SelectLocalDocuments)
                        }
                    }
                }
            }
        }
    }

    fun onPictureTaken(isSuccess: Boolean) {
        Logger.d(TAG, "onPictureTaken() -> isSuccess: $isSuccess")
        if (settings.cameraSettings.isAnyCameraActionEnabled) {
            if (!isSuccess) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                var image = takePictureInput
                Logger.d(TAG, "takenPictureInput: $image")
                if (image != null) {
                    image = mediaScanManager.decodeFile(Dispatchers.IO, image)
                    Logger.d(TAG, "takenPictureInput: $image")
                    action.postValue(MediaStoreScreen.Action.TakenPictureResult(image))
                    takePictureInput = null
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onVideoTaken(bitmap: Bitmap?) {
        Logger.d(TAG, "onVideoTaken() -> bitmap: $bitmap")
        if (settings.cameraSettings.isAnyCameraActionEnabled) {
            if (bitmap == null) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val video = takeVideoInput
                Logger.d(TAG, "takenVideoInput: $video")
                if (video != null) {
                    action.postValue(MediaStoreScreen.Action.TakenVideoResult(video))
                    takeVideoInput = null
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaImageSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaImageSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val image = mediaScanManager.loadSelectedLocalMediaImage(Dispatchers.IO, uri)
                Logger.d(TAG, "loadSelectedLocalMediaImage() -> image: $image")
                if (image == null) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaImageResult(image))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaImagesSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaImagesSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val allowedUris = uris.take(settings.maxSelectionCount)
                val images = mediaScanManager.loadSelectedLocalMediaImages(Dispatchers.IO, allowedUris)
                Logger.d(TAG, "loadSelectedLocalMediaImages() -> images: $images")
                if (images.isNullOrEmpty()) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaImagesResult(images))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaVideoSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaVideoSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val video = mediaScanManager.loadSelectedLocalMediaVideo(Dispatchers.IO, uri)
                Logger.d(TAG, "loadSelectedLocalMediaVideo() -> video: $video")
                if (video == null) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaVideoResult(video))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaVideosSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaVideosSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val allowedUris = uris.take(settings.maxSelectionCount)
                val videos = mediaScanManager.loadSelectedLocalMediaVideos(Dispatchers.IO, allowedUris)
                Logger.d(TAG, "loadSelectedLocalMediaVideos() -> videos: $videos")
                if (videos.isNullOrEmpty()) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaVideosResult(videos))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaImageOrVideoSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaImageOrVideoSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val media = mediaScanManager.loadSelectedLocalMediaImageOrVideo(Dispatchers.IO, uri)
                if (media == null) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaImageOrVideoResult(media))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaImagesAndVideosSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaImagesAndVideosSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val allowedUris = uris.take(settings.maxSelectionCount)
                val media = mediaScanManager.loadSelectedLocalMediaImagesAndVideos(Dispatchers.IO, allowedUris)
                if (media.isNullOrEmpty()) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaImagesAndVideosResult(media))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaAudioSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaAudioSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val audio = mediaScanManager.loadSelectedLocalMediaAudio(Dispatchers.IO, uri)
                if (audio == null) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaAudio(audio))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaAudiosSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaAudiosSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val allowedUris = uris.take(settings.maxSelectionCount)
                val audios = mediaScanManager.loadSelectedLocalMediaAudios(Dispatchers.IO, allowedUris)
                if (audios.isNullOrEmpty()) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaAudios(audios))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalDocumentSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalDocumentSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val document = mediaScanManager.loadSelectedLocalDocument(Dispatchers.IO, uri)
                if (document == null) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalDocument(document))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalDocumentsSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalDocumentsSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            selectionJob = viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val documents = mediaScanManager.loadSelectedLocalDocuments(Dispatchers.IO, uris)
                if (documents.isNullOrEmpty()) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalDocuments(documents))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onSubmitSelectMediaRequested() {
        selectionJob = viewModelScope.launch(Dispatchers.IO) {
            screenState.postValue(MediaStoreScreen.State.LOADING)

            if (settings.isVisualMediaMode()) {
                val selectedMedia: List<Media> = (selectedMedia.value ?: emptyList())
                    .take(settings.maxSelectionCount)
                    .mapNotNull {
                        if (it is UIMedia) {
                            when (it.media) {
                                is Image -> {
                                    val image = mediaScanManager.loadSelectedLocalMediaImage(Dispatchers.IO, it.media.uri)
                                    it.media.complete(image)
                                }
                                is Video -> {
                                    val video = mediaScanManager.loadSelectedLocalMediaVideo(Dispatchers.IO, it.media.uri)
                                    it.media.complete(video)
                                }
                                else -> null
                            }
                        } else {
                            null
                        }
                    }

                action.postValue(MediaStoreScreen.Action.SubmitSelectedMedia(selectedMedia))
            } else if (settings.isAudibleMediaMode()) {
                val selectedMedia = (selectedMedia.value ?: emptyList())
                    .take(settings.maxSelectionCount)
                    .mapNotNull {
                        when (it.multimedia) {
                            is Audio -> {
                                val audio = mediaScanManager.loadSelectedLocalMediaAudio(Dispatchers.IO, it.multimedia.uri)
                                it.multimedia.complete(audio)
                            }
                            else -> null
                        }
                    }

                action.postValue(MediaStoreScreen.Action.SubmitSelectedMultimedia(selectedMedia))
            }

            screenState.postValue(MediaStoreScreen.State.CONTENT)
        }
    }

    fun onCancelMediaSelectionRequested() {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                selectionJob?.cancelAndJoin()
                selectionJob = null
            }
            screenState.postValue(MediaStoreScreen.State.CONTENT)
        }
    }

}