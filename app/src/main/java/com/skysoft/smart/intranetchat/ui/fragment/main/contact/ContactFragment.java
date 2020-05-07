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

public class ContactFragment extends BaseFragment {

    private ListView contactListView;
    private TextView mPageTitle;

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
        mPageTitle.setText("联系人");
        IntranetChatApplication.setsContactListAdapter(new ContactListAdapter(IntranetChatApplication.getsContactList(),getContext()));
        contactListView.setAdapter(IntranetChatApplication.getsContactListAdapter());
    }
}