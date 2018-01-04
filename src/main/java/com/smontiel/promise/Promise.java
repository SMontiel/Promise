package com.smontiel.promise;

import com.smontiel.promise.exceptions.Exceptions;
import com.smontiel.promise.internal.Functions;
import com.smontiel.promise.internal.ObjectHelper;
import com.smontiel.promise.internal.PromisePlugins;
import com.smontiel.promise.internal.operators.*;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Salvador Montiel on 02/enero/2018.
 */
// TODO: Check exceptions on 'then' methods
// TODO: Handle states (Funfilled, pending, rejected)
// TODO: Make 'fromCallable' method async
// TODO: Add Java 6 support
public abstract class Promise<T> implements PromiseSource<T> {
    /**
     * Returns a Promise that invokes an {@link Observer}'s {@link Observer#onComplete onComplete} method when the
     * Observer subscribes to it.
     *
     * @param value
     *            the particular object to pass to {@link Observer#onComplete onComplete}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the PromiseSource
     * @return a Promise that invokes the {@link Observer}'s {@link Observer#onComplete onComplete} method when
     *         the Observer subscribes to it
     * @since 0.1
     */
    public static <T> Promise<T> resolve(T value) {
        ObjectHelper.requireNonNull(value, "The item is null");
        return PromisePlugins.onAssembly(new PromiseJust<T>(value));
    }

    /**
     * Returns a Promise that invokes an {@link Observer}'s {@link Observer#onError onError} method when the
     * Observer subscribes to it.
     *
     * @param exception
     *            the particular Throwable to pass to {@link Observer#onError onError}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the PromiseSource
     * @return a Promise that invokes the {@link Observer}'s {@link Observer#onError onError} method when
     *         the Observer subscribes to it
     * @since 0.1
     */
    public static <T> Promise<T> reject(final Throwable exception) {
        ObjectHelper.requireNonNull(exception, "e is null");
        return reject(Functions.justCallable(exception));
    }

    /**
     * Returns a Promise that invokes an {@link Observer}'s {@link Observer#onError onError} method when the
     * Observer subscribes to it.
     *
     * @param errorSupplier
     *            a Callable factory to return a Throwable for each individual Observer
     * @param <T>
     *            the type of the items (ostensibly) emitted by the PromiseSource
     * @return a Promise that invokes the {@link Observer}'s {@link Observer#onError onError} method when
     *         the Observer subscribes to it
     * @since 0.1
     */
    public static <T> Promise<T> reject(Callable<? extends Throwable> errorSupplier) {
        ObjectHelper.requireNonNull(errorSupplier, "errorSupplier is null");
        return PromisePlugins.onAssembly(new PromiseError<T>(errorSupplier));
    }

    /**
     * Returns a Promise that, when an observer subscribes to it, invokes a function you specify and then
     * emits the value returned from that function.
     *
     * This allows you to defer the execution of the function you specify until an observer subscribes to the
     * PromiseSource. That is to say, it makes the function "lazy."
     *
     * @param supplier
     *         a function, the execution of which should be deferred; {@code fromCallable} will invoke this
     *         function only when an observer subscribes to the PromiseSource that {@code fromCallable} returns
     * @param <T>
     *         the type of the item emitted by the PromiseSource
     * @return a Promise whose {@link Observer}s' subscriptions trigger an invocation of the given function
     * @since 0.1
     */
    public static <T> Promise<T> fromCallable(Callable<? extends T> supplier) {
        ObjectHelper.requireNonNull(supplier, "supplier is null");
        return PromisePlugins.onAssembly(new PromiseFromCallable<T>(supplier));
    }

    /**
     * Returns a Promise that calls the appropriate onComplete consumer (shared between all subscribers) whenever a signal with the same type
     * passes through, before forwarding them to downstream.
     *
     * @param onComplete
     *             the {@code Consumer<T>} you have designed to accept emissions from the PromiseSource
     * @return the source PromiseSource with the side-effecting behavior applied
     * @since 0.1
     */
    public final Promise<T> then(Consumer<? super T> onComplete) {
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        return PromisePlugins.onAssembly(new PromiseDoOnEach<T>(this, onComplete, Functions.emptyConsumer(), Functions.EMPTY_RUNNABLE));
    }

    /**
     * Returns a Promise that applies a specified function to each item emitted by the source PromiseSource and
     * emits the results of these function applications.
     *
     * @param <R> the output type
     * @param mapper
     *            a function to apply to each item emitted by the PromiseSource
     * @return a Promise that emits the items from the source PromiseSource, transformed by the specified
     *         function
     * @since 0.1
     */
    public final <R> Promise<R> then(Function<? super T, ? extends R> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return PromisePlugins.onAssembly(new PromiseThen<T, R>(this, mapper));
    }

    /**
     * Returns a Promise that calls the appropriate onError consumer (shared between all subscribers) whenever a signal with the same type
     * passes through, before forwarding them to downstream.
     *
     * @param onError
     *             the {@code Consumer<Throwable>} you have designed to accept any error notification from the
     *             PromiseSource
     * @return the source PromiseSource with the side-effecting behavior applied
     * @since 0.1
     */
    public final Promise<T> fail(Consumer<? super Throwable> onError) {
        ObjectHelper.requireNonNull(onError, "onError is null");
        return PromisePlugins.onAssembly(new PromiseDoOnEach<T>(this, Functions.emptyConsumer(), onError, Functions.EMPTY_RUNNABLE));
    }

    /**
     * Subscribes to a PromiseSource and ignores {@code onComplete} emission.
     * <p>
     * If the Promise emits an error, it is wrapped into an
     * {@link com.smontiel.promise.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the PromisePlugins.onError handler.
     * @since 0.1
     */
    public void done() {
        subscribe(new Observer<T>() {

            @Override public void onComplete(T value) {}

            @Override public void onError(Throwable e) {}
        });
    }

    /**
     * Subscribes to a PromiseSource and ignores {@code onComplete} emission.
     * <p>
     * If the Promise emits an error, it is wrapped into an
     * {@link com.smontiel.promise.exceptions.OnErrorNotImplementedException OnErrorNotImplementedException}
     * and routed to the PromisePlugins.onError handler.
     *
     * @since 0.1
     */
    public final void subscribe() {
        done();
    }

    /**
     * Subscribes to a PromiseSource and provides a Observer to handle one of {@code onComplete}
     * and {@code onError}.
     *
     * @param observer
     *          the Observer, never null
     * @since 0.1
     */
    @Override
    public final void subscribe(Observer<? super T> observer) {
        ObjectHelper.requireNonNull(observer, "observer is null");
        try {
            observer = PromisePlugins.onSubscribe(this, observer);

            ObjectHelper.requireNonNull(observer, "Plugin returned null Observer");

            subscribeActual(observer);
        } catch (NullPointerException e) { // NOPMD
            throw e;
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            // can't call onError because no way to know if a Disposable has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            PromisePlugins.onError(e);

            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions");
            npe.initCause(e);
            throw npe;
        }
    }

    /**
     * Operator implementations (both source and intermediate) should implement this method that
     * performs the necessary business logic.
     * <p>There is no need to call any of the plugin hooks on the current Promise instance.
     * @param observer the incoming Observer, never null
     * @since 0.1
     */
    protected abstract void subscribeActual(Observer<? super T> observer);
}