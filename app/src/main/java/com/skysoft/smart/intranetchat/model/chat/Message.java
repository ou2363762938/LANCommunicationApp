package com.skysoft.smart.intranetchat.model.chat;

import android.content.Context;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.chat.MessageSignal;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.ChatRecordDao;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.LatestChatHistoryEntity;
import com.skysoft.smart.intranetchat.model.net_model.SendMessage;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.listsort.MessageListSort;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomMessageAdapter;

import org.greenrobot.eventbus.EventBus;

public class Message {
    private static final String TAG = "Message";
    private Context mContext;
    private ChatRoomMessageAdapter mChatRoomMessageAdapter;
    private static Message sInstance;

    private Message(Context context) {
        this.mContext = context;
    }

    public static void init(Context context, ChatRoomMessageAdapter chatRoomMessageAdapter) {
        sInstance = new Message(context);
        sInstance.mChatRoomMessageAdapter = chatRoomMessageAdapter;
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
    public void ReceiveMessage(String messageJson, String host) {
        TLog.d(TAG, "----> messageJson: " + messageJson + ", host: " + host);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (host.equals(IntranetChatApplication.getHostIp())){
                    return;
                }

                MessageBean messageBean = (MessageBean) GsonTools.formJson(messageJson,MessageBean.class);
                if (messageBean.getSender().equals(IntranetChatApplication.getsMineUserInfo().getIdentifier())){
                    return;
                }

                messageBean.setHost(host);
                MessageSignal signal = new MessageSignal();
                signal.setBean(messageBean);

                ChatRecordEntity recordEntity = SendMessage.initChatRecordEntity(messageBean);      //初始化聊天记录
                recordEntity.setSender(messageBean.getSender());
                switch (messageBean.getType()){
                    case 0:
                        recordEntity.setIsReceive(ChatRoomConfig.RECEIVE_MESSAGE);
                        break;
                    case 1:
                        recordEntity.setIsReceive(ChatRoomConfig.RECEIVE_AT_MESSAGE);
                        break;
                    case 2:
                        recordEntity.setIsReceive(ChatRoomConfig.RECEIVE_REPLAY_MESSAGE);
                        break;
                }

                mChatRoomMessageAdapter = IntranetChatApplication.getsChatRoomMessageAdapter();
                //如果当前处于messageBean对应的聊天室
                if (mChatRoomMessageAdapter != null &&
                        mChatRoomMessageAdapter.
                        getReceiverIdentifier().
                        equals(recordEntity.getReceiver())){
                    recordEntity.setHost(host);
                    EventBus.getDefault().post(recordEntity);
                    signal.setIn(true);
                }else {
                    //添加记录到数据库中
                    ChatRecordDao chatRecordDao = MyDataBase.getInstance().getChatRecordDao();
                    long latestRecordTime = chatRecordDao.getLatestRecordTime(recordEntity.getReceiver());
                    if (latestRecordTime != 0 && recordEntity.getTime() - latestRecordTime > 2*60*1000){
                        ChatRecordEntity recordTime = ChatRoomMessageAdapter.generatorTimeRecord(recordEntity.getReceiver(),latestRecordTime);
                        chatRecordDao.insert(recordTime);
                    }
                    chatRecordDao.insert(recordEntity);
                }

                //-------------------------------------------------------------------------
                LatestChatHistoryEntity historyEntity = IntranetChatApplication.
                        sLatestChatHistoryMap.get(messageBean.getReceiver());

                if (null == historyEntity){     //新建最近记录
                    signal.setNew(true);
                    IntranetChatApplication.getMessageList()
                            .add(messageBean.getReceiver());      //添加到最近记录列表中

                    historyEntity = new LatestChatHistoryEntity();
                    boolean isGroup = !messageBean.
                            getReceiver().
                            endsWith(messageBean.getSender());     //是否为群聊
                    historyEntity.setGroup( isGroup ? 0 : 1);

                    ContactEntity contactEntity = null;     //设置记录对应聊天室参数
                    if (isGroup){
                        contactEntity = IntranetChatApplication.sGroupContactMap.get(messageBean.getReceiver());
                    }else {
                        contactEntity = IntranetChatApplication.sContactMap.get(messageBean.getReceiver());
                    }

                    historyEntity.setUserName(contactEntity.getName());     //聊天室名
                    historyEntity.setStatus(contactEntity.getStatus());        //聊天室状态
                    historyEntity.setUserHeadPath(contactEntity.getAvatarPath());       //聊天室头像地址
                    historyEntity.setUserHeadIdentifier(contactEntity.getAvatarIdentifier());   //聊天室头像唯一标识符
                    IntranetChatApplication.sLatestChatHistoryMap.put(messageBean.getReceiver(),historyEntity);     //添加到Map中
                }

                historyEntity.setHost(host);       //记录IP
                historyEntity.setContent(messageBean.getMsg());     //记录聊天内容
                historyEntity.setContentTimeMill(messageBean.getTimeStamp());       //记录聊天时间
                //将毫秒数时间转为String
                historyEntity.setContentTime(RoomUtils.millsToTime(historyEntity.getContentTimeMill()));
                if (signal.isIn()) {
                    historyEntity.setUnReadNumber(0);
                } else {
                    historyEntity.setUnReadNumber(historyEntity.getUnReadNumber()+1);
                }

                MessageListSort.CollectionsList(IntranetChatApplication.getMessageList());        //对记录排序

                EventBus.getDefault().post(signal);

                if (signal.isNew()) {
                    MyDataBase.getInstance().getLatestChatHistoryDao().insert(historyEntity);
                } else {
                    MyDataBase.getInstance().getLatestChatHistoryDao().update(historyEntity);
                }
            }
        }).start();


    }

    public void SendMessage() {

    }
}
