package kz.zhombie.bazaar

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.*

sealed class OpenFile {
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

fun File.open(context: Context): OpenFile {
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

fun Uri.getMimeType(context: Context): String? {
    return if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        context.contentResolver.getType(this)
    } else {
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.getDefault()))
    }
}