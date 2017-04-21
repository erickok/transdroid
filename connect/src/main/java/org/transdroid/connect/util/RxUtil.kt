package org.transdroid.connect.util

import io.reactivex.Flowable

fun <T : Any> Flowable<Array<T>>.flatten(): Flowable<T> = this.flatMapIterable { items -> items.toList() }
