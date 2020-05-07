/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/30
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.tools.ChatRoom.BindRecord;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;
import com.skysoft.smart.intranetchat.tools.CreateNotifyBitmap;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.database.dao.ChatRecordDao;
import com.skysoft.smart.intranetchat.model.net_model.SendFile;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;
import com.skysoft.smart.intranetchat.ui.activity.camera.PictureShowActivity;
import com.skysoft.smart.intranetchat.ui.activity.camera.VideoPlayActivity;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.model.camera.manager.MyMediaPlayerManager;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.PopupWindow.PopupWindowAdapter;
import com.skysoft.smart.intranetchat.ui.activity.userinfoshow.UserInfoShowActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity.sIsAudioRecording;

public class ChatRoomMessageAdapter extends RecyclerView.Adapter<ChatRoomMessageViewHolder> implements View.OnTouchListener {
    private static String TAG = ChatRoomMessageAdapter.class.getSimpleName();
    private Context context;

    private String mMineAvatarPath;
    private String mReceiverIdentifier;

    private int mTopPosition;

    private boolean mFirstLoadHistory = true;
    private boolean isUpLongClickAvatar = true;
    private boolean isGroup;

    private View mInflate;

    private List<ChatRecordEntity> messageBeanList = new ArrayList<>();
    private RecyclerView mPopupRecyclerView;
    private ArrayList<String> mHasCode = new ArrayList<>();

    private OnClickReplayOrNotify mOnClickReplayOrNotify;
    private OnScrollToPosition onScrollToPosition;
    private ChatRoomMessageViewHolder mLongClickHolder;
    private ChatRoomMessageViewHolder holder;

    private BindRecord mBindRecord;

    public ChatRoomMessageAdapter(Context context, String mMineAvatarPath, String receiverIdentifier, Boolean isGroup) {
        this.context = context;
        this.mMineAvatarPath = mMineAvatarPath;
        this.mReceiverIdentifier = receiverIdentifier;
        this.isGroup = isGroup;
        this.mBindRecord = new BindRecord(context,this);
        this.mInflate = LayoutInflater.from(context).inflate(R.layout.chat_message_popup_window, null);
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
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
        return onScrollToPosition;
    }

    public void setOnScrollToPosition(OnScrollToPosition onScrollToPosition) {
        this.onScrollToPosition = onScrollToPosition;
    }

    public String getReceiverIdentifier() {
        return mReceiverIdentifier;
    }

    public void setReceiverIdentifier(String mReceiverIdentifier) {
        this.mReceiverIdentifier = mReceiverIdentifier;
    }

    public View getInflate() {
        return mInflate;
    }

