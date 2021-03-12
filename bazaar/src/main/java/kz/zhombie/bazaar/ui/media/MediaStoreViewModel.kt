package kz.zhombie.bazaar.ui.media

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvasilkov.gestures.animation.ViewPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.zhombie.bazaar.api.core.settings.Mode
import kz.zhombie.bazaar.api.model.Folder
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import kz.zhombie.bazaar.core.media.MediaScanManager
import kz.zhombie.bazaar.ui.model.UIFolder
import kz.zhombie.bazaar.ui.model.UIMedia
import kotlin.reflect.KClass

internal class MediaStoreViewModel : ViewModel() {

    companion object {
        private val TAG: String = MediaStoreViewModel::class.java.simpleName
    }

    private lateinit var settings: MediaStoreScreen.Settings
    private lateinit var mediaScanManager: MediaScanManager

    private val allMedia = mutableListOf<Media>()

    private val screenState by lazy { MutableLiveData<MediaStoreScreen.State>() }
    fun getScreenState(): LiveData<MediaStoreScreen.State> = screenState

    private val action by lazy { MutableLiveData<MediaStoreScreen.Action>() }
    fun getAction(): LiveData<MediaStoreScreen.Action> = action

    private val selectedMedia by lazy { MutableLiveData<List<UIMedia>>() }
    fun getSelectedMedia(): LiveData<List<UIMedia>> = selectedMedia

    private val displayedMedia by lazy { MutableLiveData<List<UIMedia>>() }
    fun getDisplayedMedia(): LiveData<List<UIMedia>> = displayedMedia

    private val displayedFolders by lazy { MutableLiveData<List<UIFolder>>() }
    fun getDisplayedFolders(): LiveData<List<UIFolder>> = displayedFolders

    private val isFoldersDisplayed by lazy { MutableLiveData(false) }
    fun getIsFoldersDisplayed(): LiveData<Boolean> = isFoldersDisplayed

    private val activeFolder by lazy { MutableLiveData<UIFolder>() }
    fun getActiveFolder(): LiveData<UIFolder> = activeFolder

    private val activeViewPosition by lazy { MutableLiveData<ViewPosition>() }
    fun getActiveViewPosition(): LiveData<ViewPosition> = activeViewPosition

