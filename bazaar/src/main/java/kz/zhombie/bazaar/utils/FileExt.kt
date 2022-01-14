package kz.zhombie.bazaar.utils

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

sealed class OpenFile {
    data class Success constructor(val intent: Intent) : OpenFile()

    sealed class Error : OpenFile() {
        object Unknown : Error()
        object FileDoesNotExist : Error()
    }
}


fun File.open(
    context: Context,
    authority: String = "${context.applicationContext.packageName}.provider"
): OpenFile {
    return if (exists()) {
        try {
            val uri = FileProvider.getUriForFile(context, authority, this)
            val mimeType = uri.getMimeType(context)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val resolveInfo =
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in resolveInfo) {
                context.grantUriPermission(
                    info.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            OpenFile.Success(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            OpenFile.Error.Unknown
        }
    } else {
        OpenFile.Error.FileDoesNotExist
    }
}

fun OpenFile.Success.tryToOpen(context: Context): Boolean {
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
