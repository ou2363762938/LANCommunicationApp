/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/11/6
 * Description: [Intranet Chat] [APP][UI] Group chat
 */
package com.skysoft.smart.intranetchat.ui.activity.chatroom.EstablishGroup;

import androidx.annotation.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.skysoft.smart.intranetchat.bean.signal.AvatarSignal;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.group.EstablishGroupAdapter;
import com.skysoft.smart.intranetchat.model.group.GroupManager;

import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.BaseActivity;
import com.skysoft.smart.intranetchat.bean.network.GroupMemberList;
import com.skysoft.smart.intranetchat.database.table.GroupMemberEntity;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;
import com.skysoft.smart.intranetchat.tools.toastutil.ToastUtil;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class EstablishGroupActivity extends BaseActivity {
    private static String TAG = EstablishGroupActivity.class.getSimpleName();
    private boolean isGroup;
    private int mGroupId;
    private TextView mCancel;
    private TextView mConfirm;

    private ListView mContactListView;
    private EstablishGroupAdapter mAdapter;
    private List<GroupMemberEntity> groupMemberEntities;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_establish_group);
        initData();
        initView();
        setListener();
    }

    private void initView() {
        mContactListView = findViewById(R.id.establish_group_list);
        mCancel = findViewById(R.id.establish_group_cancel);
        mConfirm = findViewById(R.id.establish_group_confirm);

        if (mGroupId != -1){
            //查看群成员
            mConfirm.setVisibility(View.GONE);
            mCancel.setText(getText(R.string.back));
        }

        mContactListView.setAdapter(mAdapter);
    }

    private void initData() {
        EventBus.getDefault().register(this);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mGroupId = bundle.getInt(ChatRoomConfig.GROUP);

        mAdapter = GroupManager.getInstance().initAdapter(this, mGroupId);
    }

    private void setListener() {
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QuickClickListener.isFastClick()) {
                    if (mAdapter.getSelectedNumber() < 1) {
                        ToastUtil.toast(EstablishGroupActivity.this, getString(R.string.build_group_number));
                        return;
                    }

                    GroupManager.getInstance().establishGroup(EstablishGroupActivity.this);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        GroupManager.getInstance().setAdapter(null);
    }

    public static void go(Activity activity, int groupId){
        Intent intent = new Intent(activity,EstablishGroupActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt(ChatRoomConfig.GROUP,groupId);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveGroupMembers(GroupMemberList membersBean){
        GroupManager.getInstance().receiveMembers(membersBean);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveAvatarSignal(AvatarSignal signal) {
        mAdapter.notifyDataSetChanged();
    }
}
