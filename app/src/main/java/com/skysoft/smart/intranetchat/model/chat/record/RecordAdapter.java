/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/30
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.model.chat.record;

import android.content.Context;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.database.dao.RecordDao;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;

import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomMessageViewHolder;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.OnClickReplayOrNotify;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.OnScrollToPosition;

import java.util.ArrayList;
import java.util.List;


public class RecordAdapter extends RecyclerView.Adapter<ChatRoomMessageViewHolder> implements View.OnTouchListener {
    private static String TAG = RecordAdapter.class.getSimpleName();
    private Context mContext;

    private int mTopPosition;
    private int mReceiver;

    private boolean mFirstLoadHistory = true;
    private boolean isUpLongClickAvatar = true;
    private boolean isGroup;

    private View mInflate;

    private List<RecordEntity> mRecordList = new ArrayList<>();
    private RecyclerView mPopupRecyclerView;
    private ArrayList<String> mHasCode = new ArrayList<>();

    private OnClickReplayOrNotify mOnClickReplayOrNotify;
    private OnScrollToPosition mOnScrollToPosition;
    private ChatRoomMessageViewHolder mLongClickHolder;
    private ChatRoomMessageViewHolder mHolder;

    private BindRecord mBindRecord;

    public RecordAdapter(Context mContext, int receiver, Boolean isGroup, List<RecordEntity> records) {
        this.mContext = mContext;
        this.mReceiver = receiver;
        this.isGroup = isGroup;
        this.mRecordList = records;
        this.mBindRecord = new BindRecord(mContext,this);
        this.mInflate = LayoutInflater.from(mContext).inflate(R.layout.chat_message_popup_window, null);
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public int getReceiver() {
        return mReceiver;
    }

    public OnClickReplayOrNotify getOnClickReplayOrNotify() {
        return mOnClickReplayOrNotify;
    }

    public void setOnClickReplayOrNotify(OnClickReplayOrNotify mOnClickReplayOrNotify) {
        this.mOnClickReplayOrNotify = mOnClickReplayOrNotify;
    }

    public int getTopPosition() {
        return mTopPosition;
    }

    public void setTopPosition(int mTopPosition) {
        this.mTopPosition = mTopPosition;
    }

    public OnScrollToPosition getOnScrollToPosition() {
        return mOnScrollToPosition;
    }

    public void setOnScrollToPosition(OnScrollToPosition mOnScrollToPosition) {
        this.mOnScrollToPosition = mOnScrollToPosition;
    }

    public View getInflate() {
        return mInflate;
    }

    @NonNull
    @Override
    public ChatRoomMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.listview_chat_room, parent, false);
        mHolder =  new ChatRoomMessageViewHolder(inflate);
        return mHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomMessageViewHolder holder, int position) {
        mTopPosition = position;
        RecordEntity bean = mRecordList.get(position);
        String hexString = Integer.toHexString(holder.hashCode());
        if (mHasCode.contains(hexString)){
            holder.hiddenAll();
            onBindView(holder,bean);
        }else {
            mHasCode.add(hexString);
            onBindView(holder,bean);
        }
    }

    private void onBindView(ChatRoomMessageViewHolder holder, RecordEntity bean){
        if (bean.getType() == ChatRoomConfig.RECORD_TIME){
            holder.getTime().setText(RoomUtils.millToFullTime(bean.getTime()));
            holder.getTime().setVisibility(View.VISIBLE);
            return;
        }
        //显示头像
        bindAvatar(holder,bean);

        //显示内容
        mBindRecord.bind(holder,bean);
    }

    public void bindAvatar(ChatRoomMessageViewHolder holder, RecordEntity bean){
        mBindRecord.bindAvatar(holder,bean);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.list_view_chat_room_sender_avatar){
            if (event.getAction() == MotionEvent.ACTION_UP){
                TLog.d(TAG, "onTouch: up");
                isUpLongClickAvatar = true;     //松开长按
                v.setOnTouchListener(null);     //取消触摸监听
                QuickClickListener.isFastClick();       //避免松开手指立即触发单击事件
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return mRecordList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void addAll(List<RecordEntity> records) {
        mRecordList.addAll(0,records);
    }

    public void setPopupRecyclerView(RecyclerView recyclerView) {
        this.mPopupRecyclerView = recyclerView;
    }

    public RecyclerView getPopupRecyclerView() {
        return this.mPopupRecyclerView;
    }

//    /**
//     * 回复消息*/
//    public void replayChatRecord(){
//        if (null != mLongClickHolder && null != mOnClickReplayOrNotify){
//            mOnClickReplayOrNotify.onClickReplay(mRecordList.get(mLongClickPosition),mLongClickHolder.getSenderName().getText().toString());
//        }
//    }
}
