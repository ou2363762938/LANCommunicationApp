/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication]Create a UDP data sender
 */
package com.skysoft.smart.intranetchat.app;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.skysoft.smart.intranetchat.IIntranetChatAidlInterfaceCallback;
import com.skysoft.smart.intranetchat.app.impl.OnEstablishCallConnect;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveCallHungUp;
import com.skysoft.smart.intranetchat.app.impl.HandleVoiceCallResponse;
import com.skysoft.smart.intranetchat.app.impl.HandleInfo;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveCallBean;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveMessage;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveRequestConsent;
import com.skysoft.smart.intranetchat.bean.network.InCallBean;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.model.chat.Message;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.filemanager.FileManager;
import com.skysoft.smart.intranetchat.model.group.GroupManager;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.login.Login;
import com.skysoft.smart.intranetchat.model.net_model.SendRequest;
import com.skysoft.smart.intranetchat.model.net_model.SendResponse;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.AskBean;
import com.skysoft.smart.intranetchat.model.network.bean.AskResourceBean;
import com.skysoft.smart.intranetchat.model.network.bean.EstablishGroupBean;
import com.skysoft.smart.intranetchat.model.network.bean.FileBean;
import com.skysoft.smart.intranetchat.model.network.bean.NotificationMessageBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReceiveAndSaveFileBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReceiveFileContentBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReplayMessageBean;
import com.skysoft.smart.intranetchat.model.network.bean.ResponseBean;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.model.network.bean.VoiceCallDataBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import org.greenrobot.eventbus.EventBus;

public class IntranetChatCallback extends IIntranetChatAidlInterfaceCallback.Stub {
    private final String TAG = IntranetChatCallback.class.getSimpleName();
    private HandleInfo handleInfo;
    private HandleVoiceCallResponse handleVoiceCallResponse;
    private OnReceiveCallHungUp hungUpInAnswer;
    private OnReceiveCallBean onReceiveCallBean;
    private OnReceiveMessage onReceiveMessage;
    private OnEstablishCallConnect onEstablishCallConnect;
    private OnReceiveRequestConsent onReceiveRequestConsent;

    public void setHandleInfo(HandleInfo handleInfo) {
        this.handleInfo = handleInfo;
    }

    public void setHungUpInAnswer(OnReceiveCallHungUp hungUpInAnswer) {
        this.hungUpInAnswer = hungUpInAnswer;
    }

    public void setHandleVoiceCallResponse(HandleVoiceCallResponse handleVoiceCallResponse) {
        this.handleVoiceCallResponse = handleVoiceCallResponse;
    }

    public void setOnReceiveCallBean(OnReceiveCallBean onReceiveCallBean) {
        this.onReceiveCallBean = onReceiveCallBean;
    }

    public void setOnReceiveMessage(OnReceiveMessage onReceiveMessage) {
        this.onReceiveMessage = onReceiveMessage;
    }

    public void setOnEstablishCallConnect(OnEstablishCallConnect onEstablishCallConnect) {
        this.onEstablishCallConnect = onEstablishCallConnect;
    }

    public void setOnReceiveRequestConsent(OnReceiveRequestConsent onReceiveRequestConsent) {
        this.onReceiveRequestConsent = onReceiveRequestConsent;
    }

    @Override
    public void onReceiveUserInfo(String userInfoJson, String host) throws RemoteException {
        TLog.d(TAG, "onReceiveUserInfo: userInfoJson: " + userInfoJson + ", host: " + host);
        if (host.equals(MineInfoManager.getInstance().getHost())){
            return;
        }
        UserInfoBean userInfoBean = (UserInfoBean) GsonTools.formJson(userInfoJson,UserInfoBean.class);
        if (userInfoBean != null) {
            ContactManager.getInstance().receiveUserInfo(userInfoBean,host);
        }
    }

    @Override
    public void onReceiveMessage(String messageJson, String host) throws RemoteException {
        Log.d(TAG, "---------> : " + messageJson);
        Message.getInstance().receive(messageJson,host);
    }

    @Override
    public void receiveNotifyMessageBean(String notifyMessageJson, String host) throws RemoteException {
        TLog.d(TAG,"notifyMessageJson " + notifyMessageJson);
        NotificationMessageBean notificationMessageBean = (NotificationMessageBean) GsonTools.formJson(notifyMessageJson,NotificationMessageBean.class);
    }

