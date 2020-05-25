/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/15
 * Description: [PT-40][Intranet Chat] [APP][UI] Home page ui
 */
package com.skysoft.smart.intranetchat.ui.fragment.main.contact;

import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.BaseFragment;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.signal.AvatarSignal;
import com.skysoft.smart.intranetchat.bean.signal.ContactSignal;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.listener.AdapterOnClickListener;
import com.skysoft.smart.intranetchat.model.contact.ContactListAdapter;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity;
import com.skysoft.smart.intranetchat.ui.activity.userinfoshow.UserInfoShowActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ContactFragment extends BaseFragment {

    private ListView contactListView;
    private TextView mPageTitle;
    private ContactListAdapter mContactAdapter;

    private AdapterOnClickListener mListener = new AdapterOnClickListener(){
        @Override
        public void onItemClickListener(View view, Object obj, int position) {
            //TODO 点击事件
            super.onItemClickListener(view, obj, position);
            ContactEntity contact = (ContactEntity) obj;
            switch (view.getId()) {
                case R.id.contact_head:
                    UserInfoShowActivity.go(getContext(),contact.getId());
                    break;
                case R.id.contact_list_item:
                    LatestManager.getInstance().refreshUnRead(contact.getId());
                    ChatRoomActivity.go(getContext(), GsonTools.toJson(contact),false);
                    break;

            }
        }
    };

    @Override
    protected int getLayout() {
        return R.layout.fragment_main_contact;
    }

    @Override
    protected void initView(View view) {
        contactListView = view.findViewById(R.id.list_main_contact);
        mPageTitle = view.findViewById(R.id.page_title);
    }

    @Override
    protected void initData() {
        EventBus.getDefault().register(this);

        mPageTitle.setText("联系人");
        mContactAdapter = ContactManager.getInstance().initAdapter(getContext());
        mContactAdapter.setListener(mListener);
        contactListView.setAdapter(mContactAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mContactAdapter = null;
        ContactManager.getInstance().setAdapter(null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveContactSignal(ContactSignal signal) {
        mContactAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveAvatarSignal(AvatarSignal signal) {
        mContactAdapter.notifyDataSetChanged();
    }
}