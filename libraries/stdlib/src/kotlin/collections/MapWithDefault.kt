/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package kotlin.collections

/**
 * Returns the value for the given key, or the implicit default value for this map.
 * By default no implicit value is provided for maps and a [NoSuchElementException] is thrown.
 * To create a map with implicit default value use [withDefault] method.
 *
 * @throws NoSuchElementException when the map doesn't contain a value for the specified key and no implicit default was provided for that map.
 */
@kotlin.jvm.JvmName("getOrImplicitDefaultNullable")
@PublishedApi
internal fun <K, V> local Map<K, V>.getOrImplicitDefault(key: K): V {
    if (this is MapWithDefault)
        return this.getOrImplicitDefault(key)

    return getOrElseNullable(key, { throw NoSuchElementException("Key $key is missing in the map.") })
}

/**
 * Returns a wrapper of this read-only map, having the implicit default value provided with the specified function [defaultValue].
 *
 * This implicit default value is used when the original map doesn't contain a value for the key specified
 * and a value is obtained with [Map.getValue] function, for example when properties are delegated to the map.
 *
 * When this map already has an implicit default value provided with a former call to [withDefault], it is being replaced by this call.
 *
 * @sample samples.collections.Maps.Usage.getValueWithDefault
 * @sample samples.collections.Maps.Usage.getValueWithReplacedDefault
 */
public fun <K, V> local Map<K, V>.withDefault(local defaultValue: (key: K) -> V): Map<K, V>_{this&defaultValue} =
    when (this) {
        is MapWithDefault -> this.map.withDefault(defaultValue)
        else -> MapWithDefaultImpl(this, defaultValue)
    }

/**
 * Returns a wrapper of this mutable map, having the implicit default value provided with the specified function [defaultValue].
 *
 * This implicit default value is used when the original map doesn't contain a value for the key specified
 * and a value is obtained with [Map.getValue] function, for example when properties are delegated to the map.
 *
 * When this map already has an implicit default value provided with a former call to [withDefault], it is being replaced by this call.
 *
 * @sample samples.collections.Maps.Usage.getValueWithDefault
 * @sample samples.collections.Maps.Usage.getValueWithReplacedDefault
 * @sample samples.collections.Maps.Usage.changesToMutableMapWithDefaultPropagateToUnderlyingMap
 */
@kotlin.jvm.JvmName("withDefaultMutable")
public fun <K, V> local MutableMap<K, V>.withDefault(local defaultValue: (key: K) -> V): MutableMap<K, V>_{this&defaultValue} =
    when (this) {
        is MutableMapWithDefault -> this.map.withDefault(defaultValue)
        else -> MutableMapWithDefaultImpl(this, defaultValue)
    }


private interface MapWithDefault<K, out V> : Map<K, V> {
    public val map: Map<K, V>_{this}
    public fun getOrImplicitDefault(key: K): V
}

private interface MutableMapWithDefault<K, V> : MutableMap<K, V>, MapWithDefault<K, V> {
    public override val map: MutableMap<K, V>_{this}
}


private local class MapWithDefaultImpl<K, out V>(public override val map: Map<K, V>_{this}, private val default: (key: K) ->_{this} V) : MapWithDefault<K, V> {
    override fun equals(other: Any?): Boolean = map.equals(other)
    override fun hashCode(): Int = map.hashCode()
    override fun toString(): String = map.toString()
    override val size: Int get() = map.size
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: @UnsafeVariance V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map.get(key)
    override val keys: Set<K>_{this} get() = map.keys
    override val values: Collection<V> get()_{this} = map.values
    override val entries: Set<Map.Entry<K, V>>_{this} get() = map.entries

    override fun getOrImplicitDefault(key: K): V = map.getOrElseNullable(key, { default(key) })
}

private local class MutableMapWithDefaultImpl<K, V>(public override val map: MutableMap<K, V>_{this}, private val default: (key: K) ->_{this} V) : MutableMapWithDefault<K, V> {
    override fun equals(other: Any?): Boolean = map.equals(other)
    override fun hashCode(): Int = map.hashCode()
    override fun toString(): String = map.toString()
    override val size: Int get() = map.size
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: @UnsafeVariance V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map.get(key)
    override val keys: MutableSet<K>_{this} get() = map.keys
    override val values: MutableCollection<V>_{this} get() = map.values
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>_{this} get() = map.entries

    override fun put(key: K, value: V): V? = map.put(key, value)
    override fun remove(key: K): V? = map.remove(key)
    override fun putAll(local from: Map<out K, V>) = map.putAll(from)
    override fun clear() = map.clear()

    override fun getOrImplicitDefault(key: K): V = map.getOrElseNullable(key, { default(key) })
}

