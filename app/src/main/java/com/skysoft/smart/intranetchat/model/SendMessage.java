/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/11/1
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.model;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.SendMessageBean;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.LatestChatHistoryEntity;
import com.skysoft.smart.intranetchat.model.camera.entity.EventMessage;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReceiveAndSaveFileBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.tools.listsort.MessageListSort;
import com.skysoft.smart.intranetchat.tools.toastutil.ToastUtil;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomMessageAdapter;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class SendMessage {
    private static String TAG = SendMessage.class.getSimpleName();
    public static void sendMessage(MessageBean messageBean,String host){
        try {
            IntranetChatApplication.sAidlInterface.sendMessage(GsonTools.toJson(messageBean),host);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void broadMessage(MessageBean messageBean){
        try {
            IntranetChatApplication.sAidlInterface.broadcastMessage(GsonTools.toJson(messageBean));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息同时更新消息界面*/
    public static ChatRecordEntity sendMessage(SendMessageBean sendMessageBean){
        MessageBean messageBean = new MessageBean();
        messageBean.setMsg(sendMessageBean.getMessage());
        messageBean.setTimeStamp(System.currentTimeMillis());
        messageBean.setSender(IntranetChatApplication.getsMineUserInfo().getIdentifier());
        if (sendMessageBean.isGroup()){
            messageBean.setReceiver(sendMessageBean.getReciever());
        }else {
            messageBean.setReceiver(messageBean.getSender());
        }
        ChatRecordEntity chatRecordEntity = new ChatRecordEntity();
        chatRecordEntity.setContent(sendMessageBean.getMessage());
        chatRecordEntity.setIsReceive(ChatRoomConfig.SEND_MESSAGE);
        chatRecordEntity.setReceiver(sendMessageBean.getReciever());
        chatRecordEntity.setSender(IntranetChatApplication.getsMineUserInfo().getIdentifier());
        chatRecordEntity.setTime(messageBean.getTimeStamp());
        chatRecordEntity.setType(ChatRoomConfig.RECORD_TEXT);//发送消息
        if (!sendMessageBean.isGroup()){
            if (!TextUtils.isEmpty(sendMessageBean.getHost())){
                SendMessage.sendMessage(messageBean,sendMessageBean.getHost());
            }
        }else {
            SendMessage.broadMessage(messageBean);
        }

        refreshMessageFragment(sendMessageBean);
        return chatRecordEntity;
    }

    /**
     * 接收或者发送消息后更新消息界面
     * @param sendMessageBean 更新消息列表需要的数据
     * content 消息显示的内容
     * receiverAvatarPath 聊天室的头像地址
     * host 聊天室的IP
     *  receiverIdentifier 聊天室的唯一标识符
     *  receiverName 聊天室的名字*/
    public static void refreshMessageFragment(SendMessageBean sendMessageBean) {
        List<LatestChatHistoryEntity> messageList = IntranetChatApplication.getMessageList();
        Iterator<LatestChatHistoryEntity> iterator = messageList.iterator();
        while (iterator.hasNext()) {
            LatestChatHistoryEntity next = iterator.next();
            if (next.getUserIdentifier().equals(sendMessageBean.getReciever())) {
                next.setContent(sendMessageBean.getMessage());
                next.setHost(sendMessageBean.getHost());
                next.setUnReadNumber(0);
                next.setContentTime(ChatRoomActivity.millsToTime(System.currentTimeMillis()));
                next.setContentTimeMill(System.currentTimeMillis());
                next.setStatus(com.skysoft.smart.intranetchat.model.network.Config.STATUS_ONLINE);
                next.setUserName(sendMessageBean.getName());
                next.setUserHeadPath(sendMessageBean.getAvatar());
                MessageListSort.CollectionsList(IntranetChatApplication.getMessageList());
                IntranetChatApplication.getsMessageListAdapter().notifyDataSetChanged();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyDataBase.getInstance().getLatestChatHistoryDao().update(next);
                    }
                }).start();
                return;
            }
        }
        LatestChatHistoryEntity latestChatHistoryEntity = adapterToLatest(sendMessageBean);
        messageList.add(latestChatHistoryEntity);
        MessageListSort.CollectionsList(IntranetChatApplication.getMessageList());
        IntranetChatApplication.getsMessageListAdapter().notifyDataSetChanged();
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyDataBase.getInstance().getLatestChatHistoryDao().insert(latestChatHistoryEntity);
            }
        }).start();
        //发送消息
    }

    /**构造一个最新的LatestChatHistoryEntity
     * @param sendMessageBean 更新消息列表需要的数据
     * message LatestChatHistoryEntity显示的内容
     * host LatestChatHistoryEntity记录的IP地址
     * receiverName LatestChatHistoryEntity记录的接收者名字
     * receiverIdentifier LatestChatHistoryEntity记录的接收者标识符
     * receiverAvatarPath LatestChatHistoryEntity记录的接收者头像地址
     * @return 构建的LatestChatHistoryEntity*/
    public static LatestChatHistoryEntity adapterToLatest(SendMessageBean sendMessageBean) {
        LatestChatHistoryEntity latestChatHistoryEntity = new LatestChatHistoryEntity();
        latestChatHistoryEntity.setContentTimeMill(System.currentTimeMillis());
        latestChatHistoryEntity.setContentTime(ChatRoomActivity.millsToTime(System.currentTimeMillis()));
        latestChatHistoryEntity.setContent(sendMessageBean.getMessage());
        latestChatHistoryEntity.setHost(sendMessageBean.getHost());
        latestChatHistoryEntity.setStatus(com.skysoft.smart.intranetchat.model.network.Config.STATUS_ONLINE);
        latestChatHistoryEntity.setUnReadNumber(0);
        latestChatHistoryEntity.setUserName(sendMessageBean.getName());
        latestChatHistoryEntity.setUserIdentifier(sendMessageBean.getReciever());
        latestChatHistoryEntity.setUserHeadPath(sendMessageBean.getAvatar());
        return latestChatHistoryEntity;
    }

    /**
     * 发送文件后更新消息界面
     * @param eventMessage 携带文件信息
     * @param sendMessageBean 携带接收者信息
     * @param refresh 是否刷新消息界面
     * @return 发送成功后返回消息记录*/
    public static ChatRecordEntity sendMessage(EventMessage eventMessage,SendMessageBean sendMessageBean,boolean refresh){
        if (!IntranetChatApplication.isNetWortState()){
            ToastUtil.toast(IntranetChatApplication.getContext(), IntranetChatApplication.getContext().getString(R.string.Toast_text_non_lan));
            return null;
        }
        //记录文件
        ChatRecordEntity chatRecordEntity = new ChatRecordEntity();
        chatRecordEntity.setTime(System.currentTimeMillis());
        chatRecordEntity.setSender(IntranetChatApplication.getsMineUserInfo().getIdentifier());
        chatRecordEntity.setReceiver(sendMessageBean.getReciever());
        chatRecordEntity.setPath(eventMessage.getMessage());
        //发送文件
        Identifier identifier = new Identifier();
        String fileIdentifier = identifier.getFileIdentifier(eventMessage.getMessage());
        chatRecordEntity.setContent(fileIdentifier);
        ReceiveAndSaveFileBean rasfb = new ReceiveAndSaveFileBean();
        rasfb.setIdentifier(fileIdentifier);
        rasfb.setPath(eventMessage.getMessage());
        rasfb.setSender(IntranetChatApplication.getsMineUserInfo().getIdentifier());
        if (sendMessageBean.isGroup()){
            rasfb.setReceiver(sendMessageBean.getReciever());
        }else {
            rasfb.setReceiver(rasfb.getSender());
        }
        return handleEventMessage(eventMessage,chatRecordEntity,rasfb,sendMessageBean,refresh);
    }

    /**
     * 处理携带文件信息的EventMessage
     * @param eventMessage 携带文件信息
     * @param sendMessageBean 携带接收者信息
     * @param refresh 是否刷新消息界面
     * @return 发送成功后返回消息记录*/
    public static ChatRecordEntity handleEventMessage(EventMessage eventMessage, ChatRecordEntity chatRecordEntity, ReceiveAndSaveFileBean rasfb, SendMessageBean sendMessageBean,boolean refresh) {
        int type = 0;
        int isReceive = 0;
        String content = null;
        int fileType = 0;
        switch (eventMessage.getType()) {
            case 1:
                type = ChatRoomConfig.RECORD_IMAGE;
                isReceive = ChatRoomConfig.SEND_IMAGE;
                content = IntranetChatApplication.getContext().getString(R.string.image);
                fileType = Config.FILE_PICTURE;
                break;
            case 2:
                type = ChatRoomConfig.RECORD_VIDEO;
                isReceive = ChatRoomConfig.SEND_VIDEO;
                content = IntranetChatApplication.getContext().getString(R.string.video);
                fileType = Config.FILE_VIDEO;
                chatRecordEntity.setPath(ChatRoomMessageAdapter.createVideoThumbnailFile(chatRecordEntity.getPath()));
                chatRecordEntity.setFileName(eventMessage.getMessage());
                break;
            case 3:
                type = ChatRoomConfig.RECORD_VOICE;
                isReceive = ChatRoomConfig.SEND_VOICE;
                content = IntranetChatApplication.getContext().getString(R.string.voice);
                fileType = Config.FILE_VOICE;
                break;
            case 4:
                type = ChatRoomConfig.RECORD_FILE;
                isReceive = ChatRoomConfig.SEND_FILE;
                content = IntranetChatApplication.getContext().getString(R.string.file);
                fileType = Config.FILE_COMMON;
                break;
            default:
                return null;
        }
        chatRecordEntity.setType(type);
        chatRecordEntity.setIsReceive(isReceive);
        chatRecordEntity.setLength((int) eventMessage.getLength());
        //记录文件
        FileEntity fileEntity = new FileEntity();
        fileEntity.setPath(eventMessage.getMessage());
        fileEntity.setIdentifier(rasfb.getIdentifier());
        switch (eventMessage.getType()) {
            case 1:
                fileEntity.setType(Config.FILE_PICTURE);
                break;
            case 2:
                fileEntity.setType(Config.FILE_VIDEO);
                break;
            case 4:
                fileEntity.setType(Config.FILE_COMMON);
                break;
        }
        File file = new File(eventMessage.getMessage());
        fileEntity.setFileName(file.getName());
        if (eventMessage.getType() == 4){
            chatRecordEntity.setLength(file.length());
            chatRecordEntity.setFileName(fileEntity.getFileName());
        }
        //发送录音文件
        SendFile sendFile = new SendFile();
        Log.d(TAG, "handleEventMessage: fileType = " + fileType);
        int b = 0;
        if (sendMessageBean.isGroup()){
            b = sendFile.sendFile(rasfb, "255.255.255.255", fileType, (int) eventMessage.getLength());
        }else {
            b = sendFile.sendFile(rasfb, sendMessageBean.getHost(), fileType, (int) eventMessage.getLength());
        }
        if (b == Config.SEND_SUCCESS){
            //存储文件
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //存储图片和视频
                    if (chatRecordEntity.getType() != ChatRoomConfig.RECORD_VOICE) {
                        MyDataBase.getInstance().getFileDao().insert(fileEntity);
                    }
                }
            }).start();
            //刷新最近聊天
            if (refresh){
                sendMessageBean.setMessage(content);
                refreshMessageFragment(sendMessageBean);
            }
        }else if (b == Config.SEND_FILE_BIG){
            ToastUtil.toast(IntranetChatApplication.getContext(),IntranetChatApplication.getContext().getString(R.string.file_size_too_big));
        }else if(b == Config.SEND_FILE_NOT_FOUND){
            ToastUtil.toast(IntranetChatApplication.getContext(),IntranetChatApplication.getContext().getString(R.string.file_not_found));
        }else if (b == Config.SEND_FILE_ZEOR){
            ToastUtil.toast(IntranetChatApplication.getContext(),IntranetChatApplication.getContext().getString(R.string.file_size_0B));
        }
        return chatRecordEntity;
    }
}
