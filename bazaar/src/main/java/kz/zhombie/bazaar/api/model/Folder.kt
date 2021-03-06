package kz.zhombie.bazaar.api.model

import android.net.Uri

data class Folder constructor(
    val id: Long,
    val bucketId: String,
    val name: String,
    val dateAdded: Long,
    val items: List<Media>,

    private var coverPath: Uri? = null,
) {

    fun getCoverPath(): Uri? {
        return when {
            coverPath != null -> coverPath
            items.isNotEmpty() -> items.first().uri
            else -> null
        };
    }

    fun setCoverPath(coverPath: Uri) {
        this.coverPath = coverPath
    }

    fun resetCoverPath() {
        this.coverPath = null
    }

}