    private var takePictureInput: Image? = null
    private var takeVideoInput: Video? = null

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
                when (settings.mode) {
                    Mode.IMAGE ->
                        mediaScanManager.loadLocalImages(Dispatchers.IO) {
                            onLocalMediaLoaded(it)
                        }
                    Mode.VIDEO ->
                        mediaScanManager.loadLocalVideos(Dispatchers.IO) {
                            onLocalMediaLoaded(it)
                        }
                    Mode.IMAGE_AND_VIDEO ->
                        mediaScanManager.loadLocalImagesAndVideos(Dispatchers.IO) {
                            onLocalMediaLoaded(it)
                        }
                }
            }
        }
    }

    private fun onLocalMediaLoaded(media: List<Media>) {
        viewModelScope.launch(Dispatchers.IO) {
            Logger.d(TAG, "onMediaLoaded() -> data.size: ${media.size}")

            allMedia.addAll(media)

            val uiMedia = media.map { UIMedia(it, isSelectable = true, isSelected = false, isVisible = true) }

            val defaultFolder = when (settings.mode) {
                Mode.IMAGE ->
                    UIFolder(Folder(UIFolder.ALL_MEDIA_ID, "Все фото", uiMedia.map { it.media }))
                Mode.VIDEO ->
                    UIFolder(Folder(UIFolder.ALL_MEDIA_ID, "Все видео", uiMedia.map { it.media }))
                Mode.IMAGE_AND_VIDEO ->
                    UIFolder(Folder(UIFolder.ALL_MEDIA_ID, "Все медиа", uiMedia.map { it.media }))
            }

            activeFolder.postValue(defaultFolder)
            displayedMedia.postValue(uiMedia)

            val folders = media.mapNotNull { media ->
                val folderId = media.folderId ?: return@mapNotNull null
                val folderDisplayName = media.folderDisplayName ?: return@mapNotNull null
                val items = uiMedia.mapNotNull { if (it.media.folderId == folderId) it.media else null }
                UIFolder(Folder(folderId, folderDisplayName, items))
            }
                .distinctBy { it.folder.id }
                .sortedBy { it.folder.displayName }
                .toMutableList()

            folders.add(0, defaultFolder)

            displayedFolders.postValue(folders)
        }
    }

    fun onMediaCheckboxClicked(uiMedia: UIMedia) {
        viewModelScope.launch(Dispatchers.IO) {
            Logger.d(TAG, "onImageCheckboxClicked() -> uiMedia: $uiMedia")

            // Selected
            val newSelected = mutableListOf<UIMedia>()
            val currentSelectedMedia = selectedMedia.value
            if (!currentSelectedMedia.isNullOrEmpty()) {
                newSelected.addAll(currentSelectedMedia)
            }
            val index = newSelected.indexOfFirst { it.media.id == uiMedia.media.id }
            if (index > -1) {
                newSelected.removeAll { it.media.id == uiMedia.media.id }
            } else {
                newSelected.add(uiMedia)
            }
            selectedMedia.postValue(newSelected)

            // All
            with(displayedMedia.value?.toMutableList() ?: mutableListOf()) {
                indexOfFirst { it.media.id == uiMedia.media.id }
                    .takeIf { index -> index > -1 }
                    ?.let { index ->
                        this[index] = this[index].copy(isSelected = !this[index].isSelected)

                        if (newSelected.size >= settings.maxSelectionCount) {
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

    fun onLayoutChange(viewPosition: ViewPosition) {
        activeViewPosition.postValue(viewPosition)
    }

    fun onVisibilityChange(id: Long, isVisible: Boolean, delayDuration: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(delayDuration)

            with(displayedMedia.value?.toMutableList() ?: mutableListOf()) {
                indexOfFirst { it.media.id == id }
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
                .map { UIMedia(it, isSelectable = true, isSelected = false, isVisible = true) }
                .toMutableList()

            val selectedMedia = selectedMedia.value ?: emptyList()
            selectedMedia.forEach { eachSelectedMedia ->
                val index = folderUiMedia.indexOfFirst { it.media.id == eachSelectedMedia.media.id }
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

    fun onSelectLocalMediaGalleryRequested() {
        Logger.d(TAG, "onSelectLocalMediaGalleryRequested()")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                if (settings.mode == Mode.IMAGE) {
                    if (settings.maxSelectionCount == 1) {
                        action.postValue(MediaStoreScreen.Action.SelectLocalMediaGalleryImage)
                    } else {
                        action.postValue(MediaStoreScreen.Action.SelectLocalMediaGalleryImages)
                    }
                } else if (settings.mode == Mode.VIDEO) {
                    if (settings.maxSelectionCount == 1) {
                        action.postValue(MediaStoreScreen.Action.SelectLocalMediaGalleryVideo)
                    } else {
                        action.postValue(MediaStoreScreen.Action.SelectLocalMediaGalleryVideos)
                    }
                } else if (settings.mode == Mode.IMAGE_AND_VIDEO) {
                    if (settings.maxSelectionCount == 1) {
                        action.postValue(MediaStoreScreen.Action.SelectLocalMediaGalleryImageOrVideo)
                    } else {
                        action.postValue(MediaStoreScreen.Action.SelectLocalMediaGalleryImagesOrVideos)
                    }
                }
            }
        }
    }

    fun onPictureTaken(isSuccess: Boolean) {
        Logger.d(TAG, "onPictureTaken() -> isSuccess: $isSuccess")
        if (settings.cameraSettings.isAnyCameraActionEnabled) {
            if (!isSuccess) return
            viewModelScope.launch(Dispatchers.IO) {
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
            viewModelScope.launch(Dispatchers.IO) {
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

    fun onLocalMediaGalleryImageSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaGalleryImageSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val image = mediaScanManager.loadLocalSelectedMediaGalleryImage(Dispatchers.IO, uri)
                Logger.d(TAG, "loadSelectedGalleryImage() -> image: $image")
                if (image == null) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryImageResult(image))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaGalleryImagesSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaGalleryImagesSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val allowedUris = uris.take(settings.maxSelectionCount)
                val images = mediaScanManager.loadLocalSelectedMediaGalleryImages(Dispatchers.IO, allowedUris)
                Logger.d(TAG, "loadLocalSelectedMediaGalleryImages() -> images: $images")
                if (images.isNullOrEmpty()) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryImagesResult(images))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaGalleryVideoSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaGalleryVideoSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val video = mediaScanManager.loadLocalSelectedMediaGalleryVideo(Dispatchers.IO, uri)
                Logger.d(TAG, "loadLocalSelectedMediaGalleryVideo() -> video: $video")
                if (video == null) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryVideoResult(video))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaGalleryVideosSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaGalleryVideosSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val allowedUris = uris.take(settings.maxSelectionCount)
                val videos = mediaScanManager.loadLocalSelectedMediaGalleryVideos(Dispatchers.IO, allowedUris)
                Logger.d(TAG, "loadLocalSelectedMediaGalleryVideos() -> videos: $videos")
                if (videos.isNullOrEmpty()) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryVideosResult(videos))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaGalleryImageOrVideoSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaGalleryImageOrVideoSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val media = mediaScanManager.loadLocalSelectedMediaGalleryImageOrVideo(Dispatchers.IO, uri)
                if (media == null) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryImageOrVideoResult(media))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onLocalMediaGalleryImagesOrVideosSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaGalleryImagesOrVideosSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            viewModelScope.launch(Dispatchers.IO) {
                screenState.postValue(MediaStoreScreen.State.LOADING)

                val allowedUris = uris.take(settings.maxSelectionCount)
                val media = mediaScanManager.loadLocalSelectedMediaGalleryImagesOrVideos(Dispatchers.IO, allowedUris)
                if (media.isNullOrEmpty()) {
                    action.postValue(MediaStoreScreen.Action.Empty)
                } else {
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryImagesOrVideosResult(media))
                }

                screenState.postValue(MediaStoreScreen.State.CONTENT)
            }
        }
    }

    fun onSubmitSelectMediaRequested() {
        viewModelScope.launch(Dispatchers.IO) {
            screenState.postValue(MediaStoreScreen.State.LOADING)

            val selectedMedia = (selectedMedia.value ?: emptyList())
                .take(settings.maxSelectionCount)
                .mapNotNull {
                    when (it.media) {
                        is Image ->
                            mediaScanManager.loadLocalSelectedMediaGalleryImage(Dispatchers.IO, it.media.uri)
                        is Video ->
                            mediaScanManager.loadLocalSelectedMediaGalleryVideo(Dispatchers.IO, it.media.uri)
                        else ->
                            null
                    }
                }

            action.postValue(MediaStoreScreen.Action.SubmitSelectedMedia(selectedMedia))
            screenState.postValue(MediaStoreScreen.State.CONTENT)
        }
    }

}