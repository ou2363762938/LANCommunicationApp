/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/18
 * Description:  [Communication]Document information collection
 */
package com.skysoft.smart.intranetchat.app;

import android.os.RemoteException;
import android.util.Log;

import com.skysoft.smart.intranetchat.network.Config;
import com.skysoft.smart.intranetchat.network.bean.AskBean;
import com.skysoft.smart.intranetchat.network.bean.FileBean;
import com.skysoft.smart.intranetchat.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.server.IntranetChatAidl;
import com.skysoft.smart.intranetchat.tools.GsonTools;

import java.io.File;

public class Login {
    public static void login(UserInfoBean userInfoBean) {
        try {
            //广播登录
            IntranetChatApplication.sAidlInterface.broadcastUserInfo(GsonTools.toJson(userInfoBean));
            AskBean askBean = new AskBean();
            askBean.setRequestType(Config.REQUEST_USERINFO);
            //请求他人的用户信息
            IntranetChatApplication.sAidlInterface.requestAllUserInfo(GsonTools.toJson(askBean));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastUserInfo(){
        try {
            IntranetChatApplication.sAidlInterface.broadcastUserInfo(GsonTools.toJson(IntranetChatApplication.getsMineUserInfo()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastChangeAvatar(){
        try {
            SendFile sendFile = new SendFile();
            String path = IntranetChatApplication.getsMineAvatarPath();
            UserInfoBean mineUserInfo = IntranetChatApplication.getsMineUserInfo();
            FileBean fileBean = sendFile.generatorFileBean(new File(path), mineUserInfo.getAvatarIdentifier(), mineUserInfo.getIdentifier(), mineUserInfo.getIdentifier(), Config.FILE_AVATAR);
            IntranetChatApplication.getsMineUserInfo().setRemark(fileBean.getMd5());
            IntranetChatApplication.sAidlInterface.addResourceManagerBean(GsonTools.toJson(fileBean),path,true,false);
            IntranetChatApplication.sAidlInterface.broadcastUserInfo(GsonTools.toJson(IntranetChatApplication.getsMineUserInfo()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void notifyChangeAvatar(String host){
        try {
            SendFile sendFile = new SendFile();
            String path = IntranetChatApplication.getsMineAvatarPath();
            UserInfoBean mineUserInfo = IntranetChatApplication.getsMineUserInfo();
            FileBean fileBean = sendFile.generatorFileBean(new File(path), mineUserInfo.getAvatarIdentifier(), mineUserInfo.getIdentifier(), mineUserInfo.getIdentifier(), Config.FILE_AVATAR);
            IntranetChatApplication.getsMineUserInfo().setRemark(fileBean.getMd5());
            IntranetChatApplication.sAidlInterface.addResourceManagerBean(GsonTools.toJson(fileBean),path,true,false);
            IntranetChatApplication.sAidlInterface.sendUserInfo(GsonTools.toJson(IntranetChatApplication.getsMineUserInfo()),host);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void requestUserInfo(String host){
        AskBean askBean = new AskBean();
        askBean.setRequestType(Config.REQUEST_USERINFO);
        try {
            IntranetChatApplication.sAidlInterface.requestUserInfo(GsonTools.toJson(askBean),host);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void hostChanged(String host){
        try {
            IntranetChatApplication.sAidlInterface.hostChanged(host);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
