package kz.zhombie.bazaar.api.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * [id] - The unique ID of the [Entity]
 * [uri] - The path of the [Entity]
 * [title] - The title of the [Entity]
 * [displayName] - The display name of the [Entity]. For example, an [Entity] stored at
 * {@code /storage/0000-0000/DCIM/Vacation/IMG1024.JPG} would have a display name of {@code IMG1024.JPG}.
 * [size] - The size of the [Entity]
 * [dateAdded] - The time the [Entity] was first added (milliseconds)
 * [dateModified] - The time the [Entity] was last modified (milliseconds)
 * [dateCreated] - The time the [Entity] was created. If image or video, it is as same as date taken (milliseconds)
 */
open class Entity constructor(
    open val id: Long,
    open val uri: Uri,
    open val title: String,
    open val displayName: String,
    open val size: Long,
    open val dateAdded: Long,
    open val dateModified: Long,
    open val dateCreated: Long?,
    open val thumbnail: Bitmap? = null,
    open val folderId: Long? = null,
    open val folderDisplayName: String? = null
)