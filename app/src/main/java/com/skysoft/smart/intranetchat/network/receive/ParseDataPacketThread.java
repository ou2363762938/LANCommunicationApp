/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication] Create a new process service with message sending and receiving.
 */
package com.skysoft.smart.intranetchat.network.receive;

import android.util.Log;

import com.skysoft.smart.intranetchat.network.bean.EstablishGroupBean;
import com.skysoft.smart.intranetchat.network.bean.ResponseBean;
import com.skysoft.smart.intranetchat.network.manager.FileWaitToSend;
import com.skysoft.smart.intranetchat.network.manager.PathManager;
import com.skysoft.smart.intranetchat.network.manager.ResourceManager;
import com.skysoft.smart.intranetchat.network.Config;
import com.skysoft.smart.intranetchat.network.bean.AskBean;
import com.skysoft.smart.intranetchat.network.bean.AskResourceBean;
import com.skysoft.smart.intranetchat.network.bean.DataPacketBean;
import com.skysoft.smart.intranetchat.network.bean.FileBean;
import com.skysoft.smart.intranetchat.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.network.bean.ResourceManagerBean;
import com.skysoft.smart.intranetchat.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.network.manager.ResponseSender;
import com.skysoft.smart.intranetchat.network.manager.Sender;
import com.skysoft.smart.intranetchat.network.manager.SocketManager;
import com.skysoft.smart.intranetchat.server.IntranetChatAidl;
import com.skysoft.smart.intranetchat.tools.GsonTools;

public class ParseDataPacketThread extends Thread {
    private static final String TAG = ParseDataPacketThread.class.getSimpleName() +",   "+ Thread.currentThread().getName();

    private DataPacketBean dataPacketBean;
    private String host;

    public ParseDataPacketThread(String data, String host){
        this.dataPacketBean = (DataPacketBean) GsonTools.formJson(data,DataPacketBean.class);
        this.host = host;
    }

    @Override
    public void run() {
        super.run();
        parseDataPacketBean(dataPacketBean,host);
    }

    /*解析收数据包，提取数据包中的数据*/
    private void parseDataPacketBean(DataPacketBean dataPacketBean,String host){
        if (dataPacketBean == null){
            Log.e(TAG, "parseDataPacketBean: can not parse null data packet" );
            return;
        }
        switch (dataPacketBean.getCode()){
            case Config.CODE_MESSAGE:
                /*receive message*/
                onReceiveMessageBean(dataPacketBean,host);
                break;
            case Config.CODE_USERINFO:
                /*receive userInfo*/
                onReceiveUserInfoBean(dataPacketBean,host);
                break;
            case Config.CODE_REQUEST:
                /*receive request*/
                onReceiveRequestBean(dataPacketBean,host);
                break;
            case Config.CODE_FILE:
                /*receive file*/
                onReceiveFileBean(dataPacketBean,host);
                break;
            case Config.CODE_ASK_RESOURCE:
                /*receive ask resource*/
                onReceiveAskResourceBean(dataPacketBean,host);
                break;
            case Config.CODE_RESPONSE:
                OnReceiveResponseBean onReceiveResponseBean = new OnReceiveResponseBean();
                onReceiveResponseBean.onReceiveResponseBean(dataPacketBean,host);
                break;
            case Config.CODE_VOICE_CALL:
                onReceiveVoiceCall(dataPacketBean,host);
                break;
            case Config.CODE_VIDEO_CALL:
                onReceiveVideoCall(dataPacketBean,host);
                break;
            case Config.CODE_ESTABLISH_GROUP:
                onReceiveEstablishGroupBean(dataPacketBean,host);
                break;
        }
    }

    /*当收到建群通知*/
    private void onReceiveEstablishGroupBean(DataPacketBean dataPacketBean, String host) {
        EstablishGroupBean establishGroupBean = (EstablishGroupBean) GsonTools.formJson(dataPacketBean.getData(), EstablishGroupBean.class);
        if (establishGroupBean == null){
            return;
        }
        MonitorUdpReceivePortThread.broadcastReceive(Config.CODE_ESTABLISH_GROUP,dataPacketBean.getData(),host);
    }

    private void onReceiveVideoCall(DataPacketBean dataPacketBean, String host) {
        Log.d(TAG, "onReceiveVideoCall: " + host);
        UserInfoBean userInfoBean = (UserInfoBean) GsonTools.formJson(dataPacketBean.getData(),UserInfoBean.class);
        if (userInfoBean == null || userInfoBean.getIdentifier().equals(IntranetChatAidl.getUserInfoBean().getIdentifier())){
            return;
        }
        MonitorUdpReceivePortThread.broadcastReceive(Config.CODE_VIDEO_CALL,dataPacketBean.getData(),host);
    }

    private void onReceiveVoiceCall(DataPacketBean dataPacketBean, String host) {
        Log.d(TAG, "onReceiveVoiceCall: " + host);
        UserInfoBean userInfoBean = (UserInfoBean) GsonTools.formJson(dataPacketBean.getData(),UserInfoBean.class);
        if (userInfoBean == null){
            return;
        }
        MonitorUdpReceivePortThread.broadcastReceive(Config.CODE_VOICE_CALL,dataPacketBean.getData(),host);
    }

