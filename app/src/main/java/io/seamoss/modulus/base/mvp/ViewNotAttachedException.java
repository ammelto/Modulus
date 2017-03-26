package io.seamoss.modulus.base.mvp;

/**
 * Created by Alexander Melton on 2/12/2017.
 */

public class ViewNotAttachedException extends RuntimeException {

    public ViewNotAttachedException() {
        super("Must attach view to presenter before attempting to perform view operations");
    }
}
