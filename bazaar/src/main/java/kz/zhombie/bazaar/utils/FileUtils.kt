package kz.zhombie.bazaar.utils

import android.webkit.MimeTypeMap
import java.io.File

internal fun getExtension(filename: String, mimeType: String): String? {
    val extension = filename.substringAfterLast('.', "")
    return if (extension.isNotBlank()) extension else MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
}


internal fun File.getExtension(mimeType: String? = null): String? {
    val extension = name.substringAfterLast('.', "")
    return if (extension.isNotBlank()) {
        extension
    } else if (!mimeType.isNullOrBlank()) {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    } else {
        null
    }
}