package com.smontiel.promise.internal.operators;

import com.smontiel.promise.Observer;
import com.smontiel.promise.Promise;
import com.smontiel.promise.exceptions.Exceptions;
import com.smontiel.promise.internal.ObjectHelper;

import java.util.concurrent.Callable;

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
        T value;
        try {
            value = ObjectHelper.requireNonNull(callable.call(), "Callable returned null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            s.onError(e);
            return;
        }
        s.onComplete(value);
    }

    @Override
    public T call() throws Exception {
        return ObjectHelper.requireNonNull(callable.call(), "The callable returned a null value");
    }
}
