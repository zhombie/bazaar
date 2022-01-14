package kz.zhombie.bazaar.sample.engine

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.*
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.museum.PaintingLoader

class GlideImageLoader constructor(
    private val context: Context
) : ImageLoader, DefaultLifecycleObserver {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private val ioScope = CoroutineScope(Dispatchers.IO + exceptionHandler)

    override fun enqueue(request: PaintingLoader.Request) {
        request.map().into(request.imageView)
    }

    override suspend fun execute(request: PaintingLoader.Request) {
        request.map().into(request.imageView)
    }

    private fun PaintingLoader.Request.map(): RequestBuilder<Drawable> {
        val options = RequestOptions().apply {
            if (errorDrawable == null) {
                placeholder(R.drawable.bazaar_bg_rounded_white_with_stroke)
            } else {
                placeholder(errorDrawable)
            }
            if (placeholderDrawable == null) {
                placeholder(R.drawable.bazaar_bg_rounded_white_with_stroke)
            } else {
                placeholder(placeholderDrawable)
            }

            when (val size = size) {
                PaintingLoader.Request.Size.Inherit -> {
                }
                PaintingLoader.Request.Size.Original -> {
                }
                is PaintingLoader.Request.Size.Pixel -> {
                    override(size.width, size.height)
                }
            }
        }

        return Glide.with(context)
            .load(data)
            .centerCrop()
            .apply(options)
    }

    override fun dispose(imageView: ImageView) {
        ioScope.launch {
            Glide.with(imageView.context)
                .clear(imageView)
        }
    }

    override fun clearCache() {
        ioScope.launch {
            Glide.get(context).clearMemory()
            Glide.get(context).clearDiskCache()
        }.invokeOnCompletion {
            ioScope.cancel()
        }
    }

    /**
     * [androidx.lifecycle.DefaultLifecycleObserver] implementation
     */

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        clearCache()
    }

}