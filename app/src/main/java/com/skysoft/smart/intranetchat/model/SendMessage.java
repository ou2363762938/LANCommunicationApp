/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/11/1
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.model;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.SendMessageBean;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.LatestChatHistoryEntity;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.listsort.MessageListSort;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoomActivity;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoomConfig;

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
}
