package com.persistentbit.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A Result represents the result of a function.<br>
 * It can be a {@link Success} with a result value.<br>
 * It can be a {@link Failure} with a runtime exception.<br>
 * It can be an {@link Empty} representing an empty value
 *
 * @author Peter Muys
 * @since 27/12/2016
 */
public abstract class Result<T> implements Iterable<T>, Serializable {

    /**
     * Map the Success value or return a Failure or a Empty
     *
     * @param mapper the value mapper
     * @param <U>    The resulting value type
     * @return a new Result
     */
    public abstract <U> Result<U> map(Function<T, U> mapper);

    /**
     * Flatmap the Success value or return a Failure or a Empty
     *
     * @param mapper the value mapper
     * @param <U>    The resulting value type
     * @return a new Result
     */
    public abstract <U> Result<U> flatMap(Function<T, Result<U>> mapper);

    /**
     * Convert this result to an optional
     *
     * @return Some value for success or an empty optional
     */
    public abstract Optional<T> getOpt();

    public boolean isPresent() {
        return getOpt().isPresent();
    }

    public abstract boolean isEmpty();

    public boolean isError() {
        return !isPresent() && !isEmpty();
    }

    public abstract Result<T> mapError(Function<Throwable,? extends Throwable> mapper);
    public Result<T> verify(Predicate<T> verification){
        return verify(verification,v -> new IllegalStateException("Verification for " + v + " failed!"));
    }

    public Result<T> verify(Predicate<T> verification,String errorMessage){
        return verify(verification,v -> new IllegalStateException("Verification for " + v + " failed:" + errorMessage));
    }

    public abstract <E extends Throwable> Result<T> verify(Predicate<T> verification, Function<T, E> failureExceptionSupplier);



    /**
     * Get the Success value or the supplied else value on error or empty
     *
     * @param elseValue The return value when there is not Success value
     * @return Success value or elseValue
     */
    public T orElse(T elseValue) {
        return getOpt().orElse(elseValue);
    }

    /**
     * Get the Success value or the supplied else generated value  on error or empty
     *
     * @param elseValueSupplier Supplier for the return value when there is not Success value
     * @return Success value or elseValue
     */
    public T orElseGet(Supplier<T> elseValueSupplier) {
        return getOpt().orElseGet(elseValueSupplier);
    }

    /**
     * Get the Success value or throw an Exception
     *
     * @param exceptionSupplier The Exception supplier
     * @param <E>               The Exception type
     * @return The value on Success
     * @throws E Exception thrown when there is no value
     */
    public <E extends Throwable> T orElseThrow(Supplier<? extends E> exceptionSupplier) throws E {
        return getOpt().orElseThrow(exceptionSupplier);
    }

    /**
     * Get the Success value or throw a RuntimeException.<br>
     *
     * @return The value on Success
     */
    public abstract T orElseThrow();

    @Override
    public Iterator<T> iterator() {
        return getOpt().map(v -> Collections.singletonList(v).iterator()).orElseGet(Collections::emptyIterator);
    }


    /**
     * When we have a Success value, filter the value, else just return empty or failure
     *
     * @param filter The filter predicate
     * @return The filtered result
     */
    public abstract Result<T> filter(Predicate<T> filter);


    /**
     * Run code if this is an Empty result
     *
     * @param r The code to run
     */
    public abstract void ifEmpty(Runnable r);

    /**
     * Run code if this is a Success value
     *
     * @param f The code to run
     */
    public abstract void ifPresent(Consumer<T> f);

    /**
     * Run code if this is a Failure result
     *
     * @param e The Failure exception
     */
    public abstract void ifFailure(Consumer<Throwable> e);

    /**
     * Create a Success result
     *
     * @param value The NOT NULLABLE Success value
     * @param <U>   The Result value type
     * @return a Success
     */
    public static <U> Success<U> success(U value) {
        return new Success<>(value);
    }

    static private final Empty theEmpty = new Empty();

    /**
     * Create an Empty result
     *
     * @param <U> The normal value type
     * @return an Empty
     */
    @SuppressWarnings("unchecked")
    public static <U> Empty<U> empty() {
        return (Empty<U>) theEmpty;
    }

    /**
     * Create a Success or Empty result
     *
     * @param value The Nullable value
     * @param <U>   The type of the value
     * @return An Empty result if value is null or else a Success result
     */
    public static <U> Result<U> result(U value) {
        return value == null ? empty() : success(value);
    }

    /**
     * Create a failure result
     *
     * @param error The failure Exception message
     * @param <U>   The result type
     * @return a Failure result
     */
    public static <U> Failure<U> failure(String error) {
        return new Failure<>(error);
    }

    /**
     * Create a failure result
     *
     * @param exception The failure RuntimeException
     * @param <U>       The result type
     * @return a Failure result
     */
    public static <E extends Throwable,U> Failure<U> failure(E exception) {
        return new Failure<>(exception);
    }

