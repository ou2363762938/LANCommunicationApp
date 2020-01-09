package com.skysoft.smart.intranetchat.ui.activity.chatroom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import de.hdodenhof.circleimageview.CircleImageView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.BaseActivity;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.TransmitBean;
import com.skysoft.smart.intranetchat.database.table.LatestChatHistoryEntity;
import com.skysoft.smart.intranetchat.tools.customstatusbar.CustomStatusBarBackground;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransmitActivity extends BaseActivity implements View.OnClickListener{
    private static final String TAG = "TransmitActivity";

    public static void startActivity(Context context){
        Intent intent = new Intent(context,TransmitActivity.class);
        context.startActivity(intent);
    }

    private TextView mClose;
    private ConstraintLayout mSearchBox;
    private LinearLayout mRecentlyChat;
    private List<TransmitBean> mTransmitUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transmit);
        setStatusView(R.id.custom_status_bar_background);
        initView();
        initData();
    }

    private void initView() {
        mClose = findViewById(R.id.close_transmit);
        mSearchBox = findViewById(R.id.transmit_search_box);
        mRecentlyChat = findViewById(R.id.transmit_recently_chat);

        mClose.setOnClickListener(this);
        mSearchBox.setOnClickListener(this::onClick);
    }

    private void initData() {
        mTransmitUsers = new ArrayList<>();
        //获得显示内容
        List<LatestChatHistoryEntity> messageList = IntranetChatApplication.getMessageList();
        Iterator<LatestChatHistoryEntity> iterator = messageList.iterator();
        while (iterator.hasNext()){
            LatestChatHistoryEntity next = iterator.next();
            mTransmitUsers.add(new TransmitBean(next.getUserHeadPath(),next.getUserName(),next.getUserIdentifier()));
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        Iterator<TransmitBean> beanIterator = mTransmitUsers.iterator();
        int index = 0;
        while (beanIterator.hasNext()){
            //加载联系人布局
            View view = inflater.inflate(R.layout.listview_main_contact, null);
            view.findViewById(R.id.contact_state).setVisibility(View.GONE);
            CircleImageView avatar = view.findViewById(R.id.contact_head);
            TextView name = view.findViewById(R.id.contact_name);

            //取出加载内容
            TransmitBean next = beanIterator.next();
            if (!TextUtils.isEmpty(next.getmAvatarPath())){
                Log.d(TAG, "initData: avatar = " + next.getmAvatarPath());
                Glide.with(this).load(next.getmAvatarPath()).into(avatar);
            }else {
                Log.d(TAG, "initData: avatar = null");
                avatar.setImageResource(R.drawable.default_head);
            }

            if (!TextUtils.isEmpty(next.getmUseName())){
                name.setText(next.getmUseName());
            }else {
                continue;
            }
            //设置背景为透明色
            view.setBackgroundColor(getResources().getColor(R.color.color_transparent));
            view.setOnClickListener(this::onClick);

            mRecentlyChat.addView(view,index++,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    @Override
    public void onClick(View v) {
    }
}
