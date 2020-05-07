/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/15
 * Description: [PT-40][Intranet Chat] [APP][UI] Home page ui
 */
package com.skysoft.smart.intranetchat.ui.fragment.main.message;


import com.skysoft.smart.intranetchat.app.BaseFragment;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.tools.listsort.MessageListSort;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.EstablishGroup.EstablishGroupActivity;

public class MessageFragment extends BaseFragment implements View.OnClickListener {

    private static String TAG = MessageFragment.class.getSimpleName();
    private ListView mMessageListView;
    private TextView mEstablishGroup;
    private TextView mPageTitle;

    @Override
    protected int getLayout() {
        return R.layout.fragment_main_message;
    }

    @Override
    protected void initView(View view) {
        mPageTitle = view.findViewById(R.id.page_title);
        mMessageListView = view.findViewById(R.id.list_main_message);
        mEstablishGroup = view.findViewById(R.id.page_establish_group);
        mEstablishGroup.setOnClickListener(this::onClick);
    }

    @Override
    protected  void initData() {
        MessageListSort.CollectionsList(IntranetChatApplication.getMessageList());
        IntranetChatApplication.setsMessageListAdapter(new MessageListAdapter(IntranetChatApplication.getMessageList(),getContext()));
        mMessageListView.setAdapter(IntranetChatApplication.getsMessageListAdapter());

        mPageTitle.setText("消息");
        mEstablishGroup.setText(getResources().getText(R.string.MessageFragment_mEstablishGroup));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.page_establish_group:
                EstablishGroupActivity.go(getActivity(), "group", false);
                break;
                default:
                    break;
        }
    }
}