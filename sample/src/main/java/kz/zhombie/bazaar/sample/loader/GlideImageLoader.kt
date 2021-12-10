package kz.zhombie.bazaar.sample.loader

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.museum.PaintingLoader

class GlideImageLoader constructor(
    private val context: Context,
    private val owner: LifecycleOwner
) : ImageLoader, DefaultLifecycleObserver {

    override fun enqueue(request: PaintingLoader.Request) {
        request.map().into(request.imageView)
    }

    override suspend fun execute(request: PaintingLoader.Request) {
        request.map().into(request.imageView)
    }

    private fun PaintingLoader.Request.map(): RequestBuilder<Drawable> {
        val options = RequestOptions()

        if (errorDrawable == null) {
            options.placeholder(R.drawable.bazaar_bg_rounded_white_with_stroke)
        } else {
            options.placeholder(errorDrawable)
        }
        if (placeholderDrawable == null) {
            options.placeholder(R.drawable.bazaar_bg_rounded_white_with_stroke)
        } else {
            options.placeholder(placeholderDrawable)
        }

        when (val size = size) {
            PaintingLoader.Request.Size.Inherit -> {
            }
            PaintingLoader.Request.Size.Original -> {
            }
            is PaintingLoader.Request.Size.Pixel -> {
                options.override(size.width, size.height)
            }
        }

        return Glide.with(context)
            .load(data)
            .centerCrop()
            .apply(options)
    }

    override fun dispose(imageView: ImageView) {
        owner.lifecycleScope.launch(Dispatchers.IO) {
            Glide.with(context).clear(imageView)
        }
    }

    override fun clearCache() {
        owner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                Glide.get(context).clearMemory()
                Glide.get(context).clearDiskCache()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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