/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package com.smontiel.promise.internal;

import com.smontiel.promise.exceptions.OnErrorNotImplementedException;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

/**
 * Utility methods to convert the BiFunction, Function3..Function9 instances to Function of Object array.
 */
public final class Functions {

    /** Utility class. */
    private Functions() {
        throw new IllegalStateException("No instances!");
    }

    /** A singleton identity function. */
    static final Function<Object, Object> IDENTITY = new Identity();

    /**
     * Returns an identity function that simply returns its argument.
     * @param <T> the input and output value type
     * @return the identity function
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<T, T> identity() {
        return (Function<T, T>)IDENTITY;
    }

    public static final Runnable EMPTY_RUNNABLE = new EmptyRunnable();

    static final Consumer<Object> EMPTY_CONSUMER = new EmptyConsumer();

    /**
     * Returns an empty consumer that does nothing.
     * @param <T> the consumed value type, the value is ignored
     * @return an empty consumer that does nothing.
     */
    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> emptyConsumer() {
        return (Consumer<T>)EMPTY_CONSUMER;
    }

    public static final Consumer<Throwable> ERROR_CONSUMER = new ErrorConsumer();

    /**
     * Wraps the consumed Throwable into an OnErrorNotImplementedException and
     * signals it to the plugin error handler.
     */
    public static final Consumer<Throwable> ON_ERROR_MISSING = new OnErrorMissingConsumer();

    public static final LongConsumer EMPTY_LONG_CONSUMER = new EmptyLongConsumer();

    static final Predicate<Object> ALWAYS_TRUE = new TruePredicate();

    static final Predicate<Object> ALWAYS_FALSE = new FalsePredicate();

    static final Callable<Object> NULL_SUPPLIER = new NullCallable();

