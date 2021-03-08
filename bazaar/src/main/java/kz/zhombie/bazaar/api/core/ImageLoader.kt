package kz.zhombie.bazaar.api.core

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView

interface ImageLoader {
    fun loadGridItemImage(context: Context, imageView: ImageView, uri: Uri)
    fun loadGridItemImage(context: Context, imageView: ImageView, bitmap: Bitmap)
    fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri)
}