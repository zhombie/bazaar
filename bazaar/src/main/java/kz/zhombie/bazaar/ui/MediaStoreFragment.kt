package kz.zhombie.bazaar.ui

import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kz.zhombie.bazaar.R
import kz.zhombie.bazaar.Settings
import kz.zhombie.bazaar.core.Logger
import kz.zhombie.bazaar.model.Image
import kz.zhombie.bazaar.model.Media
import kz.zhombie.bazaar.model.Video
import kz.zhombie.bazaar.ui.model.UIMedia
import kz.zhombie.bazaar.utils.ContentResolverCompat
import kz.zhombie.bazaar.utils.readImage
import kz.zhombie.bazaar.utils.readVideo

class MediaStoreFragment : BottomSheetDialogFragment(), MediaAdapter.Callback {

    companion object {
        private val TAG: String = MediaStoreFragment::class.java.simpleName

        fun newInstance(): MediaStoreFragment {
            val fragment = MediaStoreFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    private var titleButton: MaterialButton? = null
    private var recyclerView: RecyclerView? = null
    private var selectButton: MaterialButton? = null

    private lateinit var viewModel: MediaStoreViewModel

    private var headerAdapter: HeaderAdapter? = null
    private var mediaAdapter: MediaAdapter? = null
    private var concatAdapter: ConcatAdapter? = null

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

        titleButton = view.findViewById(R.id.titleButton)
        recyclerView = view.findViewById(R.id.recyclerView)
        selectButton = view.findViewById(R.id.selectButton)

        setupHeaderView()
        setupRecyclerView()
        setupSelectButton()

        loadImages()

        viewModel.getSelectedMedia().observe(viewLifecycleOwner, {
        })

        viewModel.getAllMedia().observe(viewLifecycleOwner, {
            mediaAdapter?.submitList(it)
        })
    }

    private fun setupHeaderView() {
        titleButton?.text = "Название альбома"
    }

    private fun setupRecyclerView() {
        Logger.d(TAG, "setupRecyclerView()")

        headerAdapter = HeaderAdapter()
        mediaAdapter = MediaAdapter(Settings.getImageLoader(), this)
        recyclerView?.adapter = ConcatAdapter(headerAdapter, mediaAdapter)

        val layoutManager = GridLayoutManager(
            context,
            3,
            GridLayoutManager.VERTICAL,
            false
        )

        recyclerView?.layoutManager = layoutManager

        recyclerView?.addItemDecoration(
            SpacingItemDecoration(
                requireContext().resources.getDimensionPixelOffset(R.dimen.item_margin_left),
                requireContext().resources.getDimensionPixelOffset(R.dimen.item_margin_top),
                requireContext().resources.getDimensionPixelOffset(R.dimen.item_margin_right),
                requireContext().resources.getDimensionPixelOffset(R.dimen.item_margin_bottom)
            )
        )
    }

    private fun setupSelectButton() {
        selectButton?.text = buildSpannedString {
            val title = "Выбрать"
            val subtitle = "Выбрано 5 файлов"

            append(title + "\n" + subtitle)

            setSpan(ForegroundColorSpan(Color.parseColor("#333333")), 0, title.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, title.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            setSpan(RelativeSizeSpan(0.7F), title.length, title.length + subtitle.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(Color.parseColor("#8290A0")), title.length, title.length + subtitle.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
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
                    viewModel.onMediaLoaded(data)
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

    override fun onImageCheckboxClicked(uiMedia: UIMedia) {
        viewModel.onImageCheckboxClicked(uiMedia)
    }

    override fun onVideoCheckboxClicked(position: Int, video: Video, isSelected: Boolean) {

    }

}