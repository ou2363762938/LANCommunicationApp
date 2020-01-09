package com.skysoft.smart.intranetchat.app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.tools.customstatusbar.CustomStatusBarBackground;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";

    private View mStatus;
    private int mTitleDrawableResource = R.drawable.custom_gradient_main_title;

    public void setTitleDrawable(int resource){
        this.mTitleDrawableResource = resource;
    }

    public void setStatusView(@IdRes int viewId){
        mStatus = findViewById(viewId);
        Log.d(TAG, "onCreate: mStatus = " + mStatus);
        CustomStatusBarBackground.customStatusBarTransparent(this);
        CustomStatusBarBackground.drawableViewStatusBar(this, mTitleDrawableResource,mStatus);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //沉浸式状态栏
    }
}
