package com.skysoft.smart.intranetchat.ui.activity.chatroom;

import androidx.constraintlayout.widget.ConstraintLayout;
import de.hdodenhof.circleimageview.CircleImageView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.BaseActivity;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.model.SendMessage;
import com.skysoft.smart.intranetchat.bean.TransmitBean;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.LatestChatHistoryEntity;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransmitActivity extends BaseActivity implements View.OnClickListener{
    private static final String TAG = "TransmitActivity";

    public static void startActivity(Context context, String message, int recordType, String transmitRoomIdentifier){
        Intent intent = new Intent(context,TransmitActivity.class);
        intent.putExtra("message",message);
        intent.putExtra("recordType",recordType);
        intent.putExtra("transmitRoomIdentifier",transmitRoomIdentifier);
        context.startActivity(intent);
    }

    private TextView mClose;
    private ConstraintLayout mSearchBox;
    private LinearLayout mRecentlyChat;
    private List<TransmitBean> mTransmitUsers;
    private String mMessage;
    private int mRecordType;        //转发消息的内型
    private String mTransmitRoomIdentifier;     //转发消息的聊天室Identifier

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transmit);
        setStatusView(R.id.custom_status_bar_background);
        getIntentContent();
        initView();
        initData();
    }

    private void getIntentContent() {
        Intent intent = getIntent();
        mMessage = intent.getStringExtra("message");
        mRecordType = intent.getIntExtra("recordType",-1);
        mTransmitRoomIdentifier = intent.getStringExtra("transmitRoomIdentifier");
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
            //不转发给自己
            if (next.getUserIdentifier().equals(mTransmitRoomIdentifier)){
                continue;
            }
            TransmitBean transmitBean = new TransmitBean(next.getUserHeadPath(), next.getUserName(), next.getUserIdentifier(), next.getGroup() == 1 ? true : false, next.getHost());
            //消息列表的host是空值
            transmitBean.setmHost(next.getHost());
//            Iterator<ContactEntity> contactIterator = IntranetChatApplication.getsContactList().iterator();
//            while (contactIterator.hasNext()){
//                ContactEntity contactEntity = contactIterator.next();
//                if (next.getUserIdentifier().equals(contactEntity.getIdentifier())){
//                    transmitBean.setmHost(contactEntity.getHost());
//                }
//            }
            mTransmitUsers.add(transmitBean);
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
                Glide.with(this).load(next.getmAvatarPath()).into(avatar);
            }else {
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
    protected Dialog onCreateDialog(int id) {
        View view = LayoutInflater.from(TransmitActivity.this).inflate(R.layout.dialog_confirm_transmit, null);

        AlertDialog alertDialog = new AlertDialog.Builder(TransmitActivity.this)
                .setView(view)
                .create();

        Window window = alertDialog.getWindow();
        window.setBackgroundDrawable(new BitmapDrawable());
        buildDialogView(alertDialog,view,id);
        return alertDialog;
    }

    private void buildDialogView(AlertDialog alertDialog, View view, int id) {
        CircleImageView avatar = view.findViewById(R.id.transmit_avatar);
        TextView name = view.findViewById(R.id.transmit_name);
        TextView message = view.findViewById(R.id.transmit_message);
        EditText leaveWord = view.findViewById(R.id.transmit_input_leave);
        Button cancel = view.findViewById(R.id.transmit_cancel);
        Button send = view.findViewById(R.id.transmit_send);

        TransmitBean transmitBean = mTransmitUsers.get(id);
        if (!TextUtils.isEmpty(transmitBean.getmAvatarPath())){
            Glide.with(TransmitActivity.this).load(transmitBean.getmAvatarPath()).into(avatar);
        }
        name.setText(transmitBean.getmUseName());
        message.setText(mMessage);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.transmit_cancel){
                    alertDialog.dismiss();
                }else {
                    String leave = leaveWord.getText().toString();
                    Log.d(TAG, "onClick: leave = " + leave);
                    alertDialog.dismiss();
                    TransmitActivity.this.finish();
                }
            }
        };
        cancel.setOnClickListener(onClickListener);
        send.setOnClickListener(onClickListener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.close_transmit:
                finish();
                break;
            case R.id.contact_list_item:
                int i = ((ViewGroup) v.getParent()).indexOfChild(v);
                Log.d(TAG, "onClick: i = " + i);
                showDialog(i);
//                if (mRecordType == ChatRoomConfig.RECORD_TEXT){
//                    transmitMessage(i);
//                }
                break;
        }
    }

    //转发文字
    private void transmitMessage(int i) {
        MessageBean messageBean = new MessageBean();
        messageBean.setMsg(mMessage);
        messageBean.setTimeStamp(System.currentTimeMillis());
        messageBean.setSender(IntranetChatApplication.getsMineUserInfo().getIdentifier());
        if (mTransmitUsers.get(i).isGroup()){
            messageBean.setReceiver(mTransmitUsers.get(i).getmUserIdentifier());
        }else {
            messageBean.setReceiver(messageBean.getSender());
        }
        ChatRecordEntity chatRecordEntity = new ChatRecordEntity();
        chatRecordEntity.setContent(mMessage);
        chatRecordEntity.setIsReceive(ChatRoomConfig.SEND_MESSAGE);
        chatRecordEntity.setReceiver(mTransmitUsers.get(i).getmUserIdentifier());
        chatRecordEntity.setSender(IntranetChatApplication.getsMineUserInfo().getIdentifier());
        chatRecordEntity.setTime(messageBean.getTimeStamp());
        chatRecordEntity.setType(ChatRoomConfig.RECORD_TEXT);//发送消息
        if (!mTransmitUsers.get(i).isGroup()){
            if (!TextUtils.isEmpty(mTransmitUsers.get(i).getmHost())){
                SendMessage.sendMessage(messageBean,mTransmitUsers.get(i).getmHost());
            }
        }else {
            SendMessage.broadMessage(messageBean);
        }
    }
}
