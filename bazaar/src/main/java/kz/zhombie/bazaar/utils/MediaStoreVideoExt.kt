package kz.zhombie.bazaar.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.ui.model.VideoMetadata
import java.io.FileNotFoundException

internal suspend fun Uri?.retrieveVideoMetadata(
    context: Context,
    dispatcher: CoroutineDispatcher
): VideoMetadata? = withContext(dispatcher) {
    if (this@retrieveVideoMetadata == null) return@withContext null
    var metadata: VideoMetadata? = null
    try {
        @Suppress("BlockingMethodInNonBlockingContext")
        context.contentResolver
            ?.openFileDescriptor(this@retrieveVideoMetadata, "r")
            ?.use {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(it.fileDescriptor)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
                metadata = VideoMetadata(
                    duration = duration ?: 0,
                    width = width ?: 0,
                    height = height ?: 0,
                    frame = retriever.frameAtTime ?: retriever.getFrameAtTime(1000L)
                )
                retriever.release()
            }
    } catch (e: FileNotFoundException) {
        return@withContext null
    }
    return@withContext metadata
}