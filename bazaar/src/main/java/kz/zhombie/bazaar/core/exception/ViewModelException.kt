package kz.zhombie.bazaar.core.exception

import androidx.lifecycle.ViewModelProvider

internal class ViewModelException constructor(
    private val factoryClass: ViewModelProvider.Factory,
    private val modelClass: Class<*>
) : IllegalStateException() {

    override val message: String
        get() = "Cannot create ${modelClass.simpleName} with [${factoryClass::class.java.simpleName}]"

}