package com.persistentbit.core.collections;

import com.persistentbit.core.Tuple2;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;



/**
 * Interface defining a persistent map.<br>
 * The map should be able to contain null keys and values<br>
 * @see PMap
 * @see POrderedMap
 * @author Peter Muys
 * @since 13/07/2016
 */
public interface IPMap<K,V>  extends PStream<Tuple2<K,V>>{
    /**
     * Does this map contains the provided key?<br>
     * Also returns true for keys that are set with a null value
     * @param key The key to check
     * @return true if this map contains the key
     */
    boolean containsKey(Object key);

    /**
     * Returns a new IPMap with all values mapped
     * @param mapper The value mapper function
     * @param <M> The new type for values
     * @return A new IPMap instance
     */
    <M> IPMap<K,M> mapValues(Function<V,M> mapper);

    /**
     * Create a new map with the key and value added
     * @param key The key to add
     * @param val The value to add
     * @return A new IPMap with the key and value added
     */
    IPMap<K, V> put(K key, V val);

    /**
     * Get the value of a key or a default value if the map doesn't contain the key
     * @param key The key to get
     * @param notFound The default value when not found
     * @return The value or notFound value
     */
    V getOrDefault(Object key, V notFound);

    /**
     * Same as getOrDefault(key,null)
     * @param key The key to get
     * @return The value or null when not found
     */
    V get(Object key);

    /**
     * get the optional value for a key
     * @param key The key to find
     * @return Optional of the value or empty when the value is not found or the value is null
     */
    Optional<V> getOpt(Object key);

    /**
     * Create a new IPMap with the provided key and value removed
     * @param key The key to remove
     * @return The new map
     */
    IPMap<K, V> removeKey(Object key);

    /**
     * Get all the keys
     * @return pstream of the keys
     */
    PStream<K>   keys();

    /**
     * Get all the values
     * @return pstream of all the values
     */
    PStream<V>   values();

    /**
     * Returns the persistent map as an immutable java Map
     * @return The java map.
     */
    default Map<K,V> map() {
        return new PMapMap<K,V>(this);
    }

}