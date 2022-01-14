package kz.zhombie.bazaar.sample.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Build
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.clear
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.fetch.VideoFrameFileFetcher
import coil.fetch.VideoFrameUriFetcher
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.OriginalSize
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.util.CoilUtils
import kotlinx.coroutines.Dispatchers
import kz.zhombie.bazaar.sample.R
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.museum.component.CircularProgressDrawable
import okhttp3.Cache

class CoilImageLoader constructor(
    private val context: Context,
    isLoggingEnabled: Boolean = false
) : kz.zhombie.bazaar.api.core.ImageLoader, DefaultLifecycleObserver {

    companion object {
        private val TAG = CoilImageLoader::class.java.simpleName
    }

    private val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .allowHardware(true)
        .availableMemoryPercentage(0.25)
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
//        .logger(if (isLoggingEnabled) DebugLogger() else null)
        .logger(null)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .launchInterceptorChainOnMainThread(false)
        .dispatcher(Dispatchers.Default)
        .bitmapPoolingEnabled(true)
        .bitmapPoolPercentage(0.5)
        .networkCachePolicy(CachePolicy.ENABLED)
        .networkObserverEnabled(true)
        .bitmapConfig(Bitmap.Config.RGB_565)
        .build()

    private var cache: Cache? = null

    private var circularProgressDrawable: CircularProgressDrawable? = null

    init {
        try {
            cache = CoilUtils.createDefaultCache(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCircularProgressDrawable(): CircularProgressDrawable? {
        if (circularProgressDrawable == null) {
            circularProgressDrawable = CircularProgressDrawable(context).apply {
                setStyle(CircularProgressDrawable.LARGE)
                arrowEnabled = false
                centerRadius = 60F
                strokeCap = Paint.Cap.ROUND
                strokeWidth = 11F
                setColorSchemeColors(ContextCompat.getColor(context, R.color.bazaar_white))
            }
        }
        return circularProgressDrawable
    }

    private fun startProgress() {
        if (circularProgressDrawable?.isRunning == false) {
            circularProgressDrawable?.start()
        }
    }

    private fun stopProgress() {
        if (circularProgressDrawable?.isRunning == true) {
            circularProgressDrawable?.stop()
        }
    }

    override fun enqueue(request: PaintingLoader.Request) {
        request.map().enqueueInternally()
    }

    override suspend fun execute(request: PaintingLoader.Request) {
        request.map().executeInternally()
    }

    private fun PaintingLoader.Request.map(): ImageRequest =
        ImageRequest.Builder(context)
            .bitmapConfig(bitmapConfig)
            .data(data)
            .placeholder(placeholderDrawable ?: getCircularProgressDrawable())
            .apply {
                if (crossfade.isEnabled) {
                    crossfade(crossfade.isEnabled)
                    crossfade(crossfade.duration)
                }

                if (errorDrawable == null) {
                    error(R.drawable.bazaar_bg_black)
                } else {
                    error(errorDrawable)
                }

                when (scale) {
                    PaintingLoader.Request.Scale.FILL ->
                        scale(Scale.FILL)
                    PaintingLoader.Request.Scale.FIT ->
                        scale(Scale.FIT)
                }

                when (size) {
                    PaintingLoader.Request.Size.Inherit -> {
                        precision(Precision.AUTOMATIC)
                        size(ViewSizeResolver(imageView))
                    }
                    PaintingLoader.Request.Size.Original -> {
                        precision(Precision.AUTOMATIC)
                        size(OriginalSize)
                    }
                    is PaintingLoader.Request.Size.Pixel -> {
                        with(size) {
                            if (this is PaintingLoader.Request.Size.Pixel) {
                                precision(Precision.EXACT)
                                size(width, height)
                            }
                        }
                    }
                }
            }
            .listener(
                onStart = {
                    startProgress()
                    listener?.onStart(this)
                },
                onCancel = {
                    stopProgress()
                    listener?.onCancel(this)
                },
                onError = { _, throwable ->
                    stopProgress()
                    listener?.onError(this, throwable)
                },
                onSuccess = { _, _: ImageResult.Metadata ->
                    stopProgress()
                    listener?.onSuccess(this)
                }
            )
            .target(imageView)
            .build()

    private fun ImageRequest.enqueueInternally(): Disposable =
        imageLoader.enqueue(this)

    private suspend fun ImageRequest.executeInternally(): ImageResult =
        imageLoader.execute(this)

    override fun dispose(imageView: ImageView) {
        imageView.clear()
        imageView.setImageDrawable(null)
    }

    override fun clearCache() {
        try {
            cache?.directory()?.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopProgress()

        imageLoader.memoryCache.clear()
    }

    /**
     * [androidx.lifecycle.DefaultLifecycleObserver] implementation
     */

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        clearCache()
    }

}