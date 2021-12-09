package kz.zhombie.bazaar

import android.content.Context
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.api.core.ImageRequestBuilder

// ImageLoader

val Context.imageLoader: ImageLoader
    get() = Bazaar.getImageLoader(this)

val Fragment.imageLoader: ImageLoader
    get() = Bazaar.getImageLoader(requireContext())

val RecyclerView.ViewHolder.imageLoader: ImageLoader
    get() = itemView.context.imageLoader


inline fun ImageView.load(
    data: Any?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequestBuilder.() -> Unit = {}
) {
    val request = ImageRequestBuilder(context)
        .apply(builder)
        .setData(data)
        .into(this)
        .build()
    imageLoader.enqueue(request)
}

fun ImageView.dispose() {
    context.imageLoader.dispose(this)
}
