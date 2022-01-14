package kz.zhombie.bazaar.sample.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kz.zhombie.bazaar.sample.R

class UIViewHolder constructor(activity: AppCompatActivity) {

    val imageLoaderView: MaterialTextView = activity.findViewById(R.id.imageLoaderView)
    val imageLoaderButton: MaterialButton = activity.findViewById(R.id.imageLoaderButton)
    val modeView: MaterialTextView = activity.findViewById(R.id.modeView)
    val modeButton: MaterialButton = activity.findViewById(R.id.modeButton)
    val maxSelectionCountView: MaterialTextView = activity.findViewById(R.id.maxSelectionCountView)
    val maxSelectionCountButton: MaterialButton = activity.findViewById(R.id.maxSelectionCountButton)
    val showButton: MaterialButton = activity.findViewById(R.id.showButton)
    val recyclerView: RecyclerView = activity.findViewById(R.id.recyclerView)

}