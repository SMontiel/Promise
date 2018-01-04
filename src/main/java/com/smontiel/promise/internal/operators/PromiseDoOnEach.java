/**
 * Copyright (c) 2016-present, Salvador Montiel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smontiel.promise.internal.operators;

import com.smontiel.promise.Observer;
import com.smontiel.promise.PromiseSource;
import com.smontiel.promise.exceptions.CompositeException;
import com.smontiel.promise.exceptions.Exceptions;
import com.smontiel.promise.internal.PromisePlugins;

import java.util.function.Consumer;

public final class PromiseDoOnEach<T> extends AbstractPromiseWithUpstream<T, T> {
    final Consumer<? super T> onComplete;
    final Consumer<? super Throwable> onError;
    final Runnable onAfterTerminate;

    public PromiseDoOnEach(PromiseSource<T> source, Consumer<? super T> onComplete,
                           Consumer<? super Throwable> onError,
                           Runnable onAfterTerminate) {
        super(source);
        this.onComplete = onComplete;
        this.onError = onError;
        this.onAfterTerminate = onAfterTerminate;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new DoOnEachObserver<T>(t, onComplete, onError, onAfterTerminate));
    }

    static final class DoOnEachObserver<T> implements Observer<T> {
        final Observer<? super T> actual;
        final Consumer<? super T> onComplete;
        final Consumer<? super Throwable> onError;
        final Runnable onAfterTerminate;

        boolean done;

        DoOnEachObserver(
                Observer<? super T> actual,
                Consumer<? super T> onComplete,
                Consumer<? super Throwable> onError,
                Runnable onAfterTerminate) {
            this.actual = actual;
            this.onComplete = onComplete;
            this.onError = onError;
            this.onAfterTerminate = onAfterTerminate;
        }

        @Override
        public void onComplete(T t) {
            if (done) {
                return;
            }
            try {
                onComplete.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
                return;
            }

            done = true;
            actual.onComplete(t);

            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                PromisePlugins.onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                PromisePlugins.onError(t);
                return;
            }
            done = true;
            try {
                onError.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                t = new CompositeException(t, e);
            }
            actual.onError(t);

            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                PromisePlugins.onError(e);
            }
        }
    }
}
