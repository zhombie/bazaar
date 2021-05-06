package kz.zhombie.bazaar.api.model

import android.net.Uri

data class Folder constructor(
    val id: Long,
    val displayName: String? = null,
    val items: List<Multimedia>
) {

    val cover: Uri?
        get() = if (items.isNotEmpty()) items.first().uri else null

    val size: Int
        get() = items.size

}
