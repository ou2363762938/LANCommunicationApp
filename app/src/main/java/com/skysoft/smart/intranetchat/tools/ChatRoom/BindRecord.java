package com.skysoft.smart.intranetchat.tools.ChatRoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.model.camera.manager.MyMediaPlayerManager;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.CreateNotifyBitmap;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.ui.activity.camera.PictureShowActivity;
import com.skysoft.smart.intranetchat.ui.activity.camera.VideoPlayActivity;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomMessageAdapter;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomMessageViewHolder;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.OnLongClickRecord;
import com.skysoft.smart.intranetchat.ui.activity.userinfoshow.UserInfoShowActivity;

import static com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity.sIsAudioRecording;

public class BindRecord {
    private static final String TAG = "BindRecord";
    private Context mContext;
    private ChatRoomMessageAdapter mChatRoomAdapter;

    public BindRecord(Context mmContext, ChatRoomMessageAdapter adapter) {
        this.mContext = mmContext;
        this.mChatRoomAdapter = adapter;
    }
    
    //加载视屏的预览图
    public void bindVideo(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        ImageView thumbnail = null;
        switch (bean.getIsReceive()){
            case ChatRoomConfig.RECEIVE_VIDEO:
                thumbnail = holder.getSenderVideoThumbnail();
                break;
            case ChatRoomConfig.SEND_VIDEO:
                thumbnail = holder.getMineVideoThumbnail();
                break;
        }
        if (TextUtils.isEmpty(bean.getPath())){
            //视频预览图路径为空时判断正在下载或者接收已经失败
            onLoading(thumbnail,bean);
        }else {
            Glide.with(mContext).load(bean.getPath()).into(new ImageViewTarget<Drawable>(thumbnail) {
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
                                VideoPlayActivity.goActivity(mContext, bean.getFileName());
                            }
                        }
                    }).start();
                }
            }
        });

        thumbnail.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder,mChatRoomAdapter));
    }

    //加载文件
    public void bindFile(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
        ConstraintLayout file = null;
        String size = computingFileSize(bean.getLength());
        TLog.d(TAG, "bindFile: size = " + size);
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

//        file.setOnLongClickListener(new OnLongClickRecord(bean, holder));
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

    //加载图片
    public void bindImage(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
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
            TLog.d(TAG, "bindImage: path = null");
            //图片路径为空时判断正在下载或者接收已经失败
            onLoading(image,bean);
            return;
        }
        Glide.with(mContext).load(bean.getPath()).into(image);
        image.setVisibility(View.VISIBLE);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QuickClickListener.isFastClick()) {
                    String path = bean.getPath();
                    PictureShowActivity.goActivity(mContext, path);
                }
            }
        });

        image.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }

    /**
     * 未接收完图片和视频前显示正在加载*/
    public void onLoading(ImageView image,ChatRecordEntity bean){
        if (bean.isReceiveSuccess()){
            FileEntity fileEntity = IntranetChatApplication.getMonitorReceiveFile().get(bean.getContent());
            if (fileEntity != null && fileEntity.getType() != Config.STEP_DOWN_LOAD_FAILURE && System.currentTimeMillis() - fileEntity.getTime() > 10*60*100){
                Glide.with(mContext).load(R.drawable.ic_file_damage_fill).into(image);
                bean.setReceiveSuccess(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyDataBase.getInstance().getChatRecordDao().update(bean);
                    }
                }).start();
            }else {
                Glide.with(mContext).load(R.color.imageRecordBackgroundColor).into(new ImageViewTarget<Drawable>(image) {
                    @Override
                    protected void setResource(Drawable resource) {
                        this.view.setBackground(resource);
                        this.view.setBackgroundResource(R.drawable.ic_loading);
                    }
                });
            }
        }else {
            Glide.with(mContext).load(R.drawable.ic_file_damage_fill).into(image);
        }
    }

    //加载语音
    public void bindVoice(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
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

        voice.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }

    //当点击语音框时，播放语音
    public void onClickVoice(String path) {
        if (sIsAudioRecording){
            return;
        }
        MyMediaPlayerManager.getsInstance().play(path);
//        1572865304716
    }

    //加载文本内容
    public void bindText(ChatRoomMessageViewHolder holder, ChatRecordEntity bean) {
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

        message.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }

    public void bindAtText(ChatRoomMessageViewHolder holder, ChatRecordEntity bean){
        TextView message = null;
        if (bean.getIsReceive() == ChatRoomConfig.RECEIVE_AT_MESSAGE){
            holder.getSenderMessage().setText(bean.getContent());
            holder.getSenderMessage().setVisibility(View.VISIBLE);
            message = holder.getSenderMessage();

            String at = bean.getFileName();
            buildNotify(at,holder.getSenderMessage());
        }else {
            holder.getMineMessage().setText(bean.getContent());
            holder.getMineMessage().setVisibility(View.VISIBLE);
            message = holder.getMineMessage();
        }

        message.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }

    /**
     * 根据唯一标识符寻找本地名字替代@后的名字
     * @param at @队列
     * @param message */
    public void buildNotify(String at,TextView message){
        if (!TextUtils.isEmpty(at)){
            Editable editableText = message.getEditableText();

            String[] array = (String[]) GsonTools.formJson(at, String[].class);
            for (int i = array.length-1; i >= 0; i--){
                String[] split = array[i].split("\\|");

                int st = Integer.parseInt(split[1]);        //at开始位置
                int length = Integer.parseInt(split[2]);    //用户名长度

                ContactEntity entity = IntranetChatApplication.sContactMap.get(split[0]);
                if (null != entity){
                    editableText.replace(st+1,st+length+1,entity.getName());
                }else if (split[0].equals(IntranetChatApplication.getsMineUserInfo().getIdentifier())){
                    String name = IntranetChatApplication.getsMineUserInfo().getName();
                    SpannableStringBuilder spannableString = new SpannableStringBuilder("@"+name);    //构建SpannableStringBuilder
                    Bitmap bitmap = CreateNotifyBitmap.notifyBitmap(mContext,"@"+name);      //构建内容为notify的bitmap
                    ImageSpan imageSpan = new ImageSpan(mContext,bitmap);      //构建内容为bitmap的ImageSpan
                    spannableString.setSpan(imageSpan,
                            0,name.length()+1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);        //SpannableStringBuilder添加ImageSpan

                    editableText.replace(st,st+length+1,spannableString);
                }
            }

            message.setText(editableText);
        }
    }

    public void bindAvatar(ChatRoomMessageViewHolder holder, ChatRecordEntity bean,  boolean sender, String avatarPath){
        if (sender){
            ContactEntity next = IntranetChatApplication.sContactMap.get(bean.getSender());
            if (null == next){
                throw new NullPointerException("can not found this contact");
            }
            //填充头像
            if (!TextUtils.isEmpty(next.getAvatarPath())){
                Glide.with(mContext).load(next.getAvatarPath()).into(holder.getSenderAvatar());
            }else {
                Glide.with(mContext).load(R.drawable.default_head).into(holder.getSenderAvatar());
            }
            //填充用户名
            if (!TextUtils.isEmpty(next.getName())){
                holder.getSenderName().setText(next.getName());
            }
            holder.getSenderName().setVisibility(View.VISIBLE);
            holder.getSenderAvatar().setVisibility(View.VISIBLE);
            //单击事件
            holder.getSenderAvatar().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (QuickClickListener.isFastClick()){       //防止长按后立即触发单击事件
                        UserInfoShowActivity.go(mContext,next.getName(),next.getAvatarPath(),bean.getSender());
                    }
                }
            });

            //长按头像显示@*****
