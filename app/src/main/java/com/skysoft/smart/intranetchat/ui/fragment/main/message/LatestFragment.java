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
import com.skysoft.smart.intranetchat.bean.signal.AvatarSignal;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.listener.AdapterOnClickListener;
import com.skysoft.smart.intranetchat.model.latest.LatestListAdapter;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.EstablishGroup.EstablishGroupActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LatestFragment extends BaseFragment implements View.OnClickListener {

    private static String TAG = LatestFragment.class.getSimpleName();
    private ListView mLatestListView;
    private TextView mEstablishGroup;
    private TextView mPageTitle;

    private LatestListAdapter mAdapter;
    private AdapterOnClickListener mListener = new AdapterOnClickListener(){
        @Override
        public void onItemClickListener(View view, Object obj, int position) {
            super.onItemClickListener(view,obj,position);
            switch (view.getId()) {
                case R.id.message_list_item:
                    LatestEntity latest = (LatestEntity) obj;
                    LatestManager.getInstance().clickLatest(getContext(),latest,position);
                    break;
                case R.id.delete:
                    LatestManager.getInstance().delete(position);
                    break;
                case R.id.top:
                    LatestManager.getInstance().top(position);
                    break;
            }
        }
    };

    @Override
    protected int getLayout() {
        return R.layout.fragment_main_message;
    }

    @Override
    protected void initView(View view) {
        mPageTitle = view.findViewById(R.id.page_title);
        mLatestListView = view.findViewById(R.id.list_main_message);
        mEstablishGroup = view.findViewById(R.id.page_establish_group);
        mEstablishGroup.setOnClickListener(this::onClick);
    }

    @Override
    protected  void initData() {
        EventBus.getDefault().register(this);
        LatestManager latestManager = LatestManager.getInstance();
        latestManager.sortLatest();
        mAdapter = latestManager.initAdapter(getContext());
        mAdapter.setListener(mListener);
        mLatestListView.setAdapter(mAdapter);

        mPageTitle.setText("消息");
        mEstablishGroup.setText(getResources().getText(R.string.MessageFragment_mEstablishGroup));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveAvatarSignal(AvatarSignal signal) {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.page_establish_group:
                EstablishGroupActivity.go(getActivity(), -1);
                break;
                default:
                    break;
        }
    }
}