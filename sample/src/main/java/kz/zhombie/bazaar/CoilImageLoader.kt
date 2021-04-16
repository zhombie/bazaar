package kz.zhombie.bazaar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.util.DebugLogger
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.museum.component.CircularProgressDrawable

class CoilImageLoader constructor(context: Context) : ImageLoader {

    companion object {
        private val TAG = CoilImageLoader::class.java.simpleName
    }

    private val imageLoader by lazy {
        coil.ImageLoader.Builder(context)
            .componentRegistry {
                // Video frame
                add(VideoFrameFileFetcher(context))
                add(VideoFrameUriFetcher(context))
                add(VideoFrameDecoder(context))

                // GIF
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder(context))
                } else {
                    add(GifDecoder())
                }
            }
            .crossfade(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    private val circularProgressDrawable by lazy {
        val it = CircularProgressDrawable(context)
        it.setStyle(CircularProgressDrawable.LARGE)
        it.arrowEnabled = false
        it.centerRadius = 60F
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = 11F
        it.setColorSchemeColors(ContextCompat.getColor(context, R.color.bazaar_white))
        it
    }

    override fun loadSmallImage(context: Context, imageView: ImageView, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(false)
            .data(uri)
            .error(R.drawable.museum_bg_black)
            .placeholder(R.drawable.museum_bg_black)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(300, 300)
            .target(imageView)
            .build()

        imageLoader.enqueue(request)
    }

    override fun loadSmallImage(context: Context, imageView: ImageView, bitmap: Bitmap) {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(false)
            .data(bitmap)
            .error(R.drawable.museum_bg_black)
            .placeholder(R.drawable.museum_bg_black)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(300, 300)
            .target(imageView)
            .build()

        imageLoader.enqueue(request)
    }

    override fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(false)
            .data(uri)
            .error(R.drawable.museum_bg_black)
            .placeholder(circularProgressDrawable)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(ViewSizeResolver(imageView))
            .listener(
                onStart = {
                    Log.d(TAG, "onStart()")
                    if (!circularProgressDrawable.isRunning) {
                        circularProgressDrawable.start()
                    }
                },
                onCancel = {
                    Log.d(TAG, "onCancel()")
                    if (circularProgressDrawable.isRunning) {
                        circularProgressDrawable.stop()
                    }
                },
                onError = { _, throwable ->
                    Log.d(TAG, "onError() -> throwable: $throwable")
                    if (circularProgressDrawable.isRunning) {
                        circularProgressDrawable.stop()
                    }
                },
                onSuccess = { _, metadata: ImageResult.Metadata ->
                    Log.d(TAG, "onError() -> metadata: $metadata")
                    if (circularProgressDrawable.isRunning) {
                        circularProgressDrawable.stop()
                    }
                },
            )
            .target(imageView)
            .build()

        imageLoader.enqueue(request)
    }

    fun clearCache() {
        circularProgressDrawable.stop()
        imageLoader.memoryCache.clear()
    }

}