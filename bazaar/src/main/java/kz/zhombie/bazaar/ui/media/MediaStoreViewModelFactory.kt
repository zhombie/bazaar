package kz.zhombie.bazaar.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kz.zhombie.bazaar.core.MediaScanManager
import kz.zhombie.bazaar.core.exception.ViewModelException

internal class MediaStoreViewModelFactory constructor(
    private val mediaScanManager: MediaScanManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaStoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaStoreViewModel(mediaScanManager) as T
        }
        throw ViewModelException(this, modelClass)
    }

}