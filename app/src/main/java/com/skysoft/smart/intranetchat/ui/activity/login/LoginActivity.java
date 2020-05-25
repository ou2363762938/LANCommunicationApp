/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/15
 * Description: [PT-39][Intranet Chat] [APP][UI] Login UI
 */
package com.skysoft.smart.intranetchat.ui.activity.login;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.bean.base.DeviceInfoBean;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.skysoft.smart.intranetchat.MainActivity;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.model.login.Login;
import com.skysoft.smart.intranetchat.tools.customstatusbar.CustomStatusBarBackground;
import com.skysoft.smart.intranetchat.tools.toastutil.ToastUtil;
import com.skysoft.smart.intranetchat.ui.activity.camera.CameraActivity;
import com.skysoft.smart.intranetchat.ui.activity.camera.ClipImageActivity;
import com.skysoft.smart.intranetchat.model.camera.entity.EventMessage;
import com.skysoft.smart.intranetchat.tools.Mac;
import com.skysoft.smart.intranetchat.tools.permissionmanage.PermissionManage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

//B: [PT-60][Intranet Chat] [APP][UI] contact list page,Allen Luo,2019/10/30
public class LoginActivity extends AppCompatActivity {

    public static final String TAG = "LoginActivity";
    private static BottomSheetDialog bottomSheetDialog;
    private int mMaxBottomHeight = 0;
    private OnSoftKeyboardStateChangedListener mOSSCL = new OnSoftKeyboardStateChangedListener() {

        @Override
        public void onSoftKeyboardStateChangedListener(boolean isKeyBoardShow, int keyboardHeight, int screenSize) {
            if (isKeyBoardShow){
                TLog.d(TAG,"Height --------> " + keyboardHeight);
                DeviceInfoBean.getInstance().set(keyboardHeight,screenSize);
            }
        }
    };
    private ViewTreeObserver.OnGlobalLayoutListener mLayoutChangeListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Point screenSize = new Point();
            getWindowManager().getDefaultDisplay().getSize(screenSize);

            Rect rect = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            int heightDifference = 0;
            mMaxBottomHeight = mMaxBottomHeight < rect.bottom ? rect.bottom : mMaxBottomHeight;
            if (screenSize.y == mMaxBottomHeight){
                heightDifference = screenSize.y - rect.bottom;
            }else {
                heightDifference = screenSize.y - rect.bottom + rect.top;
            }
            boolean isKeyboardShowing = heightDifference > screenSize.y/3;
            mOSSCL.onSoftKeyboardStateChangedListener(isKeyboardShowing,heightDifference,screenSize.y);
        }
    };;
    private String avatarPath = null;

    @BindView(R.id.activity_login_head)
    CircleImageView mineHead;
    @BindView(R.id.activity_login_name)
    EditText mineName;
    @BindView(R.id.activity_login_button)
    Button buttonLogin;

    private boolean isRegister = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        CustomStatusBarBackground.customStatusBarTransparent(this);
        EventBus.getDefault().register(this);

        PermissionManage.allPermissionManage(this);
        ButterKnife.bind(this);
        String mac = Mac.getMac(this);
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(mLayoutChangeListener);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEventMessage(EventMessage eventMessage){
        TLog.d(TAG, "handleEventMessage: eventMessage.getMessage() = " + eventMessage.getMessage());
        //注册时获得头像
        if (eventMessage.getType() == 1 || eventMessage.getType() == 4){
            if (!TextUtils.isEmpty(eventMessage.getMessage())){
                Glide.with(this).load(eventMessage.getMessage()).into(mineHead);
                avatarPath = eventMessage.getMessage();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnClick({R.id.activity_login_head, R.id.activity_login_button})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.activity_login_head:
                dialogView();
                break;
            case R.id.activity_login_button:
                String userName = mineName.getText().toString();
                if (TextUtils.isEmpty(userName)){
                    return;
                }
                if (userName.length() > 12 || userName.length() < 2){
                    ToastUtil.toast(LoginActivity.this, getString(R.string.limit_name_length));
                    return;
                }

                Login.register(LoginActivity.this,userName,avatarPath);

                Intent main = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(main);
                finish();
                break;
        }
    }
    //E: [PT-60][Intranet Chat] [APP][UI] contact list page,Allen Luo,2019/10/30
    public void dialogView(){
        bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_photo_choose, null);
        dialogView.findViewById(R.id.dialog_button_photograph).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraActivity.goActivity(LoginActivity.this,true);
                bottomSheetDialog.dismiss();
            }
        });
        dialogView.findViewById(R.id.dialog_button_map_depot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goPhotoAlbum();
                bottomSheetDialog.dismiss();
            }
        });
        dialogView.findViewById(R.id.dialog_button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();
            }
        });
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
    }

    private void goPhotoAlbum() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK) {
            //图片
            ClipImageActivity.goActivity(this, data.getData(),true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
