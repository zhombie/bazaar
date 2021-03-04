package kz.zhombie.bazaar.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kz.zhombie.bazaar.model.Media
import kz.zhombie.bazaar.ui.model.UIMedia

class MediaStoreViewModel : ViewModel() {

    private val selectedMedia by lazy { MutableLiveData<List<UIMedia>>() }
    fun getSelectedMedia(): LiveData<List<UIMedia>> = selectedMedia

    private val allMedia by lazy { MutableLiveData<List<UIMedia>>() }
    fun getAllMedia(): LiveData<List<UIMedia>> = allMedia

    fun onImageCheckboxClicked(uiMedia: UIMedia) {
        val data = mutableListOf<UIMedia>()
        val currentSelectedMedia = selectedMedia.value
        if (!currentSelectedMedia.isNullOrEmpty()) {
            data.addAll(currentSelectedMedia)
        }
        if (uiMedia.isSelected) {
            data.add(uiMedia)
        } else {
            data.removeAll { it.media.id == uiMedia.media.id }
        }
        selectedMedia.postValue(data)

        val currentAllMedia = allMedia.value?.toMutableList() ?: mutableListOf()
        val index = currentAllMedia.indexOfFirst { it.media.id == uiMedia.media.id }
        currentAllMedia[index] = uiMedia.copy(isSelected = !uiMedia.isSelected)
        allMedia.postValue(currentAllMedia)
    }

    fun onMediaLoaded(data: List<Media>) {
        allMedia.postValue(data.map { UIMedia(it, false) })
    }

}