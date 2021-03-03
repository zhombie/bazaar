package kz.zhombie.bazaar

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import coil.Coil
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kz.zhombie.bazaar.api.ImageLoader

class CoilImageLoader : ImageLoader {

    override fun loadImage(context: Context, imageView: ImageView, uri: Uri) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .precision(Precision.AUTOMATIC)
            .scale(Scale.FIT)
            .size(450, 450)
            .target(imageView)
            .build()

        Coil.enqueue(request)
    }

}