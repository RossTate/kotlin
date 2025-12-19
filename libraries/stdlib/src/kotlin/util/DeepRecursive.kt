/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

/**
 * Defines deep recursive function that keeps its stack on the heap,
 * which allows very deep recursive computations that do not use the actual call stack.
 * To initiate a call to this deep recursive function use its [invoke] function.
 * As a rule of thumb, it should be used if recursion goes deeper than a thousand calls.
 *
 * The [DeepRecursiveFunction] takes one parameter of type [T] and returns a result of type [R].
 * The [block] of code defines the body of a recursive function. In this block
 * [callRecursive][DeepRecursiveScope.callRecursive] function can be used to make a recursive call
 * to the declared function. Other instances of [DeepRecursiveFunction] can be called
 * in this scope with `callRecursive` extension, too.
 *
 * For example, take a look at the following recursive tree class and a deeply
 * recursive instance of this tree with 100K nodes:
 *
 * ```
 * class Tree(val left: Tree? = null, val right: Tree? = null)
 * val deepTree = generateSequence(Tree()) { Tree(it) }.take(100_000).last()
 * ```
 *
 * A regular recursive function can be defined to compute a depth of a tree:
 *
 * ```
 * fun depth(t: Tree?): Int =
 *     if (t == null) 0 else max(depth(t.left), depth(t.right)) + 1
 * println(depth(deepTree)) // StackOverflowError
 * ```
 *
 * If this `depth` function is called for a `deepTree` it produces `StackOverflowError` because of deep recursion.
 * However, the `depth` function can be rewritten using `DeepRecursiveFunction` in the following way, and then
 * it successfully computes [`depth(deepTree)`][DeepRecursiveFunction.invoke] expression:
 *
 * ```
 * val depth = DeepRecursiveFunction<Tree?, Int> { t ->
 *     if (t == null) 0 else max(callRecursive(t.left), callRecursive(t.right)) + 1
 * }
 * println(depth(deepTree)) // Ok
 * ```
 *
 * Deep recursive functions can also mutually call each other using a heap for the stack via
 * [callRecursive][DeepRecursiveScope.callRecursive] extension. For example, the
 * following pair of mutually recursive functions computes the number of tree nodes at even depth in the tree.
 *
 * ```
 * val mutualRecursion = object {
 *     val even: DeepRecursiveFunction<Tree?, Int> = DeepRecursiveFunction { t ->
 *         if (t == null) 0 else odd.callRecursive(t.left) + odd.callRecursive(t.right) + 1
 *     }
 *     val odd: DeepRecursiveFunction<Tree?, Int> = DeepRecursiveFunction { t ->
 *         if (t == null) 0 else even.callRecursive(t.left) + even.callRecursive(t.right)
 *     }
 * }
 * ```
 *
 * @param [T] the function parameter type.
 * @param [R] the function result type.
 * @param block the function body.
 */
@SinceKotlin("1.7")
public local class DeepRecursiveFunction<T, R>(
    internal val block: local DeepRecursiveScope<T, R>.(T) ->_{this} R
)

/**
 * Initiates a call to this deep recursive function, forming a root of the call tree.
 *
 * This operator should not be used from inside of [DeepRecursiveScope] as it uses the call stack slot for
 * initial recursive invocation. From inside of [DeepRecursiveScope] use
 * [callRecursive][DeepRecursiveScope.callRecursive].
 */
@SinceKotlin("1.7")
public operator fun <T, R> local DeepRecursiveFunction<T, R>.invoke(value: T): R =
    DeepRecursiveScopeImpl<T, R>(this).with { block(value) }

/**
 * A scope class for [DeepRecursiveFunction] function declaration that defines [callRecursive] methods to
 * recursively call this function or another [DeepRecursiveFunction] putting the call activation frame on the heap.
 *
 * @param [T] function parameter type.
 * @param [R] function result type.
 */
@SinceKotlin("1.7")
@WasExperimental(ExperimentalStdlibApi::class)
public local sealed class DeepRecursiveScope<T, R> {
    /**
     * Makes recursive call to this [DeepRecursiveFunction] function putting the call activation frame on the heap,
     * as opposed to the actual call stack that is used by a regular recursive call.
     */
    public abstract fun callRecursive(value: T): R

    /**
     * Makes call to the specified [DeepRecursiveFunction] function putting the call activation frame on the heap,
     * as opposed to the actual call stack that is used by a regular call.
     */
    public abstract fun <U, S> local DeepRecursiveFunction<U, S>.callRecursive(value: U): S

    @Deprecated(
        level = DeprecationLevel.ERROR,
        message =
        "'invoke' should not be called from DeepRecursiveScope. " +
                "Use 'callRecursive' to do recursion in the heap instead of the call stack.",
        replaceWith = ReplaceWith("this.callRecursive(value)")
    )
    @Suppress("UNUSED_PARAMETER")
    public operator fun DeepRecursiveFunction<*, *>.invoke(value: Any?): Nothing =
        throw UnsupportedOperationException("Should not be called from DeepRecursiveScope")
}

// ================== Implementation ==================

private local class DeepRecursiveScopeImpl<T, R>(
    function: DeepRecursiveFunction<T, R>_{this}
) : DeepRecursiveScope<T, R>() {
    override fun callRecursive(value: T): R = onFreshStack {
        function.block(value)
    }

    override fun <U, S> local DeepRecursiveFunction<U, S>.callRecursive(value: U): S =
        DeepRecursiveScopeImpl(this).callRecursive(value)
}
