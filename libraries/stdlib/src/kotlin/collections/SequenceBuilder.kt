/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SequencesKt")
@file:OptIn(ExperimentalTypeInference::class)

package kotlin.sequences

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builds a [Sequence] lazily yielding values one by one.
 *
 * @see kotlin.sequences.generateSequence
 *
 * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
 * @sample samples.collections.Sequences.Building.buildFibonacciSequence
 */
@SinceKotlin("1.3")
@Suppress("DEPRECATION")
public fun <T> sequence(@BuilderInference local block: local SequenceScope<T, block>.() -> Unit): Sequence<T>_{block} = Sequence { iterator(block) }

/**
 * Builds an [Iterator] lazily yielding values one by one.
 *
 * @sample samples.collections.Sequences.Building.buildIterator
 * @sample samples.collections.Iterables.Building.iterable
 */
@SinceKotlin("1.3")
@Suppress("DEPRECATION")
public fun <T> iterator(@BuilderInference local block: local SequenceScope<T, block>.() -> Unit): Iterator<T>_{block} = object : Generator<T>() {
    private interface Yielder {
        fun yield(local StackRoot, T)
        fun yieldAll(local StackRoot, Iterator<T>_{block})
    }
    var stack: Stack? = Stack()
    var yielder: Yielder?
    var suspension: StackSuspension<Unit, Unit>_{block}? = null

    override fun generate(local yieldReturn: (T) -> Nothing, local yieldAllReturn: (Iterator<T>_{block}) -> Nothing) = mount { guardian ->
        stack?.attach {
            yielder = object : Yielder {
                override fun yield(local root: StackRoot, element: T) {
                    guardian.suspend(Unit) {
                        yielder = null
                        suspension = it
                        yieldReturn(element)
                    }
                }
                override fun yieldAll(local root: StackRoot, iterator: Iterator<T>_{block}) {
                    guardian.suspend(Unit) {
                        yielder = null
                        suspension = it
                        yieldAllReturn(element)
                    }
                }
            }
            if (suspension == null) {
                stack.execute { root ->
                    object scope : SequenceScope<T, block>() {
                        override fun yield(value: T) {
                            yielder!.yield(root, value)
                        }
                        override fun yieldAll(iterator: Iterator<T>_{block}) {
                            yielder!.yieldAll(root, iterator)
                        }
                    }
                    scope.block()
                    guardian.consult {
                        return@generate
                    }
                }
            } else {
                suspension.resume { Unit }
            }
        }
    }
}

private local abstract class Generator<out T>: AbstractIterator<T>() {
    private[this] var after: Iterator<T>_{this}? = null
    override fun computeNext() {
        after?.let {
            if (it.hasNext()) {
                setNext(it.next())
                return@computeNext
            } else {
                after = null
            }
        }
        generate(
            {
                setNext(it)
                return@computeNext
            },
            {
                if (it.hasNext()) {
                    setNext(it.next())
                    after = it
                    return@computeNext
                }
            }
        )
    }
    abstract fun generate(local yield: (T) -> Nothing, local yieldAll: (Iterator<T>_{this}) -> Nothing)
}

/**
 * The scope for yielding values of a [Sequence] or an [Iterator], provides [yield] and [yieldAll] suspension functions.
 *
 * @see sequence
 * @see iterator
 *
 * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
 * @sample samples.collections.Sequences.Building.buildFibonacciSequence
 */
@RestrictsSuspension
@SinceKotlin("1.3")
public local abstract class SequenceScope<in T, out local owner> internal constructor() {
    /**
     * Yields a value to the [Iterator] being built and suspends
     * until the next value is requested.
     *
     * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
     * @sample samples.collections.Sequences.Building.buildFibonacciSequence
     */
    public abstract fun yield(value: T)

    /**
     * Yields all values from the `iterator` to the [Iterator] being built
     * and suspends until all these values are iterated and the next one is requested.
     *
     * The sequence of values returned by the given iterator can be potentially infinite.
     *
     * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
     */
    public abstract fun yieldAll(iterator: Iterator<T>_{owner})

    /**
     * Yields a collections of values to the [Iterator] being built
     * and suspends until all these values are iterated and the next one is requested.
     *
     * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
     */
    public fun yieldAll(elements: Iterable<T>_{owner}) {
        if (elements is Collection && elements.isEmpty()) return
        return yieldAll(elements.iterator())
    }

    /**
     * Yields potentially infinite sequence of values  to the [Iterator] being built
     * and suspends until all these values are iterated and the next one is requested.
     *
     * The sequence can be potentially infinite.
     *
     * @sample samples.collections.Sequences.Building.buildSequenceYieldAll
     */
    public fun yieldAll(sequence: Sequence<T>_{owner}): Unit = yieldAll(sequence.iterator())
}
