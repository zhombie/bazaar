package kz.zhombie.bazaar.ui.arbat.museum

import android.content.Context
import android.net.Uri
import android.widget.ImageView

interface ArtworkLoader {
    fun loadFullscreenImage(context: Context, imageView: ImageView, uri: Uri)
}