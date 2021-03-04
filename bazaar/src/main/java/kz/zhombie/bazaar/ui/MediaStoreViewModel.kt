package kz.zhombie.bazaar.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kz.zhombie.bazaar.core.Logger
import kz.zhombie.bazaar.model.Media
import kz.zhombie.bazaar.ui.model.UIMedia

class MediaStoreViewModel : ViewModel() {

    companion object {
        private val TAG: String = MediaStoreViewModel::class.java.simpleName
    }

    private val selectedMedia by lazy { MutableLiveData<List<UIMedia>>() }
    fun getSelectedMedia(): LiveData<List<UIMedia>> = selectedMedia

    private val allMedia by lazy { MutableLiveData<List<UIMedia>>() }
    fun getAllMedia(): LiveData<List<UIMedia>> = allMedia

    fun onImageCheckboxClicked(uiMedia: UIMedia) {
        Logger.d(TAG, "uiMedia: $uiMedia")

        // Selected
        val newSelected = mutableListOf<UIMedia>()
        val currentSelectedMedia = selectedMedia.value
        if (!currentSelectedMedia.isNullOrEmpty()) {
            newSelected.addAll(currentSelectedMedia)
        }
        if (uiMedia.isSelected) {
            newSelected.add(uiMedia)
        } else {
            newSelected.removeAll { it.media.id == uiMedia.media.id }
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

    fun onMediaLoaded(data: List<Media>) {
        allMedia.postValue(data.map { UIMedia(it, false) })
    }

}