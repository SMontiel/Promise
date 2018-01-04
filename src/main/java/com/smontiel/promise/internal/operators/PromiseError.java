package com.smontiel.promise.internal.operators;

import com.smontiel.promise.Observer;
import com.smontiel.promise.Promise;
import com.smontiel.promise.exceptions.Exceptions;
import com.smontiel.promise.internal.ObjectHelper;

import java.util.concurrent.Callable;

public final class PromiseError<T> extends Promise<T> {
    final Callable<? extends Throwable> errorSupplier;

    public PromiseError(Callable<? extends Throwable> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        Throwable error;
        try {
            error = ObjectHelper.requireNonNull(errorSupplier.call(), "Callable returned null throwable. Null values are generally not allowed in operators and sources.");
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            error = t;
        }
        s.onError(error);
    }
}
