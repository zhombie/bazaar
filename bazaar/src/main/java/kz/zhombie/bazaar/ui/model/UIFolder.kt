package kz.zhombie.bazaar.ui.model

import android.content.Context
import kz.garage.multimedia.store.model.Folder
import kz.zhombie.bazaar.R

internal data class UIFolder constructor(
    val folder: Folder,
    val displayType: DisplayType = DisplayType.UNKNOWN
) {

    companion object {
        const val ALL_MEDIA_ID = 0L
    }

    enum class DisplayType {
        UNKNOWN,
        IMAGES,
        VIDEOS,
        MEDIA,
        AUDIOS,
        DOCUMENTS
    }

    fun getDisplayName(context: Context): String {
        return when (displayType) {
            DisplayType.UNKNOWN -> {
                val name = folder.displayName
                if (!name.isNullOrBlank()) {
                    name
                } else {
                    context.getString(R.string.bazaar_all_media)
                }
            }
            DisplayType.IMAGES -> context.getString(R.string.bazaar_all_images)
            DisplayType.VIDEOS -> context.getString(R.string.bazaar_all_videos)
            DisplayType.MEDIA -> context.getString(R.string.bazaar_all_media)
            DisplayType.AUDIOS -> context.getString(R.string.bazaar_all_audios)
            DisplayType.DOCUMENTS -> context.getString(R.string.bazaar_all_documents)
        }
    }

}