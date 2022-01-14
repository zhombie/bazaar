package kz.zhombie.bazaar.ui.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import kz.zhombie.bazaar.core.media.MediaPagingDataSource

internal class MediaSelectionViewModel constructor(
    application: Application
) : AndroidViewModel(application) {

    private val applicationContext: Context
        get() = getApplication()

    val contents = Pager(
        PagingConfig(pageSize = 20)
    ) {
        MediaPagingDataSource(
            applicationContext,
            MediaPagingDataSource.QueryParams.image()
        )
    }.flow.cachedIn(viewModelScope)

}