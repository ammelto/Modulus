package io.seamoss.modulus.base.mvp;


public interface Presenter<V extends BaseView> {

    void attachView(V view);

    void detachView();
}
