package kz.zhombie.bazaar.sample.ui.cursor

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.loader.content.AsyncTaskLoader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kz.garage.multimedia.store.model.Content
import kz.garage.multimedia.store.model.Image
import kz.garage.multimedia.store.projection.Projection

class MediaAsyncCursorTaskLoader constructor(
    context: Context
) : AsyncTaskLoader<List<Content>>(context) {

    companion object {
        private val TAG = MediaAsyncCursorTaskLoader::class.java.simpleName
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private val ioScope = CoroutineScope(Dispatchers.IO + exceptionHandler)

    private val forceLoadContentObserver = ForceLoadContentObserver()

    override fun onStartLoading() {
        super.onStartLoading()

        Log.d(TAG, "onStartLoading()")

//        if (takeContentChanged()) {
            forceLoad()
//        }

        context.contentResolver
            .registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, forceLoadContentObserver
            )
    }

    override fun onForceLoad() {
        super.onForceLoad()

        Log.d(TAG, "onForceLoad()")
    }

    override fun onStopLoading() {
        super.onStopLoading()

        Log.d(TAG, "onStopLoading()")

        cancelLoad()
    }

    override fun onReset() {
        super.onReset()

        Log.d(TAG, "onReset()")

        stopLoading()

        context.contentResolver
            .unregisterContentObserver(forceLoadContentObserver)

        ioScope.cancel()
    }

    override fun onAbandon() {
        super.onAbandon()

        Log.d(TAG, "onAbandon()")

        context.contentResolver
            .unregisterContentObserver(forceLoadContentObserver)

        ioScope.cancel()
    }

    override fun loadInBackground(): List<Content>? {
        Log.d(TAG, "loadInBackground()")

        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection: Array<String> = Projection.Image.get().toTypedArray()
        val selection = "(${MediaStore.Images.ImageColumns.MIME_TYPE}=? OR ${MediaStore.Images.ImageColumns.MIME_TYPE}=?) AND ${MediaStore.Images.ImageColumns.SIZE}>=?"
        val selectionArgs: Array<String> = arrayOf("image/jpeg", "image/png", "102400")

        val sortBy = MediaStore.Images.ImageColumns.DATE_ADDED
        val sortOrder = "DESC"

        val images = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bundle = Bundle()

            // Selection
            bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)

            // Sort
            bundle.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(sortBy))
            bundle.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                if (sortOrder == "ASC") {
                    ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                } else {
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                }
            )

            context.contentResolver
                ?.query(uri, projection, bundle, null)
                ?.use { cursor ->
                    val list = mutableListOf<Image>()
                    if (cursor.moveToFirst()) {
                        while (cursor.moveToNext()) {
//                            cursor.readImage()?.let { list.add(it) }
                        }
                    }
                    list
                }
        } else {
            val args = "$sortBy $sortOrder"

            context.contentResolver
                ?.query(uri, projection, selection, selectionArgs, args, null)
                ?.use { cursor ->
                    val list = mutableListOf<Image>()
                    if (cursor.moveToFirst()) {
                        while (cursor.moveToNext()) {
//                            cursor.readImage()?.let { list.add(it) }
                        }
                    }
                    list
                }
        }

        Log.d(TAG, "loadInBackground() -> ${images?.size}")

        return images
    }

}