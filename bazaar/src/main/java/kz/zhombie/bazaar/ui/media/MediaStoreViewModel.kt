package kz.zhombie.bazaar.ui.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexvasilkov.gestures.animation.ViewPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.zhombie.bazaar.core.Logger
import kz.zhombie.bazaar.api.model.Media
import kz.zhombie.bazaar.ui.model.UIMedia

internal class MediaStoreViewModel : ViewModel() {

    companion object {
        private val TAG: String = MediaStoreViewModel::class.java.simpleName
    }

    private val selectedMedia by lazy { MutableLiveData<List<UIMedia>>() }
    fun getSelectedMedia(): LiveData<List<UIMedia>> = selectedMedia

    private val allMedia by lazy { MutableLiveData<List<UIMedia>>() }
    fun getAllMedia(): LiveData<List<UIMedia>> = allMedia

    private val viewPosition by lazy { MutableLiveData<ViewPosition>() }
    fun getViewPosition(): LiveData<ViewPosition> = viewPosition

    fun onImageCheckboxClicked(uiMedia: UIMedia) {
        Logger.d(TAG, "onImageCheckboxClicked() -> uiMedia: $uiMedia")

        viewModelScope.launch(Dispatchers.IO) {
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
            with(allMedia.value?.toMutableList() ?: mutableListOf()) {
                indexOfFirst { it.media.id == uiMedia.media.id }
                    .takeIf { index -> index > -1 }
                    ?.let { index ->
                        this[index] = this[index].copy(isSelected = !this[index].isSelected)
                        allMedia.postValue(this)
                    }
            }
        }
    }

    fun onMediaLoaded(data: List<Media>) {
        allMedia.postValue(data.map { UIMedia(it, isSelected = false, isVisible = true) })
    }

    fun onLayoutChange(viewPosition: ViewPosition) {
        this.viewPosition.postValue(viewPosition)
    }

    fun onVisibilityChange(id: Long, isVisible: Boolean, delayDuration: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(delayDuration)

            with(allMedia.value?.toMutableList() ?: mutableListOf()) {
                indexOfFirst { it.media.id == id }
                    .takeIf { index -> index > -1 }
                    ?.let { index ->
                        this[index] = this[index].copy(isVisible = isVisible)
                        allMedia.postValue(this)
                    }
            }
        }
    }

}