    /*当从数据包获取到FileBean时的处理方法*/
    private void onReceiveFileBean(DataPacketBean dataPacketBean, String host) {
        Log.d(TAG, "onReceiveFileBean: " + host);
        FileBean fileBean = (FileBean) GsonTools.formJson(dataPacketBean.getData(), FileBean.class);
        if (fileBean == null || fileBean.getSender().equals(IntranetChatAidl.getUserInfoBean().getIdentifier())){
            return;
        }

        MonitorUdpReceivePortThread.broadcastReceive(Config.CODE_FILE,dataPacketBean.getData(),host);

        //生成文件的存储目录
        String path = PathManager.fromType(fileBean.getType());
        ResourceManagerBean resourceManagerBean = new ResourceManagerBean(fileBean,path);
        resourceManagerBean.setReceive(true);
        ResourceManager.getInstance().setResource(fileBean.getFileUniqueIdentifier(),resourceManagerBean);

        //只要不是普通文件(图片，语音，视频)，直接开始请求资源
        if (fileBean.getType() != Config.FILE_COMMON){
            AskResourceBean askResourceBean = new AskResourceBean();
            askResourceBean.setResourceType(Config.RESOURCE_FILE);
            askResourceBean.setResourceUniqueIdentifier(fileBean.getFileUniqueIdentifier());
            Sender.sender(GsonTools.toJson(askResourceBean),Config.CODE_ASK_RESOURCE,host);
        }
    }

    /*当从数据包获取到AskResourceBean时的处理方法*/
    private void onReceiveAskResourceBean(DataPacketBean dataPacketBean, String host) {
        Log.d(TAG, "onReceiveAskResourceBean: " + host);
        AskResourceBean askResourceBean = (AskResourceBean) GsonTools.formJson(dataPacketBean.getData(),AskResourceBean.class);
        if (askResourceBean == null){
            return;
        }

        Log.d(TAG, "onReceiveAskResourceBean: found resource");
        /*没有找到资源*/
        if(!ResourceManager.getInstance().exist(askResourceBean.getResourceUniqueIdentifier())){
            ResponseSender.response(Config.RESPONSE_NOT_REQUEST_THIS_RESOURCE,askResourceBean.getResourceUniqueIdentifier(),host);
            Log.d(TAG, "onReceiveAskResourceBean: not found resource");
            MonitorUdpReceivePortThread.broadcastReceive(Config.NOT_FOUND_RESOURCE,dataPacketBean.getData(),host);
            return;
        }

        Log.d(TAG, "onReceiveAskResourceBean: busy");
        /*设备忙碌*/
        if (SocketManager.getInstance().getSocketNumber() == SocketManager.MAX_NUMBER){
            ResourceManagerBean resource = ResourceManager.getInstance().getResource(askResourceBean.getResourceUniqueIdentifier());
            if (resource.getDownLoadNum() != 0){
                //已经有人下载成功
                String transpondHost = resource.getDownLoadList();
                ResponseSender.response(Config.RESPONSE_BUSY_TRANSPOND,askResourceBean.getResourceUniqueIdentifier(),host,transpondHost);
            }else {
                //暂时还没有人下载成功
                ResponseSender.response(Config.RESPONSE_BUSY,askResourceBean.getResourceUniqueIdentifier(),host);
            }
            return;
        }

        Log.d(TAG, "onReceiveAskResourceBean: broadcast");
        MonitorUdpReceivePortThread.broadcastReceive(Config.CODE_ASK_RESOURCE,dataPacketBean.getData(),host);

        Log.d(TAG, "onReceiveAskResourceBean: add to wait");
        //添加资源到待发送列表
        FileWaitToSend.getInstance().setWaitToSend(host,askResourceBean);

        Log.d(TAG, "onReceiveAskResourceBean: response");
        //通知对方向我请求
        ResponseSender.response(Config.RESPONSE_RESOURCE_OK,askResourceBean.getResourceUniqueIdentifier(),host);
    }

    /*当从数据包获取到RequestBean时的处理方法*/
    private void onReceiveRequestBean(DataPacketBean dataPacketBean, String host) {

        AskBean askBean = (AskBean) GsonTools.formJson( dataPacketBean.getData(), AskBean.class);
        Log.d(TAG, "onReceiveRequestBean: " + askBean.getRequestType() + ",   " + host);
        if (askBean == null){
            return;
        }

        if (askBean.getRequestType() == Config.REQUEST_CONSENT_CALL){
            ResponseBean responseBean = new ResponseBean(Config.RESPONSE_WAITING_CONSENT,null);
            Sender.sender(GsonTools.toJson(responseBean),Config.CODE_RESPONSE,host);
        }
        MonitorUdpReceivePortThread.broadcastReceive(Config.CODE_REQUEST,dataPacketBean.getData(),host);
    }

    /*当从数据包获取到MessageBean时的处理方法*/
    private void onReceiveMessageBean(DataPacketBean dataPacketBean, String host){
        Log.d(TAG, "onReceiveMessageBean: " + host);
        MessageBean messageBean = (MessageBean) GsonTools.formJson(dataPacketBean.getData(),MessageBean.class);
        if (messageBean == null || messageBean.getSender().equals(IntranetChatAidl.getUserInfoBean().getIdentifier())){
            return;
        }
        MonitorUdpReceivePortThread.broadcastReceive(Config.CODE_MESSAGE,dataPacketBean.getData(),host);
    }

    /*当从数据包获取到UserInfoBean时的处理方法*/
    private void onReceiveUserInfoBean(DataPacketBean dataPacketBean, String host){
        Log.d(TAG, "onReceiveUserInfoBean: " + host);
        UserInfoBean userInfoBean = (UserInfoBean) GsonTools.formJson(dataPacketBean.getData(),UserInfoBean.class);
        if (userInfoBean == null || userInfoBean.getIdentifier().equals(IntranetChatAidl.getUserInfoBean().getIdentifier())){
            return;
        }
        MonitorUdpReceivePortThread.broadcastReceive(Config.CODE_USERINFO,dataPacketBean.getData(),host);
    }
}