    @Override
    public void receiveReplayMessageBean(String replayMessageJson, String host) throws RemoteException {
        TLog.d(TAG,"replayMessageJson " + replayMessageJson);
        ReplayMessageBean replayMessageBean = (ReplayMessageBean) GsonTools.formJson(replayMessageJson,ReplayMessageBean.class);
    }


    @Override
    public void onReceiveRequest(String requestJson, String host) throws RemoteException {
        TLog.d(TAG, "onReceiveRequest: requestJson: " + requestJson + ", host: " + host);
        if (host.equals(MineInfoManager.getInstance().getHost())){
            return;
        }
        AskBean askBean = (AskBean) GsonTools.formJson(requestJson, AskBean.class);
        switch (askBean.getRequestType()){
            case Config.REQUEST_USERINFO:
                UserInfoBean userInfo = MineInfoManager.getInstance().getUserInfo();
                userInfo.setRemark(null);
                IntranetChatApplication
                        .sAidlInterface
                        .sendUserInfo(GsonTools.toJson(userInfo),host);
                break;
            case Config.REQUEST_CONSENT_CALL:
                if (onReceiveRequestConsent != null){
                    //请求接电话
                    onReceiveRequestConsent.onReceiveRequestConsent();
                }
                break;
        }
    }

    @Override
    public void onReceiveFileBean(String fileJson, String host) throws RemoteException {
        TLog.d(TAG, "onReceiveFile: fileJson" + fileJson + ",host: " + host);
        //向host请求文件
        if (host.equals(MineInfoManager.getInstance().getHost())){
            return;
        }
        FileBean fileBean = (FileBean) GsonTools.formJson(fileJson,FileBean.class);
        if (fileBean.getSender().equals(MineInfoManager.getInstance().getIdentifier())){
            return;
        }

        FileManager.getInstance().requestFile(fileBean, host);
    }

    @Override
    public void onReceiveAskResource(String askResourceJson, String host) throws RemoteException {
        if (host.equals(MineInfoManager.getInstance().getHost())){
            return;
        }
        AskResourceBean bean = (AskResourceBean) GsonTools.formJson(askResourceJson,AskResourceBean.class);
        switch (bean.getResourceType()){
            case Config.RESOURCE_AVATAR:        //某个用户向我请求头像数据
                Login.notifyChangeAvatar(host);
                break;
            case Config.REQUEST_MONITOR:        //某个用户请求做我的监视者
                if (ContactManager.getInstance().watchMeNumber() < 4){
                    ContactManager.getInstance().addWatchMe(bean.getResourceUniqueIdentifier());
                }else {     //拒绝被他监视
                    SendResponse.sendMonitorResponse(Config.RESPONSE_REFUSE_MONITOR,
                            MineInfoManager.getInstance().getIdentifier(),
                            host);
                }
                break;
            case Config.REQUEST_BE_MONITOR:     //对方请求我监听他
                if (ContactManager.getInstance().watchNumber() < 4){       //我监视的人数小于4，同意
                    ContactManager.getInstance().addWatch(bean.getResourceUniqueIdentifier());
                }else {     //拒绝监视对方
                    SendResponse.sendMonitorResponse(Config.RESPONSE_REFUSE_BE_MONITOR,
                            MineInfoManager.getInstance().getIdentifier(),
                            host);
                }
                break;
            case Config.HEARTBEAT:      //收到对方发送的心跳
                //更新心跳
                ContactManager.getInstance().heartbeat(bean.getResourceUniqueIdentifier());
                break;
            case Config.REQUEST_HEARTBEAT:      //对方认为我假死，向我请求心跳
                SendRequest.sendHeartbeat(MineInfoManager.getInstance().getIdentifier(),host);
                //如果对方不在我的监视者列表中
                if (!ContactManager.getInstance().isWatch(bean.getResourceUniqueIdentifier())) {
                    if (ContactManager.getInstance().watchNumber() < 4){       //如果我的监视者小于4人
                        //接收对方为监视者
                        ContactManager.getInstance().addWatch(bean.getResourceUniqueIdentifier());
                    }else {
                        //否则拒绝对方成为我的监视者
                        SendResponse.sendMonitorResponse(Config.RESPONSE_REFUSE_BE_MONITOR,
                                MineInfoManager.getInstance().getIdentifier(),
                                host);
                    }
                }
                break;
            case Config.RESOURCE_FILE:
                FileManager.getInstance().receiveRequest(bean.getResourceUniqueIdentifier(), host);
                break;
        }
    }

