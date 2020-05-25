package com.skysoft.smart.intranetchat.model.chat.record;

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
import com.skysoft.smart.intranetchat.bean.chat.RecordCallBean;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.camera.manager.MyMediaPlayerManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.CreateNotifyBitmap;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;
import com.skysoft.smart.intranetchat.ui.activity.camera.PictureShowActivity;
import com.skysoft.smart.intranetchat.ui.activity.camera.VideoPlayActivity;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomMessageViewHolder;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.OnLongClickRecord;
import com.skysoft.smart.intranetchat.ui.activity.userinfoshow.UserInfoShowActivity;

import static com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity.sIsAudioRecording;

public class BindRecord {
    private static final String TAG = "BindRecord";
    private Context mContext;
    private RecordAdapter mChatRoomAdapter;

    public BindRecord(Context mmContext, RecordAdapter adapter) {
        this.mContext = mmContext;
        this.mChatRoomAdapter = adapter;
    }

    public void bind(ChatRoomMessageViewHolder holder,
                     RecordEntity bean) {
        switch (bean.getType()) {
            case ChatRoomConfig.RECORD_TEXT:
                bindText(holder,bean);
                break;
            case ChatRoomConfig.RECORD_FILE:
                bindFile(holder,bean);
                break;
            case ChatRoomConfig.RECORD_CALL:
                bindCall(holder,bean);
                break;
                default:
                    break;
        }
    }

    private void bindFile(ChatRoomMessageViewHolder holder, RecordEntity bean) {
        bean.setFileEntity((FileEntity) GsonTools.formJson(bean.getFile(),FileEntity.class));
        switch (bean.getFileEntity().getType()) {
            case Config.FILE_VOICE:
                bindVoice(holder,bean);
                break;
            case Config.FILE_VIDEO:
                bindVideo(holder,bean);
                break;
            case Config.FILE_PICTURE:
                bindImage(holder,bean);
                break;
            case Config.FILE_COMMON:
                bindCommon(holder,bean);
                break;
        }
    }

