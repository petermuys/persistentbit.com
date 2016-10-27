package com.persistentbit.core.collections;

import java.util.Iterator;
import java.util.Set;

/**
 * A Persistent Set where the order of adding elements is preserved when iterating
 * @author Peter Muys
 * @since 13/07/2016
 */
public class POrderedSet<T> extends PStreamDirect<T,POrderedSet<T>> implements IPSet<T>{
    private static final POrderedSet<Object> sEmpty = new POrderedSet<>();

    @SuppressWarnings("unchecked")
    public static <T> POrderedSet<T> empty() {
        return (POrderedSet<T>)sEmpty;
    }

    private final POrderedMap<T,T> map;

    public POrderedSet() {
        this(POrderedMap.empty());
    }

    public static POrderedSet<Integer> forInt() {
        return empty();
    }
    public static POrderedSet<Long> forLong() {
        return empty();
    }

    public static POrderedSet<String> forString() {
        return empty();
    }
    public static POrderedSet<Boolean> forBoolean() {
        return empty();
    }

    private POrderedSet(POrderedMap<T,T> map){
        this.map = map;
    }


    @Override
    public PStream<T> lazy() {
        return new PStreamLazy<T>() {
            @Override
            public Iterator<T> iterator() {
                return POrderedSet.this.iterator();
            }

            @Override
            public POrderedSet<T> porderedset() {
                return POrderedSet.this;
            }

            @Override
            public PSet<T> pset() {
                return new PSet<>(map.pmap());
            }
        };

    }

    @Override
    protected POrderedSet<T> toImpl(PStream<T> lazy) {
        return lazy.porderedset();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public PSet<T> pset() {
        return new PSet<>(map.pmap());
    }

    @Override
    public POrderedSet<T> porderedset() {
        return this;
    }

    @Override
    public POrderedSet<T> distinct() {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public POrderedSet<T> plusAll(Iterable<? extends T> iter) {
        return PStream.from(iter).with(this, POrderedSet::plus);
    }
    public POrderedSet<T> plus(T value){
        return new POrderedSet<>(map.put(value,value));
    }

    @Override
    public boolean contains(Object value) {
        return map.containsKey(value);
    }


    @Override
    public Iterator<T> iterator() {
        return map.keys().iterator();
    }

    public Set<T> toSet() {
        return new PSetSet<>(this);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj instanceof IPSet == false){
            return false;
        }
        IPSet other = (IPSet)obj;
        if(this.size() != other.size()){
            return false;
        }
        for(T v : this){
            if(other.contains(v) == false){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
