package kz.zhombie.bazaar

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import kz.zhombie.bazaar.api.ImageLoader

class CoilImageLoader : ImageLoader {

    override fun loadImage(context: Context, imageView: ImageView, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.DISABLED)
            .data(uri)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(300, 300)
            .target(imageView)
            .transformations(
                RoundedCornersTransformation(
//                    context.resources.getDimension(R.dimen.rounded_corner_radius),
//                    context.resources.getDimension(R.dimen.rounded_corner_radius),
//                    context.resources.getDimension(R.dimen.rounded_corner_radius),
//                    context.resources.getDimension(R.dimen.rounded_corner_radius),
                20F, 20F, 20F, 20F
                )
            )
            .build()

        Coil.enqueue(request)
    }

}