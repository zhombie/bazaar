package kz.zhombie.bazaar

import android.app.Application
import kz.zhombie.bazaar.api.core.ImageLoader
import kz.zhombie.bazaar.loader.CoilImageLoader
import kz.zhombie.cinema.Cinema
import kz.zhombie.museum.Museum
import kz.zhombie.museum.PaintingLoader
import kz.zhombie.radio.Radio

class App : Application(),
    Bazaar.Factory,
    Cinema.Factory,
    Museum.Factory,
    Radio.Factory,
    ImageLoader.Factory,
    PaintingLoader.Factory {

    companion object {
        private val TAG = App::class.java.simpleName
    }

    private var imageLoader: ImageLoader? = null

    override fun getBazaarConfiguration(): Bazaar.Configuration =
        Bazaar.Configuration(getLoggingEnabled())

    override fun getCinemaConfiguration(): Cinema.Configuration =
        Cinema.Configuration(getLoggingEnabled())

    override fun getMuseumConfiguration(): Museum.Configuration =
        Museum.Configuration(getLoggingEnabled())

    override fun getRadioConfiguration(): Radio.Configuration =
        Radio.Configuration(getLoggingEnabled())

    override fun getImageLoader(): ImageLoader =
        getImageLoaderInternally()

    override fun getPaintingLoader(): PaintingLoader =
        getImageLoaderInternally()

    private fun getLoggingEnabled(): Boolean = BuildConfig.DEBUG

    private fun getImageLoaderInternally(): ImageLoader {
        if (imageLoader == null) {
            imageLoader = CoilImageLoader(this, getLoggingEnabled())
        }
        return requireNotNull(imageLoader)
    }

}