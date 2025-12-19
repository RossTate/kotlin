/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.sequences

internal expect local class ConstrainedOnceSequence<T> : Sequence<T> {
    constructor(local sequence: Sequence<T>_{this})

    override fun iterator(): Iterator<T>_{this}
}
