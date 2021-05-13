package kz.zhombie.bazaar.utils

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

sealed class OpenFileAction {
    data class Success constructor(val intent: Intent) : OpenFileAction()

    data class Error constructor(val reason: Reason) : OpenFileAction() {
        enum class Reason {
            UNKNOWN,
            FILE_DOES_NOT_EXIST
        }
    }
}


fun File.open(context: Context, authority: String = "${context.applicationContext.packageName}.provider"): OpenFileAction {
    return if (exists()) {
        try {
            val uri = FileProvider.getUriForFile(context, authority, this)
            val mimeType = uri.getMimeType(context)
            OpenFileAction.Success(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            OpenFileAction.Error(OpenFileAction.Error.Reason.UNKNOWN)
        }
    } else {
        OpenFileAction.Error(OpenFileAction.Error.Reason.FILE_DOES_NOT_EXIST)
    }
}

fun OpenFileAction.Success.tryToLaunch(context: Context): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        false
    }
}


fun Uri.getMimeType(context: Context): String? {
    return when (scheme) {
        ContentResolver.SCHEME_CONTENT -> {
            context.contentResolver.getType(this)
        }
        ContentResolver.SCHEME_FILE -> {
            val path = path
            if (path.isNullOrBlank()) return null
            val file = File(path)
            MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.name.substringAfterLast('.', ""))
        }
        else -> null
    }
}


fun getExtension(filename: String, mimeType: String): String? {
    val extension = filename.substringAfterLast('.', "")
    return if (extension.isNotBlank()) {
        extension
    } else {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }
}


fun File.getExtension(mimeType: String? = null): String? {
    val extension = name.substringAfterLast('.', "")
    return if (extension.isNotBlank()) {
        extension
    } else if (!mimeType.isNullOrBlank()) {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    } else {
        null
    }
}