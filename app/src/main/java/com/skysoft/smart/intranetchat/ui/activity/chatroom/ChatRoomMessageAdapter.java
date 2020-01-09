/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/30
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.ui.activity.chatroom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import com.skysoft.smart.intranetchat.app.SendFile;
import com.skysoft.smart.intranetchat.network.Config;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;
import com.skysoft.smart.intranetchat.ui.activity.camera.PictureShowActivity;
import com.skysoft.smart.intranetchat.ui.activity.camera.VideoPlayActivity;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.GroupMembersBean;
import com.skysoft.smart.intranetchat.camera.manager.MyMediaPlayerManager;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.network.bean.ReceiveAndSaveFileBean;
import com.skysoft.smart.intranetchat.ui.activity.userinfoshow.UserInfoShowActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoomActivity.sIsAudioRecording;

public class ChatRoomMessageAdapter extends RecyclerView.Adapter<ChatRoomMessageViewHolder>{
    private static String TAG = ChatRoomMessageAdapter.class.getSimpleName();
    private Context context;
    private List<ChatRecordEntity> messageBeanList = new ArrayList<>();
    private Map<String, GroupMembersBean> avatars = new HashMap<>();
    private String mineAvatarPath;
    private OnScrollToPosition onScrollToPosition;
    private ChatRoomMessageViewHolder holder;
    private String mReceiverIdentifier;
    private int mTopPosition;
    private boolean mFirstLoadHistory = true;
    private View mInflate;
    private RecyclerView mPopupRecyclerView;
    private ArrayList<String> mHasCode = new ArrayList<>();
    private Map<String ,ChatRoomMessageViewHolder> mVideoHolder = new HashMap<>();

    public int getTopPosition() {
        return mTopPosition;
    }

    public void setTopPosition(int mTopPosition) {
        this.mTopPosition = mTopPosition;
    }

    public Map<String, GroupMembersBean> getAvatars() {
        return avatars;
    }

    public void setAvatars(String sender, GroupMembersBean bean) {
        this.avatars.put(sender, bean);
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

    public ChatRoomMessageAdapter(Context context, String mineAvatarPath, String recieverIdentifier) {
        this.context = context;
        this.mineAvatarPath = mineAvatarPath;
        this.mReceiverIdentifier = recieverIdentifier;
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
            holder.getTime().setText(ChatRoomActivity.millToFullTime(bean.getTime()));
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
                bindAvatar(holder,bean,true);
                break;
            case ChatRoomConfig.SEND_IMAGE:
            case ChatRoomConfig.SEND_MESSAGE:
            case ChatRoomConfig.SEND_VIDEO:
            case ChatRoomConfig.SEND_VOICE:
            case ChatRoomConfig.SEND_FILE:
            case ChatRoomConfig.SEND_VIDEO_CALL:
            case ChatRoomConfig.SEND_VOICE_CALL:
                bindAvatar(holder,bean,false);
                break;
        }
        //显示内容
        switch (bean.getType()){
            case ChatRoomConfig.RECORD_TEXT:
                bindText(holder, bean);
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
                Log.d(TAG, "onBindViewHolder: " + bean.getType());
                break;
        }
    }

