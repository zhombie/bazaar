package kz.zhombie.bazaar.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kz.zhombie.bazaar.core.exception.ViewModelException

internal class MediaStoreViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaStoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaStoreViewModel() as T
        }
        throw ViewModelException(this, modelClass)
    }

}