    static final Comparator<Object> NATURAL_COMPARATOR = new NaturalObjectComparator();

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>)ALWAYS_TRUE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysFalse() {
        return (Predicate<T>)ALWAYS_FALSE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Callable<T> nullSupplier() {
        return (Callable<T>)NULL_SUPPLIER;
    }

    /**
     * Returns a natural order comparator which casts the parameters to Comparable.
     * @param <T> the value type
     * @return a natural order comparator which casts the parameters to Comparable
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalOrder() {
        return (Comparator<T>)NATURAL_COMPARATOR;
    }

    static final class JustValue<T, U> implements Callable<U>, Function<T, U> {
        final U value;

        JustValue(U value) {
            this.value = value;
        }

        @Override
        public U call() throws Exception {
            return value;
        }

        @Override
        public U apply(T t) {
            return value;
        }
    }

    /**
     * Returns a Callable that returns the given value.
     * @param <T> the value type
     * @param value the value to return
     * @return the new Callable instance
     */
    public static <T> Callable<T> justCallable(T value) {
        return new JustValue<Object, T>(value);
    }

    /**
     * Returns a Function that ignores its parameter and returns the given value.
     * @param <T> the function's input type
     * @param <U> the value and return type of the function
     * @param value the value to return
     * @return the new Function instance
     */
    public static <T, U> Function<T, U> justFunction(U value) {
        return new JustValue<T, U>(value);
    }

    static final class CastToClass<T, U> implements Function<T, U> {
        final Class<U> clazz;

        CastToClass(Class<U> clazz) {
            this.clazz = clazz;
        }

        @Override
        public U apply(T t) {
            return clazz.cast(t);
        }
    }

    /**
     * Returns a function that cast the incoming values via a Class object.
     * @param <T> the input value type
     * @param <U> the output and target type
     * @param target the target class
     * @return the new Function instance
     */
    public static <T, U> Function<T, U> castFunction(Class<U> target) {
        return new CastToClass<T, U>(target);
    }

    static final class ArrayListCapacityCallable<T> implements Callable<List<T>> {
        final int capacity;

        ArrayListCapacityCallable(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public List<T> call() throws Exception {
            return new ArrayList<T>(capacity);
        }
    }

    public static <T> Callable<List<T>> createArrayList(int capacity) {
        return new ArrayListCapacityCallable<T>(capacity);
    }

    static final class EqualsPredicate<T> implements Predicate<T> {
        final T value;

        EqualsPredicate(T value) {
            this.value = value;
        }

        @Override
        public boolean test(T t) {
            return ObjectHelper.equals(t, value);
        }
    }

    public static <T> Predicate<T> equalsWith(T value) {
        return new EqualsPredicate<T>(value);
    }

    enum HashSetCallable implements Callable<Set<Object>> {
        INSTANCE;
        @Override
        public Set<Object> call() throws Exception {
            return new HashSet<Object>();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Callable<Set<T>> createHashSet() {
        return (Callable)HashSetCallable.INSTANCE;
    }

    static final class ClassFilter<T, U> implements Predicate<T> {
        final Class<U> clazz;

        ClassFilter(Class<U> clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean test(T t) {
            return clazz.isInstance(t);
        }
    }

    public static <T, U> Predicate<T> isInstanceOf(Class<U> clazz) {
        return new ClassFilter<T, U>(clazz);
    }

    static final class BooleanSupplierPredicateReverse<T> implements Predicate<T> {
        final BooleanSupplier supplier;

        BooleanSupplierPredicateReverse(BooleanSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public boolean test(T t) {
            return !supplier.getAsBoolean();
        }
    }

    public static <T> Predicate<T> predicateReverseFor(BooleanSupplier supplier) {
        return new BooleanSupplierPredicateReverse<T>(supplier);
    }


    static final class ToMapKeySelector<K, T> implements BiConsumer<Map<K, T>, T> {
        private final Function<? super T, ? extends K> keySelector;

        ToMapKeySelector(Function<? super T, ? extends K> keySelector) {
            this.keySelector = keySelector;
        }

        @Override
        public void accept(Map<K, T> m, T t) {
            K key = keySelector.apply(t);
            m.put(key, t);
        }
    }

    public static <T, K> BiConsumer<Map<K, T>, T> toMapKeySelector(final Function<? super T, ? extends K> keySelector) {
        return new ToMapKeySelector<K, T>(keySelector);
    }

    static final class ToMapKeyValueSelector<K, V, T> implements BiConsumer<Map<K, V>, T> {
        private final Function<? super T, ? extends V> valueSelector;
        private final Function<? super T, ? extends K> keySelector;

        ToMapKeyValueSelector(Function<? super T, ? extends V> valueSelector,
                              Function<? super T, ? extends K> keySelector) {
            this.valueSelector = valueSelector;
            this.keySelector = keySelector;
        }

        @Override
        public void accept(Map<K, V> m, T t) {
            K key = keySelector.apply(t);
            V value = valueSelector.apply(t);
            m.put(key, value);
        }
    }

    public static <T, K, V> BiConsumer<Map<K, V>, T> toMapKeyValueSelector(final Function<? super T, ? extends K> keySelector, final Function<? super T, ? extends V> valueSelector) {
        return new ToMapKeyValueSelector<K, V, T>(valueSelector, keySelector);
    }

    static final class ToMultimapKeyValueSelector<K, V, T> implements BiConsumer<Map<K, Collection<V>>, T> {
        private final Function<? super K, ? extends Collection<? super V>> collectionFactory;
        private final Function<? super T, ? extends V> valueSelector;
        private final Function<? super T, ? extends K> keySelector;

        ToMultimapKeyValueSelector(Function<? super K, ? extends Collection<? super V>> collectionFactory,
                                   Function<? super T, ? extends V> valueSelector, Function<? super T, ? extends K> keySelector) {
            this.collectionFactory = collectionFactory;
            this.valueSelector = valueSelector;
            this.keySelector = keySelector;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void accept(Map<K, Collection<V>> m, T t) {
            K key = keySelector.apply(t);

            Collection<V> coll = m.get(key);
            if (coll == null) {
                coll = (Collection<V>)collectionFactory.apply(key);
                m.put(key, coll);
            }

            V value = valueSelector.apply(t);

            coll.add(value);
        }
    }

    public static <T, K, V> BiConsumer<Map<K, Collection<V>>, T> toMultimapKeyValueSelector(
            final Function<? super T, ? extends K> keySelector, final Function<? super T, ? extends V> valueSelector,
            final Function<? super K, ? extends Collection<? super V>> collectionFactory) {
        return new ToMultimapKeyValueSelector<K, V, T>(collectionFactory, valueSelector, keySelector);
    }

    enum NaturalComparator implements Comparator<Object> {
        INSTANCE;

        @SuppressWarnings("unchecked")
        @Override
        public int compare(Object o1, Object o2) {
            return ((Comparable<Object>)o1).compareTo(o2);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalComparator() {
        return (Comparator<T>)NaturalComparator.INSTANCE;
    }

    static final class ListSorter<T> implements Function<List<T>, List<T>> {
        final Comparator<? super T> comparator;

        ListSorter(Comparator<? super T> comparator) {
            this.comparator = comparator;
        }

        @Override
        public List<T> apply(List<T> v) {
            Collections.sort(v, comparator);
            return v;
        }
    }

    public static <T> Function<List<T>, List<T>> listSorter(final Comparator<? super T> comparator) {
        return new ListSorter<T>(comparator);
    }

    static final class Identity implements Function<Object, Object> {
        @Override
        public Object apply(Object v) {
            return v;
        }

        @Override
        public String toString() {
            return "IdentityFunction";
        }
    }

    static final class EmptyRunnable implements Runnable {
        @Override
        public void run() { }

        @Override
        public String toString() {
            return "EmptyRunnable";
        }
    }

    static final class EmptyConsumer implements Consumer<Object> {
        @Override
        public void accept(Object v) { }

        @Override
        public String toString() {
            return "EmptyConsumer";
        }
    }

    static final class ErrorConsumer implements Consumer<Throwable> {
        @Override
        public void accept(Throwable error) {
            PromisePlugins.onError(error);
        }
    }

    static final class OnErrorMissingConsumer implements Consumer<Throwable> {
        @Override
        public void accept(Throwable error) {
            PromisePlugins.onError(new OnErrorNotImplementedException(error));
        }
    }

    static final class EmptyLongConsumer implements LongConsumer {
        @Override
        public void accept(long v) { }
    }

    static final class TruePredicate implements Predicate<Object> {
        @Override
        public boolean test(Object o) {
            return true;
        }
    }

    static final class FalsePredicate implements Predicate<Object> {
        @Override
        public boolean test(Object o) {
            return false;
        }
    }

    static final class NullCallable implements Callable<Object> {
        @Override
        public Object call() {
            return null;
        }
    }

    static final class NaturalObjectComparator implements Comparator<Object> {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public int compare(Object a, Object b) {
            return ((Comparable)a).compareTo(b);
        }
    }
}
