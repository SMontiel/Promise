package com.smontiel.promise.internal.operators;

import com.smontiel.promise.Observer;
import com.smontiel.promise.Promise;
import com.smontiel.promise.exceptions.Exceptions;
import com.smontiel.promise.internal.ObjectHelper;
import com.smontiel.promise.internal.PromisePlugins;
import com.smontiel.promise.internal.operators.utils.QueueDisposable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Calls a Callable and emits its resulting single value or signals its exception.
 * @param <T> the value type
 */
public final class PromiseFromCallable<T> extends Promise<T> implements Callable<T> {
    final Callable<? extends T> callable;

    public PromiseFromCallable(Callable<? extends T> callable) {
        this.callable = callable;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        DeferredScalarDisposable<T> d = new DeferredScalarDisposable<T>(s);
        T value;
        try {
            value = ObjectHelper.requireNonNull(callable.call(), "Callable returned null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            s.onError(e);
            return;
        }
        d.complete(value);
    }

    @Override
    public T call() throws Exception {
        return ObjectHelper.requireNonNull(callable.call(), "The callable returned a null value");
    }

    /**
     * Represents a fuseable container for a single value.
     *
     * @param <T> the value type received and emitted
     */
    static class DeferredScalarDisposable<T> extends BasicIntQueueDisposable<T> {

        private static final long serialVersionUID = -5502432239815349361L;

        /** The target of the events. */
        protected final Observer<? super T> actual;

        /** The value stored temporarily when in fusion mode. */
        protected T value;

        /** Indicates there was a call to complete(T). */
        static final int TERMINATED = 2;

        /** Indicates this Disposable is in fusion mode and is currently empty. */
        static final int FUSED_EMPTY = 8;
        /** Indicates this Disposable is in fusion mode and has a value. */
        static final int FUSED_READY = 16;
        /** Indicates this Disposable is in fusion mode and its value has been consumed. */
        static final int FUSED_CONSUMED = 32;

        /**
         * Constructs a DeferredScalarDisposable by wrapping the Observer.
         * @param actual the Observer to wrap, not null (not verified)
         */
        public DeferredScalarDisposable(Observer<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public final int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                lazySet(FUSED_EMPTY);
                return ASYNC;
            }
            return NONE;
        }

        /**
         * Complete the target with a single value or indicate there is a value available in
         * fusion mode.
         * @param value the value to signal, not null (not verified)
         */
        public final void complete(T value) {
            int state = get();
            if ((state & (FUSED_READY | FUSED_CONSUMED | TERMINATED)) != 0) {
                return;
            }
            if (state == FUSED_EMPTY) {
                this.value = value;
                lazySet(FUSED_READY);
            } else {
                lazySet(TERMINATED);
            }
            Observer<? super T> a = actual;
            a.onComplete(value);
        }

        /**
         * Complete the target with an error signal.
         * @param t the Throwable to signal, not null (not verified)
         */
        public final void error(Throwable t) {
            int state = get();
            if ((state & (FUSED_READY | FUSED_CONSUMED | TERMINATED)) != 0) {
                PromisePlugins.onError(t);
                return;
            }
            lazySet(TERMINATED);
            actual.onError(t);
        }

        @Override
        public final T poll() throws Exception {
            if (get() == FUSED_READY) {
                T v = value;
                value = null;
                lazySet(FUSED_CONSUMED);
                return v;
            }
            return null;
        }

        @Override
        public final boolean isEmpty() {
            return get() != FUSED_READY;
        }

        @Override
        public final void clear() {
            lazySet(FUSED_CONSUMED);
            value = null;
        }
    }

    /**
     * An abstract QueueDisposable implementation, extending an AtomicInteger,
     * that defaults all unnecessary Queue methods to throw UnsupportedOperationException.
     * @param <T> the output value type
     */
    public static abstract class BasicIntQueueDisposable<T>
            extends AtomicInteger
            implements QueueDisposable<T> {


        private static final long serialVersionUID = -1001730202384742097L;

        @Override
        public final boolean offer(T e) {
            throw new UnsupportedOperationException("Should not be called");
        }

        @Override
        public final boolean offer(T v1, T v2) {
            throw new UnsupportedOperationException("Should not be called");
        }
    }


}
