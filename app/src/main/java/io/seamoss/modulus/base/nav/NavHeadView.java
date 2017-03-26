package io.seamoss.modulus.base.nav;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.seamoss.modulus.R;

/**
 * Created by Alexander Melton on 2/12/2017.
 */

public class NavHeadView extends LinearLayout{

    @BindView(R.id.nav_profile_image)
    ImageView profilePic;
    @BindView(R.id.nav_profile_name)
    TextView profileName;
    @BindView(R.id.nav_active_board)
    TextView activeBoard;
    @BindView(R.id.background_nav)
    ImageView background;


    public NavHeadView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.view_nav_head, this);
        if(isInEditMode()) return;

        ButterKnife.bind(this);

        buildHeaderView();
    }

    private void buildHeaderView(){


    }

}
