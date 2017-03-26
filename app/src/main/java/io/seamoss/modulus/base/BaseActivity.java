package io.seamoss.modulus.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.seamoss.modulus.Modulus;
import io.seamoss.modulus.R;

/**
 * Created by Alexander Melton on 2/11/2017.
 */

public abstract class BaseActivity extends AppCompatActivity{
    @Inject public Context context;

    @Nullable @BindView(R.id.toolbar)
    public Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());
        ButterKnife.bind(this);

        Modulus.getInstance().getAppGraph().inject(this);


        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

    }

    protected @LayoutRes int getLayoutResource(){
        return R.layout.activity_base;
    }
    protected void attachFragment(int containerViewId, Fragment fragment){
        attachFragment(containerViewId, fragment, false);
    }

    protected void attachFragment(int containerViewId, Fragment fragment, boolean addToBackstack){
        FragmentTransaction fragmentTransaction = this.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(containerViewId, fragment, fragment.getClass().getSimpleName());

        if(addToBackstack){
            fragmentTransaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getActiveFragment();
        if (fragment != null && fragment instanceof BackPressedInterface) {
            // if onBackPressed is true then the fragment overrides the Activity's onBackPressed
            if (((BackPressedInterface) fragment).onBackPressed()) {
                return;
            }
        }

        if (getFragmentManager().getBackStackEntryCount() == 1) {
            getFragmentManager().popBackStack();
            super.onBackPressed();
        } else if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    private Fragment getActiveFragment(){
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        if(fragmentManager.getBackStackEntryCount() > 0){
            return fragmentManager
                    .findFragmentByTag(
                            fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1)
                    .getName());
        }
        return null;
    }
}
