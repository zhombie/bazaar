package kz.zhombie.bazaar.api.core

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import kz.zhombie.museum.PaintingLoader

interface ImageLoader : PaintingLoader {
    override fun loadSmallImage(context: Context, imageView: ImageView, uri: Uri)
    fun loadSmallImage(context: Context, imageView: ImageView, bitmap: Bitmap)
}