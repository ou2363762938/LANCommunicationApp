package com.skysoft.smart.intranetchat.ui.activity.chatroom.PopupWindow;

import androidx.constraintlayout.widget.ConstraintLayout;
import de.hdodenhof.circleimageview.CircleImageView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import com.google.gson.Gson;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.chat.Message;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.filemanager.FileManager;
import com.skysoft.smart.intranetchat.model.group.GroupManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.BaseActivity;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.network.SendMessageBean;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.RecordDao;
import com.skysoft.smart.intranetchat.model.net_model.SendMessage;
import com.skysoft.smart.intranetchat.bean.chat.TransmitBean;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.model.camera.entity.EventMessage;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.model.chat.record.RecordAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransmitActivity extends BaseActivity implements View.OnClickListener{
    private static final String TAG = "TransmitActivity";

    public static void startActivity(Context context, String message, int recordType, int receiver){
        Intent intent = new Intent(context,TransmitActivity.class);
        intent.putExtra("message",message);
        intent.putExtra("recordType",recordType);
        intent.putExtra("receiver",receiver);
        context.startActivity(intent);
    }

    private TextView mClose;
    private ConstraintLayout mSearchBox;
    private LinearLayout mRecentlyChat;
    private List<TransmitBean> mTransmitUsers;      //转发界面的最近聊天列表
    private String mMessage;
    private int mRecordType;        //转发消息的内型
    private int mReceiver;     //转发消息的聊天室Identifier
    private FileEntity mFile;
    private ScrollView mScroll;     //title以下的内容
    private ConstraintLayout mTitle;
    private LinearLayout mSearchInputBox;
    private LinearLayout mSearchResultBox;
    private ListView mSearchResultList;     //搜索结果的list
    private TextView mNoMoreResult;     //没有更多搜索结果
    private EditText mInputSearchKey;
    private ImageView mClearInputSearchKey;   //删除搜索框输入的内容
    private TextView mCancelSearch;     //取消搜索

    private SearchResultAdapter mAdapter;

    private TextWatcher mInputSearchKeyListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            TLog.d(TAG, "afterTextChanged: s = " + s.toString());
            if (!TextUtils.isEmpty(s.toString())){
                mSearchResultBox.setBackgroundColor(getResources().getColor(R.color.color_white));
                mNoMoreResult.setVisibility(View.VISIBLE);
                if(mSearchResultList.getVisibility() == View.GONE){
                    mSearchResultList.setVisibility(View.VISIBLE);
                }
            }else {
                mSearchResultBox.setBackgroundColor(getResources().getColor(R.color.color_light_black));
                mNoMoreResult.setVisibility(View.GONE);
                mSearchResultList.setVisibility(View.GONE);
            }

            mAdapter.onInputSearchKeyChange(s.toString());
        }
    };

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
        mReceiver = intent.getIntExtra("receiver",-1);
    }

    private void initView() {
        mClose = findViewById(R.id.close_transmit);
        mSearchBox = findViewById(R.id.transmit_search_box);
        mRecentlyChat = findViewById(R.id.transmit_recently_chat);
        mScroll = findViewById(R.id.transmit_scroll);
        mTitle = findViewById(R.id.transmit_title);
        mSearchInputBox = findViewById(R.id.transmit_search_input_box);
        mSearchResultBox = findViewById(R.id.transmit_search_result_box);
        mSearchResultList = findViewById(R.id.transmit_search_result_list);
        mNoMoreResult = findViewById(R.id.transmit_no_more_result);
        mInputSearchKey = findViewById(R.id.transmit_search);
        mClearInputSearchKey = findViewById(R.id.transmit_clear_search_content);
        mCancelSearch = findViewById(R.id.transmit_cancel_search);

        mClose.setOnClickListener(this);
        mSearchBox.setOnClickListener(this::onClick);
        mClearInputSearchKey.setOnClickListener(this::onClick);
        mCancelSearch.setOnClickListener(this::onClick);
        mInputSearchKey.addTextChangedListener(mInputSearchKeyListener);
        mAdapter = new SearchResultAdapter(this,
                new OnSelectSearchResultListener() {
            @Override
            public void onSelectSearchResultListener(TransmitBean bean) {
                closeSearchBox();
                mTransmitUsers.add(bean);
                showDialog(mTransmitUsers.size()-1);
            }
        });
        mSearchResultList.setAdapter(mAdapter);
    }

    private void initData() {
        mTransmitUsers = LatestManager.getInstance().getTransmits(mReceiver);

        LayoutInflater inflater = LayoutInflater.from(this);
        int index = 0;
        for (TransmitBean bean:mTransmitUsers) {
            //加载联系人布局
            View view = inflater.inflate(R.layout.listview_main_contact, null);
            view.findViewById(R.id.contact_state).setVisibility(View.GONE);
            CircleImageView avatar = view.findViewById(R.id.contact_head);
            TextView name = view.findViewById(R.id.contact_name);

            if (bean.isGroup()) {
                GroupEntity group = GroupManager.getInstance().getGroup(bean.getUser());
                bean.setHost(group.getHost());
                bean.setAvatar(group.getAvatar());
                bean.setName(group.getName());
            } else {
                ContactEntity contact = ContactManager.getInstance().getContact(bean.getUser());
                bean.setHost(contact.getHost());
                bean.setAvatar(contact.getAvatar());
                bean.setName(contact.getName());
            }

            //取出加载内容
            AvatarManager.getInstance().loadContactAvatar(this,avatar,bean.getAvatar());

            if (!TextUtils.isEmpty(bean.getName())){
                name.setText(bean.getName());
            }else {
                continue;
            }

            //设置背景为透明色
            view.setBackgroundColor(getResources().getColor(R.color.color_transparent));
            view.setOnClickListener(this::onClick);

            mRecentlyChat.addView(view,
                    index++,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        View view = LayoutInflater.from(TransmitActivity.this).inflate(R.layout.dialog_confirm_transmit, null);

        AlertDialog alertDialog = new AlertDialog.Builder(TransmitActivity.this)
                .setView(view)
                .create();

        Window window = alertDialog.getWindow();
        window.setBackgroundDrawable(new BitmapDrawable());     //显示Dialog圆角
        buildDialogView(alertDialog,view,id);       //填充dialog的控件，添加点击事件等
        return alertDialog;
    }

    /**
     * 给Dialog绑定事件
     * @param alertDialog 弹出的alertDialog，点击取消和发送时关闭alertDialog
     * @param view 自定义的view，给view绑定事件
     * @param id alertDialog对应mTransmitUsers的位置*/
    private void buildDialogView(AlertDialog alertDialog, View view, int id) {
        CircleImageView avatar = view.findViewById(R.id.transmit_avatar);   //转发对象的头像
        TextView name = view.findViewById(R.id.transmit_name);
        TextView message = view.findViewById(R.id.transmit_message);    //显示文字消息
        ImageView image = view.findViewById(R.id.transmit_image);       //显示图片
        EditText leaveWord = view.findViewById(R.id.transmit_input_leave);      //留言
        Button cancel = view.findViewById(R.id.transmit_cancel);        //取消按钮
        Button send = view.findViewById(R.id.transmit_send);        //确认按钮

        TransmitBean bean = mTransmitUsers.get(id);     //转发对象
        AvatarManager.getInstance().loadContactAvatar(this,avatar,bean.getAvatar());
        name.setText(bean.getName());

        if (mRecordType == ChatRoomConfig.RECORD_FILE){
            //显示转发的图片，不显示图片路径
            message.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            if (mFile == null) {
                mFile = (FileEntity) GsonTools.formJson(mMessage, FileEntity.class);
            }
            Glide.with(TransmitActivity.this).load(mFile.getPath()).into(image);
        }else {
            //显示转发的文字
            message.setText(mMessage);
        }

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.transmit_cancel){
                    alertDialog.dismiss();
                }else {
                    String leave = leaveWord.getText().toString();      //获得输入的留言
                    TLog.d(TAG, "onClick: leave = " + leave);

                    if (mRecordType == ChatRoomConfig.RECORD_TEXT){
                        //转发文字
                        transmitMessage(mMessage,mTransmitUsers.get(id));
                    }else if (mRecordType == ChatRoomConfig.RECORD_FILE){
                        //转发图片
                        transmitFile(id);
                    }
                    if (!TextUtils.isEmpty(leave)){     //如果输入留言，转发留言
                        mMessage = leave;
                        transmitMessage(leave,mTransmitUsers.get(id));
                    }

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
                TLog.d(TAG, "onClick: i = " + i);
                showDialog(i);
                break;
            case R.id.transmit_search_box:
                openSearchBox();
                break;
            case R.id.transmit_clear_search_content:
                mInputSearchKey.setText("");
                break;
            case R.id.transmit_cancel_search:
                closeSearchBox();
                break;
        }
    }

    /**
     * 点击搜索框，弹出搜索界面*/
    private void openSearchBox() {
        mScroll.setVisibility(View.GONE);
        mTitle.setVisibility(View.GONE);

        mSearchInputBox.setVisibility(View.VISIBLE);
        mSearchResultBox.setVisibility(View.VISIBLE);
        mSearchResultBox.setBackgroundColor(getResources().getColor(R.color.color_light_black));
    }

    private void closeSearchBox() {
        mInputSearchKey.setText("");
        mSearchResultBox.setVisibility(View.GONE);
        mNoMoreResult.setVisibility(View.GONE);
        mSearchResultList.setVisibility(View.GONE);
        mSearchInputBox.setVisibility(View.GONE);

        mTitle.setVisibility(View.VISIBLE);
        mScroll.setVisibility(View.VISIBLE);
    }

    /**
     * 转发文字
     * @param message 转发的文字
     * @param bean 转发对象*/
    public static void transmitMessage(String message, TransmitBean bean){
        if (bean.isGroup()) {
            Message.getInstance().send(
                    GroupManager.getInstance().getGroup(bean.getUser()),
                    message);
        } else {
            Message.getInstance().send(
                    ContactManager.getInstance().getContact(bean.getUser()),
                    message);
        }

        RecordManager.getInstance().recordText(message,-1);
        LatestManager.getInstance().send(bean.getUser(),message,bean.isGroup());
    }

    /**
     * 转发文件，目前仅支持图片和视频
     * @param i 转发对象在mTransmitUsers中的位置*/
    private void transmitFile(int i){
        TransmitBean bean = mTransmitUsers.get(i);
        FileManager.getInstance().notify(bean.getUser(),mFile,bean.isGroup());
        RecordManager.getInstance().recordFile(mFile,-1);
        LatestManager.getInstance().send(bean.getUser(),mFile,bean.isGroup());
    }

    @Override
    public void onBackPressed() {

        if (mTitle.getVisibility() == View.GONE){
            closeSearchBox();
            return;
        }
        super.onBackPressed();
    }
}
