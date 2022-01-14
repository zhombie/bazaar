package kz.zhombie.bazaar.core.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.garage.multimedia.store.model.Content
import kz.garage.multimedia.store.projection.Projection
import kz.zhombie.bazaar.core.media.utils.readAudio
import kz.zhombie.bazaar.core.media.utils.readImage
import kz.zhombie.bazaar.core.media.utils.readVideo

class MediaPagingDataSource constructor(
    private val context: Context,
    private val queryParams: QueryParams
): PagingSource<Int, Content>() {

    companion object {
        private val TAG = MediaPagingDataSource::class.java.simpleName
    }

    class QueryParams constructor(
        val uri: Uri,
        val projection: Projection,
        val selection: String? = null,
        val selectionArgs: Array<String> = emptyArray(),
        val sortBy: String? = null,
        val sortOrder: SortOrder = SortOrder.ASCENDING
    ) {

        companion object {
            fun image(): QueryParams = QueryParams(
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection = Projection.Image,
                selection = "(${MediaStore.Images.ImageColumns.MIME_TYPE}=? OR ${MediaStore.Images.ImageColumns.MIME_TYPE}=?) AND ${MediaStore.Images.ImageColumns.SIZE}>=?",
                selectionArgs = arrayOf("image/jpeg", "image/png", "102400"),
                sortBy = MediaStore.Images.ImageColumns.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING
            )

            fun video(): QueryParams = QueryParams(
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection = Projection.Video,
                selection = "${MediaStore.Video.VideoColumns.MIME_TYPE}=? AND ${MediaStore.Video.VideoColumns.SIZE}>=?",
                selectionArgs = arrayOf("video/mp4", "102400"),
                sortBy = MediaStore.Video.VideoColumns.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING
            )

            fun imageAndVideo(): QueryParams = QueryParams(
                uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Files.getContentUri("external")
                },
                projection = Projection.File,
                selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?) AND ${MediaStore.Files.FileColumns.SIZE}>=?",
                selectionArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                    "102400"
                ),
                sortBy = MediaStore.Files.FileColumns.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING
            )

            fun audio(): QueryParams = QueryParams(
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection = Projection.Audio,
                selection = "(${MediaStore.Audio.AudioColumns.MIME_TYPE}=? OR ${MediaStore.Audio.AudioColumns.MIME_TYPE}=? OR ${MediaStore.Audio.AudioColumns.MIME_TYPE}=?) AND ${MediaStore.Audio.AudioColumns.SIZE}>=?",
                selectionArgs = arrayOf("audio/mpeg", "audio/ogg", "audio/opus", "102400"),
                sortBy = MediaStore.Audio.AudioColumns.DATE_ADDED,
                sortOrder = SortOrder.DESCENDING
            )
        }

        enum class SortOrder {
            ASCENDING,
            DESCENDING
        }

        fun hasSelection(): Boolean =
            !selection.isNullOrBlank() && !selectionArgs.isNullOrEmpty()

    }

    override fun getRefreshKey(state: PagingState<Int, Content>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Content> {
        val limit = params.loadSize
        val page = params.key ?: 1
        val offset = (page - 1) * limit

        val data: List<Content>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bundle = Bundle()

            // Selection
            if (queryParams.hasSelection()) {
                bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, queryParams.selection)
                bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, queryParams.selectionArgs)
            }

            // Sort
            if (!queryParams.sortBy.isNullOrBlank()) {
                bundle.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(queryParams.sortBy))
                bundle.putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    when (queryParams.sortOrder) {
                        QueryParams.SortOrder.ASCENDING ->
                            ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                        QueryParams.SortOrder.DESCENDING ->
                            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    }
                )
            }

            // Limit & offset
            bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, offset)

            context.contentResolver
                ?.query(
                    queryParams.uri,
                    queryParams.projection.get().toTypedArray(),
                    bundle,
                    null
                )
                ?.use { cursor -> cursor.map() }
        } else {
            val extraArgs = if (queryParams.sortBy.isNullOrEmpty()) {
                val sortOrder = when (queryParams.sortOrder) {
                    QueryParams.SortOrder.ASCENDING -> "ASC"
                    QueryParams.SortOrder.DESCENDING -> "DESC"
                }
                "${queryParams.sortBy} $sortOrder LIMIT $limit OFFSET $offset"
            } else {
                "LIMIT $limit OFFSET $offset"
            }

            context.contentResolver
                ?.query(
                    queryParams.uri,
                    queryParams.projection.get().toTypedArray(),
                    if (queryParams.hasSelection()) queryParams.selection else null,
                    if (queryParams.hasSelection()) queryParams.selectionArgs else null,
                    extraArgs,
                    null
                )
                ?.use { cursor -> cursor.map() }
        }

        return LoadResult.Page(
            data = data ?: emptyList(),
            prevKey = null,
            nextKey = if (data.isNullOrEmpty()) null else page + 1
        )
    }

    private suspend inline fun Cursor.map() = withContext(Dispatchers.IO) {
        val list = mutableListOf<Content>()
        if (moveToFirst()) {
            while (moveToNext()) {
                val mimeType = getStringOrNull(getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
                if (!mimeType.isNullOrBlank()) {
                    val item = when {
                        mimeType.startsWith("image") -> readImage()
                        mimeType.startsWith("video") -> readVideo()
                        mimeType.startsWith("audio") -> readAudio()
//                        mimeType == "application/pdf" -> readDocument()
                        else -> null
                    }
                    if (item != null) {
                        list.add(item)
                    }
                }
            }
        }
        list
    }

}