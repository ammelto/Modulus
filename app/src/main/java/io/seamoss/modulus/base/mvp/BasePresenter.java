package io.seamoss.modulus.base.mvp;

/**
 * Created by Alexander Melton on 2/12/2017.
 */

public class BasePresenter<V extends BaseView> implements Presenter<V> {

    private V view;

    @Override
    public void attachView(V view) {
        this.view = view;
    }

    @Override
    public void detachView() {
        this.view = null;
    }

    public boolean isViewAttached() {
        return this.view != null;
    }

    public V getView() {
        if (isViewAttached()) {
            return this.view;
        }

        throw new ViewNotAttachedException();
    }
}
