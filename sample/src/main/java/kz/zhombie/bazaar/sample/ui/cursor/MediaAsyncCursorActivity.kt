package kz.zhombie.bazaar.sample.ui.cursor

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kz.garage.multimedia.store.model.Content
import kz.zhombie.bazaar.sample.R

class MediaAsyncCursorActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<List<Content>> {

    companion object {
        private const val TAG = "LoaderActivity"
    }

    private var recyclerView: RecyclerView? = null

    private val adapter = MediaAsyncCursorAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paging)

//        Bazaar.setImageLoader(GlideImageLoader(this))

        LoaderManager.getInstance(this)
            .initLoader(123, null, this)

        recyclerView = findViewById(R.id.recyclerView)

        recyclerView?.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        recyclerView?.adapter = adapter
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Content>> {
        Log.d(TAG, "onCreateLoader() -> $id, $args")

        return MediaAsyncCursorTaskLoader(this)
    }

    override fun onLoadFinished(loader: Loader<List<Content>>, data: List<Content>?) {
        Log.d(TAG, "onLoadFinished() -> $loader, $data")

        if (data != null) {
            adapter.contents = data
        }
    }

    override fun onLoaderReset(loader: Loader<List<Content>>) {
        Log.d(TAG, "onLoaderReset() -> $loader")
    }

}