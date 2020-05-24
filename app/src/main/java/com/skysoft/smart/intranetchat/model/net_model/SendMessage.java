/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/11/1
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.model.net_model;

import android.os.RemoteException;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.bean.network.SendAtMessageBean;
import com.skysoft.smart.intranetchat.bean.network.SendReplayMessageBean;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.network.bean.NotificationMessageBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReplayMessageBean;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.network.SendMessageBean;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.model.camera.entity.EventMessage;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReceiveAndSaveFileBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.tools.toastutil.ToastUtil;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.model.chat.record.RecordAdapter;

import java.io.File;

public class SendMessage {
    private static String TAG = SendMessage.class.getSimpleName();
    private static String sMineId = MineInfoManager.getInstance().getIdentifier();
    public static void sendCommonMessage(MessageBean messageBean){
        try {
            IntranetChatApplication.
                    sAidlInterface.
                    sendMessage(GsonTools.toJson(messageBean),
                            messageBean.getHost());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * at消息只有群众存在，所以只需要广播
     * @param messageBean @消息*/
    public static void broadcastAtMessage(NotificationMessageBean messageBean){
        try {
            IntranetChatApplication.sAidlInterface.broadcastAtMessage(GsonTools.toJson(messageBean));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 回复消息只有群中存在，所以只需要广播
     * @param messageBean 回复消息*/
    public static void broadcastReplayMessage(ReplayMessageBean messageBean){
        try {
            IntranetChatApplication.sAidlInterface.broadcastReplayMessage(GsonTools.toJson(messageBean));
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

    public static MessageBean generatorMessageBean(ContactEntity contact, String message) {
        MessageBean bean = new MessageBean();
        bean.setMsg(message);
        bean.setTimeStamp(System.currentTimeMillis());
        bean.setSender(sMineId);
        bean.setReceiver(sMineId);
        bean.setHost(contact.getHost());

        return bean;
    }

    public static MessageBean generatorMessageBean(GroupEntity group, String message) {
        MessageBean bean = new MessageBean();
        bean.setMsg(message);
        bean.setTimeStamp(System.currentTimeMillis());
        bean.setSender(sMineId);
        bean.setReceiver(group.getIdentifier());
        bean.setHost("255.255.255.255");
        return bean;
    }

    /**
     * 发送@消息同时更新消息界面*/
    public static RecordEntity broadcastAtMessage(SendAtMessageBean sendMessageBean){
        //初始化NotificationMessageBean
        NotificationMessageBean messageBean = initMessageBean(new NotificationMessageBean(),sendMessageBean);

        messageBean.setmNotificationUsers(sendMessageBean.getmAt());    //装填@对象
        messageBean.setType(1);

        SendMessage.broadcastAtMessage(messageBean);        //广播@消息

        //初始化ChatRecordEntity
//        RecordEntity recordEntity = initChatRecordEntity(messageBean,sendMessageBean);
//        recordEntity.setIsReceive(ChatRoomConfig.SEND_AT_MESSAGE);

        return null;
    }

    /**
     * 发送回复消息同事更新消息界面
     * @param sendMessageBean 回复消息*/
    public static RecordEntity broadcastReplayMessage(SendReplayMessageBean sendMessageBean){
        //初始化ReplayMessageBean
        ReplayMessageBean messageBean = initMessageBean(new ReplayMessageBean(), sendMessageBean);

        messageBean.setmNotificationUsers(sendMessageBean.getmAt());        //装填@对象
        messageBean.setmReplayContent(sendMessageBean.getReplayContent());      //装填回复内容
        messageBean.setmReplayName(sendMessageBean.getReplayName());            //装填回复对象名称
        messageBean.setmReplayType(sendMessageBean.getReplayType());            //装填回复内容内型
        messageBean.setType(2);

        SendMessage.broadcastReplayMessage(messageBean);        //广播回复消息

        //初始化ChatRecordEntity
//        RecordEntity recordEntity = initChatRecordEntity(messageBean,sendMessageBean);
//        recordEntity.setIsReceive(ChatRoomConfig.SEND_REPLAY_MESSAGE);

        return null;
    }

    /**
     * SendMessageBean初始化为MessageBean
     * @param sendMessageBean
     * @return */
    public static <T extends MessageBean> T initMessageBean(T messageBean, SendMessageBean sendMessageBean){
        messageBean.setMsg(sendMessageBean.getMessage());       //消息内容
        messageBean.setTimeStamp(System.currentTimeMillis());   //消息时间戳
        messageBean.setSender(MineInfoManager.getInstance().getIdentifier());      //记录发送者
        if (sendMessageBean.isGroup()){     //群聊接收者为群
            messageBean.setReceiver(sendMessageBean.getReciever());
        }else {         //私聊接受者为自己
            messageBean.setReceiver(messageBean.getSender());
        }
        return messageBean;
    }

}
