package io.seamoss.modulus.views.signin;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

import butterknife.BindView;
import io.seamoss.modulus.Modulus;
import io.seamoss.modulus.R;
import io.seamoss.modulus.base.BaseActivity;
import io.seamoss.modulus.views.home.HomeActivity;

/**
 * Created by Alexander Melton on 3/27/2017.
 */

public class SigninActivity extends BaseActivity implements SigninView{

    @Inject
    SigninPresenter signinPresenter;

    @BindView(R.id.authentication_guest_sign_in)
    Button guestSignIn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Modulus.getInstance().getAppGraph().inject(this);
        guestSignIn.setOnClickListener(this::onGuestSignIn);

    }

    public void onGuestSignIn(View v){
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtras(getIntent());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        signinPresenter.attachView(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        signinPresenter.detachView();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_signin;
    }
}
