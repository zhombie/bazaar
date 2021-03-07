package kz.zhombie.bazaar.core.exception

import androidx.recyclerview.widget.RecyclerView

internal class ViewHolderException constructor(private val viewType: Int) : IllegalStateException() {

    override val message: String
        get() = "There is no [${RecyclerView.ViewHolder::class.java.simpleName}] for this [${viewType}]."

}