package com.persistentbit.core.collections;



import com.persistentbit.core.Tuple2;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * User: petermuys
 * Date: 8/07/16
 * Time: 09:07
 */
public class PMap<K, V> extends PStreamDirect<Tuple2<K,V>,PMap<K,V>>{
    final private static PMap sEmpty = new PMap(0, null, false, null);
    static public final <K,V> PMap<K,V> empty() {
        return (PMap<K,V>) sEmpty;
    }
    final private static Object sNotFound = new Object();


    final int size;


    final MapNode root;
    final boolean hasNull;


    final V nullValue;


    public PMap() {
        this(0, null, false, null);
    }


    private PMap(int size, MapNode root, boolean hasNull, V nullValue) {
        this.size = size;
        this.root = root;
        this.hasNull = hasNull;
        this.nullValue = nullValue;
    }

    @Override
    PMap<K, V> toImpl(PStream<Tuple2<K, V>> lazy) {
        PMap<K,V> r = empty();
        return r.plusAll(lazy);
    }


    @Override
    public PMap<K, V> plus(Tuple2<K, V> value) {
        return this.put(value._1,value._2);
    }



    @Override
    public PMap<K, V> plusAll(Iterable<Tuple2<K, V>> iter) {
        PMap<K,V> r = this;
        for(Tuple2<K,V> t : iter){
            r = r.plus(t);
        }
        return r;
    }

    static private int hash(Object o) {
        return o == null ? 0 : o.hashCode();
    }


    public boolean containsKey(Object key) {
        if (key == null) {
            return hasNull;
        }
        return (root != null) ? root.find(0, key.hashCode(), key, sNotFound) != sNotFound
                : false;
    }

    public <M> PMap<K,M> mapValues(Function<V,M> mapper){

        PMap<K,M> r = PMap.empty();
        return with(r,(m,e)-> m = m.put(e._1,mapper.apply(e._2)) );
    }


