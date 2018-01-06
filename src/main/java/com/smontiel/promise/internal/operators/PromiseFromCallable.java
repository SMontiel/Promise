package com.smontiel.promise.internal.operators;

import com.smontiel.promise.Observer;
import com.smontiel.promise.Promise;
import com.smontiel.promise.exceptions.Exceptions;
import com.smontiel.promise.internal.ObjectHelper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Calls a Callable and emits its resulting single value or signals its exception.
 * @param <T> the value type
 */
public final class PromiseFromCallable<T> extends Promise<T> implements Callable<T> {
    final Callable<T> callable;

    public PromiseFromCallable(Callable<T> callable) {
        this.callable = callable;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ListenerFutureTask<T> futureTask = new ListenerFutureTask<T>(callable, s, executorService);
        executorService.execute(futureTask);
    }

    @Override
    public T call() throws Exception {
        return ObjectHelper.requireNonNull(callable.call(), "The callable returned a null value");
    }

    private class ListenerFutureTask<T> extends FutureTask<T> {
        private Observer<? super T> actual;
        private ExecutorService executorService;

        ListenerFutureTask(Callable<T> callable, Observer<? super T> actual, ExecutorService executorService) {
            super(callable);
            this.actual = actual;
            this.executorService = executorService;
        }

        @Override
        protected void done() {
            super.done();

            try {
                T value = ObjectHelper.requireNonNull(get(), "Callable returned null");
                actual.onComplete(value);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            } finally {
                executorService.shutdown();
            }
        }
    }
}
