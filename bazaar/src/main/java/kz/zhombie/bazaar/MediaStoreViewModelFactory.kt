package kz.zhombie.bazaar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MediaStoreViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaStoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaStoreViewModel() as T
        }
        throw IllegalStateException("Cannot create ${modelClass.simpleName} with [${MediaStoreViewModelFactory::class.java.simpleName}]")
    }

}