    @Override
    public void onReceiveAndSaveFile(String sender,
                                     String receiver,
                                     String identifier,
                                     String path,
                                     String host) throws RemoteException {
    }

    @Override
    public void onReceiveFile(String receive,String host) throws RemoteException {
        ReceiveFileContentBean bean = (ReceiveFileContentBean) GsonTools.formJson(receive,ReceiveFileContentBean.class);
        FileManager.getInstance().receiveFile(bean,host);
    }

    @Override
    public void onReceiveVoiceCall(String userInfoJson, String host) throws RemoteException {
        TLog.d(TAG, "onReceiveVoiceCall: userInfoJson: " + userInfoJson + ",host: " + host);
//        Login.requestUserInfo(host);
        if (handleInfo != null){
            handleInfo.onReceiveVoiceCall((UserInfoBean) GsonTools.formJson(userInfoJson,UserInfoBean.class),host);
        }
    }

    @Override
    public void onReceiveVideoCall(String userInfoJson, String host) throws RemoteException {
//        Login.requestUserInfo(host);
        TLog.d(TAG, "onReceiveVideoCall: userInfoJson = " + userInfoJson + ", host = " + host);
        if (handleInfo != null){
            handleInfo.onReceiveVideoCall((UserInfoBean) GsonTools.formJson(userInfoJson,UserInfoBean.class),host);
        }
    }

    @Override
    public void onReceiveConsentVoiceCall(String host) throws RemoteException {
//        Login.requestUserInfo(host);
        if (handleVoiceCallResponse != null){
            handleVoiceCallResponse.onReceiveConsentVoiceCall(host);
        }
    }

    @Override
    public void onReceiveRefuseVoiceCall(String host) throws RemoteException {
//        Login.requestUserInfo(host);
        if (handleVoiceCallResponse != null){
            handleVoiceCallResponse.onReceiveRefuseVoiceCall(host);
        }
    }

    @Override
    public void onReceiveVoiceCallData(String callBeanJson) throws RemoteException {
        VoiceCallDataBean data = (VoiceCallDataBean) GsonTools.formJson(callBeanJson, VoiceCallDataBean.class);
        if (onReceiveCallBean != null){
            onReceiveCallBean.onReceiveVoiceCallData(data.getData());
        }
    }

    @Override
    public void onReceiveHungUpVoiceCall(String host) throws RemoteException {
        TLog.d(TAG, "onReceiveHungUpVoiceCall: " + host);
//        Login.requestUserInfo(host);
        if (hungUpInAnswer != null){
            hungUpInAnswer.onReceiveHungUpVoiceCall(host);
        }
    }

    @Override
    public void onEstablishCallConnect() throws RemoteException {
        if (onEstablishCallConnect != null){
            onEstablishCallConnect.onEstablishCallConnect();
        }
    }

    @Override
    public void onReceiveWaitingConsentCall() throws RemoteException {
        if(handleVoiceCallResponse != null){
            handleVoiceCallResponse.onReceiveWaitingConsentCall();
        }
    }

    @Override
    public void onReceiveConsentOutTime() throws RemoteException {
        if (handleVoiceCallResponse != null){
            handleVoiceCallResponse.onReceiveConsentOutTime();
        }
    }

    @Override
    public void onReceiveEstablishBeanJson(String establishBeanJson, String host) throws RemoteException {
        TLog.d(TAG, "onReceiveEstablishBeanJson: establishBeanJson = " + establishBeanJson + ", host = " + host);
        if (host.equals(MineInfoManager.getInstance().getHost())){
            return;
        }

        EstablishGroupBean establishGroupBean = (EstablishGroupBean) GsonTools.formJson(establishBeanJson,EstablishGroupBean.class);
        if (establishGroupBean != null){
            GroupManager.getInstance().receiveGroup(establishGroupBean,host);
        }
    }

