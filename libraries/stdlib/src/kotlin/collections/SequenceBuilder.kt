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
public fun <T> iterator(@BuilderInference local block: local SequenceScope<T, block>.() -> Unit): Iterator<T>_{block} = object : AbstractIterator<T>() {
    inner local class YieldEnvironment(
        private val exit: () ->_{this} Nothing
    ) : ReturningStackEnvironment<Unit> {
        override fun returnIt(_: Unit): Nothing {
            done()
            exit()
        }
        fun suspendYield(
            local resumption: StackResumption<YieldEnvironment, () -> Nothing, block, this>,
            value: T,
            additional: Iteraot<T>_{block}? = null
        ) {
            resumption.suspend {
                suspension = it
                setNext(value)
                queued = additional
                exit()
            }
        }
    }
    
    var queued: Iterator<T>_{block}? = null
    var suspension: StackSuspension<YieldEnvironment, () -> Nothing, global>_{block}? = null
    
    override fun computeNext() {
        queued?.let {
            if (it.hasNext()) {
                setNext(it.next())
                return@computeNext
            } else {
                queued = null
            }
        }
        if (suspension == null) {
            StackResource().initiate(YieldEnvironment { return@computeNext }) { root ->
                object : SequenceScope<T, block>() {
                    override fun yield(value: T) {
                        root.loosen { (environment, resumption) ->
                            environment.suspendYield(resumption, value)
                        }
                    }
                    override fun yieldAll(iterator: Iterator<T>_{block}) {
                        if (iterator.hasNext())
                            root.loosen { (environment, resumption) ->
                                environment.suspendYield(resumption, iterator.next(), iterator)
                            }
                    }
                }.block()
            }
        } else {
            suspension.resume(YieldEnvironment { return@computeNext }) { it() }
        }
    }
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
