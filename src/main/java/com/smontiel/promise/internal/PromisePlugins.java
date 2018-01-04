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
package com.smontiel.promise.internal;

import com.smontiel.promise.Observer;
import com.smontiel.promise.Promise;
import com.smontiel.promise.exceptions.CompositeException;
import com.smontiel.promise.exceptions.OnErrorNotImplementedException;
import com.smontiel.promise.exceptions.UndeliverableException;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class to inject handlers to certain standard Promise operations.
 * DO NOT TOUCH - This is magic!
 */
public final class PromisePlugins {
    static volatile Consumer<? super Throwable> errorHandler;

    static volatile Function<? super Runnable, ? extends Runnable> onScheduleHandler;

    @SuppressWarnings("rawtypes")
    static volatile Function<? super Promise, ? extends Promise> onPromiseAssembly;

    @SuppressWarnings("rawtypes")
    static volatile BiFunction<? super Promise, ? super Observer, ? extends Observer> onPromiseSubscribe;

    static volatile BooleanSupplier onBeforeBlocking;

    /** Prevents changing the plugins. */
    static volatile boolean lockdown;

    /**
     * If true, attempting to run a blockingX operation on a (by default)
     * computation or single scheduler will throw an IllegalStateException.
     */
    static volatile boolean failNonBlockingScheduler;

    /**
     * Prevents changing the plugins from then on.
     * <p>This allows container-like environments to prevent clients
     * messing with plugins.
     */
    public static void lockdown() {
        lockdown = true;
    }

    /**
     * Returns true if the plugins were locked down.
     * @return true if the plugins were locked down
     */
    public static boolean isLockdown() {
        return lockdown;
    }

