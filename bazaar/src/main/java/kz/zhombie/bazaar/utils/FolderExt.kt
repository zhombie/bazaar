package kz.zhombie.bazaar.utils

import kotlin.random.Random

// TODO: Remove after garage upgrade
internal fun generateId(): Long =
    (System.currentTimeMillis() + Random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE))