    //加载视屏的预览图
    private void bindVideo(ChatRoomMessageViewHolder holder, RecordEntity bean) {
        ImageView thumbnail = null;
        if (bean.getSender() == -1) {
            thumbnail = holder.getMineVideoThumbnail();
        } else {
            thumbnail = holder.getSenderVideoThumbnail();
        }

        String thumbnailPath = bean.getFileEntity().getThumbnail();
        if (TextUtils.isEmpty(thumbnailPath)){
            Glide.with(mContext).load(R.drawable.ic_file_damage_fill).into(thumbnail);
        }else {
            Glide.with(mContext).load(thumbnailPath).into(new ImageViewTarget<Drawable>(thumbnail) {
                @Override
                protected void setResource(Drawable resource) {
                    this.view.setBackground(resource);
                    this.view.setImageResource(R.drawable.ic_play_video);
                }
            });

            String path = bean.getFileEntity().getPath();
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (QuickClickListener.isFastClick()) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (!TextUtils.isEmpty(path)){
                                    VideoPlayActivity.goActivity(mContext, path);
                                }
                            }
                        }).start();
                    }
                }
            });
        }
        thumbnail.setVisibility(View.VISIBLE);


        thumbnail.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder,mChatRoomAdapter));
    }

    //加载文件
    private void bindCommon(ChatRoomMessageViewHolder holder, RecordEntity bean) {
        ConstraintLayout file = null;
        String size = computingFileSize(bean.getFileEntity().getFileLength());
        String name = bean.getFileEntity().getName();

        if (bean.getSender() == -1) {
            holder.getMineFileName().setText(name);
            holder.getMineFileSize().setText(size);
            holder.getMineFile().setVisibility(View.VISIBLE);
            file = holder.getMineFile();
        } else {
            holder.getSenderFileName().setText(name);
            holder.getSenderFileSize().setText(size);
            holder.getSenderFile().setVisibility(View.VISIBLE);
            file = holder.getSenderFile();
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
    private void bindImage(ChatRoomMessageViewHolder holder, RecordEntity bean) {
        ImageView image = null;
        if (bean.getSender() == -1) {
            image = holder.getMineImage();
        } else {
            image = holder.getSenderImage();
        }

        String path = bean.getFileEntity().getPath();
        if (TextUtils.isEmpty(path)){
            //图片路径为空时判断正在下载或者接收已经失败
            Glide.with(mContext).load(R.drawable.ic_file_damage_fill).into(image);
            return;
        }

        Glide.with(mContext).load(path).into(image);
        image.setVisibility(View.VISIBLE);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QuickClickListener.isFastClick()) {
                    PictureShowActivity.goActivity(mContext, path);
                }
            }
        });

        image.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }


    //加载语音
    private void bindVoice(ChatRoomMessageViewHolder holder, RecordEntity bean) {
        LinearLayout voice = null;
        TextView voiceLength = null;
        if (bean.getSender() == -1) {
            voiceLength = holder.getMineVoiceLength();
            voice = holder.getMineVoice();
        } else {
            holder.getSenderVoice().setVisibility(View.VISIBLE);
            voiceLength = holder.getSenderVoiceLength();
            voice = holder.getSenderVoice();
        }

        int length = bean.getFileEntity().getContentLength();
        voiceLength.setText(
                String.valueOf(length));
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) voiceLength.getLayoutParams();
        double percent = (double) length / ChatRoomConfig.MAX_VOICE_LENGTH;
        int width = (int) (percent * ChatRoomConfig.MAX_VOICE_TEXT_VIEW_LENGTH) + ChatRoomConfig.BASE_VOICE_LENGTH;
        layoutParams.width = width;
        voiceLength.setLayoutParams(layoutParams);
        voiceLength.setText(String.valueOf(length));
        voice.setVisibility(View.VISIBLE);

        String path = bean.getFileEntity().getPath();
        voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QuickClickListener.isFastClick()) {

                    if (!TextUtils.isEmpty(path)){
                        onClickVoice(path);
                    }
                }
            }
        });

        voice.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }

    //当点击语音框时，播放语音
    private void onClickVoice(String path) {
        if (sIsAudioRecording){
            return;
        }
        MyMediaPlayerManager.getsInstance().play(path);
//        1572865304716
    }

    //加载文本内容
    private void bindText(ChatRoomMessageViewHolder holder,
                          RecordEntity bean) {
        TextView message = null;
        if (bean.getSender() == -1){
            holder.getMineMessage().setText(bean.getContent());
            holder.getMineMessage().setVisibility(View.VISIBLE);
            message = holder.getSenderMessage();
        }else {
            holder.getSenderMessage().setText(bean.getContent());
            holder.getSenderMessage().setVisibility(View.VISIBLE);
            message = holder.getMineMessage();
        }

        message.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }

    private void bindAtText(ChatRoomMessageViewHolder holder, RecordEntity bean){
//        TextView message = null;
//        if (bean.getIsReceive() == ChatRoomConfig.RECEIVE_AT_MESSAGE){
//            holder.getSenderMessage().setText(bean.getContent());
//            holder.getSenderMessage().setVisibility(View.VISIBLE);
//            message = holder.getSenderMessage();
//
//            String at = bean.getName();
//            buildNotify(at,holder.getSenderMessage());
//        }else {
//            holder.getMineMessage().setText(bean.getContent());
//            holder.getMineMessage().setVisibility(View.VISIBLE);
//            message = holder.getMineMessage();
//        }
//
//        message.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }

    /**
     * 根据唯一标识符寻找本地名字替代@后的名字
     * @param at @队列
     * @param message */
    private void buildNotify(String at,TextView message){
        if (!TextUtils.isEmpty(at)){
            Editable editableText = message.getEditableText();

            String[] array = (String[]) GsonTools.formJson(at, String[].class);
            for (int i = array.length-1; i >= 0; i--){
                String[] split = array[i].split("\\|");

                int st = Integer.parseInt(split[1]);        //at开始位置
                int length = Integer.parseInt(split[2]);    //用户名长度

                ContactEntity entity = ContactManager.getInstance().getContact(split[0]);
                if (null != entity){
                    editableText.replace(st+1,st+length+1,entity.getName());
                }else if (split[0].equals(MineInfoManager.getInstance().getIdentifier())){
                    String name = MineInfoManager.getInstance().getName();
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

    public void bindAvatar(ChatRoomMessageViewHolder holder, RecordEntity bean){
        AvatarManager avatarManager = AvatarManager.getInstance();
        if (bean.getSender() == -1) {
            avatarManager.loadContactAvatar(mContext,
                    holder.getMineAvatar(),
                    -1);
            holder.getMineAvatar().setVisibility(View.VISIBLE);
        } else {
            avatarManager.loadContactAvatar(mContext,
                    holder.getSenderAvatar(),
                    bean.getSender());
            ContactEntity contact = ContactManager.
                    getInstance().
                    getContact(bean.getSender());
            holder.getSenderName().setText(contact.getName());

            holder.getSenderName().setVisibility(View.VISIBLE);
            holder.getSenderAvatar().setVisibility(View.VISIBLE);

            holder.getSenderAvatar().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (QuickClickListener.isFastClick()){       //防止长按后立即触发单击事件
                        UserInfoShowActivity.go(mContext,bean.getSender());
                    }
                }
            });
        }
    }

    private void bindCall(ChatRoomMessageViewHolder holder, RecordEntity bean){
        LinearLayout callBox = null;
        RecordCallBean recordCall = (RecordCallBean) GsonTools.formJson(bean.getContent(),RecordCallBean.class);
        switch (recordCall.getType()){
            case ChatRoomConfig.RECEIVE_VOICE_CALL:
                holder.getSenderCallBox().setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.ic_voice_call).into(holder.getSenderCallImage());
                holder.getSenderCallText().setText(recordCall.getContent());
                break;
            case ChatRoomConfig.RECEIVE_VIDEO_CALL:
                holder.getSenderCallBox().setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.ic_video_call).into(holder.getSenderCallImage());
                holder.getSenderCallText().setText(recordCall.getContent());
                break;
            case ChatRoomConfig.SEND_VOICE_CALL:
                holder.getMineCallBox().setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.ic_voice_call).into(holder.getMineCallImage());
                holder.getMineCallText().setText(recordCall.getContent());
                break;
            case ChatRoomConfig.SEND_VIDEO_CALL:
                holder.getMineCallBox().setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.ic_video_call).into(holder.getMineCallImage());
                holder.getMineCallText().setText(recordCall.getContent());
                break;
        }
        if (recordCall.getLength() == -1){
            holder.getSenderCallImage().setColorFilter(R.color.color_gray_dark);
            holder.getSenderCallText().setTextColor(mContext.getResources().getColor(R.color.color_gray_dark));
        }
        if (recordCall.getType() == ChatRoomConfig.RECEIVE_VOICE_CALL
                || recordCall.getType() == ChatRoomConfig.RECEIVE_VIDEO_CALL){
            callBox = holder.getSenderCallBox();
        }else {
            callBox = holder.getMineCallBox();
        }

        callBox.setOnLongClickListener(new OnLongClickRecord(mContext,bean, holder, mChatRoomAdapter));
    }
}
