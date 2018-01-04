package com.smontiel.promise;

/**
 * Provides a mechanism for receiving push-based notifications.
 * <p>
 * A well-behaved
 * {@code Promise} will call an Observer's {@link #onComplete} method exactly once or the Observer's
 * {@link #onError} method exactly once.
 *
 * @param <T>
 *          the type of item the Observer expects to observe
 * @since 0.1
 */
public interface Observer<T> {

    /**
     * Provides the Observer with a new item to observe.
     * <p>
     * If the {@link Promise} calls this method, it will not thereafter call
     * {@link #onError}.
     *
     * @param t
     *          the item emitted by the Promise
     * @since 0.1
     */
    void onComplete(T t);

    /**
     * Notifies the Observer that the {@link Promise} has experienced an error condition.
     * <p>
     * If the {@link Promise} calls this method, it will not thereafter call
     * {@link #onComplete}.
     *
     * @param e
     *          the exception encountered by the Promise
     * @since 0.1
     */
    void onError(Throwable e);

}
