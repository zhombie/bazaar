package kz.zhombie.bazaar.api.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * [id] - The unique ID of the [Multimedia]
 * [uri] - The uri path of the [Multimedia] (usually content://...)
 * [path] The absolute path of the [Multimedia] (local file absolute path)
 * [title] - The title of the [Multimedia]
 * [displayName] - The display name of the [Multimedia]. For example, an [Multimedia] stored at
 * {@code /storage/0000-0000/DCIM/Vacation/IMG1024.JPG} would have a display name of {@code IMG1024.JPG}.
 * [mimeType] - The MIME type of the [Multimedia]
 * [extension] - The extension of [Multimedia]
 * [size] - The size of the [Multimedia]
 * [dateAdded] - The time the [Multimedia] was first added (milliseconds)
 * [dateModified] - The time the [Multimedia] was last modified (milliseconds)
 * [dateCreated] - The time the [Multimedia] was created. If image or video, it is as same as date taken (milliseconds)
 * [thumbnail] - The thumbnail/cover image of the [Multimedia]
 * [folderId] - The primary folder ID of this [Multimedia]
 * [folderDisplayName] - The primary folder display name of this [Multimedia]
 */
open class Multimedia constructor(
    open val id: Long,
    open val uri: Uri,
    open val path: String? = null,
    open val title: String,
    open val displayName: String,
    open val mimeType: String? = null,
    open val extension: String? = null,
    open val size: Long,
    open val dateAdded: Long,
    open val dateModified: Long,
    open val dateCreated: Long?,
    open val thumbnail: Bitmap? = null,
    open val folderId: Long? = null,
    open val folderDisplayName: String? = null
) {

    override fun toString(): String {
        return "${Multimedia::class.java.simpleName}(id=$id, uri=$uri, path=$path, title=$title, displayName=$displayName, mimeType=$mimeType, extension=$extension, size=$size, dateAdded=$dateAdded, dateCreated=$dateCreated, dateModified=$dateModified, thumbnail=$thumbnail, folderId=$folderId, folderDisplayName=$folderDisplayName)"
    }

}