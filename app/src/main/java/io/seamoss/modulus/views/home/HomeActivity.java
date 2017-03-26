package io.seamoss.modulus.views.home;

import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import dagger.Module;
import io.seamoss.modulus.Modulus;
import io.seamoss.modulus.base.BaseActivity;
import io.seamoss.modulus.base.nav.BaseNavActivity;

/**
 * Created by Alexander Melton on 3/26/2017.
 */

public class HomeActivity extends BaseNavActivity implements HomeView {

    @Inject
    HomePresenter homePresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Home");

        Modulus.getInstance().getAppGraph().inject(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        this.homePresenter.attachView(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.homePresenter.detachView();
    }

    @Override
    protected int getLayoutResource() {
        return super.getLayoutResource();
    }
}
