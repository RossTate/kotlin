/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SequencesKt")

package kotlin.sequences

import kotlin.internal.InlineOnly
import kotlin.random.Random

/**
 * Given an [iterator] function constructs a [Sequence] that returns values through the [Iterator]
 * provided by that function.
 * The values are evaluated lazily, and the sequence is potentially infinite.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromIterator
 */
@kotlin.internal.InlineOnly
public inline fun <T> Sequence(local iterator: () -> Iterator<T>_{iterator}): Sequence<T>_{iterator} = object : Sequence<T> {
    override fun iterator(): Iterator<T>_{iterator} = iterator()
}

/**
 * Creates a sequence that returns all elements from this iterator. The sequence is constrained to be iterated only once.
 *
 * @sample samples.collections.Sequences.Building.sequenceFromIterator
 */
public fun <T> local Iterator<T>.asSequence(): Sequence<T>_{this} = Sequence { this }.constrainOnce()

/**
 * Creates a sequence that returns the specified values.
 *
 * @sample samples.collections.Sequences.Building.sequenceOfValues
 */
public fun <T> sequenceOf(local vararg elements: T): Sequence<T>_{elements} = elements.asSequence()

/**
 * Creates a [Sequence] that contains a single given element.
 *
 * @param element the single element to be contained in the resulting sequence.
 * @return a sequence containing only the specified [element].
 * @sample samples.collections.Sequences.Building.sequenceOfSingleValue
 */
@SinceKotlin("2.2")
public fun <T> sequenceOf(element: T): Sequence<T> = Sequence {
    object : Iterator<T> {
        private var _hasNext: Boolean = true

        override fun next(): T {
            if (!_hasNext) throw NoSuchElementException()
            _hasNext = false
            return element
        }

        override fun hasNext(): Boolean = _hasNext
    }
}

/**
 * Creates an empty [Sequence].
 *
 * @return an empty sequence.
 * @sample samples.collections.Sequences.Building.sequenceOfEmpty
 */
@SinceKotlin("2.2")
@InlineOnly
public inline fun <T> sequenceOf(): Sequence<T> = emptySequence()

/**
 * Returns an empty sequence.
 */
public fun <T> emptySequence(): Sequence<T> = EmptySequence

private object EmptySequence : Sequence<Nothing>, DropTakeSequence<Nothing> {
    override fun iterator(): Iterator<Nothing> = EmptyIterator
    override fun drop(n: Int) = EmptySequence
    override fun take(n: Int) = EmptySequence
}

/**
 * Returns this sequence if it's not `null` and the empty sequence otherwise.
 * @sample samples.collections.Sequences.Usage.sequenceOrEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun <T> local Sequence<T>?.orEmpty(): Sequence<T>_{this} = this ?: emptySequence()


/**
 * Returns a sequence that iterates through the elements either of this sequence
 * or, if this sequence turns out to be empty, of the sequence returned by [defaultValue] function.
 *
 * @sample samples.collections.Sequences.Usage.sequenceIfEmpty
 */
@SinceKotlin("1.3")
public fun <T> local Sequence<T>.ifEmpty(local defaultValue: () -> Sequence<T>_{defaultValue}): Sequence<T>_{this&defaultValue} = sequence {
    val iterator = this@ifEmpty.iterator()
    if (iterator.hasNext()) {
        yieldAll(iterator)
    } else {
        yieldAll(defaultValue())
    }
}

/**
 * Returns a sequence of all elements from all sequences in this sequence.
 *
 * The operation is _intermediate_ and _stateless_.
 *
 * @sample samples.collections.Sequences.Transformations.flattenSequenceOfSequences
 */
public fun <T> local Sequence<Sequence<T>_{this}>.flatten(): Sequence<T>_{this} = flatten { it.iterator() }

/**
 * Returns a sequence of all elements from all iterables in this sequence.
 *
 * The operation is _intermediate_ and _stateless_.
 *
 * @sample samples.collections.Sequences.Transformations.flattenSequenceOfLists
 */
@kotlin.jvm.JvmName("flattenSequenceOfIterable")
public fun <T> local Sequence<Iterable<T>_{this}>.flatten(): Sequence<T>_{this} = flatten { it.iterator() }

private fun <T, R> local Sequence<T>.flatten(iterator: (T) ->_{this} Iterator<R>_{this}): Sequence<R>_{this} {
    if (this is TransformingSequence<*, *>) {
        return (this as TransformingSequence<*, T>).flatten(iterator)
    }
    return FlatteningSequence(this, { it }, iterator)
}

