package io.seamoss.modulus.di.modules;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.seamoss.modulus.views.capture.CapturePresenter;
import io.seamoss.modulus.views.home.HomePresenter;
import io.seamoss.modulus.views.signin.SigninPresenter;

/**
 * Created by Alexander Melton on 3/26/2017.
 */

@Module
public class PresenterModule {

    @Provides
    HomePresenter providesHomePresenter(){ return new HomePresenter(); }

    @Provides
    SigninPresenter providesSigninPresenter(){ return new SigninPresenter();}

    @Provides
    CapturePresenter providesCapturePresenter(){ return new CapturePresenter(); }
}
