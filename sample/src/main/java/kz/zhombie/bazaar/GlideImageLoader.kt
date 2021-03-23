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
            .error(R.drawable.bazaar_bg_rounded_white_with_stroke)
            .fallback(R.drawable.bazaar_bg_rounded_white_with_stroke)
            .override(300, 300)
            .placeholder(R.drawable.bazaar_bg_rounded_white_with_stroke)
            .into(imageView)
    }

    override fun loadGridItemImage(context: Context, imageView: ImageView, bitmap: Bitmap) {
        Glide.with(context)
            .load(bitmap)
            .centerCrop()
            .error(R.drawable.bazaar_bg_rounded_white_with_stroke)
            .fallback(R.drawable.bazaar_bg_rounded_white_with_stroke)
            .override(300, 300)
            .placeholder(R.drawable.bazaar_bg_rounded_white_with_stroke)
            .into(imageView)
    }

    override fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri) {
        Glide.with(context)
            .load(uri)
            .centerCrop()
            .error(R.drawable.bazaar_bg_black)
            .fallback(R.drawable.bazaar_bg_black)
            .fitCenter()
            .placeholder(R.drawable.bazaar_bg_black)
            .into(imageView)
    }

}