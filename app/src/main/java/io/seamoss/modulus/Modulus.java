package io.seamoss.modulus;

import android.app.Application;

import io.seamoss.modulus.di.AppGraph;
import io.seamoss.modulus.di.DaggerAppGraph;
import io.seamoss.modulus.di.modules.AppModule;
import io.seamoss.modulus.di.modules.NetworkModule;
import io.seamoss.modulus.di.modules.PresenterModule;
import timber.log.Timber;

/**
 * Created by Alexander Melton on 3/26/2017.
 */

public class Modulus extends Application {
    private AppGraph appGraph;
    private static Modulus instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        appGraph = DaggerAppGraph
                .builder()
                .appModule(new AppModule(this))
                .presenterModule(new PresenterModule())
                .networkModule(new NetworkModule())
                .build();

        appGraph.inject(this);

        Timber.plant(new Timber.DebugTree());
    }

    public static Modulus getInstance(){return instance; }

    public AppGraph getAppGraph(){ return appGraph; }
}
