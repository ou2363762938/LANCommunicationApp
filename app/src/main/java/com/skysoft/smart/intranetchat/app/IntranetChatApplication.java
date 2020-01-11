/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication] Create a new process service with message sending and receiving.
 */
package com.skysoft.smart.intranetchat.app;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.ashokvarma.bottomnavigation.TextBadgeItem;
import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.IIntranetChatAidlInterface;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveCallBean;
import com.skysoft.smart.intranetchat.bean.GroupMembersBean;
import com.skysoft.smart.intranetchat.bean.LoadResourceBean;
import com.skysoft.smart.intranetchat.bean.NotifyContactOutLine;
import com.skysoft.smart.intranetchat.bean.RecordCallBean;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.ChatRecordDao;
import com.skysoft.smart.intranetchat.database.dao.ContactDao;
import com.skysoft.smart.intranetchat.database.dao.LatestChatHistoryDao;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.EquipmentInfoEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.LatestChatHistoryEntity;
import com.skysoft.smart.intranetchat.database.table.MineInfoEntity;
import com.skysoft.smart.intranetchat.database.table.RefuseGroupEntity;
import com.skysoft.smart.intranetchat.model.EstablishGroup;
import com.skysoft.smart.intranetchat.model.Login;
import com.skysoft.smart.intranetchat.tools.listsort.MessageListSort;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.AskResourceBean;
import com.skysoft.smart.intranetchat.model.network.bean.EstablishGroupBean;
import com.skysoft.smart.intranetchat.model.network.bean.FileBean;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReceiveAndSaveFileBean;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.server.IntranetChatServer;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomMessageAdapter;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.EstablishGroup.EstablishGroupAdapter;
import com.skysoft.smart.intranetchat.ui.fragment.main.contact.ContactListAdapter;
import com.skysoft.smart.intranetchat.ui.fragment.main.message.MessageListAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import de.hdodenhof.circleimageview.CircleImageView;

import static androidx.core.app.NotificationCompat.VISIBILITY_SECRET;

//B: [Intranet Chat] [APP] [Communication] Create a new process service with message sending and receiving,Oliver ou,2019/10/30

public class IntranetChatApplication extends Application {
    private final String TAG = IntranetChatApplication.class.getSimpleName();

    public static IIntranetChatAidlInterface sAidlInterface = null;

    private static IntranetChatCallback sCallback = new IntranetChatCallback();

    //B:[Intranet Chat] [APP][UI] Chat Room Oliver Ou 2019/10/31
    public static UserInfoBean sMineUserInfo = null;
    //B:[Intranet Chat] [APP][UI] Chat Room Oliver Ou 2019/11/1
    public static String sMineAvatarPath;
    //E:[Intranet Chat] [APP][UI] Chat Room Oliver Ou 2019/11/1
    private static List<LatestChatHistoryEntity> sLatestChatHistoryList = new ArrayList<>();
    private static List<ContactEntity> sContactList = new ArrayList<>();
    private static List<ContactEntity> sGroupContactList = new ArrayList<>();
    private static MessageListAdapter sMessageListAdapter;
    private static ContactListAdapter sContactListAdapter;
    private static ChatRoomMessageAdapter sChatRoomMessageAdapter;
    private static TextBadgeItem mTextBadgeItem;
    private static ArrayBlockingQueue<byte[]> mDatasQueue = new ArrayBlockingQueue<byte[]>(1024);
    private static int sTotalUnReadNumber = 0;
    private static EquipmentInfoEntity sEquipmentInfoEntity = new EquipmentInfoEntity();
    private static List<RefuseGroupEntity> sRefuseGroupList = new ArrayList<>();
    private static long sBaseTimeLine;
    private static NotificationManager sNotificationManager;
    private static int sCurrentProgress;
    private static int sCreateProgress;
    private static boolean sInCall = false;
    private static long sStartCallTime = 0;
    private static long sEndCallTime = 0;
    private static boolean sNetWorkState = true;
    private static int sNetWorkType = 0;
    private static String sFilterIdentifier;
    private static CircleImageView sShowUserInfoAvatar;
    private static TextView sShowUserInfoName;
    private static Map<String,Long> sHeartbeatDetection = new HashMap<>();
    private static boolean sRequestAvatar = false;
    private static boolean sInShowUserInfoActivity = false;
    private static TextView sShowUserState;
    private static EstablishGroupAdapter sEstablishGroupAdapter;
    private static String sHostIp;
    private static Map<String ,FileEntity> sMonitorReceiveFile = new HashMap<>();

    //监测文件接收情况
    public static void initMonitorReceiveFile(Map<String ,FileEntity> init){
        if (init != null && init.size() != 0){
            sMonitorReceiveFile.putAll(init);
        }
    }

    public static Map<String, FileEntity> getMonitorReceiveFile() {
        return sMonitorReceiveFile;
    }

    public static void setMonitorReceiveFile(Map<String, FileEntity> sMonitorReceiveFile) {
        IntranetChatApplication.sMonitorReceiveFile = sMonitorReceiveFile;
    }

    public static String getHostIp() {
        return sHostIp;
    }

    public static void setHostIp(String sHostIp) {
        IntranetChatApplication.sHostIp = sHostIp;
    }

    public static boolean isRequestAvatar() {
        return sRequestAvatar;
    }

    public static void setRequestAvatar(boolean sRequestAvatar) {
        IntranetChatApplication.sRequestAvatar = sRequestAvatar;
    }

    public static boolean isInShowUserInfoActivity() {
        return sInShowUserInfoActivity;
    }

    public static void setInShowUserInfoActivity(boolean sInShowUserInfoActivity) {
        IntranetChatApplication.sInShowUserInfoActivity = sInShowUserInfoActivity;
    }

    public static EstablishGroupAdapter getEstablishGroupAdapter() {
        return sEstablishGroupAdapter;
    }

    public static void setEstablishGroupAdapter(EstablishGroupAdapter sEstablishGroupAdapter) {
        IntranetChatApplication.sEstablishGroupAdapter = sEstablishGroupAdapter;
    }

    public static TextView getShowUserState() {
        return sShowUserState;
    }

    public static void setShowUserState(TextView sShowUserState) {
        IntranetChatApplication.sShowUserState = sShowUserState;
    }

    public static CircleImageView getShowUserInfoAvatar() {
        return sShowUserInfoAvatar;
    }

    public static void setShowUserInfoAvatar(CircleImageView sShowUserInfoAvatar) {
        IntranetChatApplication.sShowUserInfoAvatar = sShowUserInfoAvatar;
    }

    public static TextView getShowUserInfoName() {
        return sShowUserInfoName;
    }