    public PMap<K, V> put(K key, V val) {
        if (key == null) {
            if (hasNull && val == nullValue)
                return this;
            return new PMap<K, V>(hasNull ? size : size + 1,
                    root, true, val);
        }
        Box addedLeaf = new Box(null);
        MapNode newroot = (root == null ? BitmapIndexedNode.EMPTY : root).assoc(0, key.hashCode(), key, val, addedLeaf);
        if (newroot == root)
            return this;
        return new PMap<K, V>(addedLeaf.val == null ? size
                : size + 1, newroot, hasNull, nullValue);
    }





    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V notFound) {
        if (key == null)
            return hasNull ? nullValue : notFound;
        return (V) (root != null ? root.find(0, key.hashCode(), key, notFound) : notFound);
    }

    public V get(Object key){
        return getOrDefault(key,null);
    }

    public Optional<V> getOpt(Object key){
        return Optional.ofNullable(getOrDefault(key,null));
    }


    public PMap<K, V> removeKey(Object key) {
        if (key == null)
            return hasNull ? new PMap<>(size - 1, root, false, null) : this;

        if (root == null)
            return this;
        MapNode newroot = root.without(0, key.hashCode(), key);
        if (newroot == root)
            return this;
        return new PMap<K, V>(size - 1, newroot, hasNull,
                nullValue);
    }

    public PStream<K>   keys(){
        return map(e-> e._1);
    }

    public PStream<V>   values() {
        return map(e-> e._2);
    }



    public Map<K,V> toMap() {
        return new Map<K, V>() {
            @Override
            public int size() {
                return PMap.this.size();
            }

            @Override
            public boolean isEmpty() {
                return PMap.this.isEmpty();
            }

            @Override
            public boolean containsKey(Object key) {
                return PMap.this.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                return PMap.this.values().contains(value);
            }

            @Override
            public V get(Object key) {
                return getOrDefault(key,null);
            }

            @Override
            public V put(K key, V value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public V remove(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends K, ? extends V> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<K> keySet() {
                return keys().pset().toSet();
            }

            @Override
            public Collection<V> values() {
                return PMap.this.values().plist().list();
            }

            @Override
            public Set<Entry<K, V>> entrySet() {

                return PMap.this.map(t -> PMap.this.toMapEntry(t)).pset().toSet();
            }
        };
    }
    private Map.Entry<K,V> toMapEntry(Tuple2<K,V> e) {
        return (PMapEntry<K,V>)e;

    }




    public Iterator<Tuple2<K, V>> iterator() {
        final Iterator<?> rootIter = (root == null) ? Collections.emptyIterator() : root.iterator();
        if (hasNull) {
            return new Iterator<Tuple2<K, V>>() {
                private boolean seen = false;

                public boolean hasNext() {
                    if (!seen)
                        return true;
                    else
                        return rootIter.hasNext();
                }

                public PMapEntry<K, V> next() {
                    if (!seen) {
                        seen = true;
                        return (PMapEntry<K, V>) nullValue;
                    } else
                        return (PMapEntry<K, V>) rootIter.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else
            return (Iterator<Tuple2<K, V>>) rootIter;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size ==0;
    }

    static int mask(int hash, int shift) {
        return (hash >>> shift) & 0x01f;
    }


    static private class Box {

        public Object val;

        public Box(Object val) {
            this.val = val;
        }
    }

    interface MapNode extends Serializable {
        MapNode assoc(int shift, int hash, Object key, Object val, Box addedLeaf);

        MapNode without(int shift, int hash, Object key);

        PMapEntry find(int shift, int hash, Object key);

        Object find(int shift, int hash, Object key, Object notFound);

        Iterator iterator();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    final static class ArrayNode implements MapNode {
        int count;
        final MapNode[] array;
        //final AtomicReference<Thread> edit;

        ArrayNode(int count, MapNode[] array) {
            this.array = array;
            //this.edit = edit;
            this.count = count;
        }

        public MapNode assoc(int shift, int hash, Object key, Object val,
                             Box addedLeaf) {
            int idx = mask(hash, shift);
            MapNode node = array[idx];
            if (node == null)
                return new ArrayNode(count + 1, cloneAndSet(array, idx,
                        BitmapIndexedNode.EMPTY.assoc(shift + 5, hash, key,
                                val, addedLeaf)));
            MapNode n = node.assoc(shift + 5, hash, key, val, addedLeaf);
            if (n == node)
                return this;
            return new ArrayNode(count, cloneAndSet(array, idx, n));
        }

        public MapNode without(int shift, int hash, Object key) {
            int idx = mask(hash, shift);
            MapNode node = array[idx];
            if (node == null)
                return this;
            MapNode n = node.without(shift + 5, hash, key);
            if (n == node)
                return this;
            if (n == null) {
                if (count <= 8) // shrink
                    return pack(idx);
                return new ArrayNode(count - 1,
                        cloneAndSet(array, idx, n));
            } else
                return new ArrayNode(count, cloneAndSet(array, idx, n));
        }

        public PMapEntry find(int shift, int hash, Object key) {
            int idx = mask(hash, shift);
            MapNode node = array[idx];
            if (node == null)
                return null;
            return node.find(shift + 5, hash, key);
        }

        public Object find(int shift, int hash, Object key, Object notFound) {
            int idx = mask(hash, shift);
            MapNode node = array[idx];
            if (node == null)
                return notFound;
            return node.find(shift + 5, hash, key, notFound);
        }

        private MapNode pack(int idx) {
            Object[] newArray = new Object[2 * (count - 1)];
            int j = 1;
            int bitmap = 0;
            for (int i = 0; i < idx; i++)
                if (array[i] != null) {
                    newArray[j] = array[i];
                    bitmap |= 1 << i;
                    j += 2;
                }
            for (int i = idx + 1; i < array.length; i++)
                if (array[i] != null) {
                    newArray[j] = array[i];
                    bitmap |= 1 << i;
                    j += 2;
                }
            return new BitmapIndexedNode(bitmap, newArray);
        }


        /*public ISeq nodeSeq() {
            return Seq.create(array);
        }*/

        public Iterator<Object> iterator() {
            return new Iter(array);
        }

        static class Iter implements Iterator {
            private final MapNode[] array;
            private int i = 0;
            private Iterator nestedIter;

            private Iter(MapNode[] array) {
                this.array = array;
            }

            public boolean hasNext() {
                while (true) {
                    if (nestedIter != null)
                        if (nestedIter.hasNext())
                            return true;
                        else
                            nestedIter = null;

                    if (i < array.length) {
                        MapNode node = array[i++];
                        if (node != null)
                            nestedIter = node.iterator();
                    } else
                        return false;
                }
            }

            public Object next() {
                if (hasNext())
                    return nestedIter.next();
                else
                    throw new IllegalStateException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }

    final static class BitmapIndexedNode implements MapNode {
        static final BitmapIndexedNode EMPTY = new BitmapIndexedNode(0, new Object[0]);

        int bitmap;
        Object[] array;


        final int index(int bit) {
            return Integer.bitCount(bitmap & (bit - 1));
        }

        BitmapIndexedNode(int bitmap,
                          Object[] array) {
            this.bitmap = bitmap;
            this.array = array;

        }

        public MapNode assoc(int shift, int hash, Object key, Object val,
                             Box addedLeaf) {
            int bit = bitpos(hash, shift);
            int idx = index(bit);
            if ((bitmap & bit) != 0) {
                Object keyOrNull = array[2 * idx];
                Object valOrNode = array[2 * idx + 1];
                if (keyOrNull == null) {
                    MapNode n = ((MapNode) valOrNode).assoc(shift + 5, hash, key,
                            val, addedLeaf);
                    if (n == valOrNode)
                        return this;
                    return new BitmapIndexedNode(bitmap, cloneAndSet(
                            array, 2 * idx + 1, n));
                }
                if (key.equals(keyOrNull)) {
                    if (val == valOrNode)
                        return this;
                    return new BitmapIndexedNode(bitmap, cloneAndSet(
                            array, 2 * idx + 1, val));
                }
                addedLeaf.val = addedLeaf;
                return new BitmapIndexedNode(bitmap, cloneAndSet(
                        array,
                        2 * idx,
                        null,
                        2 * idx + 1,
                        createNode(shift + 5, keyOrNull, valOrNode, hash, key,
                                val)));
            } else {
                int n = Integer.bitCount(bitmap);
                if (n >= 16) {
                    MapNode[] nodes = new MapNode[32];
                    int jdx = mask(hash, shift);
                    nodes[jdx] = EMPTY.assoc(shift + 5, hash, key, val,
                            addedLeaf);
                    int j = 0;
                    for (int i = 0; i < 32; i++)
                        if (((bitmap >>> i) & 1) != 0) {
                            if (array[j] == null)
                                nodes[i] = (MapNode) array[j + 1];
                            else
                                nodes[i] = EMPTY.assoc(shift + 5,
                                        hash(array[j]), array[j], array[j + 1],
                                        addedLeaf);
                            j += 2;
                        }
                    return new ArrayNode(n + 1, nodes);
                } else {
                    Object[] newArray = new Object[2 * (n + 1)];
                    System.arraycopy(array, 0, newArray, 0, 2 * idx);
                    newArray[2 * idx] = key;
                    addedLeaf.val = addedLeaf;
                    newArray[2 * idx + 1] = val;
                    System.arraycopy(array, 2 * idx, newArray, 2 * (idx + 1),
                            2 * (n - idx));
                    return new BitmapIndexedNode(bitmap | bit, newArray);
                }
            }
        }

        public MapNode without(int shift, int hash, Object key) {
            int bit = bitpos(hash, shift);
            if ((bitmap & bit) == 0)
                return this;
            int idx = index(bit);
            Object keyOrNull = array[2 * idx];
            Object valOrNode = array[2 * idx + 1];
            if (keyOrNull == null) {
                MapNode n = ((MapNode) valOrNode).without(shift + 5, hash, key);
                if (n == valOrNode)
                    return this;
                if (n != null)
                    return new BitmapIndexedNode(bitmap, cloneAndSet(
                            array, 2 * idx + 1, n));
                if (bitmap == bit)
                    return null;
                return new BitmapIndexedNode(bitmap ^ bit, removePair(
                        array, idx));
            }
            if (key.equals(keyOrNull))
                // TODO: collapse
                return new BitmapIndexedNode(bitmap ^ bit, removePair(
                        array, idx));
            return this;
        }

        public PMapEntry find(int shift, int hash, Object key) {
            int bit = bitpos(hash, shift);
            if ((bitmap & bit) == 0)
                return null;
            int idx = index(bit);
            Object keyOrNull = array[2 * idx];
            Object valOrNode = array[2 * idx + 1];
            if (keyOrNull == null)
                return ((MapNode) valOrNode).find(shift + 5, hash, key);
            if (key.equals(keyOrNull))
                return new PMapEntry(keyOrNull, valOrNode);
            return null;
        }

        public Object find(int shift, int hash, Object key, Object notFound) {
            int bit = bitpos(hash, shift);
            if ((bitmap & bit) == 0)
                return notFound;
            int idx = index(bit);
            Object keyOrNull = array[2 * idx];
            Object valOrNode = array[2 * idx + 1];
            if (keyOrNull == null)
                return ((MapNode) valOrNode).find(shift + 5, hash, key, notFound);
            if (key.equals(keyOrNull))
                return valOrNode;
            return notFound;
        }

        /*public ISeq<Object> nodeSeq() {
            return NodeSeq.create(array);
        }*/

        public Iterator<Object> iterator() {
            return new NodeIter(array);
        }


    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    final static class HashCollisionNode implements MapNode {

        final int hash;
        int count;
        Object[] array;

        HashCollisionNode(int hash, int count, Object... array) {
            this.hash = hash;
            this.count = count;
            this.array = array;
        }

        public MapNode assoc(int shift, int hash, Object key, Object val,
                             Box addedLeaf) {
            if (hash == this.hash) {
                int idx = findIndex(key);
                if (idx != -1) {
                    if (array[idx + 1] == val)
                        return this;
                    return new HashCollisionNode(hash, count,
                            cloneAndSet(array, idx + 1, val));
                }
                Object[] newArray = new Object[2 * (count + 1)];
                System.arraycopy(array, 0, newArray, 0, 2 * count);
                newArray[2 * count] = key;
                newArray[2 * count + 1] = val;
                addedLeaf.val = addedLeaf;
                return new HashCollisionNode(hash, count + 1, newArray);
            }
            // nest it in a bitmap node
            return new BitmapIndexedNode(bitpos(this.hash, shift),
                    new Object[]{null, this}).assoc(shift, hash, key, val,
                    addedLeaf);
        }

        public MapNode without(int shift, int hash, Object key) {
            int idx = findIndex(key);
            if (idx == -1)
                return this;
            if (count == 1)
                return null;
            return new HashCollisionNode(hash, count - 1, removePair(
                    array, idx / 2));
        }

        public PMapEntry find(int shift, int hash, Object key) {
            int idx = findIndex(key);
            if (idx < 0)
                return null;
            if (key.equals(array[idx]))
                return new PMapEntry(array[idx], array[idx + 1]);
            return null;
        }

        public Object find(int shift, int hash, Object key, Object notFound) {
            int idx = findIndex(key);
            if (idx < 0)
                return notFound;
            if (key.equals(array[idx]))
                return array[idx + 1];
            return notFound;
        }


        public Iterator iterator() {
            return new NodeIter(array);
        }

        public int findIndex(Object key) {
            for (int i = 0; i < 2 * count; i += 2) {
                if (key.equals(array[i]))
                    return i;
            }
            return -1;
        }
    }

    private static MapNode[] cloneAndSet(MapNode[] array, int i, MapNode a) {
        MapNode[] clone = array.clone();
        clone[i] = a;
        return clone;
    }

    private static Object[] cloneAndSet(Object[] array, int i, Object a) {
        Object[] clone = array.clone();
        clone[i] = a;
        return clone;
    }

    private static Object[] cloneAndSet(Object[] array, int i, Object a, int j,
                                        Object b) {
        Object[] clone = array.clone();
        clone[i] = a;
        clone[j] = b;
        return clone;
    }

    private static Object[] removePair(Object[] array, int i) {
        Object[] newArray = new Object[array.length - 2];
        System.arraycopy(array, 0, newArray, 0, 2 * i);
        System.arraycopy(array, 2 * (i + 1), newArray, 2 * i, newArray.length
                - 2 * i);
        return newArray;
    }

    private static MapNode createNode(int shift, Object key1, Object val1,
                                      int key2hash, Object key2, Object val2) {
        int key1hash = hash(key1);
        if (key1hash == key2hash)
            return new HashCollisionNode(key1hash, 2, new Object[]{
                    key1, val1, key2, val2});
        Box addedLeaf = new Box(null);

        return BitmapIndexedNode.EMPTY.assoc(shift, key1hash, key1, val1,
                addedLeaf).assoc(shift, key2hash, key2, val2, addedLeaf);
    }

    @Override
    public PMap<K, V> distinct() {
        return this;
    }

    private static int bitpos(int hash, int shift) {
        return 1 << mask(hash, shift);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class NodeIter implements Iterator<Object> {
        private static final Object NULL = new Object();
        final Object[] array;
        private int i = 0;
        private Object nextEntry = NULL;
        private Iterator nextIter;

        NodeIter(Object[] array) {
            this.array = array;
        }

        private boolean advance() {
            while (i < array.length) {
                Object key = array[i];
                Object nodeOrVal = array[i + 1];
                i += 2;
                if (key != null) {
                    nextEntry = new PMapEntry(key, nodeOrVal);
                    return true;
                } else if (nodeOrVal != null) {
                    Iterator iter = ((MapNode) nodeOrVal).iterator();
                    if (iter != null && iter.hasNext()) {
                        nextIter = iter;
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean hasNext() {
            if (nextEntry != NULL || nextIter != null)
                return true;
            return advance();
        }

        public Object next() {
            Object ret = nextEntry;
            if (ret != NULL) {
                nextEntry = NULL;
                return ret;
            } else if (nextIter != null) {
                ret = nextIter.next();
                if (!nextIter.hasNext())
                    nextIter = null;
                return ret;
            } else if (advance())
                return next();
            throw new IllegalStateException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }



    static public void main(String...args){
        Map<Integer,String> refmap = new HashMap<>();
        PMap<Integer,String> pmap = new PMap<>();
        Random r = new Random(System.currentTimeMillis());

        for(int t=0; t<100000;t++){
            int key = r.nextInt();
            String val = ""  +r.nextGaussian();
            refmap.put(key,val);
            pmap = pmap.put(key,val);
        }
        Set<Integer> refKeys = refmap.keySet();
        PStream<Integer> pstreamKeys = pmap.keys();
        System.out.println(pstreamKeys);
        PSet<Integer> psetKeys = pstreamKeys.pset();
        System.out.println(psetKeys);
        Set<Integer> pkeys = psetKeys.toSet();

        if(refKeys.equals(pkeys) == false){
            throw new RuntimeException();
        }
        System.out.println("Min = " + pmap.keys().min() + ", max=" + pmap.keys().max());
        for(Map.Entry<Integer,String> entry : refmap.entrySet()){
            if(pmap.get(entry.getKey()).equals(entry.getValue()) == false){
                throw new RuntimeException(entry.toString());
            }
        }
        for(Tuple2<Integer,String> entry : pmap){
            if(pmap.get(entry._1).equals(entry._2) == false){
                throw new RuntimeException(entry.toString());
            }
        }
    }
}
