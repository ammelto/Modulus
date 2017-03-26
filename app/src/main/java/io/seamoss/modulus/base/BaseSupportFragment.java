package io.seamoss.modulus.base;


import android.app.Fragment;

import butterknife.Unbinder;

/**
 * Created by Alexander Melton on 2/13/2017.
 */

public abstract class BaseSupportFragment extends Fragment {
    private Unbinder unbinder = Unbinder.EMPTY;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    protected void setUnbinder(Unbinder unbinder) {
        this.unbinder = unbinder;
    }

    protected BaseActivity getBaseActivity(){
        return (BaseActivity) getActivity();
    }
}
