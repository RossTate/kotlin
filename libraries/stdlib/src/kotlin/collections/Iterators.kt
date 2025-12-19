/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections


/**
 * Returns the given iterator itself. This allows to use an instance of iterator in a `for` loop.
 * @sample samples.collections.Iterators.iterator
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> local Iterator<T>.iterator(): Iterator<T>_{this} = this

/**
 * Returns an [Iterator] that wraps each element produced by the original iterator
 * into an [IndexedValue] containing the index of that element and the element itself.
 *
 * @sample samples.collections.Iterators.withIndexIterator
 */
public fun <T> local Iterator<T>.withIndex(): Iterator<IndexedValue<T>>_{this} = IndexingIterator(this)

/**
 * Performs the given [operation] on each element of this [Iterator].
 * @sample samples.collections.Iterators.forEachIterator
 */
public inline fun <T> local Iterator<T>.forEach(local operation: (T) -> Unit): Unit {
    for (element in this) operation(element)
}

/**
 * Iterator transforming original `iterator` into iterator of [IndexedValue], counting index from zero.
 */
internal class IndexingIterator<out T>(private val iterator: Iterator<T>_{this}) : Iterator<IndexedValue<T>> {
    private var index = 0
    final override fun hasNext(): Boolean = iterator.hasNext()
    final override fun next(): IndexedValue<T> = IndexedValue(checkIndexOverflow(index++), iterator.next())
}
