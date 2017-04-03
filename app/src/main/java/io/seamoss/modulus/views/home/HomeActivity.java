package io.seamoss.modulus.views.home;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.BindView;
import dagger.Module;
import io.seamoss.modulus.Modulus;
import io.seamoss.modulus.R;
import io.seamoss.modulus.base.BaseActivity;
import io.seamoss.modulus.base.nav.BaseNavActivity;
import io.seamoss.modulus.views.capture.CaptureActivity;
import io.seamoss.modulus.views.common.CaptureContainer;
import io.seamoss.modulus.views.common.CaptureListFragment;
import timber.log.Timber;

/**
 * Created by Alexander Melton on 3/26/2017.
 */

public class HomeActivity extends BaseNavActivity implements HomeView, CaptureContainer {

    private static final int VIDEO_REQUEST_CODE = 102;

    @Inject
    HomePresenter homePresenter;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Home");

        Modulus.getInstance().getAppGraph().inject(this);
        attachFragment(R.id.fragment_container, new CaptureListFragment());
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.homePresenter.attachView(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.homePresenter.detachView();
    }

    @Override
    public void onFabClick() {
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){

            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    showExplanation("Explaination", "Rationale", Manifest.permission.CAMERA, VIDEO_REQUEST_CODE);
                } else {
                    requestPermission(Manifest.permission.CAMERA, VIDEO_REQUEST_CODE);
                }
            }else{
                Intent intent = new Intent(this, CaptureActivity.class);
                intent.putExtras(getIntent());
                startActivity(intent);
            }
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }


    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == VIDEO_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(this, CaptureActivity.class);
                intent.putExtras(getIntent());
                startActivity(intent);
            }else{
                Timber.d("FAILED " + PackageManager.PERMISSION_GRANTED + " " + grantResults[0]);
            }
        }else{
            Timber.d("Expected " + VIDEO_REQUEST_CODE + " Got: " + requestCode);
        }
        Timber.d("REQUEST RESULT");
    }
}
