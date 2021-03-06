package kz.zhombie.bazaar.api.model

import android.net.Uri

data class Album constructor(
    val id: Long,
    val displayName: String,
    val items: List<Media>
) {

    val cover: Uri?
        get() = if (items.isNotEmpty()) items.first().uri else null

    val size: Int
        get() = items.size

}