//            holder.getSenderAvatar().setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    if (null != mOnClickReplayOrNotify){
//                        //在输入框中显示@***
//                        mOnClickReplayOrNotify.onClickNotify(bean,holder.getSenderName().getText().toString());
//                    }
//                    isUpLongClickAvatar = false;        //正在长按avatar，avatar的单击事件不能触发
//                    v.setOnTouchListener(ChatRoomMessageAdapter.this::onTouch);     //监听松开avatar
//                    return false;
//                }
//            });
        }else {
            if (!TextUtils.isEmpty(avatarPath)){
                Glide.with(mContext).load(avatarPath).into(holder.getMineAvatar());
            }else {
                Glide.with(mContext).load(R.drawable.default_head).into(holder.getMineAvatar());
            }
            holder.getMineAvatar().setVisibility(View.VISIBLE);
        }
    }

    public void bindCall(ChatRoomMessageViewHolder holder, ChatRecordEntity bean){
        LinearLayout callBox = null;
        switch (bean.getIsReceive()){
            case ChatRoomConfig.RECEIVE_VOICE_CALL:
                holder.getSenderCallBox().setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.ic_voice_call).into(holder.getSenderCallImage());
                holder.getSenderCallText().setText(bean.getContent());
                break;
            case ChatRoomConfig.RECEIVE_VIDEO_CALL:
                holder.getSenderCallBox().setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.ic_video_call).into(holder.getSenderCallImage());
                holder.getSenderCallText().setText(bean.getContent());
                break;
            case ChatRoomConfig.SEND_VOICE_CALL:
                holder.getMineCallBox().setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.ic_voice_call).into(holder.getMineCallImage());
                holder.getMineCallText().setText(bean.getContent());
                break;
            case ChatRoomConfig.SEND_VIDEO_CALL:
                holder.getMineCallBox().setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.ic_video_call).into(holder.getMineCallImage());
                holder.getMineCallText().setText(bean.getContent());
                break;
        }
        if (bean.getLength() == -1){
            holder.getSenderCallImage().setColorFilter(R.color.color_gray_dark);
            holder.getSenderCallText().setTextColor(mContext.getResources().getColor(R.color.color_gray_dark));
        }
        if (bean.getIsReceive() == ChatRoomConfig.RECEIVE_VOICE_CALL || bean.getIsReceive() == ChatRoomConfig.RECEIVE_VIDEO_CALL){
            callBox = holder.getSenderCallBox();
        }else {
            callBox = holder.getMineCallBox();
        }

        callBox.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }
}