/**
 * Returns a pair of lists, where
 * *first* list is built from the first values of each pair from this sequence,
 * *second* list is built from the second values of each pair from this sequence.
 *
 * The operation is _terminal_.
 *
 * @sample samples.collections.Sequences.Transformations.unzip
 */
public fun <T, R> local Sequence<Pair<T, R>>.unzip(): Pair<List<T>, List<R>> {
    val listT = ArrayList<T>()
    val listR = ArrayList<R>()
    for (pair in this) {
        listT.add(pair.first)
        listR.add(pair.second)
    }
    return listT to listR
}

/**
 * Returns a sequence that yields elements of this sequence randomly shuffled.
 *
 * Note that every iteration of the sequence returns elements in a different order.
 *
 * The operation is _intermediate_ and _stateful_.
 */
@SinceKotlin("1.4")
public fun <T> local Sequence<T>.shuffled(): Sequence<T>_{this} = shuffled(Random)

/**
 * Returns a sequence that yields elements of this sequence randomly shuffled
 * using the specified [random] instance as the source of randomness.
 *
 * Note that every iteration of the sequence returns elements in a different order.
 *
 * The operation is _intermediate_ and _stateful_.
 */
@SinceKotlin("1.4")
public fun <T> local Sequence<T>.shuffled(local random: Random): Sequence<T>_{this&random} = sequence<T> {
    val buffer = toMutableList()
    while (buffer.isNotEmpty()) {
        val j = random.nextInt(buffer.size)
        val last = buffer.removeLast()
        val value = if (j < buffer.size) buffer.set(j, last) else last
        yield(value)
    }
}


/**
 * A sequence that returns the values from the underlying [sequence] that either match or do not match
 * the specified [predicate].
 *
 * @param sendWhen If `true`, values for which the predicate returns `true` are returned. Otherwise,
 * values for which the predicate returns `false` are returned
 */
