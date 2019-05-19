package com.rxjava.rxlife;


import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleOwner;

import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * 感知Activity、Fragment生命周期的观察者
 * User: ljx
 * Date: 2019/3/30
 * Time: 21:16
 */
final class LifeMaybeObserver<T> extends AbstractLifecycle<Disposable> implements MaybeObserver<T> {

    private MaybeObserver<? super T> downstream;

    LifeMaybeObserver(MaybeObserver<? super T> downstream, LifecycleOwner owner, Event event) {
        super(owner, event);
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (DisposableHelper.setOnce(this, d)) {
            try {
                addObserver();
                downstream.onSubscribe(d);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                d.dispose();
                onError(ex);
            }
        }
    }

    @Override
    public void onSuccess(T t) {
        if (isDisposed()) return;
        lazySet(DisposableHelper.DISPOSED);
        try {
            removeObserver();
            downstream.onSuccess(t);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    }

    @Override
    public void onError(Throwable t) {
        if (isDisposed()) {
            RxJavaPlugins.onError(t);
            return;
        }
        lazySet(DisposableHelper.DISPOSED);
        try {
            removeObserver();
            downstream.onError(t);
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            RxJavaPlugins.onError(new CompositeException(t, e));
        }
    }

    @Override
    public void onComplete() {
        if (isDisposed()) return;
        lazySet(DisposableHelper.DISPOSED);
        try {
            removeObserver();
            downstream.onComplete();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            RxJavaPlugins.onError(e);
        }
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

}