package com.smontiel.promise.internal.operators;

import com.smontiel.promise.PromiseSource;

/**
 * Interface indicating the implementor has an upstream PromiseSource-like source available
 * via {@link #source()} method.
 *
 * @param <T> the value type
 */
public interface HasUpstreamPromiseSource<T> {
    /**
     * Returns the upstream source of this Promise.
     * <p>Allows discovering the chain of promises.
     * @return the source PromiseSource
     */
    PromiseSource<T> source();
}
