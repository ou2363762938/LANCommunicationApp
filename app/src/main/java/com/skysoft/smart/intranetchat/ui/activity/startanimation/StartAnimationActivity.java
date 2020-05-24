/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/15
 * Description: [PT-38][Intranet Chat] [APP][UI]Program launch page and animation
 */
package com.skysoft.smart.intranetchat.ui.activity.startanimation;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.model.filemanager.FilePath;
import com.skysoft.smart.intranetchat.model.login.InitData;
import com.skysoft.smart.intranetchat.model.login.Login;
import com.skysoft.smart.intranetchat.tools.customstatusbar.CustomStatusBarBackground;

import java.util.Timer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;

public class StartAnimationActivity extends AppCompatActivity {
    private final String TAG = "StartAnimationActivity";

    @BindView(R.id.start_animation_logo)
    ImageView sAnimation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startanimation);

        initView();
        initData();
        Timer timer=new Timer();
        timer.schedule(Login.start(this),2001);
    }

    private void initView() {
        CustomStatusBarBackground.customStatusBarTransparent(this);
        ButterKnife.bind(this);
        rotateAnimRun(sAnimation);
    }

    private void initData() {
        FilePath.getInstance().init(
                this.getExternalFilesDir("voice").getPath(),
                this.getExternalFilesDir("video").getPath(),
                this.getExternalFilesDir("images").getPath(),
                this.getExternalFilesDir("avatar").getPath(),
                this.getExternalFilesDir("common").getPath(),
                this.getExternalFilesDir("temp").getPath()
        );


        new InitData().start();
    }

    public void rotateAnimRun(View view)
    {
        AnimatorSet set=new AnimatorSet();
        ObjectAnimator animatorTranslate=ObjectAnimator.ofFloat(sAnimation,"translationY",0,0);
        ObjectAnimator animatorScaleX=ObjectAnimator.ofFloat(sAnimation,"ScaleX",1f,2f);
        ObjectAnimator animatorScaleY=ObjectAnimator.ofFloat(sAnimation,"ScaleY",1f,2f);
        ObjectAnimator animatorAlpha=ObjectAnimator.ofFloat(sAnimation,"alpha",1f,2f);
        set.play(animatorTranslate)
                .with(animatorScaleX).with(animatorScaleY).with(animatorAlpha);
        set.setDuration(2000);
        set.setInterpolator(new AccelerateInterpolator());
        set.start();
    }
    public void onBackPressed(){
    }
}
