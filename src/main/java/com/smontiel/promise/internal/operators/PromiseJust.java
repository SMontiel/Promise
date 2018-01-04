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
import com.smontiel.promise.Promise;
import com.smontiel.promise.internal.ScalarCallable;

/**
 * Represents a constant scalar value.
 * @param <T> the value type
 */
public final class PromiseJust<T> extends Promise<T> implements ScalarCallable<T> {

    private final T value;

    public PromiseJust(final T value) {
        this.value = value;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        s.onComplete(value);
    }

    @Override
    public T call() {
        return value;
    }
}
