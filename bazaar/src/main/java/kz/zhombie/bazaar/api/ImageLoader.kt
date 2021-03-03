package kz.zhombie.bazaar.api

import android.content.Context
import android.net.Uri
import android.widget.ImageView

interface ImageLoader {
    fun loadImage(context: Context, imageView: ImageView, uri: Uri)
}