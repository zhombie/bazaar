package kz.zhombie.bazaar.ui.media

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

    private var takenPictureInput: Image? = null

    init {
        Logger.d(TAG, "created")
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
                val takenPictureInput = mediaScanManager.createCameraInputTempFile()
                this@MediaStoreViewModel.takenPictureInput = takenPictureInput
                Logger.d(TAG, "takenPictureInput: $takenPictureInput")
                if (takenPictureInput != null) {
                    action.postValue(MediaStoreScreen.Action.TakePicture(takenPictureInput.uri))
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
                        action.postValue(MediaStoreScreen.Action.SelectMediaGalleryImage)
                    } else {
                        action.postValue(MediaStoreScreen.Action.SelectMediaGalleryImages)
                    }
                } else if (settings.mode == Mode.VIDEO) {
                    if (settings.maxSelectionCount == 1) {
                        action.postValue(MediaStoreScreen.Action.SelectMediaGalleryVideo)
                    } else {
                        action.postValue(MediaStoreScreen.Action.SelectMediaGalleryVideos)
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
                val takenPictureInput = takenPictureInput
                Logger.d(TAG, "takenPictureInput: $takenPictureInput")
                if (takenPictureInput != null) {
                    action.postValue(MediaStoreScreen.Action.TakenPictureResult(takenPictureInput))
                }
            }
        }
    }

    fun onMediaGalleryImageSelected(uri: Uri?) {
        Logger.d(TAG, "onGalleryImageSelected() -> uri: $uri")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uri == null) return
            viewModelScope.launch(Dispatchers.IO) {
                mediaScanManager.loadSelectedGalleryImage(Dispatchers.IO, uri) { image ->
                    Logger.d(TAG, "loadSelectedGalleryImage() -> image: $image")
                    action.postValue(MediaStoreScreen.Action.SelectedMediaGalleryImageResult(image))
                }
            }
        }
    }

    fun onMediaGalleryImagesSelected(uris: List<Uri>?) {
        Logger.d(TAG, "onGalleryImagesSelected() -> uris: $uris")
        if (settings.isLocalMediaSearchAndSelectEnabled) {
            if (uris.isNullOrEmpty()) return
            val allowedUris = uris.take(settings.maxSelectionCount)
            viewModelScope.launch(Dispatchers.IO) {
                mediaScanManager.loadSelectedGalleryImages(Dispatchers.IO, allowedUris) { images ->
                    Logger.d(TAG, "loadSelectedGalleryImages() -> images: $images")
                    action.postValue(MediaStoreScreen.Action.SelectedMediaGalleryImagesResult(images))
                }
            }
        }
    }

}