package com.skysoft.smart.intranetchat.model.chat;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.util.Log;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.signal.MessageSignal;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.RecordDao;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.net_model.SendMessage;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.Utils;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.model.chat.record.RecordAdapter;

import org.greenrobot.eventbus.EventBus;

public class Message {
    private static final String TAG = "Message";
    private static String sMineId;
    private final String THREAD_NAME = "MessageThread";
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private static Message sInstance;

    private Message() {
        mHandlerThread = new HandlerThread(THREAD_NAME);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        sMineId = MineInfoManager.getInstance().getIdentifier();
    }

    public static void init(Context context) {
        sInstance = new Message();
    }

    public static Message getInstance() {
        if (sInstance == null) {
            throw new NullPointerException();
        }
        return sInstance;
    }

    /**
     * 1 排除自己发送的消息
     * 2 刷新消息列表
     *   2.1 消息列表包含此item，修改item
     *   2.2 消息列表不包含此item，新建item
     *   2.3 刷新数据库
     *   2.4 未读消息+1
     *   2.5 排序消息列表
     *   2.6 通知栏
     * 3 刷新聊天室*/
    public void receive(String messageJson, String host) {
        TLog.d(TAG, "----> messageJson: " + messageJson + ", host: " + host);
        Runnable receive = new Runnable() {
            @Override
            public void run() {
                if (host.equals(IntranetChatApplication.getHostIp())){
                    return;
                }

                MessageBean messageBean = (MessageBean) GsonTools.formJson(messageJson,MessageBean.class);
                if (messageBean.getSender().equals(
                        MineInfoManager.getInstance().getIdentifier())){
                    return;
                }

                messageBean.setHost(host);
                MessageSignal signal = new MessageSignal();
                signal.setBean(messageBean);
                signal.setIn(RecordManager.getInstance().isInRoom());
                EventBus.getDefault().post(signal);

                RecordManager.getInstance().recordText(messageBean);
                LatestManager.getInstance().receive(messageBean.getReceiver(),messageBean.getMsg());
            }
        };

        mHandler.post(receive);
    }

    public void send(ContactEntity contact, String message) {
        Runnable send = new Runnable() {
            @Override
            public void run() {
                MessageBean bean = generatorMessageBean(contact, message);
                send(bean);
            }
        };
        mHandler.post(send);
    }

    public void send(GroupEntity group, String message) {
        Runnable send = new Runnable() {
            @Override
            public void run() {
                MessageBean bean = generatorMessageBean(group, message);
                send(bean);
            }
        };
        mHandler.post(send);
    }

    private void send(MessageBean bean) {
        TLog.d(TAG, "------> " + bean.toString());
        try {
            IntranetChatApplication.
                    sAidlInterface.
                    sendMessage(GsonTools.toJson(bean),
                            bean.getHost());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private MessageBean generatorMessageBean(ContactEntity contact, String message) {
        MessageBean bean = new MessageBean();
        bean.setMsg(message);
        bean.setTimeStamp(System.currentTimeMillis());
        bean.setSender(MineInfoManager.getInstance().getIdentifier());
        bean.setReceiver(MineInfoManager.getInstance().getIdentifier());
        bean.setHost(contact.getHost());

        return bean;
    }

    private MessageBean generatorMessageBean(GroupEntity group, String message) {
        MessageBean bean = new MessageBean();
        bean.setMsg(message);
        bean.setTimeStamp(System.currentTimeMillis());
        bean.setSender(sMineId);
        bean.setReceiver(group.getIdentifier());
        bean.setHost("255.255.255.255");
        return bean;
    }
}
