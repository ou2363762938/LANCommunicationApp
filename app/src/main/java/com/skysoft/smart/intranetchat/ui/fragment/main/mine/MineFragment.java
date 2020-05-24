/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/15
 * Description: [PT-40][Intranet Chat] [APP][UI] Home page ui
 */
package com.skysoft.smart.intranetchat.ui.fragment.main.mine;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.app.BaseFragment;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.DialogUtil;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.model.login.Login;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;
import com.skysoft.smart.intranetchat.tools.toastutil.ToastUtil;
import com.skysoft.smart.intranetchat.ui.activity.camera.CameraActivity;
import com.skysoft.smart.intranetchat.model.camera.entity.EventMessage;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.ui.activity.camera.ClipImageActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;


public class MineFragment extends BaseFragment implements View.OnClickListener{
    private String TAG = MineFragment.class.getSimpleName();
    private TextView mTvName;
    private CircleImageView mCiAvatar;
    private TextView mTvStatus;
    private String[] mStatus;

    private TextView mPageTitle;

    @Override
    protected int getLayout() {
        return R.layout.fragment_main_mine;
    }

    @Override
    protected void initView(View root) {
        mTvName = root.findViewById(R.id.mine_info_name);
        root.findViewById(R.id.mine_info_name_constraint).setOnClickListener(this::onClick);
        mCiAvatar = root.findViewById(R.id.mine_info_head);
        root.findViewById(R.id.mine_info_head_constraint).setOnClickListener(this::onClick);
        mTvStatus = root.findViewById(R.id.mine_info_status);
        root.findViewById(R.id.mine_info_status_constraint).setOnClickListener(this::onClick);
        mPageTitle = root.findViewById(R.id.page_title);

        MineInfoManager manager = MineInfoManager.getInstance();
        if (manager.getName()!=null){
            mTvName.setText(manager.getName());
        }
        if (!TextUtils.isEmpty(manager.getAvatarPath())){
            Glide.with(root).load(manager.getAvatarPath()).into(mCiAvatar);
        }else {
            Glide.with(root).load(R.drawable.default_head).into(mCiAvatar);
        }
    }

    @Override
    protected void initData() {
        EventBus.getDefault().register(this);
        mStatus = getActivity().getResources().getStringArray(R.array.Status);

        mPageTitle.setText("个人");
    }

    @Override
    public void onClick(View v) {
        if (QuickClickListener.isFastClick()) {
            switch (v.getId()) {
                case R.id.mine_info_head_constraint:
                    changeAvatar();
                    break;
                case R.id.mine_info_name_constraint:
                    changeUserName();
                    break;
                case R.id.mine_info_status_constraint:
                    changeStatus();
                    break;
                    default:
                        break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveEventMessage(EventMessage eventMessage){
        TLog.d(TAG, "onReceiveEventMessage: eventMessage.getType() = " + eventMessage.getType());
        if (eventMessage.getType() == 1 || eventMessage.getType() == 4){
            if (!TextUtils.isEmpty(eventMessage.getMessage())){
                Glide.with(getContext()).load(eventMessage.getMessage()).into(mCiAvatar);

                MineInfoManager.getInstance().setAvatar(eventMessage.getMessage());
                Login.broadcastChangeAvatar();
            }
        }
    }

    private void changeUserName() {
        View inflate = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_input_name, null);
        TextView inputNewName = inflate.findViewById(R.id.dialog_input);
        inputNewName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        inputNewName.setText(MineInfoManager.getInstance().getName());
        DialogUtil.createDialog(getActivity(), inflate, "", "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = inputNewName.getText().toString();
                if (TextUtils.isEmpty(newName) || newName.equals(MineInfoManager.getInstance().getName())){
                    return;
                }
                if (newName.length() <= 1 || newName.length() > 12){
                    ToastUtil.toast(getActivity(),getString(R.string.limit_name_length));
                    return;
                }
                mTvName.setText(newName);
                MineInfoManager.getInstance().setName(newName);
                Login.broadcastUserInfo();
            }
        }, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();
    }

    private void changeAvatar() {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_photo_choose, null);
        dialogView.findViewById(R.id.dialog_button_photograph).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraActivity.goActivity(getContext(),true);
                dialog.dismiss();
            }
        });
        dialogView.findViewById(R.id.dialog_button_map_depot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goPhotoAlbum();
                dialog.dismiss();
            }
        });
        dialogView.findViewById(R.id.dialog_button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(dialogView);
        dialog.show();
    }

    private void changeStatus() {
        AlertDialog dialog = DialogUtil.createListDialog(getContext(), "状态", R.array.Status, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int status = 0;
                mTvStatus.setText(mStatus[which]);
                switch (which){
                    case 0:
                        status = Config.STATUS_ONLINE;
                        break;
                    case 1:
                        status = Config.STATUS_BUSY;
                        break;
                }
                if (status != MineInfoManager.getInstance().getStatus()){
                    MineInfoManager.getInstance().setStatus(status);
                    Login.broadcastUserInfo();
                }
            }
        });
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void goPhotoAlbum() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK) {
            //图片
            ClipImageActivity.goActivity(getContext(), data.getData(),true);
        }
    }
}
