package kz.zhombie.bazaar

import android.content.ContentUris
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kz.zhombie.bazaar.model.Image
import kz.zhombie.bazaar.model.Video
import java.util.concurrent.TimeUnit

class MediaStoreFragment : BottomSheetDialogFragment() {

    companion object {
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
    }

    private fun readImageAtCursor(cursor: Cursor): Image? {
        try {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID))
            val externalContentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.TITLE))
            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME))
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SIZE))

            var dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_ADDED))
            dateAdded = TimeUnit.SECONDS.toMillis(dateAdded)

            var dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_MODIFIED))
            dateModified = TimeUnit.SECONDS.toMillis(dateModified)

            val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN))
            } else {
                -1L
            }

            val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MIME_TYPE))
            val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.HEIGHT))

            return Image(
                id = id,
                uri = externalContentUri,
                title = title,
                displayName = displayName,
                size = size,
                dateAdded = dateAdded,
                dateModified = dateModified,
                dateCreated = dateTaken,
                mimeType = mimeType,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun readVideoAtCursor(cursor: Cursor): Video? {
        try {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns._ID))
            val externalContentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.TITLE))
            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DISPLAY_NAME))
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.SIZE))

            var dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_ADDED))
            dateAdded = TimeUnit.SECONDS.toMillis(dateAdded)

            var dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_MODIFIED))
            dateModified = TimeUnit.SECONDS.toMillis(dateModified)

            val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN))
            } else {
                -1L
            }

            val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.MIME_TYPE))
            val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.HEIGHT))

            val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION))
            } else {
                null
            }

            return Video(
                id = id,
                uri = externalContentUri,
                title = title,
                displayName = displayName,
                size = size,
                dateAdded = dateAdded,
                dateModified = dateModified,
                dateCreated = dateTaken,
                mimeType = mimeType,
                width = width,
                height = height,
                duration = duration,
                cover = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}