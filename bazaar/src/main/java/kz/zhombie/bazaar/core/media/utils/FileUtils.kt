package kz.zhombie.bazaar.core.media.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

internal fun Uri.getMimeType(context: Context): String? {
    return when (scheme) {
        ContentResolver.SCHEME_CONTENT -> {
            context.contentResolver.getType(this)
        }
        ContentResolver.SCHEME_FILE -> {
            val path = path
            if (path.isNullOrBlank()) return null
            val file = File(path)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.name.substringAfterLast('.', ""))
        }
        else -> null
    }
}


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