package io.seamoss.modulus.base.nav;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;

import butterknife.BindView;
import io.seamoss.modulus.R;
import io.seamoss.modulus.base.BaseActivity;

/**
 * Created by Alexander Melton on 2/16/2017.
 */

public abstract class BaseNavActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.nav_view) NavigationView navigationView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawerLayout != null){
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_closed);

            drawerLayout.addDrawerListener(toggle);
            if(getSupportActionBar() != null) getSupportActionBar().setHomeButtonEnabled(true);

            toggle.syncState();
        }

        if(navigationView != null){
            navigationView.setNavigationItemSelectedListener(this);
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_nav;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        return true;
    }
}

