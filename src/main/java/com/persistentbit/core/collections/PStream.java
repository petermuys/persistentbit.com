package com.persistentbit.core.collections;


import com.persistentbit.core.Tuple2;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Peter Muys
 * @since 6/07/2016
 */
public interface PStream<T> extends Iterable<T> {


    static <T> PStream<T> fromIter(Iterable<T> iter){
        if(iter instanceof PStream){
            return ((PStream<T>)iter);
        }
        return new PStreamLazy<T>() {
            @Override
            public Iterator<T> iterator() {
                return iter.iterator();
            }

            @Override
            public String toString() {
                return "lazy(" + iter + ")";
            }
        };
    }
    static <T> PStream<T> sequence(T start, Function<T, T> next){
        return new PStreamLazy<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    T v = start;
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public T next() {
                        T res = v;
                        v = next.apply(v);
                        return res;
                    }
                };
            }

            @Override
            public String toString() {
                return "sequence.from(" + start + ")";
            }
        };
    }

    static PStream<Integer> sequence(int start){
        return sequence(start,i -> i+1);
    }
    static PStream<Long> sequence(long start){
        return sequence(start,i -> i+1);
    }

    static <T> PStream<T> repeatValue(T value){
        return new PStreamLazy<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public T next() {
                        return value;
                    }
                };
            }

            @Override
            public String toString() {
                return "oneValue(" + value + ")";
            }
        };
    }



    default PStream<T> lazy() {
        return this;
    }









    default PStream<T> clear(){
        return new PStream<T>(){
            @Override
            public Iterator<T> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public String toString() {
                return "clear(" + PStream.this + ")";
            }
        };
    }



    default PStream<T> limit(int count){
        return new PStream<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    int cnt = count;
                    Iterator<T> master = PStream.this.iterator();
                    @Override
                    public boolean hasNext() {
                        return cnt>0 && master.hasNext();
                    }

                    @Override
                    public T next() {
                        if(cnt <= 0){
                            throw new IllegalStateException("Over limit");
                        }
                        cnt--;
                        return master.next();
                    }
                };
            }

            @Override
            public String toString() {
                return PStream.this.toString() + ".limit(" + count + ")";
            }
        };
    }
    default PStream<T>  dropLast(){
        return new PStream<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    Iterator<T> master = PStream.this.iterator();
                    boolean hasValue = master.hasNext();
                    T value = (hasValue ? master.next() : null);
                    @Override
                    public boolean hasNext() {
                        return master.hasNext();
                    }

                    @Override
                    public T next() {
                        T res = value;
                        hasValue = master.hasNext();
                        value = (hasValue ? master.next() : null);
                        return res;
                    }
                };
            }

            @Override
            public String toString() {
                return "dropLast(" + PStream.this + ")";
            }
        };
    }


    default <R> PStream<R> map(Function<T, R> mapper){
        Iterator<T> master = iterator();
        return new PStream<R>(){
            @Override
            public Iterator<R> iterator() {
                return new Iterator<R>() {
                    @Override
                    public boolean hasNext() {
                        return master.hasNext();
                    }

                    @Override
                    public R next() {
                        return mapper.apply(master.next());
                    }
                };
            }
            @Override
            public String toString() {
                return limit(100).toString("[",",","]");
            }
        };
    }





    default PStream<T> filter(Predicate<T> p){
        return new PStream<T>(){
            @Override
            public Iterator<T> iterator() {
                return new FilteredIterator<T>(p,PStream.this.iterator());
            }

            @Override
            public String toString() {
                return "filtered(" + PStream.this + ")";
            }
        };

    }

    default Optional<T> find(Predicate<T> p){
        for(T v : this){
            if(p.test(v)){
                return Optional.ofNullable(v);
            }
        }
        return Optional.empty();


    }

    default <Z> PStream<Tuple2<Z,T>> zip(PStream<Z> zipStream){
        return new PStream<Tuple2<Z, T>>() {
            @Override
            public Iterator<Tuple2<Z, T>> iterator() {
                Iterator<Z> iz = zipStream.iterator();
                Iterator<T> it = PStream.this.iterator();
                return new Iterator<Tuple2<Z, T>>() {

                    @Override
                    public boolean hasNext() {
                        return iz.hasNext() && it.hasNext();
                    }

                    @Override
                    public Tuple2<Z, T> next() {
                        return new Tuple2<>(iz.next(),it.next());
                    }
                };
            }

            @Override
            public String toString() {
                return "zip(" + zipStream + "," + PStream.this.toString() + ")";
            }
        };
    }

    default PStream<Tuple2<Integer,T>> zipWithIndex(){
        return zip(PStream.sequence(0));
    }





    default Stream<T> stream(){
        return list().stream();
    }

    default PStream<T> sorted(Comparator<? super T> comp){
        return new PStream<T>() {
            private List<T> sorted;
            @Override
            public synchronized Iterator<T> iterator() {
                if(sorted == null){
                    sorted = new ArrayList<T>();
                    Iterator<T> thisIter = PStream.this.iterator();
                    while(thisIter.hasNext()){
                        sorted.add(thisIter.next());
                    }
                    Collections.sort(sorted,comp);
                }
                return sorted.iterator();
            }

            @Override
            public String toString() {
                return "sorted(" + PStream.this + ")";
            }
        };
    }
    default PStream<T> sorted() {
        return sorted((a,b)-> ((Comparable)a).compareTo(b));
    }

    default PStream<T> reversed() {
        return new PStreamReversed<T>(this);
    }




    default PStream<T> plusAll(Iterable<T> iter){
        return new PStreamAnd<>(this,PStream.fromIter(iter));
    }

    default boolean contains(Object value){
        for(T v : this){
            if(v == null){
                if(value == null) {
                    return true;
                }
            } else if(v.equals(value)){
                return true;
            }

        }
        return false;
    }

    default boolean containsAll(Iterable<?> iter){
        PSet<T> set = this.pset();
        for(Object v : iter){
            if(set.contains(v) == false){
                return false;
            }
        }
        return true;
    }


    default <K> PMap<K,PList<T>> groupBy(Function<T, K> keyGen){
        PMap<K,PList<T>> r = PMap.empty();
        PList<T> emptyList = PList.empty();
        for(T v : this){
            K k = keyGen.apply(v);
            PList<T> l = r.getOrDefault(k,emptyList);
            l = l.plus(v);
            r = r.put(k,l);
        }
        return r;
    }


    default PStream<T> plus(T value){
        return new PStream<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    Iterator<T> master = PStream.this.iterator();
                    boolean valueAdded = false;

                    @Override
                    public boolean hasNext() {
                        return master.hasNext() || (valueAdded==false);
                    }

                    @Override
                    public T next() {
                        if(master.hasNext()){
                            return master.next();
                        }
                        if(valueAdded == false){
                            valueAdded = true;
                            return value;
                        }
                        throw new IllegalStateException();
                    }
                };
            }

        };
    }

    default T fold(T init, BinaryOperator<T> binOp){
        T res = init;
        for(T v : this){
            res = binOp.apply(res,v);
        }
        return res;
    }

    default <X> X with(X init, BiFunction<X, T, X> binOp){
        X res = init;
        for(T v : this){
            res  = binOp.apply(res,v);
        }
        return res;
    }

    default Optional<T> headOpt() {
        Iterator<T> iter = iterator();
        if(iter.hasNext()){
            return Optional.ofNullable(iter.next());
        }
        return Optional.empty();
    }

    default T head() {
        return headOpt().get();
    }

    default PStream<T>  tail() {
        return new PStream<T>() {
            @Override
            public Iterator<T> iterator() {
                Iterator<T> iter = PStream.this.iterator();
                if(iter.hasNext()){
                    iter.next();
                }
                return iter;
            }
        };
    }

    default Optional<T> max(Comparator<T> comp){
        return headOpt().map(h -> fold(h,(a, b) -> comp.compare(a,b) >=0 ? a : b));
    }
    default Optional<T> min(Comparator<T> comp){
        return headOpt().map(h -> fold(h,(a, b) -> comp.compare(a,b) <= 0 ? a : b));
    }
    default Optional<T> min() {
        return min((a,b) -> ((Comparable)a).compareTo(b));
    }
    default Optional<T> max() {
        return max((a,b) -> ((Comparable)a).compareTo(b));
    }



    default boolean isEmpty() {
        return iterator().hasNext() == false;
    }

    default int size() {
        int count = 0;
        Iterator<T> iter = iterator();
        while(iter.hasNext()){
            count++;
            iter.next();
        }
        return count;
    }

    default PStream<T> plusAll(T val1, T... rest){
        return plus(val1).plusAll(Arrays.asList(rest));
    }

    default T[] toArray() {
        T[] arr =  newArray(size());
        int i = 0;
        for(T v : this){
            arr[i++] = v;
        }
        return arr;
    }

    default <T1> T1[] toArray(T1[] a) {
        int size = size();
        if(a.length<size){
            a = Arrays.copyOf(a,size);
        }
        Iterator<T> iter = iterator();
        for(int t=0; t<a.length;t++){
            if(iter.hasNext()){
                a[t] = (T1)iter.next();
            } else {
                a[t] = null;
            }
        }
        return a;
    }


    static <E> E[] newArray(int length, E... array) { return Arrays.copyOf(array, length); }


    default PList<T> plist() {
        return new PList<T>().plusAll(this);
    }

    default PSet<T> pset() {
        return new PSet<T>().plusAll(this);
    }

    default PStream<T> distinct() {
        return new PStream<T>() {
            @Override
            public Iterator<T> iterator() {
                Set<T> lookup = new HashSet<T>();
                Predicate<T> distinct = v -> {
                  if(lookup.contains(v)){
                      return false;
                  }
                  lookup.add(v);
                  return true;
                };
                return new FilteredIterator<T>(distinct,PStream.this.iterator());
            }

        };
    }


    default LList<T> llist() {
        LList<T> res = LList.empty();
        for (T v : reversed()) {
            res.prepend(v);
        }
        return res;
    }
    default List<T> list() {
        return plist().list();
    }


    default List<T> toList() {
        return new ArrayList<T>(this.list());
    }


    default Optional<T> join(BinaryOperator<T> joiner){
        Iterator<T> iter = iterator();
        if(iter.hasNext() == false){
            return Optional.empty();
        }
        T res = iter.next();
        while(iter.hasNext()){
            T sec = iter.next();
            res = joiner.apply(res,sec);
        }
        return Optional.of(res);
    }


    default String toString(String sep){
        return toString("",sep,"");
    }


    default String toString(String left, String sep, String right){
        return left + map(i-> "" + i).join((a,b)-> a + sep + b).orElse("") + right;
    }


}
