package io.seamoss.modulus.hardware;

/**
 * Created by Alexander Melton on 4/1/2017.
 */

public class Doppler {
    private Doppler instance;

    public Doppler getInstance() {
        if(instance == null) instance = new Doppler();
        return instance;
    }

    private Doppler(){

    }


}
