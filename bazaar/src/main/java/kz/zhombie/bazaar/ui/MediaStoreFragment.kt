package kz.zhombie.bazaar.ui

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.core.Logger
import kz.zhombie.bazaar.model.Image
import kz.zhombie.bazaar.model.Media
import kz.zhombie.bazaar.model.Video
import kz.zhombie.bazaar.utils.ContentResolverCompat
import kz.zhombie.bazaar.utils.readImage
import kz.zhombie.bazaar.utils.readVideo

class MediaStoreFragment : BottomSheetDialogFragment() {

    companion object {
        private val TAG: String = MediaStoreFragment::class.java.simpleName

        fun newInstance(): MediaStoreFragment {
            val fragment = MediaStoreFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private var appBarLayout: AppBarLayout? = null
    private var toolbar: MaterialToolbar? = null
    private var recyclerView: RecyclerView? = null

    private lateinit var viewModel: MediaStoreViewModel

    private var adapter: MediaAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, MediaStoreViewModelFactory())
            .get(MediaStoreViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media_store, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appBarLayout = view.findViewById(R.id.appBarLayout)
        toolbar = view.findViewById(R.id.toolbar)
        recyclerView = view.findViewById(R.id.recyclerView)

        setupRecyclerView()

        loadImages()
    }

    private fun setupRecyclerView() {
        Logger.d(TAG, "setupRecyclerView()")
        adapter = MediaAdapter(Settings.getImageLoader())
        recyclerView?.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        recyclerView?.adapter = adapter
    }

    private fun loadImages() {
        Logger.d(TAG, "loadImages()")
        lifecycleScope.launch(Dispatchers.IO) {
            val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = ContentResolverCompat.getProjection(ContentResolverCompat.Type.IMAGE)
            val selection: String? = null
            val selectionArgs: MutableList<String>? = null
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context?.contentResolver
                ?.query(uri, projection, selection, selectionArgs?.toTypedArray(), sortOrder)
                ?.use { cursor ->
                    val data = cursor.mapTo(Image::class.java)
                    withContext(Dispatchers.Main) {
                        adapter?.submitList(data)
                    }
                }
        }
    }

    private fun <T> Cursor.mapTo(clazz: Class<T>): List<Media> {
        Logger.d(TAG, "$count items to $clazz, ${clazz == Image::class.java}")
        val array = arrayListOf<Media>()
        array.addAll(
            generateSequence { if (moveToNext()) this else null }
                .map {
                    when (clazz) {
                        Image::class.java -> it.readImage()
                        Video::class.java -> it.readVideo()
                        else -> null
                    }
                }
                .filterNotNull()
        )
        return array
    }

}