    public static <U> Failure<U> failure(String error,Throwable causeException) {
        return failure(new RuntimeException(error,causeException));
    }

    /**
     * Create a new Function, returning a Result over the return type of the supplied function.<br>
     *
     * @param f   The function to convert
     * @param <T> The function argument type
     * @param <R> The Result value type
     * @return A new function returning a Result
     */
    public static <T, R> Function<T, Result<R>> toResult(Function<T, R> f) {
        return x -> {
            try {
                return result(f.apply(x));
            } catch (RuntimeException e) {
                return failure(e);
            } catch (Exception e) {
                return failure(new RuntimeException(e));
            }
        };

    }

    public static <T, U, R> Function<T, Function<U, Result<R>>> higherToResult(Function<T, Function<U, R>> f) {
        return x -> y -> {
            try {
                return result(f.apply(x).apply(y));
            } catch (RuntimeException e) {
                return failure(e);
            } catch (Exception e) {
                return failure(new RuntimeException(e));
            }
        };

    }

    public static class Success<T> extends Result<T> {
        private final T value;

        private Success(T value) {
            this.value = Objects.requireNonNull(value, "Success value is null, use an Empty value instead");
        }

        @Override
        public <U> Result<U> map(Function<T, U> mapper) {
            return success(mapper.apply(value));
        }



        @Override
        public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public Optional<T> getOpt() {
            return Optional.ofNullable(value);
        }

        @Override
        public T orElseThrow() {
            return value;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Result<T> mapError(Function<Throwable, ? extends Throwable> mapper) {
            return this;
        }

        @Override
        public String toString() {
            return "Success(" + value + ")";
        }

        @Override
        public <E extends Throwable> Result<T> verify(Predicate<T> verification, Function<T, E> failureExceptionSupplier) {
            return verification.test(value)
                    ? this
                    : Result.failure(failureExceptionSupplier.apply(value));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Success<?> success = (Success<?>) o;

            return value.equals(success.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public void ifEmpty(Runnable r) {

        }

        @Override
        public void ifPresent(Consumer<T> f) {
            f.accept(value);
        }

        @Override
        public void ifFailure(Consumer<Throwable> e) {

        }

        @Override
        public Result<T> filter(Predicate<T> filter) {
            return filter.test(value)
                    ? this
                    : empty();
        }
    }

    public static class Empty<T> extends Result<T> {
        @Override
        public <U> Result<U> map(Function<T, U> mapper) {
            return new Empty<>();
        }

        @Override
        public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
            return new Empty<>();
        }

        @Override
        public Optional<T> getOpt() {
            return Optional.empty();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public T orElseThrow() {
            throw new IllegalStateException("Can't get value from an Empty!");
        }

        @Override
        public String toString() {
            return "Empty()";
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Empty;
        }

        @Override
        public void ifEmpty(Runnable r) {
            r.run();
        }

        @Override
        public void ifPresent(Consumer<T> f) {

        }

        @Override
        public void ifFailure(Consumer<Throwable> e) {

        }

        @Override
        public Result<T> filter(Predicate<T> filter) {
            return this;
        }

        @Override
        public Result<T> mapError(Function<Throwable, ? extends Throwable> mapper) {
            return this;
        }

        @Override
        public <E extends Throwable> Result<T> verify(Predicate<T> verification, Function<T, E> failureExceptionSupplier) {
            return this;
        }
    }

    public static class Failure<T> extends Result<T> {
        private final Throwable exception;

        public Failure(Throwable exception) {
            this.exception = Objects.requireNonNull(exception);
        }

        public Failure(String error) {
            this(new IllegalStateException(Objects.requireNonNull(error)));
        }


        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U> map(Function<T, U> mapper) {
            return (Failure<U>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
            return (Failure<U>) this;
        }

        @Override
        public Optional<T> getOpt() {
            return Optional.empty();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        public T orElseThrow() {
            throw new IllegalStateException("Failure result", exception);
        }

        @Override
        public void ifEmpty(Runnable r) {

        }

        @Override
        public void ifPresent(Consumer<T> f) {

        }

        @Override
        public void ifFailure(Consumer<Throwable> e) {
            e.accept(exception);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Failure<?> failure = (Failure<?>) o;

            return exception.equals(failure.exception);
        }

        @Override
        public int hashCode() {
            return exception.hashCode();
        }

        @Override
        public String toString() {
            return "Failure(" + exception.getMessage() + ")";
        }

        @Override
        public Result<T> filter(Predicate<T> filter) {
            return this;
        }

        @Override
        public Result<T> mapError(Function<Throwable, ? extends Throwable> mapper) {
            return Result.failure(mapper.apply(exception));
        }

        @Override
        public <E extends Throwable> Result<T> verify(Predicate<T> verification, Function<T, E> failureExceptionSupplier) {
            return this;
        }
    }
}