internal local class FilteringSequence<T>(
    private val sequence: Sequence<T>_{this},
    private val sendWhen: Boolean = true,
    private val predicate: (T) ->_{this} Boolean
) : Sequence<T> {

    override fun iterator(): Iterator<T>_{this} = object : Iterator<T> {
        val iterator = sequence.iterator()
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var nextItem: T? = null

        private fun calcNext() {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (predicate(item) == sendWhen) {
                    nextItem = item
                    nextState = 1
                    return
                }
            }
            nextState = 0
        }

        override fun next(): T {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem
            nextItem = null
            nextState = -1
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}

/**
 * A sequence which returns the results of applying the given [transformer] function to the values
 * in the underlying [sequence].
 */

internal local class TransformingSequence<T, R>
constructor(private val sequence: Sequence<T>_{this}, private val transformer: (T) ->_{this} R) : Sequence<R> {
    override fun iterator(): Iterator<R>_{this} = object : Iterator<R> {
        val iterator = sequence.iterator()
        override fun next(): R {
            return transformer(iterator.next())
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }

    internal fun <E> flatten(local iterator: (R) -> Iterator<E>_{iterator}): Sequence<E>_{this&iterator} {
        return FlatteningSequence<T, R, E>(sequence, transformer, iterator)
    }
}

/**
 * A sequence which returns the results of applying the given [transformer] function to the values
 * in the underlying [sequence], where the transformer function takes the index of the value in the underlying
 * sequence along with the value itself.
 */
internal local class TransformingIndexedSequence<T, R>
constructor(private val sequence: Sequence<T>_{this}, private val transformer: (Int, T) ->_{this} R) : Sequence<R> {
    override fun iterator(): Iterator<R>_{this} = object : Iterator<R> {
        val iterator = sequence.iterator()
        var index = 0
        override fun next(): R {
            return transformer(checkIndexOverflow(index++), iterator.next())
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }
}

/**
 * A sequence which combines values from the underlying [sequence] with their indices and returns them as
 * [IndexedValue] objects.
 */
internal local class IndexingSequence<T>
constructor(private val sequence: Sequence<T>_{this}) : Sequence<IndexedValue<T>> {
    override fun iterator(): Iterator<IndexedValue<T>>_{this} = object : Iterator<IndexedValue<T>> {
        val iterator = sequence.iterator()
        var index = 0
        override fun next(): IndexedValue<T> {
            return IndexedValue(checkIndexOverflow(index++), iterator.next())
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }
}

/**
 * A sequence which takes the values from two parallel underlying sequences, passes them to the given
 * [transform] function and returns the values returned by that function. The sequence stops returning
 * values as soon as one of the underlying sequences stops returning values.
 */
internal local class MergingSequence<T1, T2, V>
constructor(
    private val sequence1: Sequence<T1>_{this},
    private val sequence2: Sequence<T2>_{this},
    private val transform: (T1, T2) ->_{this} V
) : Sequence<V> {
    override fun iterator(): Iterator<V>_{this} = object : Iterator<V> {
        val iterator1 = sequence1.iterator()
        val iterator2 = sequence2.iterator()
        override fun next(): V {
            return transform(iterator1.next(), iterator2.next())
        }

        override fun hasNext(): Boolean {
            return iterator1.hasNext() && iterator2.hasNext()
        }
    }
}

internal local class FlatteningSequence<T, R, E>
constructor(
    private val sequence: Sequence<T>_{this},
    private val transformer: (T) ->_{this} R,
    private val iterator: (R) ->_{this} Iterator<E>_{this}
) : Sequence<E> {
    private object State {
        const val UNDEFINED = 0
        const val READY = 1
        const val DONE = 2
    }

    override fun iterator(): Iterator<E> = object : Iterator<E> {
        val iterator = sequence.iterator()
        var itemIterator: Iterator<E>_{this}? = null

        // Use state to avoid excessive ensureItemIterator calls.
        // The state is represented by the integer to avoid an overhead associated with the enum.
        var state = State.UNDEFINED

        override fun next(): E {
            if (state == State.DONE) throw NoSuchElementException()
            if (state == State.UNDEFINED && !ensureItemIterator()) {
                throw NoSuchElementException()
            }
            state = State.UNDEFINED
            return itemIterator!!.next()
        }

        override fun hasNext(): Boolean {
            if (state == State.READY) return true
            if (state == State.DONE) return false
            return ensureItemIterator()
        }

        private fun ensureItemIterator(): Boolean {
            val itemIterator = itemIterator
            if (itemIterator != null && itemIterator.hasNext()) {
                state = State.READY
                return true
            }

            while (iterator.hasNext()) {
                val element = iterator.next()
                val nextItemIterator = iterator(transformer(element))
                if (nextItemIterator.hasNext()) {
                    this.itemIterator = nextItemIterator
                    state = State.READY
                    return true
                }
            }

            state = State.DONE
            this.itemIterator = null
            return false
        }
    }
}

internal fun <T, C, R> flatMapIndexed(local source: Sequence<T>, local transform: (Int, T) -> C, local iterator: (C) -> Iterator<R>_{iterator}): Sequence<R>_{source&transform&iterator} =
    sequence {
        var index = 0
        for (element in source) {
            val result = transform(checkIndexOverflow(index++), element)
            yieldAll(iterator(result))
        }
    }

/**
 * A sequence that supports drop(n) and take(n) operations
 */
internal interface DropTakeSequence<T> : Sequence<T> {
    fun drop(n: Int): Sequence<T>_{this}
    fun take(n: Int): Sequence<T>_{this}
}

/**
 * A sequence that skips [startIndex] values from the underlying [sequence]
 * and stops returning values right before [endIndex], i.e. stops at `endIndex - 1`
 */
internal local class SubSequence<T>(
    private val sequence: Sequence<T>_{this},
    private val startIndex: Int,
    private val endIndex: Int
) : Sequence<T>, DropTakeSequence<T> {

    init {
        require(startIndex >= 0) { "startIndex should be non-negative, but is $startIndex" }
        require(endIndex >= 0) { "endIndex should be non-negative, but is $endIndex" }
        require(endIndex >= startIndex) { "endIndex should be not less than startIndex, but was $endIndex < $startIndex" }
    }

    private val count: Int get() = endIndex - startIndex

    override fun drop(n: Int): Sequence<T>_{this} = if (n >= count) emptySequence() else SubSequence(sequence, startIndex + n, endIndex)
    override fun take(n: Int): Sequence<T>_{this} = if (n >= count) this else SubSequence(sequence, startIndex, startIndex + n)

    override fun iterator(): Iterator<T>_{this} = object : Iterator<T> {

        val iterator = sequence.iterator()
        var position = 0

        // Shouldn't be called from constructor to avoid premature iteration
        private fun drop() {
            while (position < startIndex && iterator.hasNext()) {
                val _ = iterator.next()
                position++
            }
        }

        override fun hasNext(): Boolean {
            drop()
            return (position < endIndex) && iterator.hasNext()
        }

        override fun next(): T {
            drop()
            if (position >= endIndex)
                throw NoSuchElementException()
            position++
            return iterator.next()
        }
    }
}

/**
 * A sequence that returns at most [count] values from the underlying [sequence], and stops returning values
 * as soon as that count is reached.
 */
internal local class TakeSequence<T>(
    private val sequence: Sequence<T>_{this},
    private val count: Int
) : Sequence<T>, DropTakeSequence<T> {

    init {
        require(count >= 0) { "count must be non-negative, but was $count." }
    }

    override fun drop(n: Int): Sequence<T>_{this} = if (n >= count) emptySequence() else SubSequence(sequence, n, count)
    override fun take(n: Int): Sequence<T>_{this} = if (n >= count) this else TakeSequence(sequence, n)

    override fun iterator(): Iterator<T>_{this} = object : Iterator<T> {
        var left = count
        val iterator = sequence.iterator()

        override fun next(): T {
            if (left == 0)
                throw NoSuchElementException()
            left--
            return iterator.next()
        }

        override fun hasNext(): Boolean {
            return left > 0 && iterator.hasNext()
        }
    }
}

/**
 * A sequence that returns values from the underlying [sequence] while the [predicate] function returns
 * `true`, and stops returning values once the function returns `false` for the next element.
 */
internal local class TakeWhileSequence<T>
constructor(
    private val sequence: Sequence<T>_{this},
    private val predicate: (T) ->_{this} Boolean
) : Sequence<T> {
    override fun iterator(): Iterator<T>_{this} = object : Iterator<T> {
        val iterator = sequence.iterator()
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var nextItem: T? = null

        private fun calcNext() {
            if (iterator.hasNext()) {
                val item = iterator.next()
                if (predicate(item)) {
                    nextState = 1
                    nextItem = item
                    return
                }
            }
            nextState = 0
        }

        override fun next(): T {
            if (nextState == -1)
                calcNext() // will change nextState
            if (nextState == 0)
                throw NoSuchElementException()
            @Suppress("UNCHECKED_CAST")
            val result = nextItem as T

            // Clean next to avoid keeping reference on yielded instance
            nextItem = null
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext() // will change nextState
            return nextState == 1
        }
    }
}

/**
 * A sequence that skips the specified number of values from the underlying [sequence] and returns
 * all values after that.
 */
internal local class DropSequence<T>(
    private val sequence: Sequence<T>_{this},
    private val count: Int
) : Sequence<T>, DropTakeSequence<T> {
    init {
        require(count >= 0) { "count must be non-negative, but was $count." }
    }

    override fun drop(n: Int): Sequence<T>_{this} = (count + n).let { n1 -> if (n1 < 0) DropSequence(this, n) else DropSequence(sequence, n1) }
    override fun take(n: Int): Sequence<T>_{this} = (count + n).let { n1 -> if (n1 < 0) TakeSequence(this, n) else SubSequence(sequence, count, n1) }

    override fun iterator(): Iterator<T>_{this} = object : Iterator<T> {
        val iterator = sequence.iterator()
        var left = count

        // Shouldn't be called from constructor to avoid premature iteration
        private fun drop() {
            while (left > 0 && iterator.hasNext()) {
                val _ = iterator.next()
                left--
            }
        }

        override fun next(): T {
            drop()
            return iterator.next()
        }

        override fun hasNext(): Boolean {
            drop()
            return iterator.hasNext()
        }
    }
}

/**
 * A sequence that skips the values from the underlying [sequence] while the given [predicate] returns `true` and returns
 * all values after that.
 */
internal local class DropWhileSequence<T>
constructor(
    private val sequence: Sequence<T>_{this},
    private val predicate: (T) ->_{this} Boolean
) : Sequence<T> {

    override fun iterator(): Iterator<T>_{this} = object : Iterator<T> {
        val iterator = sequence.iterator()
        var dropState: Int = -1 // -1 for not dropping, 1 for nextItem, 0 for normal iteration
        var nextItem: T? = null

        private fun drop() {
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (!predicate(item)) {
                    nextItem = item
                    dropState = 1
                    return
                }
            }
            dropState = 0
        }

        override fun next(): T {
            if (dropState == -1)
                drop()

            if (dropState == 1) {
                @Suppress("UNCHECKED_CAST")
                val result = nextItem as T
                nextItem = null
                dropState = 0
                return result
            }
            return iterator.next()
        }

        override fun hasNext(): Boolean {
            if (dropState == -1)
                drop()
            return dropState == 1 || iterator.hasNext()
        }
    }
}

internal local class DistinctSequence<T, K>(private val source: Sequence<T>_{this}, private val keySelector: (T) ->_{this} K) : Sequence<T> {
    override fun iterator(): Iterator<T>_{this} = DistinctIterator(source.iterator(), keySelector)
}

private local class DistinctIterator<T, K>(private val source: Iterator<T>_{this}, private val keySelector: (T) ->_{this} K) : AbstractIterator<T>() {
    private val observed = HashSet<K>()

    override fun computeNext() {
        while (source.hasNext()) {
            val next = source.next()
            val key = keySelector(next)

            if (observed.add(key)) {
                setNext(next)
                return
            }
        }

        done()
    }
}


private local class GeneratorSequence<T : Any>(private val getInitialValue: () ->_{this} T?, private val getNextValue: (T) ->_{this} T?) : Sequence<T> {
    override fun iterator(): Iterator<T>_{this} = object : Iterator<T> {
        var nextItem: T? = null
        var nextState: Int = -2 // -2 for initial unknown, -1 for next unknown, 0 for done, 1 for continue

        private fun calcNext() {
            nextItem = if (nextState == -2) getInitialValue() else getNextValue(nextItem!!)
            nextState = if (nextItem == null) 0 else 1
        }

        override fun next(): T {
            if (nextState < 0)
                calcNext()

            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem as T
            // Do not clean nextItem (to avoid keeping reference on yielded instance) -- need to keep state for getNextValue
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState < 0)
                calcNext()
            return nextState == 1
        }
    }
}

/**
 * Returns a wrapper sequence that provides values of this sequence, but ensures it can be iterated only one time.
 *
 * The operation is _intermediate_ and _stateless_.
 *
 * [IllegalStateException] is thrown on iterating the returned sequence for the second time and the following times.
 *
 */
public fun <T> local Sequence<T>.constrainOnce(): Sequence<T>_{this} {
    // as? does not work in js
    //return this as? ConstrainedOnceSequence<T> ?: ConstrainedOnceSequence(this)
    return if (this is ConstrainedOnceSequence<T>) this else ConstrainedOnceSequence(this)
}


/**
 * Returns a sequence which invokes the function to calculate the next value on each iteration until the function returns `null`.
 *
 * The returned sequence is constrained to be iterated only once.
 *
 * @see constrainOnce
 * @see kotlin.sequences.sequence
 *
 * @sample samples.collections.Sequences.Building.generateSequence
 */
public fun <T : Any> generateSequence(local nextFunction: () -> T?): Sequence<T>_{nextFunction} {
    return GeneratorSequence(nextFunction, { nextFunction() }).constrainOnce()
}

/**
 * Returns a sequence defined by the starting value [seed] and the function [nextFunction],
 * which is invoked to calculate the next value based on the previous one on each iteration.
 *
 * The sequence produces values until it encounters first `null` value.
 * If [seed] is `null`, an empty sequence is produced.
 *
 * The sequence can be iterated multiple times, each time starting with [seed].
 *
 * @see kotlin.sequences.sequence
 *
 * @sample samples.collections.Sequences.Building.generateSequenceWithSeed
 */
@kotlin.internal.LowPriorityInOverloadResolution
public fun <T : Any> generateSequence(seed: T?, local nextFunction: (T) -> T?): Sequence<T>_{nextFunction} =
    if (seed == null)
        EmptySequence
    else
        GeneratorSequence({ seed }, nextFunction)

/**
 * Returns a sequence defined by the function [seedFunction], which is invoked to produce the starting value,
 * and the [nextFunction], which is invoked to calculate the next value based on the previous one on each iteration.
 *
 * The sequence produces values until it encounters first `null` value.
 * If [seedFunction] returns `null`, an empty sequence is produced.
 *
 * The sequence can be iterated multiple times.
 *
 * @see kotlin.sequences.sequence
 *
 * @sample samples.collections.Sequences.Building.generateSequenceWithLazySeed
 */
public fun <T : Any> generateSequence(local seedFunction: () -> T?, local nextFunction: (T) -> T?): Sequence<T>_{seedFunction&nextFunction} =
    GeneratorSequence(seedFunction, nextFunction)
