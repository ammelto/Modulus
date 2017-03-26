package io.seamoss.modulus.di.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.seamoss.modulus.Modulus;
import io.seamoss.modulus.di.AppScope;

/**
 * Created by Alexander Melton on 3/26/2017.
 */
@Module
public class AppModule {
    private final Modulus application;

    public AppModule(Modulus app){
        this.application = app;
    }

    @Provides
    @AppScope
    Context provicesApplicationContext(){ return application; }
}
