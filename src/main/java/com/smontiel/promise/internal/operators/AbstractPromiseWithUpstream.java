package com.smontiel.promise.internal.operators;

import com.smontiel.promise.Promise;
import com.smontiel.promise.PromiseSource;

/**
 * Base class for operators with a source consumable.
 *
 * @param <T> the input source type
 * @param <U> the output type
 */
public abstract class AbstractPromiseWithUpstream<T, U> extends Promise<U> implements HasUpstreamPromiseSource<T> {

    /** The source consumable Promise. */
    protected final PromiseSource<T> source;

    /**
     * Constructs the PromiseSource with the given consumable.
     * @param source the consumable Promise
     */
    AbstractPromiseWithUpstream(PromiseSource<T> source) {
        this.source = source;
    }

    @Override
    public final PromiseSource<T> source() {
        return source;
    }
}
