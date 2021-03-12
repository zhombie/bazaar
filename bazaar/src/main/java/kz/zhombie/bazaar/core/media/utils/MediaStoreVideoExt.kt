package kz.zhombie.bazaar.core.media.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.core.media.model.VideoMetadata
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

                val embeddedPicture = retriever.embeddedPicture
                val thumbnail = if (embeddedPicture?.isNotEmpty() == true) {
                    BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                } else {
                    val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 250, 250)
                    } else {
                        retriever.frameAtTime ?: retriever.getFrameAtTime(0L)
                    }
                    frame
                }
                metadata = VideoMetadata(
                    duration = duration ?: 0,
                    width = width ?: 0,
                    height = height ?: 0,
                    thumbnail = thumbnail
                )
                retriever.release()
            }
    } catch (e: FileNotFoundException) {
        return@withContext null
    }
    return@withContext metadata
}