package io.seamoss.modulus.di.modules;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.seamoss.modulus.views.home.HomePresenter;

/**
 * Created by Alexander Melton on 3/26/2017.
 */

@Module
public class PresenterModule {

    @Provides
    HomePresenter providesHomePresenter(){ return new HomePresenter(); }
}
