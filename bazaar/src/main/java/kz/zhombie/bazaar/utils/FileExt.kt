package kz.zhombie.bazaar.utils

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

internal sealed class OpenFile {
    data class Success constructor(val intent: Intent) : OpenFile() {
        fun tryToLaunch(context: Context): Boolean {
            return try {
                context.startActivity(intent)
                true
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                false
            }
        }
    }

    data class Error constructor(val type: Type) : OpenFile() {
        enum class Type {
            UNKNOWN,
            FILE_DOES_NOT_EXIST
        }
    }
}


internal fun File.openFile(context: Context): OpenFile {
    if (exists()) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.applicationContext.packageName}.provider",
                this
            )
            val mimeType = uri.getMimeType(context)
            return OpenFile.Success(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return OpenFile.Error(OpenFile.Error.Type.UNKNOWN)
        }
    } else {
        return OpenFile.Error(OpenFile.Error.Type.FILE_DOES_NOT_EXIST)
    }
}


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