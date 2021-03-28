package kz.zhombie.bazaar.api.core

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import kz.zhombie.museum.ArtworkLoader

interface ImageLoader : ArtworkLoader {
    fun loadGridItemImage(context: Context, imageView: ImageView, uri: Uri)
    fun loadGridItemImage(context: Context, imageView: ImageView, bitmap: Bitmap)
}