    public static void setShowUserInfoName(TextView sShowUserInfoName) {
        IntranetChatApplication.sShowUserInfoName = sShowUserInfoName;
    }

    public static String getFilterIdentifier() {
        return sFilterIdentifier;
    }

    public static void setFilterIdentifier(String sFilterIdentifier) {
        IntranetChatApplication.sFilterIdentifier = sFilterIdentifier;
    }

    public static int getsNetWorkType() {
        return sNetWorkType;
    }

    public static void setsNetWorkType(int sNetWorkType) {
        IntranetChatApplication.sNetWorkType = sNetWorkType;
    }

    public static boolean isNetWortState() {
        return sNetWorkState;
    }

    public static void setNetWorkState(boolean sNewWortState) {
        IntranetChatApplication.sNetWorkState = sNewWortState;
    }

    public static long getStartCallTime() {
        return sStartCallTime;
    }

    public static void setStartCallTime(long mStartCallTime) {
        IntranetChatApplication.sStartCallTime = mStartCallTime;
    }

    public static long getEndCallTime() {
        return sEndCallTime;
    }

    public static void setEndCallTime(long mEndCallTime) {
        IntranetChatApplication.sEndCallTime = mEndCallTime;
    }

    public static boolean isInCall() {
        return sInCall;
    }

    public static void setInCall(boolean sInCall) {
        IntranetChatApplication.sInCall = sInCall;
    }

    public static int getsCurrentProgress() {
        return sCurrentProgress;
    }

    public static void setsCurrentProgress(int sCurrentProgress) {
        IntranetChatApplication.sCurrentProgress = sCurrentProgress;
    }

    public static int getsCreateProgress() {
        return sCreateProgress;
    }

    public static void setsCreateProgress(int sCreateProgress) {
        IntranetChatApplication.sCreateProgress = sCreateProgress;
    }

    public static ArrayBlockingQueue<byte[]> getmDatasQueue() {
        return IntranetChatApplication.mDatasQueue;
    }

    public static void setmDatasQueue(ArrayBlockingQueue<byte[]> mDatasQueue) {
        IntranetChatApplication.mDatasQueue = mDatasQueue;
    }
    public static List<LatestChatHistoryEntity> getMessageList() {
        return sLatestChatHistoryList;
    }

    public static List<ContactEntity> getsContactList() {
        return sContactList;
    }

    public static MessageListAdapter getsMessageListAdapter() {
        return sMessageListAdapter;
    }

    public static void setsMessageListAdapter(MessageListAdapter sMessageListAdapter) {
        IntranetChatApplication.sMessageListAdapter = sMessageListAdapter;
    }

    public static ContactListAdapter getsContactListAdapter() {
        return sContactListAdapter;
    }

    public static void setsContactListAdapter(ContactListAdapter sContactListAdapter) {
        IntranetChatApplication.sContactListAdapter = sContactListAdapter;
    }

    public static ChatRoomMessageAdapter getsChatRoomMessageAdapter() {
        return sChatRoomMessageAdapter;
    }

    public static void setsChatRoomMessageAdapter(ChatRoomMessageAdapter sChatRoomMessageAdapter) {
        IntranetChatApplication.sChatRoomMessageAdapter = sChatRoomMessageAdapter;
    }

    public static List<ContactEntity> getsGroupContactList() {
        return sGroupContactList;
    }

    public static void setsGroupContactList(List<ContactEntity> sGroupContactList) {
        if (sGroupContactList != null) {
            IntranetChatApplication.sGroupContactList = sGroupContactList;
        }
    }

    public static TextBadgeItem getmTextBadgeItem() {
        return mTextBadgeItem;
    }

    public static void setmTextBadgeItem(TextBadgeItem mTextBadgeItem) {
        IntranetChatApplication.mTextBadgeItem = mTextBadgeItem;
    }

    public static int getmTotalUnReadNumber() {
        return sTotalUnReadNumber;
    }

    public static void setmTotalUnReadNumber(int mTotalUnReadNumber) {
        IntranetChatApplication.sTotalUnReadNumber = mTotalUnReadNumber;
    }

    public static EquipmentInfoEntity getsEquipmentInfoEntity() {
        return sEquipmentInfoEntity;
    }

    public static void setsEquipmentInfoEntity(EquipmentInfoEntity sEquipmentInfoEntity) {
        IntranetChatApplication.sEquipmentInfoEntity = sEquipmentInfoEntity;
    }

    public static List<RefuseGroupEntity> getsRefuseGroupList() {
        return sRefuseGroupList;
    }

    public static void setsRefuseGroupList(List<RefuseGroupEntity> sRefuseGroupList) {
        IntranetChatApplication.sRefuseGroupList = sRefuseGroupList;
    }

    public static long getsBaseTimeLine() {
        return sBaseTimeLine;
    }

    public static void setsBaseTimeLine(long sBaseTimeLine) {
        IntranetChatApplication.sBaseTimeLine = sBaseTimeLine;
    }

    public static NotificationManager getsNotificationManager() {
        return sNotificationManager;
    }