    @Override
    public void onReceiveAskResourceNotFound(String askResourceJson, String host) throws RemoteException {
        TLog.d(TAG, "onReceiveAskResourceNotFound: askResourceJson = " + askResourceJson + ", host = " + host);
        if (host.equals(MineInfoManager.getInstance().getHost())){
            return;
        }
        AskResourceBean askResourceBean = (AskResourceBean) GsonTools.formJson(askResourceJson,AskResourceBean.class);
        if (askResourceBean.getResourceType() == Config.RESOURCE_AVATAR){
            Login.notifyChangeAvatar(host);
        }else if (askResourceBean.getResourceType() == Config.RESOURCE_GROUP){
            TLog.d(TAG, "onReceiveAskResource: ask resource group");
            new Thread(new Runnable() {
                @Override
                public void run() {
//                    GroupEntity groupEntity = MyDataBase.getInstance().getGroupDao().getGroupEntity(askResourceBean.getResourceUniqueIdentifier());
//                    EstablishGroupBean establishGroupBean = new EstablishGroupBean();
//                    List<UserInfoBean> members = new ArrayList<>();
//                    establishGroupBean.setmHolderIdentifier(groupEntity.getGroupHolder());
//                    establishGroupBean.setmGroupIdentifier(groupEntity.getGroupIdentifier());
//                    //填充群名字和头像
//                    ContactEntity next = IntranetChatApplication.sGroupContactMap.get(groupEntity.getGroupIdentifier());
//                    if (null != next){
//                        establishGroupBean.setmName(next.getName());
//                        establishGroupBean.setmGroupAvatarIdentifier(next.getAvatarIdentifier());
//                    }
//                    List<GroupMemberEntity> allGroupMember = MyDataBase.getInstance().getGroupMemberDao().getMember(groupEntity.getGroupIdentifier());
//                    Iterator<GroupMemberEntity> memberIterator = allGroupMember.iterator();
//                    //填充群成员信息
//                    while (memberIterator.hasNext()){
//                        GroupMemberEntity nextGroupMember = memberIterator.next();
//                        UserInfoBean userInfoBean = new UserInfoBean();
//                        userInfoBean.setIdentifier(nextGroupMember.getGroupMemberIdentifier());
//                        ContactEntity contactEntity = IntranetChatApplication.sGroupContactMap.get(nextGroupMember.getGroupMemberIdentifier());
//                        if (null != contactEntity){
//                            userInfoBean.setStatus(1);
//                            userInfoBean.setName(contactEntity.getName());
//                            userInfoBean.setAvatarIdentifier(contactEntity.getAvatarIdentifier());
//                        }
//                        members.add(userInfoBean);
//                    }
//                    establishGroupBean.setmUsers(members);
//                    EstablishGroup.establishGroup(establishGroupBean,host);
                }
            }).start();
        }
    }

    @Override
    public void onReceiveInCall(String host) throws RemoteException {
        TLog.d(TAG, "onReceiveInCall: host = " + host);
        if (handleVoiceCallResponse != null){
            handleVoiceCallResponse.onReceiveInCall(host);
        }
        EventBus.getDefault().post(new InCallBean(true));
    }

    @Override
    public void notifyClearQueue() throws RemoteException {
        TLog.d(TAG, "notifyClearQueue: size = " + IntranetChatApplication.getmDatasQueue().size());
        IntranetChatApplication.getmDatasQueue().clear();
    }

    /**
     * 记录开始下载文件*/
    @Override
    public void askFile(String identifier, String path) throws RemoteException {
        TLog.d(TAG, "askFile: identifier = " + identifier + ", path = " + path);
//        setMonitorReceiveFile(identifier,path,Config.STEP_ASK_FILE);
    }

    /**
     * 记录文件下载失败*/
    @Override
    public void receiveFileFailure(String identifier) throws RemoteException {
        TLog.d(TAG, "askFile: identifier = " + identifier);
//        setMonitorReceiveFile(identifier,null,Config.STEP_FAILURE);
    }

    @Override
    public void receiveMonitorResponse(int monitorResponse, String identifier) throws RemoteException {
        ContactManager manager = ContactManager.getInstance();
        if (monitorResponse == Config.RESPONSE_REFUSE_MONITOR &&
                manager.isWatch(identifier)){
            manager.removeWatch(identifier);
        }else if (monitorResponse == Config.RESPONSE_REFUSE_BE_MONITOR &&
                manager.isWatchMe(identifier)){
            manager.removeWatchMe(identifier);
        }
    }

    @Override
    public void receiveResponse(String response, String host) throws RemoteException {
        ResponseBean bean = (ResponseBean) GsonTools.formJson(response,ResponseBean.class);
        switch (bean.getCode()) {
            case Config.STEP_SUCCESS:
                TLog.d(TAG,"STEP SUCCESS");
                break;
            case Config.STEP_FAILURE:
                TLog.d(TAG,"STEP FAILURE");
                break;
        }
    }

    @Override
    public void receiveUserOutLine(String identifier) throws RemoteException {
        ContactManager.getInstance().contactOutLine(identifier);
    }
}
