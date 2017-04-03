package io.seamoss.modulus.di;

import javax.inject.Singleton;

import dagger.Component;
import io.seamoss.modulus.Modulus;
import io.seamoss.modulus.base.BaseActivity;
import io.seamoss.modulus.base.nav.BaseNavActivity;
import io.seamoss.modulus.di.modules.AppModule;
import io.seamoss.modulus.di.modules.NetworkModule;
import io.seamoss.modulus.di.modules.PresenterModule;
import io.seamoss.modulus.views.capture.CaptureActivity;
import io.seamoss.modulus.views.home.HomeActivity;
import io.seamoss.modulus.views.signin.SigninActivity;

/**
 * Created by Alexander Melton on 3/26/2017.
 */

@AppScope
@Component(modules = {AppModule.class, PresenterModule.class, NetworkModule.class})
public interface AppGraph {

    void inject(Modulus modulus);

    void inject (BaseNavActivity baseNavActivity);

    void inject (BaseActivity baseActivity);

    void inject(HomeActivity homeActivity);

    void inject(SigninActivity signinActivity);

    void inject(CaptureActivity captureActivity);
}
