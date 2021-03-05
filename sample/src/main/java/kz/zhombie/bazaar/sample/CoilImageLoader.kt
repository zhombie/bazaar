package kz.zhombie.bazaar.sample

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.api.ImageLoader

class CoilImageLoader : ImageLoader {

    override fun loadGridItemImage(context: Context, imageView: ImageView, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .data(uri)
            .error(R.drawable.ic_placeholder_white)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .placeholder(R.drawable.ic_placeholder_white)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(300, 300)
            .target(imageView)
            .build()

        Coil.enqueue(request)
    }

    override fun loadGridItemImage(context: Context, imageView: ImageView, bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
    }

    override fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(false)
            .diskCachePolicy(CachePolicy.DISABLED)
            .data(uri)
            .error(R.drawable.ic_placeholder_black)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .placeholder(R.drawable.ic_placeholder_black)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(1280, 960)
            .target(imageView)
            .build()

        Coil.enqueue(request)
    }

}