    public static void setsNotificationManager(NotificationManager sNotificationManager) {
        IntranetChatApplication.sNotificationManager = sNotificationManager;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void setContactList(ContactEntity contactEntity) {
        String defaultAvatarIdentifier = new Identifier().getDefaultAvatarIdentifier();

        //刷新心跳数据
        updateHeartbeat(contactEntity.getIdentifier());
        if (sContactList.size() == 0){
            avatarDifferentDefault(contactEntity,false);
            return;
        } else {
            int i = 0;
            for (; i < sContactList.size(); i++) {
                ContactEntity temp = sContactList.get(i);
                if (temp.getIdentifier().equals(contactEntity.getIdentifier())){
                    //设置联系人IP
                    temp.setHost(contactEntity.getHost());
                    //设置联系人状态
                    if (temp.getStatus() == Config.STATUS_OUT_LINE){
                        if (contactEntity.getStatus() != Config.STATUS_OUT_LINE){
                            temp.setStatus(contactEntity.getStatus());
                            sContactList.remove(temp);
                            sContactList.add(0,temp);
                        }
                    }else {
                        temp.setStatus(contactEntity.getStatus());
                    }

                    //在查看联系人信息时才刷新名字和头像
                    if (!TextUtils.isEmpty(sFilterIdentifier) && sFilterIdentifier.equals(contactEntity.getIdentifier())) {
                        if (sShowUserState != null){

                        }
                        if (!contactEntity.getAvatarIdentifier().equals(temp.getAvatarIdentifier())) {
                            temp.setAvatarIdentifier(contactEntity.getAvatarIdentifier());
                            if (contactEntity.getAvatarIdentifier().equals(defaultAvatarIdentifier)) {
                                temp.setAvatarPath(null);
                                updateContactAvatarInChatRoom(temp.getIdentifier(),null);
                                Glide.with(this).load(R.drawable.default_head).into(sShowUserInfoAvatar);
                            }
                        }
                        //更换联系人名字
                        Log.d(TAG, "setContactList: name = " + contactEntity.getName());
                        if (!temp.getName().equals(contactEntity.getName())){
                            temp.setName(contactEntity.getName());
                            //更换聊天室中联系人的头像
                            updateContactNameInChatRoom(temp.getIdentifier(),temp.getName());
                        }
                        if (sShowUserInfoName != null){
                            sShowUserInfoName.setText(temp.getName());
                        }
                    }

                    //刷新联系人列表
                    if (sContactListAdapter != null) {
                        sContactListAdapter.notifyDataSetChanged();
                    }

                    //更新联系人数据库
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ContactDao contactDao = MyDataBase.getInstance().getContactDao();
                            ContactEntity contact = contactDao.getContact(temp.getIdentifier());
                            if (contact == null) {
                                contactDao.insert(contactEntity);
                            } else {
                                contactDao.update(temp);
                            }
                        }
                    }).start();
                    //更新消息列表
                    Iterator<LatestChatHistoryEntity> iterator = sLatestChatHistoryList.iterator();
                    while (iterator.hasNext()) {
                        LatestChatHistoryEntity next = iterator.next();
                        if (next.getUserIdentifier().equals(temp.getIdentifier())){
                            next.setStatus(temp.getStatus());
                            next.setHost(temp.getHost());

                            boolean change = false;
                            if (temp.getAvatarPath() == null && !TextUtils.isEmpty(next.getUserHeadPath()) && temp.getAvatarIdentifier().equals(defaultAvatarIdentifier)){
                                if (!next.getUserIdentifier().equals(defaultAvatarIdentifier) && temp.getAvatarIdentifier().equals(defaultAvatarIdentifier)){
                                    next.setUserHeadPath(null);
                                    next.setUserHeadIdentifier(defaultAvatarIdentifier);
                                    change = true;
                                }
                            }

                            if (!temp.getName().equals(next.getUserName())){
                                next.setUserName(temp.getName());
                                change = true;
                            }

                            if (change){

                                if (sMessageListAdapter != null) {
                                    sMessageListAdapter.notifyDataSetChanged();
                                }

                                if (sContactListAdapter != null) {
                                    sContactListAdapter.notifyDataSetChanged();
                                }

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MyDataBase.getInstance().getLatestChatHistoryDao().update(next);
                                    }
                                }).start();
                            }
                            return;
                        }
                    }
                    return;
                }
            }
            if (i == sContactList.size()) {
                avatarDifferentDefault(contactEntity, false);
                return;
            }
        }
    }

    private void avatarDifferentDefault(ContactEntity contactEntity, boolean contain) {
        if (contactEntity.getIdentifier().equals(sMineUserInfo.getIdentifier())) {
            return;
        }
        String defaultAvatarIdentifier = new Identifier().getDefaultAvatarIdentifier();
        if (!contactEntity.getAvatarIdentifier().equals(defaultAvatarIdentifier)  && !TextUtils.isEmpty(sFilterIdentifier)) {
            AskResourceBean askResourceBean = new AskResourceBean();
            askResourceBean.setResourceUniqueIdentifier(contactEntity.getAvatarIdentifier());
            askResourceBean.setResourceType(Config.RESOURCE_AVATAR);
            try {
                sAidlInterface.askResource(GsonTools.toJson(askResourceBean), contactEntity.getHost());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            contactEntity.setAvatarIdentifier(defaultAvatarIdentifier);
        }
        if (!contain) {
            contactEntity.setAvatarIdentifier(defaultAvatarIdentifier);
            contactEntity.setNotifyId((int) (System.currentTimeMillis() - sBaseTimeLine));
            sContactList.add(contactEntity);
            if (sContactListAdapter != null) {
                sContactListAdapter.notifyDataSetChanged();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MyDataBase.getInstance().getContactDao().insert(contactEntity);
                }
            }).start();
        }
    }

    //B:[Intranet Chat] [APP][UI] Chat Room Oliver Ou 2019/11/1
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveMessageBean(LatestChatHistoryEntity latestChatHistoryEntity){
        //刷新心跳数据
        updateHeartbeat(latestChatHistoryEntity.getUserIdentifier());
        //最近消息列表显示的消息内容
        MessageBean messageBean = new MessageBean();
        messageBean.setReceiver(latestChatHistoryEntity.getUserIdentifier());
        messageBean.setSender(latestChatHistoryEntity.getSenderIdentifier());
        messageBean.setTimeStamp(System.currentTimeMillis());
        if (latestChatHistoryEntity.getType() == ChatRoomConfig.RECORD_TEXT) {
            ChatRecordEntity recordEntity = new ChatRecordEntity();
            recordEntity.setTime(System.currentTimeMillis());
            //文字消息或者文件的唯一标识符
            recordEntity.setLength(latestChatHistoryEntity.getLength());
            recordEntity.setIsReceive(ChatRoomConfig.RECEIVE_MESSAGE);
            recordEntity.setType(ChatRoomConfig.RECORD_TEXT);
            recordEntity.setContent(latestChatHistoryEntity.getContent());
            recordEntity.setReceiver(latestChatHistoryEntity.getUserIdentifier());
            recordEntity.setSender(latestChatHistoryEntity.getSenderIdentifier());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ChatRecordDao chatRecordDao = MyDataBase.getInstance().getChatRecordDao();
                    if (sChatRoomMessageAdapter == null || !sChatRoomMessageAdapter.getReceiverIdentifier().equals(recordEntity.getReceiver())){
                        long latestRecordTime = chatRecordDao.getLatestRecordTime(recordEntity.getReceiver());
                        if (latestRecordTime != 0 && recordEntity.getTime() - latestRecordTime > 2*60*1000){
                            ChatRecordEntity recordTime = ChatRoomMessageAdapter.generatorTimeRecord(recordEntity.getReceiver(),latestRecordTime);
                            chatRecordDao.insert(recordTime);
                        }
                        chatRecordDao.insert(recordEntity);
                    }
                }
            }).start();
        }
        switch (latestChatHistoryEntity.getType()) {
            case ChatRoomConfig.RECORD_IMAGE:
                latestChatHistoryEntity.setContent(getString(R.string.image));
                break;
            case ChatRoomConfig.RECORD_VIDEO:
                latestChatHistoryEntity.setContent(getString(R.string.video));
                break;
            case ChatRoomConfig.RECORD_VOICE:
                latestChatHistoryEntity.setContent(getString(R.string.voice));
                break;
        }
        //通知
        messageBean.setMsg(latestChatHistoryEntity.getContent());

        //刷新消息记录
        Iterator<LatestChatHistoryEntity> iterator = sLatestChatHistoryList.iterator();
        while (iterator.hasNext()) {
            LatestChatHistoryEntity next = iterator.next();
            if (next.getUserIdentifier().equals(latestChatHistoryEntity.getUserIdentifier())) {
                next.setHost(latestChatHistoryEntity.getHost());
                next.setStatus(latestChatHistoryEntity.getStatus());
                next.setContent(latestChatHistoryEntity.getContent());
                next.setContentTimeMill(System.currentTimeMillis());
                next.setContentTime(ChatRoomActivity.millsToTime(next.getContentTimeMill()));
                MessageListSort.CollectionsList(sLatestChatHistoryList);

                setUnReadNumber(next,latestChatHistoryEntity,messageBean);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyDataBase.getInstance().getLatestChatHistoryDao().update(next);
                    }
                }).start();
                return;
            }
        }
        latestChatHistoryEntity.setContentTimeMill(System.currentTimeMillis());
        sLatestChatHistoryList.add(latestChatHistoryEntity);
        MessageListSort.CollectionsList(sLatestChatHistoryList);
        setUnReadNumber(latestChatHistoryEntity,latestChatHistoryEntity,messageBean);
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyDataBase.getInstance().getLatestChatHistoryDao().insert(latestChatHistoryEntity);
            }
        }).start();
    }

    private void setUnReadNumber(LatestChatHistoryEntity next,LatestChatHistoryEntity latestChatHistoryEntity,MessageBean messageBean){
        if (sChatRoomMessageAdapter != null && !TextUtils.isEmpty(sChatRoomMessageAdapter.getReceiverIdentifier()) && sChatRoomMessageAdapter.getReceiverIdentifier().equals(next.getUserIdentifier())) {
            next.setUnReadNumber(0);
            sMessageListAdapter.notifyDataSetChanged();
        } else if (latestChatHistoryEntity.getType() != Config.FILE_AVATAR){
            next.setUnReadNumber(next.getUnReadNumber() + 1);
            sMessageListAdapter.notifyDataSetChanged();
            addTotalUnReadNumber();
            notification(messageBean, latestChatHistoryEntity.getHost());
        }
    }

    private static void addTotalUnReadNumber() {
        sTotalUnReadNumber += 1;
        //B: [PT-80][Intranet Chat] [APP][UI] TextBadgeItem 一直为红色,Allen Luo,2019/11/12

        mTextBadgeItem.show();
        mTextBadgeItem.setText(String.valueOf(sTotalUnReadNumber));
        //E: [PT-80][Intranet Chat] [APP][UI] TextBadgeItem 一直为红色,Allen Luo,2019/11/12
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveAndSaveFile(ReceiveAndSaveFileBean receiveAndSaveFileBean){
        //刷新心跳数据
        updateHeartbeat(receiveAndSaveFileBean.getSender());
        Log.d(TAG, "onReceiveAndSaveFile: " + receiveAndSaveFileBean.toString());
        Iterator<ContactEntity> iterator = sContactList.iterator();

        //刷新头像
        while (iterator.hasNext()) {
            ContactEntity next = iterator.next();
            if (next.getAvatarIdentifier().equals(receiveAndSaveFileBean.getIdentifier())) {
                //确认是否在查看联系人界面
                if (TextUtils.isEmpty(sFilterIdentifier) || !sFilterIdentifier.equals(next.getIdentifier())) {
                    return;
                }

                //刷新联系人展示界面的头像
                if (sShowUserInfoAvatar != null) {
                    Log.d(TAG, "onReceiveAndSaveFile: path = " + receiveAndSaveFileBean.getPath());
                    Glide.with(this).load(receiveAndSaveFileBean.getPath()).into(sShowUserInfoAvatar);
                }


                //更换聊天室中联系人的头像
                updateContactAvatarInChatRoom(receiveAndSaveFileBean.getSender(),receiveAndSaveFileBean.getPath());

                //刷新联系人列表的头像
                next.setAvatarPath(receiveAndSaveFileBean.getPath());
                if (sContactListAdapter != null) {
                    sContactListAdapter.notifyDataSetChanged();
                }

                if (sEstablishGroupAdapter != null){
                    sEstablishGroupAdapter.notifyDataSetChanged();
                }

                //刷新最近消息列表的头像
                Iterator<LatestChatHistoryEntity> historyEntityIterator = sLatestChatHistoryList.iterator();
                while (historyEntityIterator.hasNext()) {
                    LatestChatHistoryEntity historyEntity = historyEntityIterator.next();
                    if (historyEntity.getUserIdentifier().equals(next.getIdentifier())) {
                        Log.d(TAG, "onReceiveAndSaveFile: history.content = " + historyEntity.getContent());
                        historyEntity.setUserHeadPath(receiveAndSaveFileBean.getPath());
                        if (sMessageListAdapter != null) {
                            sMessageListAdapter.notifyDataSetChanged();
                        }
                        //更新最近消息数据表
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                LatestChatHistoryDao historyDao = MyDataBase.getInstance().getLatestChatHistoryDao();
                                historyDao.update(historyEntity);
                            }
                        }).start();
                        break;
                    }
                }
                //更新联系人数据表
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ContactDao contactDao = MyDataBase.getInstance().getContactDao();
                        ContactEntity contact = contactDao.getContact(next.getIdentifier());
                        if (contact == null) {
                            contactDao.insert(next);
                        } else {
                            contact.setAvatarPath(next.getAvatarPath());
                            contactDao.update(contact);
                        }
                    }
                }).start();

                if (!sInShowUserInfoActivity){
                    sFilterIdentifier = null;
                }
                sRequestAvatar = false;
                return;
            }
        }

        Log.d(TAG, "onReceiveAndSaveFile: file");
        //接收文件
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatRecordDao chatRecordDao = MyDataBase.getInstance().getChatRecordDao();
                ChatRecordEntity recordByFileIdentifier = chatRecordDao.getRecordByFileIdentifier(receiveAndSaveFileBean.getReceiver(), receiveAndSaveFileBean.getIdentifier());
                String savePath = receiveAndSaveFileBean.getPath();
                if (recordByFileIdentifier != null) {
                    recordByFileIdentifier.setPath(receiveAndSaveFileBean.getPath());
                    if (recordByFileIdentifier.getType() == ChatRoomConfig.RECORD_VIDEO) {
                        Log.d(TAG, "run: onReceiveAndSaveFile path = " + savePath);
                        String path = ChatRoomMessageAdapter.createVideoThumbnailFile(receiveAndSaveFileBean.getPath());
                        receiveAndSaveFileBean.setPath(path);
                        recordByFileIdentifier.setPath(path);
                        recordByFileIdentifier.setFileName(savePath);
                    }

                    //接收完视频后刷新消息列表
                    Iterator<ContactEntity> contactEntityIterator = null;
                    int isGroup = 0;
                    if (receiveAndSaveFileBean.getSender().equals(receiveAndSaveFileBean.getReceiver())){
                        contactEntityIterator = sContactList.iterator();
                    }else {
                        isGroup = 1;
                        contactEntityIterator = sGroupContactList.iterator();
                    }

                    while (contactEntityIterator.hasNext()){
                        ContactEntity next = contactEntityIterator.next();
                        if (next.getIdentifier().equals(receiveAndSaveFileBean.getReceiver())){
                            LatestChatHistoryEntity historyEntity = new LatestChatHistoryEntity();
                            historyEntity.setType(recordByFileIdentifier.getType());
                            historyEntity.setUserIdentifier(recordByFileIdentifier.getReceiver());
                            historyEntity.setHost(receiveAndSaveFileBean.getHost());
                            historyEntity.setUserName(next.getName());
                            historyEntity.setUserHeadIdentifier(next.getAvatarIdentifier());
                            historyEntity.setUserHeadPath(next.getAvatarPath());
                            historyEntity.setGroup(isGroup);
                            historyEntity.setSenderIdentifier(receiveAndSaveFileBean.getSender());
                            EventBus.getDefault().post(historyEntity);
                        }
                    }

                    chatRecordDao.update(recordByFileIdentifier);
                    if (sChatRoomMessageAdapter != null && !TextUtils.isEmpty(sChatRoomMessageAdapter.getReceiverIdentifier()) && sChatRoomMessageAdapter.getReceiverIdentifier().equals(recordByFileIdentifier.getReceiver())) {
                        EventBus.getDefault().post(new LoadResourceBean(receiveAndSaveFileBean,recordByFileIdentifier));
                    }
                    //不让语音文件存入文件数据表
                    if (recordByFileIdentifier.getType() == ChatRoomConfig.RECORD_VOICE) {
                        return;
                    }
                }
                //B:记录文件接收成功的代码转移到IntranetChatCallback中,Oliver Ou,2019/11/28
                //理由：其他记录文件接收情况都在IntranetChatCallback中，所以移过去，方便统一管理
                //E:记录文件接收成功的代码转移到IntranetChatCallback中,Oliver Ou,2019/11/28

            }
        }).start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveFileBean(FileEntity fileEntity) {
        Log.d(TAG, "onReceiveFileBean: ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyDataBase.getInstance().getFileDao().insert(fileEntity);
            }
        }).start();
    }

    //B:[Intranet Chat] [APP][UI] Chat Room Oliver Ou 2019/11/6
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleFile(FileBean fileBean){
        //刷新心跳数据
        updateHeartbeat(fileBean.getSender());
        //如果语音文件超过90秒，则视为垃圾语音
        if (fileBean.getType() == Config.FILE_VOICE && fileBean.getFileLength() > 90) {
            return;
        }
        ChatRecordEntity chatRecordEntity = new ChatRecordEntity();
        chatRecordEntity.setContent(fileBean.getFileUniqueIdentifier());

        chatRecordEntity.setReceiver(fileBean.getReceiver());
        chatRecordEntity.setSender(fileBean.getSender());
        switch (fileBean.getType()) {
            case Config.FILE_VOICE:
                chatRecordEntity.setType(ChatRoomConfig.RECORD_VOICE);
                chatRecordEntity.setIsReceive(ChatRoomConfig.RECEIVE_VOICE);
                break;
            case Config.FILE_PICTURE:
                chatRecordEntity.setType(ChatRoomConfig.RECORD_IMAGE);
                chatRecordEntity.setIsReceive(ChatRoomConfig.RECEIVE_IMAGE);
                break;
            case Config.FILE_VIDEO:
                chatRecordEntity.setType(ChatRoomConfig.RECORD_VIDEO);
                chatRecordEntity.setIsReceive(ChatRoomConfig.RECEIVE_VIDEO);
                break;
            case Config.FILE_COMMON:
                chatRecordEntity.setType(ChatRoomConfig.RECORD_FILE);
                chatRecordEntity.setIsReceive(ChatRoomConfig.RECEIVE_FILE);
                chatRecordEntity.setFileName(fileBean.getFileName());
                break;
            default:
                Log.d(TAG, "handleFile: unknown file type");
                return;
        }
        Log.d(TAG, "handleFile: ");

        chatRecordEntity.setLength(fileBean.getFileLength());
        chatRecordEntity.setTime(System.currentTimeMillis());
        //收到文件就展示到聊天室
        if (sChatRoomMessageAdapter != null && !TextUtils.isEmpty(sChatRoomMessageAdapter.getReceiverIdentifier()) && sChatRoomMessageAdapter.getReceiverIdentifier().equals(fileBean.getReceiver())){
            sChatRoomMessageAdapter.add(chatRecordEntity,true);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyDataBase.getInstance().getChatRecordDao().insert(chatRecordEntity);
            }
        }).start();
    }
    //E:[Intranet Chat] [APP][UI] Chat Room Oliver Ou 2019/11/6

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveEstablishGroupBean(EstablishGroupBean establishGroupBean){
        //刷新心跳数据
        updateHeartbeat(establishGroupBean.getmHolderIdentifier());
        Log.d(TAG, "onReceiveEstablishGroupBean: " + establishGroupBean.toString());
        boolean groupExist = false;
        boolean latestChatExist = false;
        //查看群是否存在
        Iterator<ContactEntity> iterator = sGroupContactList.iterator();
        while (iterator.hasNext()) {
            ContactEntity next = iterator.next();
            if (next.getIdentifier().equals(establishGroupBean.getmGroupIdentifier())) {
                next.setName(establishGroupBean.getmGroupName());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyDataBase.getInstance().getContactDao().update(next);
                    }
                }).start();
                groupExist = true;
                break;
            }
        }

        //设置LatestChatHistoryEntity的内容
        String content = null;
        if (groupExist) {
            content = "更改群名为：" + establishGroupBean.getmGroupName();
        } else {
            content = establishGroupBean.getmGroupName() + "成为好友，请开始聊天吧";
        }

        //查看最近聊天消息是否存在
        Iterator<LatestChatHistoryEntity> latestIterator = sLatestChatHistoryList.iterator();
        while (latestIterator.hasNext()) {
            LatestChatHistoryEntity historyEntity = latestIterator.next();
            if (historyEntity.getUserIdentifier().equals(establishGroupBean.getmGroupIdentifier())) {
                historyEntity.setUserName(establishGroupBean.getmGroupName());
                historyEntity.setContent(content);
                historyEntity.setContentTimeMill(System.currentTimeMillis());
                historyEntity.setContentTime(ChatRoomActivity.millsToTime(historyEntity.getContentTimeMill()));
                MessageListSort.CollectionsList(sLatestChatHistoryList);
                sMessageListAdapter.notifyDataSetChanged();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyDataBase.getInstance().getLatestChatHistoryDao().update(historyEntity);
                    }
                }).start();
                latestChatExist = true;
                break;
            }
        }

        if (groupExist && latestChatExist) {
            return;
        }

        LatestChatHistoryEntity latestChatHistoryEntity = EstablishGroup.LatestChatFromGroup(establishGroupBean);
        latestChatHistoryEntity.setContent(content);
        latestChatHistoryEntity.setGroup(1);
        latestChatHistoryEntity.setUnReadNumber(1);
        addTotalUnReadNumber();
        sLatestChatHistoryList.add(latestChatHistoryEntity);
        MessageListSort.CollectionsList(sLatestChatHistoryList);
        if (sMessageListAdapter != null) {
            sMessageListAdapter.notifyDataSetChanged();
        }
        //存数据库
        EstablishGroup.storageGroup(latestChatHistoryEntity, establishGroupBean, groupExist, latestChatExist);
        EstablishGroup.addGroupToList(establishGroupBean);
    }

    public static void setsMineAvatarPath(String avatarPath) {
        if (TextUtils.isEmpty(avatarPath)) {
            return;
        }
        sMineAvatarPath = avatarPath;
    }

    public static String getsMineAvatarPath() {
        return sMineAvatarPath;
    }
    //E:[Intranet Chat] [APP][UI] Chat Room Oliver Ou 2019/11/1
    //E:[Intranet Chat] [APP][UI] Chat Room Oliver Ou 2019/10/31


    public static void initContactList(List<ContactEntity> contactBeans) {
        if (contactBeans == null) {
            return;
        }
        sContactList.addAll(contactBeans);
    }

    public static void initLatestChatHistoryList(List<LatestChatHistoryEntity> latestChatHistoryEntities) {
        if (latestChatHistoryEntities == null) {
            return;
        }
        sLatestChatHistoryList.addAll(latestChatHistoryEntities);
    }

    public static UserInfoBean getsMineUserInfo() {
        return sMineUserInfo;
    }

    public static void setMineUserInfo(MineInfoEntity mineInfoEntity, String userName) {
        UserInfoBean mineUserInfo = new UserInfoBean();
        mineUserInfo.setName(userName);
        mineUserInfo.setIdentifier(mineInfoEntity.getMineIdentifier());
        mineUserInfo.setAvatarIdentifier(mineInfoEntity.getMineHeadIdentifier());
        mineUserInfo.setStatus(Config.STATUS_ONLINE);
        sMineAvatarPath = mineInfoEntity.getMineHeadPath();
        IntranetChatApplication.sMineUserInfo = mineUserInfo;
    }

    //E: [Intranet Chat] [APP] [Communication] Create a new process service with message sending and receiving,Oliver ou,2019/10/30
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: ");
            sAidlInterface = IIntranetChatAidlInterface.Stub.asInterface(service);
            try {
                sAidlInterface.registerCallback(sCallback);
                sCallback.setOnReceiveCallBean(onReceiveCallBean);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: ");
//            sAidlInterface = null;
            Intent intent = new Intent(IntranetChatApplication.this, IntranetChatServer.class);
////            startForegroundService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        MyDataBase.init(this);
        listeningToContactSurvive();
        sBaseTimeLine = initBaseTimeLine();
        boolean isCurrentProcess = getApplicationContext().getPackageName().equals
                (getCurrentProcessName());
        if (Build.MANUFACTURER.equals("vivo") || Build.MANUFACTURER.equals("OPPO")) {
            if (isNotificationEnabled(this)) {
//                toSettingPage(this);
            }
        }
        int pid = android.os.Process.myPid();
        sCreateProgress = pid;
        if (isCurrentProcess) {
            MyDataBase.getInstance();
            Intent intent = new Intent(this, IntranetChatServer.class);
//            startForegroundService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            sCurrentProgress = pid;
        }
    }

    private long initBaseTimeLine() {
        Date date = new Date();
        date.setYear(2019);
        date.setMonth(11);
        date.setDate(11);
        return date.getTime();
    }

    private String getCurrentProcessName() {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService
                (Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                processName = process.processName;
            }
        }
        return processName;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        try {
            sAidlInterface.unregisterCallback(sCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(mConnection);
        Log.d(TAG, "onTerminate: ");
        EventBus.getDefault().unregister(this);
    }

    public static IntranetChatCallback getsCallback() {
        return sCallback;
    }

    @SuppressLint("NewApi")
    public static boolean isNotificationEnabled(Context context) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = applicationInfo.uid;
        Class appOpsClass;
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod("checkOpNoThrow", Integer.TYPE, Integer.TYPE, String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField("OP_POST_NOTIFICATION");
            int value = (Integer) opPostNotificationValue.get(Integer.class);
            return ((Integer) checkOpNoThrowMethod.invoke(appOpsManager, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void toSettingPage(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
        }
        context.startActivity(intent);
    }

    public static void setsCallback(IntranetChatCallback sCallback) {
        IntranetChatApplication.sCallback = sCallback;
    }

    private boolean isFirst = false;
    private OnReceiveCallBean onReceiveCallBean = new OnReceiveCallBean() {
        @Override
        public void onReceiveVoiceCallData(byte[] data) {
            mDatasQueue.offer(data);
        }
    };

    public void notification(MessageBean message, String host) {
        Log.d(TAG, "notification: " + message.toString());
        if (sNotificationManager == null) {
            sNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        Iterator<ContactEntity> iterator = null;
        boolean group = false;
        if (message.getReceiver().equals(message.getSender())) {
            iterator = IntranetChatApplication.getsContactList().iterator();
        } else {
            group = true;
            iterator = IntranetChatApplication.getsGroupContactList().iterator();
        }

        String name = null;
        String avatar = null;
        int notifyId = 0;
        Log.d(TAG, "notification: notifyId " + message.getReceiver());
        while (iterator.hasNext()) {
            ContactEntity next = iterator.next();
            if (next.getIdentifier().equals(message.getReceiver())) {
                name = next.getName();
                avatar = next.getAvatarPath();
                notifyId = next.getNotifyId();
            }
        }
        Intent mainIntent = new Intent(IntranetChatApplication.this, ChatRoomActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatRoomConfig.AVATAR, avatar);
        bundle.putString(ChatRoomConfig.NAME, name);
        bundle.putString(ChatRoomConfig.HOST, host);
        bundle.putInt(ChatRoomConfig.UID, 0);
        bundle.putString(ChatRoomConfig.IDENTIFIER, message.getReceiver());
        bundle.putBoolean(ChatRoomConfig.GROUP, group);
        mainIntent.putExtras(bundle);

        RemoteViews mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_message);
        mRemoteViews.setTextViewText(R.id.notification_message, message.getMsg());
        mRemoteViews.setTextViewText(R.id.notification_name, name);
        mRemoteViews.setTextViewText(R.id.notification_message_time, ChatRoomActivity.millsToTime(message.getTimeStamp()));
        if (TextUtils.isEmpty(avatar)) {
            mRemoteViews.setImageViewResource(R.id.notification_avatar, R.drawable.default_head);
        } else {
            Log.d(TAG, "notification: avatar = " + avatar);
            Bitmap bitmap = BitmapFactory.decodeFile(avatar);
            mRemoteViews.setImageViewBitmap(R.id.notification_avatar, bitmap);
        }
        PendingIntent mainPendingIntent = PendingIntent.getActivity(IntranetChatApplication.this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notification = new Notification.Builder(IntranetChatApplication.this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.setSmallIcon(R.drawable.logo)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setCustomContentView(mRemoteViews);
        } else {
            notification.setSmallIcon(R.drawable.logo)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContent(mRemoteViews);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Config.Notification_Channel_Id, Config.Notification_Channel_Name, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            //锁屏显示通知
            notificationChannel.setLockscreenVisibility(VISIBILITY_SECRET);
            //闪关灯的灯光颜色
            notificationChannel.setLightColor(Color.RED);
            //桌面launcher的消息角标
            notificationChannel.canShowBadge();
            notificationChannel.setDescription("天軟通");
            //是否允许震动
            notificationChannel.enableVibration(true);
            notificationChannel.setSound(Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.yisell_sound), new AudioAttributes.Builder().build());
            //获取系统通知响铃声音的配置
            notificationChannel.getAudioAttributes();
            //设置可绕过  请勿打扰模式
            notificationChannel.setBypassDnd(true);
            //设置震动模式
            notificationChannel.setVibrationPattern(new long[]{100, 100, 200});
            sNotificationManager.createNotificationChannel(notificationChannel);
            notification.setChannelId(Config.Notification_Channel_Id);
        }else {
            notification.setDefaults(Notification.DEFAULT_ALL);
        }
        //B: Notification 完善(notifyId),Allen Luo,2019/11/12
        sNotificationManager.notify(notifyId, notification.build());
        Log.d(TAG, "notification: notifyId 1 = " + notifyId + ", name = " + name);
        //E: Notification 完善(notifyId),Allen Luo,2019/11/12
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEndCall(RecordCallBean recordCallBean) {
        recordCall(recordCallBean.getIdentifier(), recordCallBean.getType(), recordCallBean.getHost(), recordCallBean.isVoice());
    }

    public String chatTime(long time) {
        time /= 1000;
        int hour = (int) (time / 3600);
        int minute = (int) (time % 3600 / 60);
        int second = (int) (time % 60);
        StringBuilder sb = new StringBuilder();
        if (hour != 0) {
            sb.append(hour);
            sb.append(getString(R.string.hour));
        }
        if (minute != 0) {
            sb.append(minute);
            sb.append(getString(R.string.minute));
        }
        if (second != 0) {
            sb.append(second);
            sb.append(getString(R.string.second));
        }
        return sb.toString();
    }

    public void recordCall(String identifier, int type, String host, boolean voice) {
        StringBuilder sb = new StringBuilder();
        boolean isReceive = true;
        switch (type) {
            case ChatRoomConfig.CALL_END_LAUNCH:
            case ChatRoomConfig.CALL_END_ANSWER:
                sb.append(getString(R.string.call_time));
                sb.append(chatTime(sEndCallTime - sStartCallTime));
                isReceive = type == ChatRoomConfig.CALL_END_ANSWER;
                break;
            case ChatRoomConfig.CALL_REFUSE_LAUNCH:
                sb.append(getString(R.string.call_refuse_launch));
                isReceive = false;
                break;
            case ChatRoomConfig.CALL_REFUSE_LAUNCH_MINE:
                sb.append(getString(R.string.call_refuse_launch_mine));
                isReceive = false;
                break;
            case ChatRoomConfig.CALL_IN_CALL:
                sb.append(getString(R.string.call_in_call));
                isReceive = false;
                break;
            case ChatRoomConfig.CALL_OUT_TIME_ANSWER:
            case ChatRoomConfig.CALL_REFUSE_ANSWER:
                sb.append(getString(R.string.call_refuse_answer));
                break;
            case ChatRoomConfig.CALL_REFUSE_ANSWER_MINE:
                sb.append(getString(R.string.call_refuse_answer_mine));
                isReceive = false;
                break;
            case ChatRoomConfig.CALL_OUT_TIME_LAUNCH:
                sb.append(getString(R.string.call_out_time));
                isReceive = false;
                break;
            case ChatRoomConfig.CALL_DIE_ANSWER:
            case ChatRoomConfig.CALL_DIE_LAUNCH:
                sb.append(getString(R.string.call_die));
                isReceive = type == ChatRoomConfig.CALL_DIE_ANSWER;
                break;
        }
        String content = sb.toString();
        ChatRecordEntity recordEntity = new ChatRecordEntity();
        recordEntity.setTime(System.currentTimeMillis());
        recordEntity.setType(ChatRoomConfig.RECORD_CALL);
        recordEntity.setReceiver(identifier);
        recordEntity.setSender(identifier);
        recordEntity.setContent(content);
        if (voice && isReceive) {
            recordEntity.setIsReceive(ChatRoomConfig.RECEIVE_VOICE_CALL);
        } else if (voice && !isReceive) {
            recordEntity.setIsReceive(ChatRoomConfig.SEND_VOICE_CALL);
        } else if (!voice && isReceive) {
            recordEntity.setIsReceive(ChatRoomConfig.RECEIVE_VIDEO_CALL);
        } else {
            recordEntity.setIsReceive(ChatRoomConfig.SEND_VIDEO_CALL);
        }
        if (type == ChatRoomConfig.CALL_END_ANSWER) {
            recordEntity.setLength(-1);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatRecordDao chatRecordDao = MyDataBase.getInstance().getChatRecordDao();
                if (sChatRoomMessageAdapter == null || !sChatRoomMessageAdapter.getReceiverIdentifier().equals(recordEntity.getReceiver())){
                    long latestRecordTime = chatRecordDao.getLatestRecordTime(recordEntity.getReceiver());
                    if (latestRecordTime != 0 && recordEntity.getTime() - latestRecordTime > 2*60*1000){
                        ChatRecordEntity recordTime = ChatRoomMessageAdapter.generatorTimeRecord(recordEntity.getReceiver(),latestRecordTime);
                        chatRecordDao.insert(recordTime);
                    }
                    chatRecordDao.insert(recordEntity);
                }
            }
        }).start();

        if (sChatRoomMessageAdapter != null && !TextUtils.isEmpty(sChatRoomMessageAdapter.getReceiverIdentifier()) && sChatRoomMessageAdapter.getReceiverIdentifier().equals(identifier)) {
            sChatRoomMessageAdapter.add(recordEntity);
        }

        MessageBean messageBean = new MessageBean();
        messageBean.setReceiver(identifier);
        messageBean.setSender(identifier);
        messageBean.setMsg(content);
        messageBean.setTimeStamp(System.currentTimeMillis());

        Iterator<LatestChatHistoryEntity> latestIterator = sLatestChatHistoryList.iterator();
        while (latestIterator.hasNext()) {
            LatestChatHistoryEntity next = latestIterator.next();
            if (next.getUserIdentifier().equals(identifier)) {
                next.setContent(content);
                next.setContentTimeMill(System.currentTimeMillis());
                next.setContentTime(ChatRoomActivity.millsToTime(next.getContentTimeMill()));
                if (sChatRoomMessageAdapter != null && !TextUtils.isEmpty(sChatRoomMessageAdapter.getReceiverIdentifier()) && sChatRoomMessageAdapter.getReceiverIdentifier().equals(next.getUserIdentifier())) {
                    next.setUnReadNumber(0);
                } else {
                    next.setUnReadNumber(next.getUnReadNumber() + 1);
                    addTotalUnReadNumber();
                    notification(messageBean, next.getHost());
                }
                MessageListSort.CollectionsList(sLatestChatHistoryList);
                sMessageListAdapter.notifyDataSetChanged();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyDataBase.getInstance().getLatestChatHistoryDao().update(next);
                    }
                }).start();
                return;
            }
        }

        Iterator<ContactEntity> iterator = sContactList.iterator();
        while (iterator.hasNext()) {
            ContactEntity next = iterator.next();
            if (next.getIdentifier().equals(identifier)) {
                LatestChatHistoryEntity entity = new LatestChatHistoryEntity();
                entity.setContent(content);
                entity.setHost(host);
                //默认
                entity.setStatus(next.getStatus());
                entity.setGroup(0);
                entity.setContentTimeMill(System.currentTimeMillis());
                entity.setContentTime(ChatRoomActivity.millsToTime(entity.getContentTimeMill()));
                entity.setUserIdentifier(identifier);
                entity.setSenderIdentifier(identifier);
                entity.setUserHeadIdentifier(next.getAvatarIdentifier());
                entity.setUserHeadPath(next.getAvatarPath());
                entity.setUserName(next.getName());
                if (sChatRoomMessageAdapter != null && !TextUtils.isEmpty(sChatRoomMessageAdapter.getReceiverIdentifier()) && sChatRoomMessageAdapter.getReceiverIdentifier().equals(identifier)) {
                    entity.setUnReadNumber(0);
                } else {
                    entity.setUnReadNumber(1);
                    addTotalUnReadNumber();
                    notification(messageBean, next.getHost());
                }
                sLatestChatHistoryList.add(entity);
                MessageListSort.CollectionsList(sLatestChatHistoryList);
                sMessageListAdapter.notifyDataSetChanged();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyDataBase.getInstance().getLatestChatHistoryDao().insert(entity);
                    }
                }).start();
            }
        }
    }

    public void listeningToContactSurvive(){
        Timer listening = new Timer();
        int time = 5*60*1000;
        TimerTask listeningTask = new TimerTask() {
            @Override
            public void run() {
                boolean outLine = false;
                for (Map.Entry<String,Long> entry : sHeartbeatDetection.entrySet()){
                    if (entry.getValue() < System.currentTimeMillis() - time){
                        Log.d(TAG, "run: entry.getKey() = " + entry.getKey());
                        Iterator<ContactEntity> iterator = sContactList.iterator();
                        while (iterator.hasNext()){
                            ContactEntity next = iterator.next();
                            if (next.getIdentifier().equals(entry.getKey())){
                                Log.d(TAG, "run: " + next.toString());
                                next.setStatus(Config.STATUS_OUT_LINE);
                                if ((entry.getValue() < System.currentTimeMillis() - time + 2*1000) && !TextUtils.isEmpty(next.getHost())){
                                    Login.requestUserInfo(next.getHost());
                                    break;
                                }
                                sContactList.remove(next);
                                sContactList.add(next);
                                outLine = true;
                                break;
                            }
                        }
                    }
                }
                if (outLine){
                    NotifyContactOutLine notifyContactOutLine = new NotifyContactOutLine();
                    EventBus.getDefault().post(notifyContactOutLine);
                }
            }
        };
        listening.schedule(listeningTask,time,time);
    }

    public void updateHeartbeat(String identifier){
        if (sHeartbeatDetection.containsKey(identifier)){
            sHeartbeatDetection.remove(identifier);
        }
        sHeartbeatDetection.put(identifier,System.currentTimeMillis());
    }

    @Subscribe
    public void notifyContactOutLine(NotifyContactOutLine outLine){
        if (sContactListAdapter != null){
            sContactListAdapter.notifyDataSetChanged();
        }
    }

    public void updateContactNameInChatRoom(String identifier,String name){
        if (sChatRoomMessageAdapter != null){
            GroupMembersBean groupMembersBean = sChatRoomMessageAdapter.getAvatars().get(identifier);
            if (groupMembersBean != null){
                groupMembersBean.setmMemberName(name);
                sChatRoomMessageAdapter.notifyDataSetChanged();
            }
        }
    }

    public void updateContactAvatarInChatRoom(String identifier,String avatarPath){
        if (sChatRoomMessageAdapter != null){
            GroupMembersBean groupMembersBean = sChatRoomMessageAdapter.getAvatars().get(identifier);
            if (groupMembersBean != null){
                groupMembersBean.setmMemberAvatarPath(avatarPath);
                sChatRoomMessageAdapter.notifyDataSetChanged();
            }
        }
    }
}
