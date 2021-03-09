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
import kz.zhombie.bazaar.api.model.Album
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.api.model.Video
import kz.zhombie.bazaar.core.MediaScanManager
import kz.zhombie.bazaar.ui.model.UIAlbum
import kz.zhombie.bazaar.ui.model.UIMedia

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

    private val displayedAlbums by lazy { MutableLiveData<List<UIAlbum>>() }
    fun getDisplayedAlbums(): LiveData<List<UIAlbum>> = displayedAlbums

    private val isAlbumsDisplayed by lazy { MutableLiveData<Boolean>(false) }
    fun getIsAlbumsDisplayed(): LiveData<Boolean> = isAlbumsDisplayed

    private val activeAlbum by lazy { MutableLiveData<UIAlbum>() }
    fun getActiveAlbum(): LiveData<UIAlbum> = activeAlbum

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

            val defaultAlbum = when (settings.mode) {
                Mode.IMAGE ->
                    UIAlbum(Album(UIAlbum.ALL_MEDIA_ID, "Все фото", uiMedia.map { it.media }))
                Mode.VIDEO ->
                    UIAlbum(Album(UIAlbum.ALL_MEDIA_ID, "Все видео", uiMedia.map { it.media }))
                Mode.IMAGE_AND_VIDEO ->
                    UIAlbum(Album(UIAlbum.ALL_MEDIA_ID, "Все медиа", uiMedia.map { it.media }))
            }

            activeAlbum.postValue(defaultAlbum)
            displayedMedia.postValue(uiMedia)

            val albums = media.mapNotNull { media ->
                val folderId = media.folderId ?: return@mapNotNull null
                val folderDisplayName = media.folderDisplayName ?: return@mapNotNull null
                val items = uiMedia.mapNotNull { if (it.media.folderId == folderId) it.media else null }
                UIAlbum(Album(folderId, folderDisplayName, items))
            }
                .distinctBy { it.album.id }
                .sortedBy { it.album.displayName }
                .toMutableList()

            albums.add(0, defaultAlbum)

            displayedAlbums.postValue(albums)
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
        isAlbumsDisplayed.postValue(!(isAlbumsDisplayed.value ?: false))
    }

    fun onAlbumClicked(uiAlbum: UIAlbum) {
        viewModelScope.launch(Dispatchers.IO) {
            Logger.d(TAG, "onAlbumClicked() -> uiAlbum: ${uiAlbum.album.displayName}")

            val albumUiMedia = allMedia
                .filter {
                    if (uiAlbum.album.id == UIAlbum.ALL_MEDIA_ID) {
                        true
                    } else {
                        it.folderId == uiAlbum.album.id
                    }
                }
                .map { UIMedia(it, isSelectable = true, isSelected = false, isVisible = true) }
                .toMutableList()

            val selectedMedia = selectedMedia.value ?: emptyList()
            selectedMedia.forEach { eachSelectedMedia ->
                val index = albumUiMedia.indexOfFirst { it.media.id == eachSelectedMedia.media.id }
                if (index > -1) {
                    albumUiMedia[index] = albumUiMedia[index].copy(isSelected = true)
                }
            }

            if (selectedMedia.size >= settings.maxSelectionCount) {
                albumUiMedia.forEachIndexed { eachIndex, eachAlbumUIMedia ->
                    if (eachAlbumUIMedia.isSelected) {
                        // Ignored
                    } else {
                        albumUiMedia[eachIndex] = albumUiMedia[eachIndex].copy(isSelectable = false)
                    }
                }
            } else {
                if (albumUiMedia.any { !it.isSelectable }) {
                    albumUiMedia.forEachIndexed { eachIndex, _ ->
                        albumUiMedia[eachIndex] = albumUiMedia[eachIndex].copy(isSelectable = true)
                    }
                }
            }

            activeAlbum.postValue(uiAlbum)
            displayedMedia.postValue(albumUiMedia)

            isAlbumsDisplayed.postValue(false)
        }
    }

    fun onCameraShotRequested() {
        Logger.d(TAG, "onCameraShotRequested()")
        if (settings.cameraSettings.isAnyCameraActionEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                // TODO: ActivityResultContracts.TakeVideo returns null,
                //  that's why after fix add an ability to record a video by camera
//                if (settings.mode == Mode.IMAGE || settings.mode == Mode.IMAGE_AND_VIDEO) {
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
//                }
                val takePictureInput = mediaScanManager.createCameraPictureInputTempFile()
                this@MediaStoreViewModel.takePictureInput = takePictureInput
                Logger.d(TAG, "takePictureInput: $takePictureInput")
                if (takePictureInput != null) {
                    action.postValue(MediaStoreScreen.Action.TakePicture(takePictureInput.uri))
                }
            }
        }
    }

    fun onSelectMediaGalleryRequested() {
        Logger.d(TAG, "onSelectMediaGalleryRequested()")
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
                val takenPictureInput = takePictureInput
                Logger.d(TAG, "takenPictureInput: $takenPictureInput")
                if (takenPictureInput != null) {
                    action.postValue(MediaStoreScreen.Action.TakenPictureResult(takenPictureInput))
                }
            }
        }
    }

    fun onVideoTaken(bitmap: Bitmap?) {
        Logger.d(TAG, "onVideoTaken() -> bitmap: $bitmap")
        if (settings.cameraSettings.isAnyCameraActionEnabled) {
            if (bitmap == null) return
            viewModelScope.launch(Dispatchers.IO) {
                val takenVideoInput = takeVideoInput
                Logger.d(TAG, "takenVideoInput: $takenVideoInput")
                if (takenVideoInput != null) {
                    action.postValue(MediaStoreScreen.Action.TakenVideoResult(takenVideoInput))
                }
            }
        }
    }

    fun onLocalMediaGalleryImageSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaGalleryImageSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            viewModelScope.launch(Dispatchers.IO) {
                mediaScanManager.loadLocalSelectedMediaGalleryImage(Dispatchers.IO, uri) { image ->
                    Logger.d(TAG, "loadSelectedGalleryImage() -> image: $image")
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryImageResult(image))
                }
            }
        }
    }

    fun onLocalMediaGalleryImagesSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaGalleryImagesSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            viewModelScope.launch(Dispatchers.IO) {
                val allowedUris = uris.take(settings.maxSelectionCount)
                mediaScanManager.loadLocalSelectedMediaGalleryImages(Dispatchers.IO, allowedUris) { images ->
                    Logger.d(TAG, "loadSelectedGalleryImages() -> images: $images")
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryImagesResult(images))
                }
            }
        }
    }

    fun onLocalMediaGalleryVideoSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaGalleryVideoSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            viewModelScope.launch(Dispatchers.IO) {
                mediaScanManager.loadLocalSelectedMediaGalleryVideo(Dispatchers.IO, uri) { video ->
                    Logger.d(TAG, "loadLocalSelectedMediaGalleryVideo() -> video: $video")
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryVideoResult(video))
                }
            }
        }
    }

    fun onLocalMediaGalleryVideosSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaGalleryVideosSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            viewModelScope.launch(Dispatchers.IO) {
                val allowedUris = uris.take(settings.maxSelectionCount)
                mediaScanManager.loadLocalSelectedMediaGalleryVideos(Dispatchers.IO, allowedUris) { videos ->
                    Logger.d(TAG, "loadLocalSelectedMediaGalleryVideos() -> images: $videos")
                    action.postValue(MediaStoreScreen.Action.SelectedLocalMediaGalleryVideosResult(videos))
                }
            }
        }
    }

    fun onLocalMediaGalleryImageOrVideoSelected(uri: Uri?) {
        Logger.d(TAG, "onLocalMediaGalleryImageOrVideoSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            viewModelScope.launch(Dispatchers.IO) {
            }
        }
    }

    fun onLocalMediaGalleryImagesOrVideosSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onLocalMediaGalleryImagesOrVideosSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            viewModelScope.launch(Dispatchers.IO) {
            }
        }
    }

}