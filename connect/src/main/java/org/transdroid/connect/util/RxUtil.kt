package org.transdroid.connect.util

import io.reactivex.Flowable
import io.reactivex.Single

fun <T : Any> Single<Array<T>>.flatten(): Flowable<T> = this.flattenAsFlowable { items -> items.toList() }