    /**
     * Enables or disables the blockingX operators to fail
     * with an IllegalStateException on a non-blocking
     * scheduler such as computation or single.
     * <p>History: 2.0.5 - experimental
     * @param enable enable or disable the feature
     * @since 2.1
     */
    public static void setFailOnNonBlockingScheduler(boolean enable) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        failNonBlockingScheduler = enable;
    }

    /**
     * Returns true if the blockingX operators fail
     * with an IllegalStateException on a non-blocking scheduler
     * such as computation or single.
     * <p>History: 2.0.5 - experimental
     * @return true if the blockingX operators fail on a non-blocking scheduler
     * @since 2.1
     */
    public static boolean isFailOnNonBlockingScheduler() {
        return failNonBlockingScheduler;
    }

    /**
     * Returns the a hook consumer.
     * @return the hook consumer, may be null
     */
    public static Consumer<? super Throwable> getErrorHandler() {
        return errorHandler;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<? super Runnable, ? extends Runnable> getScheduleHandler() {
        return onScheduleHandler;
    }

    /**
     * Called when an undeliverable error occurs.
     * @param error the error to report
     */
    public static void onError(Throwable error) {
        Consumer<? super Throwable> f = errorHandler;

        if (error == null) {
            error = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
        } else {
            if (!isBug(error)) {
                error = new UndeliverableException(error);
            }
        }

        if (f != null) {
            try {
                f.accept(error);
                return;
            } catch (Throwable e) {
                // Exceptions.throwIfFatal(e); TODO decide
                e.printStackTrace(); // NOPMD
                uncaught(e);
            }
        }

        error.printStackTrace(); // NOPMD
        uncaught(error);
    }

    /**
     * Checks if the given error is one of the already named
     * bug cases that should pass through {@link #onError(Throwable)}
     * as is.
     * @param error the error to check
     * @return true if the error should pass through, false if
     * it may be wrapped into an UndeliverableException
     */
    static boolean isBug(Throwable error) {
        // user forgot to add the onError handler in subscribe
        if (error instanceof OnErrorNotImplementedException) {
            return true;
        }
        // general protocol violations
        // it's either due to an operator bug or concurrent onNext
        if (error instanceof IllegalStateException) {
            return true;
        }
        // nulls are generally not allowed
        // likely an operator bug or missing null-check
        if (error instanceof NullPointerException) {
            return true;
        }
        // bad arguments, likely invalid user input
        if (error instanceof IllegalArgumentException) {
            return true;
        }
        // Crash while handling an exception
        if (error instanceof CompositeException) {
            return true;
        }
        // everything else is probably due to lifecycle limits
        return false;
    }

    static void uncaught(Throwable error) {
        Thread currentThread = Thread.currentThread();
        Thread.UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
        handler.uncaughtException(currentThread, error);
    }

    /**
     * Called when a task is scheduled.
     * @param run the runnable instance
     * @return the replacement runnable
     */
    public static Runnable onSchedule(Runnable run) {
        ObjectHelper.requireNonNull(run, "run is null");

        Function<? super Runnable, ? extends Runnable> f = onScheduleHandler;
        if (f == null) {
            return run;
        }
        return apply(f, run);
    }

    /**
     * Removes all handlers and resets to default behavior.
     */
    public static void reset() {
        setErrorHandler(null);
        setScheduleHandler(null);

        setOnObservableAssembly(null);
        setOnObservableSubscribe(null);

        setFailOnNonBlockingScheduler(false);
        setOnBeforeBlocking(null);
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setErrorHandler(Consumer<? super Throwable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        errorHandler = handler;
    }

    /**
     * Sets the specific hook function.
     * @param handler the hook function to set, null allowed
     */
    public static void setScheduleHandler(Function<? super Runnable, ? extends Runnable> handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onScheduleHandler = handler;
    }

    /**
     * Revokes the lockdown, only for testing purposes.
     */
    /* test. */static void unlock() {
        lockdown = false;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static Function<? super Promise, ? extends Promise> getPromiseAssembly() {
        return onPromiseAssembly;
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    @SuppressWarnings("rawtypes")
    public static BiFunction<? super Promise, ? super Observer, ? extends Observer> getOnObservableSubscribe() {
        return onPromiseSubscribe;
    }

    /**
     * Sets the specific hook function.
     * @param onPromiseAssembly the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnObservableAssembly(Function<? super Promise, ? extends Promise> onPromiseAssembly) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        PromisePlugins.onPromiseAssembly = onPromiseAssembly;
    }

    /**
     * Sets the specific hook function.
     * @param onPromiseSubscribe the hook function to set, null allowed
     */
    @SuppressWarnings("rawtypes")
    public static void setOnObservableSubscribe(
            BiFunction<? super Promise, ? super Observer, ? extends Observer> onPromiseSubscribe) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        PromisePlugins.onPromiseSubscribe = onPromiseSubscribe;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @param observer the observer
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Observer<? super T> onSubscribe(Promise<T> source, Observer<? super T> observer) {
        BiFunction<? super Promise, ? super Observer, ? extends Observer> f = onPromiseSubscribe;
        if (f != null) {
            return apply(f, source, observer);
        }
        return observer;
    }

    /**
     * Calls the associated hook function.
     * @param <T> the value type
     * @param source the hook's input value
     * @return the value returned by the hook
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Promise<T> onAssembly(Promise<T> source) {
        Function<? super Promise, ? extends Promise> f = onPromiseAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }

    /**
     * Called before an operator attempts a blocking operation
     * such as awaiting a condition or signal
     * and should return true to indicate the operator
     * should not block but throw an IllegalArgumentException.
     * <p>History: 2.0.5 - experimental
     * @return true if the blocking should be prevented
     * @see #setFailOnNonBlockingScheduler(boolean)
     * @since 2.1
     */
    public static boolean onBeforeBlocking() {
        BooleanSupplier f = onBeforeBlocking;
        if (f != null) {
            try {
                return f.getAsBoolean();
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        return false;
    }

    /**
     * Set the handler that is called when an operator attempts a blocking
     * await; the handler should return true to prevent the blocking
     * and to signal an IllegalStateException instead.
     * <p>History: 2.0.5 - experimental
     * @param handler the handler to set, null resets to the default handler
     * that always returns false
     * @see #onBeforeBlocking()
     * @since 2.1
     */
    public static void setOnBeforeBlocking(BooleanSupplier handler) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        onBeforeBlocking = handler;
    }

    /**
     * Returns the current blocking handler or null if no custom handler
     * is set.
     * <p>History: 2.0.5 - experimental
     * @return the current blocking handler or null if not specified
     * @since 2.1
     */
    public static BooleanSupplier getOnBeforeBlocking() {
        return onBeforeBlocking;
    }

    /**
     * Wraps the call to the function in try-catch and propagates thrown
     * checked exceptions as RuntimeException.
     * @param <T> the input type
     * @param <R> the output type
     * @param f the function to call, not null (not verified)
     * @param t the parameter value to the function
     * @return the result of the function call
     */
    static <T, R> R apply(Function<T, R> f, T t) {
        try {
            return f.apply(t);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Wraps the call to the function in try-catch and propagates thrown
     * checked exceptions as RuntimeException.
     * @param <T> the first input type
     * @param <U> the second input type
     * @param <R> the output type
     * @param f the function to call, not null (not verified)
     * @param t the first parameter value to the function
     * @param u the second parameter value to the function
     * @return the result of the function call
     */
    static <T, U, R> R apply(BiFunction<T, U, R> f, T t, U u) {
        try {
            return f.apply(t, u);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /** Helper class, no instances. */
    private PromisePlugins() {
        throw new IllegalStateException("No instances!");
    }
}
