package com.smontiel.promise;

/**
 * Represents a basic, non-backpressured {@link Promise} source base interface,
 * consumable via an {@link Observer}.
 *
 * @param <T> the element type
 * @since 0.1
 */
public interface PromiseSource<T> {

    /**
     * Subscribes the given Observer to this PromiseSource instance.
     * @param observer the Observer, not null
     * @throws NullPointerException if {@code observer} is null
     */
    void subscribe(Observer<? super T> observer);
}