    @NonNull
    @Override
    public ChatRoomMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.listview_chat_room, parent, false);
        holder =  new ChatRoomMessageViewHolder(inflate);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomMessageViewHolder holder, int position) {
        mTopPosition = position;
        ChatRecordEntity bean = messageBeanList.get(position);
        String hexString = Integer.toHexString(holder.hashCode());
        if (mHasCode.contains(hexString)){
            holder.hiddenAll();
            onBindView(holder,bean);
        }else {
            mHasCode.add(hexString);
            onBindView(holder,bean);
        }
    }

    private void onBindView(ChatRoomMessageViewHolder holder,ChatRecordEntity bean){
        if (bean.getType() == ChatRoomConfig.RECORD_TIME){
            holder.getTime().setText(RoomUtils.millToFullTime(bean.getTime()));
            holder.getTime().setVisibility(View.VISIBLE);
            return;
        }
        //显示头像
        switch (bean.getIsReceive()){
            case ChatRoomConfig.RECEIVE_IMAGE:
            case ChatRoomConfig.RECEIVE_MESSAGE:
            case ChatRoomConfig.RECEIVE_VIDEO:
            case ChatRoomConfig.RECEIVE_VOICE:
            case ChatRoomConfig.RECEIVE_FILE:
            case ChatRoomConfig.RECEIVE_VIDEO_CALL:
            case ChatRoomConfig.RECEIVE_VOICE_CALL:
            case ChatRoomConfig.RECEIVE_AT_MESSAGE:
            case ChatRoomConfig.RECEIVE_REPLAY_MESSAGE:
                bindAvatar(holder,bean,true);
                break;
            case ChatRoomConfig.SEND_IMAGE:
            case ChatRoomConfig.SEND_MESSAGE:
            case ChatRoomConfig.SEND_VIDEO:
            case ChatRoomConfig.SEND_VOICE:
            case ChatRoomConfig.SEND_FILE:
            case ChatRoomConfig.SEND_VIDEO_CALL:
            case ChatRoomConfig.SEND_VOICE_CALL:
            case ChatRoomConfig.SEND_AT_MESSAGE:
            case ChatRoomConfig.SEND_REPLAY_MESSAGE:
                bindAvatar(holder,bean,false);
                break;
        }
        //显示内容
        switch (bean.getType()){
            case ChatRoomConfig.RECORD_TEXT:
                bindText(holder, bean);
                break;
            case ChatRoomConfig.RECORD_NOTIFY_MESSAGE:
                bindAtText(holder,bean);
                break;
            case ChatRoomConfig.RECORD_VOICE:
                bindVoice(holder, bean);
                break;
            case ChatRoomConfig.RECORD_IMAGE:
                bindImage(holder,bean);
                break;
            case ChatRoomConfig.RECORD_VIDEO:
                bindVideo(holder,bean);
                break;
            case ChatRoomConfig.RECORD_FILE:
                bindFile(holder,bean);
                break;
            case ChatRoomConfig.RECORD_CALL:
                bindCall(holder,bean);
                break;
            default:
                TLog.d(TAG, "onBindViewHolder: " + bean.getType());
                break;
        }
    }

    //加载视屏的预览图
    private void bindVideo(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        mBindRecord.bindVideo(holder,bean);
    }

    //加载文件
    private void bindFile(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        mBindRecord.bindFile(holder,bean);
    }

    //加载图片
    private void bindImage(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        mBindRecord.bindImage(holder,bean);
    }

    //加载语音
    private void bindVoice(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        mBindRecord.bindVoice(holder,bean);
    }

    //加载文本内容
    private void bindText(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        mBindRecord.bindText(holder,bean);
    }

    private void bindAtText(ChatRoomMessageViewHolder holder, ChatRecordEntity bean){
        mBindRecord.bindAtText(holder,bean);
    }

    private void bindAvatar(ChatRoomMessageViewHolder holder, ChatRecordEntity bean,  boolean sender){
        mBindRecord.bindAvatar(holder,bean,sender,mMineAvatarPath);
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

    private void bindCall(ChatRoomMessageViewHolder holder, ChatRecordEntity bean){
        mBindRecord.bindCall(holder,bean);
    }

    @Override
    public int getItemCount() {
        return messageBeanList.size();
    }

    public void add(ChatRecordEntity chatRecordEntity,boolean receive){
        if (chatRecordEntity == null) {
            return;
        }
        if (messageBeanList.size() > 0 && System.currentTimeMillis() - messageBeanList.get(messageBeanList.size() - 1).getTime() > 2*60*1000 && messageBeanList.get(messageBeanList.size() - 1).getType() != ChatRoomConfig.RECORD_TIME){
            ChatRecordEntity temp = messageBeanList.get(messageBeanList.size() - 1);
            ChatRecordEntity recordTime = generatorTimeRecord(mReceiverIdentifier,temp.getTime());
            messageBeanList.add(recordTime);
            addRecordToList(chatRecordEntity,recordTime,receive);
            return;
        }

        //填充之前没有加载的图片/视频路径
        if (receive){
            for (int i  = messageBeanList.size() - 1; i > messageBeanList.size() - 11 && i >= 0; i--){
                ChatRecordEntity recordEntity = messageBeanList.get(i);
                if ((recordEntity.getType() == ChatRoomConfig.RECORD_VIDEO || recordEntity.getType() == ChatRoomConfig.RECORD_IMAGE || recordEntity.getType() == ChatRoomConfig.RECORD_VOICE) && recordEntity.getContent().equals(chatRecordEntity.getContent())){
                    if (TextUtils.isEmpty(recordEntity.getPath())){
                        //收到文件接收失败的通知
                        if (!chatRecordEntity.isReceiveSuccess()){
                            recordEntity.setReceiveSuccess(false);
                        }else {
                            //收到文件接收成功的通知
                            recordEntity.setPath(chatRecordEntity.getPath());
                            recordEntity.setFileName(chatRecordEntity.getFileName());
                        }
                        notifyDataSetChanged();
                        return;
                    }
                }
            }
        }

        addRecordToList(chatRecordEntity,null,receive);
    }

    public void add(ChatRecordEntity chatRecordEntity) {
        add(chatRecordEntity,false);
    }

    public static ChatRecordEntity generatorTimeRecord(String identifier,long time){
        ChatRecordEntity recordTime = new ChatRecordEntity();
        recordTime.setTime(time);
        recordTime.setType(ChatRoomConfig.RECORD_TIME);
        recordTime.setReceiver(identifier);
        return recordTime;
    }

    private void addRecordToList(ChatRecordEntity chatRecordEntity,ChatRecordEntity recordTime,boolean receive){
        messageBeanList.add(chatRecordEntity);
        this.notifyItemInserted(messageBeanList.size());
        onScrollToPosition.onLoadViewOver(messageBeanList.size());
        if (!receive){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (recordTime != null){
                        MyDataBase.getInstance().getChatRecordDao().insert(recordTime);
                    }
                    MyDataBase.getInstance().getChatRecordDao().insert(chatRecordEntity);
                    TLog.d(TAG,"chatRecordEntity " + chatRecordEntity);
                }
            }).start();
        }
    }

    public void addAll(List<ChatRecordEntity> chatHistory) {
        int startPosition = 0;
        messageBeanList.addAll(0,chatHistory);
        this.notifyItemRangeInserted(startPosition,chatHistory.size());

        if (mFirstLoadHistory){
            onScrollToPosition.onLoadViewOver(messageBeanList.size());
            mFirstLoadHistory = false;
            return;
        }
    }

    public static String createVideoThumbnailFile(String path) {
        TLog.d(TAG, "createVideoThumbnailFile: onReceiveAndSaveFile 1 path = " + path);
        Bitmap bitmap = createVideoThumbnail(path);
        File parentFile = SendFile.getFile(ChatRoomConfig.PATH_VIDEO_FIRST_FRAME);
        if (!parentFile.exists()){
            parentFile.mkdirs();
        }
        TLog.d(TAG, "createVideoThumbnailFile: onReceiveAndSaveFile 2 path = " + path);
        String name = new File(path).getName();
        name = name.substring(0,name.lastIndexOf("."));
        File firstFrameFile = new File(parentFile.getPath(),"first_frame" + name + ".jpg");
        try {
            firstFrameFile.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(firstFrameFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return firstFrameFile.getPath();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    //获得视屏文件的第一帧图
    private static Bitmap createVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();
        } catch (IllegalArgumentException e) {
            // Assume this is a corrupt video file
            e.printStackTrace();
        } catch (RuntimeException e) {
            // Assume this is a corrupt video file.
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                // Ignore failures while cleaning up.
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    public void setPopupRecyclerView(RecyclerView recyclerView) {
        this.mPopupRecyclerView = recyclerView;
    }

    public RecyclerView getPopupRecyclerView() {
        return this.mPopupRecyclerView;
    }

    /**
     * 删除聊天记录*/
    public void deleteChatRecord(int position){
        int finalPosition = position;

        ChatRecordEntity[] deleteEntities = new ChatRecordEntity[2];    //被删除的两条记录
        boolean common = true;      //普通情况下只删除选中的那条记录
        if (position != 0 &&      //不是第一条记录
                messageBeanList.get(position-1).getType() == ChatRoomConfig.RECORD_TIME &&    //上一条记录是时间
                (messageBeanList.size()-1 == position ||      //最后一条
                        messageBeanList.get(position+1).getType() == ChatRoomConfig.RECORD_TIME)){        //下一条记录是时间
            common = false;
            position --;
        }else if (position == 0) {    //第一条记录,第二条记录是时间
            common = !(messageBeanList.size() > 1 && messageBeanList.get(1).getType() == ChatRoomConfig.RECORD_TIME);
        }
        //如果满足上诉条件任意一条，deleteEntities[0]为时间记录，deleteEntities[1]为聊天记录
        //反之，deleteEntities[0]为当前选中的记录
        deleteEntities[0] = messageBeanList.get(position);

        if (!common){
            //deleteEntities[1]为聊天记录，deleteEntities[0]为时间记录
            deleteEntities[1] = messageBeanList.get(position+1);
            messageBeanList.remove(position+1);   //从列表中移除position+1指向的记录
        }

        messageBeanList.remove(position);     //从列表中移除position指向的记录
        notifyDataSetChanged();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatRecordDao chatRecordDao = MyDataBase.getInstance().getChatRecordDao();

                if (finalPosition == 0){     //长按第一条记录，判断显示的第一条记录在数据库的上一条记录是否是时间记录
                    ChatRecordEntity recordBeforeTime = chatRecordDao.getLatestRecordBeforeTime(deleteEntities[0].getReceiver(), deleteEntities[0].getTime(), deleteEntities[0].getId());
                    if (null != recordBeforeTime && recordBeforeTime.getType() == ChatRoomConfig.RECORD_TIME){
                        chatRecordDao.delete(recordBeforeTime);
                    }
                }

                if (null != deleteEntities[1]){     //删除两条记录
                    chatRecordDao.delete(deleteEntities);
                }else {
                    //只删除选中的长按记录
                    chatRecordDao.delete(deleteEntities[0]);
                }
            }
        }).start();
    }

//    /**
//     * 回复消息*/
//    public void replayChatRecord(){
//        if (null != mLongClickHolder && null != mOnClickReplayOrNotify){
//            mOnClickReplayOrNotify.onClickReplay(messageBeanList.get(mLongClickPosition),mLongClickHolder.getSenderName().getText().toString());
//        }
//    }
}
