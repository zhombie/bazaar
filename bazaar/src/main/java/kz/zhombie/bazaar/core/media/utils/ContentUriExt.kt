package kz.zhombie.bazaar.core.media.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import java.net.URISyntaxException

@Suppress("DEPRECATION")
@WorkerThread
@Throws(URISyntaxException::class)
internal fun Uri.getFilePath(context: Context): String? {
    var fileUri = this
    var selection: String? = null
    var selectionArgs: Array<String>? = null
    if (DocumentsContract.isDocumentUri(context.applicationContext, fileUri)) {
        when {
            fileUri.isExternalStorageDocument() -> {
                val documentId = DocumentsContract.getDocumentId(fileUri)
                val split = documentId.split(":").toTypedArray()
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
            fileUri.isDownloadsDocument() -> {
                val id = DocumentsContract.getDocumentId(fileUri)
                fileUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
            }
            fileUri.isMediaDocument() -> {
                val docId = DocumentsContract.getDocumentId(fileUri)
                val split = docId.split(":")
                // split[0] -> type
                when (split.first()) {
                    "image" -> fileUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "audio" -> fileUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    "video" -> fileUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                selection = "_id=?"
                selectionArgs = arrayOf(split.first())
            }
        }
    }
    if ("content".equals(fileUri.scheme, ignoreCase = true)) {
        if (fileUri.isGooglePhotosUri()) {
            return fileUri.lastPathSegment
        }
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        try {
            val cursor = context.contentResolver?.query(fileUri, projection, selection, selectionArgs, null)
            var path: String? = null
            if (cursor != null) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    path = cursor.getString(columnIndex)
                }
                cursor.close()
            }
            return path
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else if ("file".equals(fileUri.scheme, ignoreCase = true)) {
        return fileUri.path
    }
    return null
}

internal fun Uri.isExternalStorageDocument(): Boolean {
    return authority == "com.android.externalstorage.documents"
}

internal fun Uri.isDownloadsDocument(): Boolean {
    return authority ==  "com.android.providers.downloads.documents"
}

internal fun Uri.isMediaDocument(): Boolean {
    return authority == "com.android.providers.media.documents"
}

internal fun Uri.isGooglePhotosUri(): Boolean {
    return authority == "com.google.android.apps.photos.content"
}