    //加载视屏的预览图
    private void bindVideo(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        ImageView thumbnail = null;
        switch (bean.getIsReceive()){
            case ChatRoomConfig.RECEIVE_VIDEO:
                thumbnail = holder.getSenderVideoThumbnail();
                if (TextUtils.isEmpty(bean.getPath())){
                    mVideoHolder.put(bean.getContent(),holder);
                }
                break;
            case ChatRoomConfig.SEND_VIDEO:
                thumbnail = holder.getMineVideoThumbnail();
                break;
        }
        if (TextUtils.isEmpty(bean.getPath())){
            //视频预览图路径为空时判断正在下载或者接收已经失败
            onLoading(thumbnail,bean);
        }else {
            Glide.with(context).load(bean.getPath()).into(new ImageViewTarget<Drawable>(thumbnail) {
                @Override
                protected void setResource(Drawable resource) {
                    this.view.setBackground(resource);
                    this.view.setImageResource(R.drawable.ic_play_video);
                }
            });
        }
        thumbnail.setVisibility(View.VISIBLE);
        thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QuickClickListener.isFastClick()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!TextUtils.isEmpty(bean.getFileName())){
                                VideoPlayActivity.goActivity(context, bean.getFileName());
                            }
                        }
                    }).start();
                }
            }
        });

        thumbnail.setOnLongClickListener(new OnLongClickRecord(bean, holder));
    }

    //加载文件
    private void bindFile(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        ConstraintLayout file = null;
        String size = computingFileSize(bean.getLength());
        Log.d(TAG, "bindFile: size = " + size);
        switch (bean.getIsReceive()){
            case ChatRoomConfig.RECEIVE_FILE:
                holder.getSenderFileName().setText(bean.getFileName());
                holder.getSenderFileSize().setText(size);
                holder.getSenderFile().setVisibility(View.VISIBLE);
                file = holder.getSenderFile();
                break;
            case ChatRoomConfig.SEND_FILE:
                holder.getMineFileName().setText(bean.getFileName());
                holder.getMineFileSize().setText(size);
                holder.getMineFile().setVisibility(View.VISIBLE);
                file = holder.getMineFile();
                break;
        }

        file.setOnLongClickListener(new OnLongClickRecord(bean, holder));
    }

    //加载图片
    private void bindImage(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        ImageView image = null;
        switch (bean.getIsReceive()){
            case ChatRoomConfig.RECEIVE_IMAGE:
                image = holder.getSenderImage();
                break;
            case ChatRoomConfig.SEND_IMAGE:
                image = holder.getMineImage();
                break;
        }
        if (TextUtils.isEmpty(bean.getPath())){
            Log.d(TAG, "bindImage: path = null");
            //图片路径为空时判断正在下载或者接收已经失败
            onLoading(image,bean);
            return;
        }
        Glide.with(context).load(bean.getPath()).into(image);
        image.setVisibility(View.VISIBLE);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QuickClickListener.isFastClick()) {
                    String path = bean.getPath();
                    PictureShowActivity.goActivity(context, path);
                }
            }
        });

        image.setOnLongClickListener(new OnLongClickRecord(bean, holder));
    }

    /**
     * 未接收完图片和视频前显示正在加载*/
    public void onLoading(ImageView image,ChatRecordEntity bean){
        if (bean.isReceiveSuccess()){
            FileEntity fileEntity = IntranetChatApplication.getMonitorReceiveFile().get(bean.getContent());
            if (fileEntity != null && fileEntity.getType() != Config.STEP_DOWN_LOAD_FAILURE && System.currentTimeMillis() - fileEntity.getTime() > 10*60*100){
                Glide.with(context).load(R.drawable.ic_file_damage_fill).into(image);
                bean.setReceiveSuccess(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyDataBase.getInstance().getChatRecordDao().update(bean);
                    }
                }).start();
            }else {
                Glide.with(context).load(R.color.color_gray_dark).into(new ImageViewTarget<Drawable>(image) {
                    @Override
                    protected void setResource(Drawable resource) {
                        this.view.setBackground(resource);
                        this.view.setBackgroundResource(R.drawable.ic_loading);
                    }
                });
            }
        }else {
            Glide.with(context).load(R.drawable.ic_file_damage_fill).into(image);
        }
    }

    //加载语音
    private void bindVoice(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        LinearLayout voice = null;
        TextView voiceLength = null;
        switch (bean.getIsReceive()) {
            case ChatRoomConfig.RECEIVE_VOICE:
                holder.getSenderVoice().setVisibility(View.VISIBLE);
                voiceLength = holder.getSenderVoiceLength();
                voice = holder.getSenderVoice();
                break;
            case ChatRoomConfig.SEND_VOICE:
                voiceLength = holder.getMineVoiceLength();
                voice = holder.getMineVoice();
                break;
            default:
                bean.getIsReceive();
                break;
        }

        voiceLength.setText(String.valueOf(bean.getLength()));
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) voiceLength.getLayoutParams();
        double percent = (double) bean.getLength() / ChatRoomConfig.MAX_VOICE_LENGTH;
        int width = (int) (percent * ChatRoomConfig.MAX_VOICE_TEXT_VIEW_LENGTH) + ChatRoomConfig.BASE_VOICE_LENGTH;
        layoutParams.width = width;
        voiceLength.setLayoutParams(layoutParams);
        voiceLength.setText(String.valueOf(bean.getLength()));
        voice.setVisibility(View.VISIBLE);
        voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QuickClickListener.isFastClick()) {
                    if (bean != null && !TextUtils.isEmpty(bean.getPath())){
                        onClickVoice(bean.getPath());
                    }
                }
            }
        });

        voice.setOnLongClickListener(new OnLongClickRecord(bean, holder));;
    }

    //加载文本内容
    private void bindText(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        TextView message = null;
        if (bean.getIsReceive() == ChatRoomConfig.RECEIVE_MESSAGE){
            holder.getSenderMessage().setText(bean.getContent());
            holder.getSenderMessage().setVisibility(View.VISIBLE);
            message = holder.getSenderMessage();
        }else {
            holder.getMineMessage().setText(bean.getContent());
            holder.getMineMessage().setVisibility(View.VISIBLE);
            message = holder.getMineMessage();
        }

        message.setOnLongClickListener(new OnLongClickRecord(bean, holder));
    }

    private void bindAvatar(ChatRoomMessageViewHolder holder, ChatRecordEntity bean,  boolean sender){
        if (sender){
            GroupMembersBean membersBean = avatars.get(bean.getSender());
            if (membersBean == null){
                Iterator<ContactEntity> iterator = IntranetChatApplication.getsContactList().iterator();
                while (iterator.hasNext()){
                    ContactEntity next = iterator.next();
                    if (next.getIdentifier().equals(bean.getSender())){
                        membersBean = new GroupMembersBean();
                        membersBean.setmMemberAvatarPath(next.getAvatarPath());
                        membersBean.setmMemberName(next.getName());
                        avatars.put(bean.getSender(),membersBean);
                        holder.getSenderName().setText(next.getName());
                    }
                }
            }
            if(membersBean == null){
                membersBean = new GroupMembersBean();
                membersBean.setmMemberName("not found contact");
            }
            if (!TextUtils.isEmpty(membersBean.getmMemberAvatarPath())){
                Glide.with(context).load(membersBean.getmMemberAvatarPath()).into(holder.getSenderAvatar());
            }else {
                Glide.with(context).load(R.drawable.default_head).into(holder.getSenderAvatar());
            }
            if (!TextUtils.isEmpty(membersBean.getmMemberName())){
                holder.getSenderName().setText(membersBean.getmMemberName());
            }
            holder.getSenderName().setVisibility(View.VISIBLE);
            holder.getSenderAvatar().setVisibility(View.VISIBLE);
            final GroupMembersBean temp = membersBean;
            holder.getSenderAvatar().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UserInfoShowActivity.go(context,temp.getmMemberName(),temp.getmMemberAvatarPath(),bean.getSender());
                }
            });
        }else {
            if (!TextUtils.isEmpty(mineAvatarPath)){
                Glide.with(context).load(mineAvatarPath).into(holder.getMineAvatar());
            }else {
                Glide.with(context).load(R.drawable.default_head).into(holder.getMineAvatar());
            }
            holder.getMineAvatar().setVisibility(View.VISIBLE);
        }
    }

    private void bindCall(ChatRoomMessageViewHolder holder, ChatRecordEntity bean){
        LinearLayout callBox = null;
        switch (bean.getIsReceive()){
            case ChatRoomConfig.RECEIVE_VOICE_CALL:
                holder.getSenderCallBox().setVisibility(View.VISIBLE);
                Glide.with(context).load(R.drawable.ic_voice_call).into(holder.getSenderCallImage());
                holder.getSenderCallText().setText(bean.getContent());
                break;
            case ChatRoomConfig.RECEIVE_VIDEO_CALL:
                holder.getSenderCallBox().setVisibility(View.VISIBLE);
                Glide.with(context).load(R.drawable.ic_video_call).into(holder.getSenderCallImage());
                holder.getSenderCallText().setText(bean.getContent());
                break;
            case ChatRoomConfig.SEND_VOICE_CALL:
                holder.getMineCallBox().setVisibility(View.VISIBLE);
                Glide.with(context).load(R.drawable.ic_voice_call).into(holder.getMineCallImage());
                holder.getMineCallText().setText(bean.getContent());
                break;
            case ChatRoomConfig.SEND_VIDEO_CALL:
                holder.getMineCallBox().setVisibility(View.VISIBLE);
                Glide.with(context).load(R.drawable.ic_video_call).into(holder.getMineCallImage());
                holder.getMineCallText().setText(bean.getContent());
                break;
        }
        if (bean.getLength() == -1){
            holder.getSenderCallImage().setColorFilter(R.color.color_gray_dark);
            holder.getSenderCallText().setTextColor(context.getResources().getColor(R.color.color_gray_dark));
        }
        if (bean.getIsReceive() == ChatRoomConfig.RECEIVE_VOICE_CALL || bean.getIsReceive() == ChatRoomConfig.RECEIVE_VIDEO_CALL){
            callBox = holder.getSenderCallBox();
        }else {
            callBox = holder.getMineCallBox();
        }

        callBox.setOnLongClickListener(new OnLongClickRecord(bean, holder));;
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
            Log.d(TAG, "add: recordTime 4 timeString = " + ChatRoomActivity.millToFullTime(recordTime.getTime()) + ", entity = " + chatRecordEntity.toString());
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

    //当点击语音框时，播放语音
    public void onClickVoice(String path) {
        if (sIsAudioRecording){
            return;
        }
        MyMediaPlayerManager.getsInstance().play(path);
//        1572865304716
    }

    //收到文件后，将文件加载到列表中
    public void onReceiveAndSaveFile(ReceiveAndSaveFileBean receiveAndSaveFileBean) {
        Log.d(TAG, "onReceiveAndSaveFile: " + receiveAndSaveFileBean.toString());
        Iterator<ChatRecordEntity> iterator = messageBeanList.iterator();
        int i = 0;
        while (iterator.hasNext()){
            ChatRecordEntity next = iterator.next();
            if (!TextUtils.isEmpty(next.getContent()) && next.getContent().equals(receiveAndSaveFileBean.getIdentifier())){
                next.setPath(receiveAndSaveFileBean.getPath());
                //加载图片
                if (next.getType() == ChatRoomConfig.RECORD_IMAGE) {
                    onBindViewHolder(holder, i);
                }

                //加载视频
                if (next.getType() == ChatRoomConfig.RECORD_VIDEO){
                    ChatRoomMessageViewHolder videoHolder = mVideoHolder.get(next.getContent());
                    onBindViewHolder(holder,i);
                    mVideoHolder.remove(next.getContent());
                    return;
                }
            }
            i++;
        }
    }

    public static String createVideoThumbnailFile(String path) {
        Log.d(TAG, "createVideoThumbnailFile: onReceiveAndSaveFile 1 path = " + path);
        Bitmap bitmap = createVideoThumbnail(path);
        File parentFile = SendFile.getFile(ChatRoomConfig.PATH_VIDEO_FIRST_FRAME);
        if (!parentFile.exists()){
            parentFile.mkdirs();
        }
        Log.d(TAG, "createVideoThumbnailFile: onReceiveAndSaveFile 2 path = " + path);
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

    private String computingFileSize(long length){
        StringBuilder sb = new StringBuilder();
        String[] unit = {"B","K","M","G","P"};
        int level = 0;
        int value = 1024;
        while(length > value){
            value *= 1024;
            level++;
        }
        int m = (int) (length / (value / 1024));
        if (m > 700){
            sb.append(0);
            sb.append('.');
            sb.append(m/10);
            sb.append(unit[level + 1]);
        }else {
            sb.append(m);
            sb.append(unit[level]);
        }
        return sb.toString();
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

    private static int makeDropDownMeasureSpec(int measureSpec){
        int mode = 0;
        if (measureSpec == ViewGroup.LayoutParams.WRAP_CONTENT){
            mode = View.MeasureSpec.UNSPECIFIED;
        }else {
            mode = View.MeasureSpec.EXACTLY;
        }
        return View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(measureSpec),mode);
    }

    public void showPopupMenu(View view, ChatRecordEntity chatRecordEntity, int relativeY, int nameHeight){
        if (null == mInflate){
            mInflate = LayoutInflater.from(context).inflate(R.layout.chat_message_popup_window, null);
        }

        PopupWindow popupWindow = new PopupWindow(mInflate,ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        if (null == mPopupRecyclerView){
            mPopupRecyclerView = mInflate.findViewById(R.id.chat_message_popup_window);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false);
            mPopupRecyclerView.setLayoutManager(linearLayoutManager);
            mPopupRecyclerView.addItemDecoration(new DividerItemDecoration(context,DividerItemDecoration.HORIZONTAL));
        }

        PopupWindowAdapter adapter = new PopupWindowAdapter(context,chatRecordEntity,popupWindow);
        mPopupRecyclerView.setAdapter(adapter);

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);

        View contentView = popupWindow.getContentView();
        contentView.measure(makeDropDownMeasureSpec(popupWindow.getWidth()),
                makeDropDownMeasureSpec(popupWindow.getHeight()));

        int offsetX = 0;
        int offsetY = -(contentView.getMeasuredHeight() + view.getHeight());
        if (chatRecordEntity.getIsReceive() == ChatRoomConfig.SEND_FILE
                || chatRecordEntity.getIsReceive() == ChatRoomConfig.SEND_IMAGE
                || chatRecordEntity.getIsReceive() == ChatRoomConfig.SEND_MESSAGE
                || chatRecordEntity.getIsReceive() == ChatRoomConfig.SEND_VIDEO
                || chatRecordEntity.getIsReceive() == ChatRoomConfig.SEND_VIDEO_CALL
                || chatRecordEntity.getIsReceive() == ChatRoomConfig.SEND_VOICE
                || chatRecordEntity.getIsReceive() == ChatRoomConfig.SEND_VOICE_CALL){
            Log.d(TAG, "showPopupMenu: right");
            offsetX = -(contentView.getMeasuredWidth() - view.getWidth());
        }

        if (relativeY < (-offsetY)){
            offsetY = 0;
        }else {
            offsetY -= nameHeight;
        }

        popupWindow.showAsDropDown(view,offsetX,offsetY,Gravity.LEFT);
    }

    private class OnLongClickRecord implements View.OnLongClickListener{
        private ChatRecordEntity mRecordEntity;
        private ChatRoomMessageViewHolder holder;

        public OnLongClickRecord(ChatRecordEntity mRecordEntity, ChatRoomMessageViewHolder holder) {
            this.mRecordEntity = mRecordEntity;
            this.holder = holder;
        }

        @Override
        public boolean onLongClick(View v) {
            showPopupMenu(v,mRecordEntity,holder.getBox().getTop() + holder.getBox().getScrollX(),
                    holder.getSenderName().getVisibility() == View.VISIBLE ? holder.getSenderName().getHeight() : 0);
            return false;
        }
    }
}
