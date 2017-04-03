package io.seamoss.modulus.views.common;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.seamoss.modulus.R;
import io.seamoss.modulus.base.BaseFragment;

/**
 * Created by Alexander Melton on 3/29/2017.
 */

public class CaptureListFragment extends BaseFragment{

    @BindView(R.id.capture_fab)
    FloatingActionButton fab;

    @BindView(R.id.capture_recycler)
    RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.fragment_capture_list, container, false);
        setUnbinder(ButterKnife.bind(this, fragmentView));

        addScrollListener(recyclerView);
        fab.setOnClickListener(this::onFabClick);

        return fragmentView;
    }

    public void onFabClick(View v){
        ((CaptureContainer) getActivity()).onFabClick();
    }

    private void addScrollListener(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
                    int fab_bottomMargin = layoutParams.bottomMargin;
                    fab.animate().translationY(fab.getHeight() + fab_bottomMargin).setInterpolator(new LinearInterpolator()).start();
                } else if (dy < 0) {
                    fab.animate().translationY(0).setInterpolator(new LinearInterpolator()).start();
                }
            }
        });
    }
}
