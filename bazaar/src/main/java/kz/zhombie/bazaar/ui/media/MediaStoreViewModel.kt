package kz.zhombie.bazaar.ui.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvasilkov.gestures.animation.ViewPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.zhombie.bazaar.api.model.Album
import kz.zhombie.bazaar.api.model.Image
import kz.zhombie.bazaar.core.logging.Logger
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.core.MediaScanManager
import kz.zhombie.bazaar.ui.model.UIAlbum
import kz.zhombie.bazaar.ui.model.UIMedia

internal class MediaStoreViewModel constructor(
    private val mediaScanManager: MediaScanManager
) : ViewModel() {

    companion object {
        private val TAG: String = MediaStoreViewModel::class.java.simpleName
    }

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

        if (allMedia.isEmpty()) {
            viewModelScope.launch {
                mediaScanManager.loadImages(Dispatchers.IO) {
                    onMediaLoaded(it)
                }
            }
        }
    }

    private fun onMediaLoaded(data: List<Media>) {
        viewModelScope.launch(Dispatchers.IO) {
            Logger.d(TAG, "onMediaLoaded() -> data.size: ${data.size}")

            allMedia.addAll(data)

            val uiMedia = data.map { UIMedia(it, isSelected = false, isVisible = true) }

            val defaultAlbum = UIAlbum(Album(0, "Все медиа", uiMedia.map { it.media }))

            activeAlbum.postValue(defaultAlbum)
            displayedMedia.postValue(uiMedia)

            val albums = data.mapNotNull { media ->
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

    fun onImageCheckboxClicked(uiMedia: UIMedia) {
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
                    if (uiAlbum.album.id == 0L) {
                        true
                    } else {
                        it.folderId == uiAlbum.album.id
                    }
                }
                .map { UIMedia(it, isSelected = false, isVisible = true) }
                .toMutableList()

            (selectedMedia.value ?: emptyList()).forEach { selectedMedia ->
                val index = albumUiMedia.indexOfFirst { it.media.id == selectedMedia.media.id }
                if (index > -1) {
                    albumUiMedia[index] = albumUiMedia[index].copy(isSelected = true)
                }
            }

            activeAlbum.postValue(uiAlbum)
            displayedMedia.postValue(albumUiMedia)

            isAlbumsDisplayed.postValue(false)
        }
    }

    fun onCameraShotRequested() {
        Logger.d(TAG, "onCameraShotRequested()")
        viewModelScope.launch(Dispatchers.IO) {
            val takenPictureInput = mediaScanManager.createCameraInputTempFile()
            this@MediaStoreViewModel.takenPictureInput = takenPictureInput
            Logger.d(TAG, "takenPictureInput: $takenPictureInput")
            if (takenPictureInput != null) {
                action.postValue(MediaStoreScreen.Action.TakePicture(takenPictureInput.uri))
            }
        }
    }

    fun onSelectFromExplorerRequested() {
    }

    fun onPictureTaken(isSuccess: Boolean) {
        Logger.d(TAG, "onPictureTaken() -> isSuccess: $isSuccess")
        viewModelScope.launch(Dispatchers.IO) {
            if (isSuccess) {
                val takenPictureInput = takenPictureInput
                Logger.d(TAG, "takenPictureInput: $takenPictureInput")
                if (takenPictureInput != null) {
                    action.postValue(MediaStoreScreen.Action.TakenPictureResult(takenPictureInput))
                }
            }
        }
    }

}