package kz.zhombie.bazaar

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import kz.zhombie.bazaar.api.core.ImageLoader

class GlideImageLoader : ImageLoader {

    override fun loadGridItemImage(context: Context, imageView: ImageView, uri: Uri) {
        Glide.with(context)
            .load(uri)
            .centerCrop()
            .error(R.drawable.ic_placeholder_white)
            .fallback(R.drawable.ic_placeholder_white)
            .override(300, 300)
            .placeholder(R.drawable.ic_placeholder_white)
            .into(imageView)
    }

    override fun loadGridItemImage(context: Context, imageView: ImageView, bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
    }

    override fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri) {
        Glide.with(context)
            .load(uri)
            .centerCrop()
            .error(R.drawable.ic_placeholder_black)
            .fallback(R.drawable.ic_placeholder_black)
            .fitCenter()
            .placeholder(R.drawable.ic_placeholder_black)
            .into(imageView)
    }

}