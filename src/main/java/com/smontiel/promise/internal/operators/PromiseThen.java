package com.smontiel.promise.internal.operators;

import com.smontiel.promise.Observer;
import com.smontiel.promise.PromiseSource;
import com.smontiel.promise.exceptions.Exceptions;
import com.smontiel.promise.internal.ObjectHelper;
import com.smontiel.promise.internal.PromisePlugins;

import java.util.function.Function;

public final class PromiseThen<T, U> extends AbstractPromiseWithUpstream<T, U> {
    final Function<? super T, ? extends U> function;

    public PromiseThen(PromiseSource<T> source, Function<? super T, ? extends U> function) {
        super(source);
        this.function = function;
    }

    @Override
    public void subscribeActual(Observer<? super U> t) {
        source.subscribe(new MapObserver<T, U>(t, function));
    }


    static final class MapObserver<T, U> implements Observer<T> {
        /** The downstream subscriber. */
        protected final Observer<? super U> actual;
        final Function<? super T, ? extends U> mapper;
        /** Flag indicating no further onXXX event should be accepted. */
        protected boolean done;

        MapObserver(Observer<? super U> actual, Function<? super T, ? extends U> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void onComplete(T t) {
            if (done) {
                return;
            }
            done = true;

            U v;

            try {
                v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            actual.onComplete(v);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                PromisePlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }

        /**
         * Rethrows the throwable if it is a fatal exception or calls {@link #onError(Throwable)}.
         * @param t the throwable to rethrow or signal to the actual subscriber
         */
        protected final void fail(Throwable t) {
            Exceptions.throwIfFatal(t);
            onError(t);
        }
    }
}
