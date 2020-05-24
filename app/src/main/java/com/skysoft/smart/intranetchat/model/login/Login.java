/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/18
 * Description:  [Communication]Document information collection
 */
package com.skysoft.smart.intranetchat.model.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.skysoft.smart.intranetchat.MainActivity;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.database.table.RefuseGroupEntity;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.net_model.SendFile;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.AskBean;
import com.skysoft.smart.intranetchat.model.network.bean.FileBean;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.server.IntranetChatServer;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.ui.activity.login.LoginActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class Login {
    private static final String TAG = "Login";

    public static TimerTask start(Activity activity) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                MineInfoManager infoManager = MineInfoManager.getInstance().init();
                //已经注册，进入主页面
                if (!TextUtils.isEmpty(infoManager.getIdentifier())){
                    Login.login(MineInfoManager.getInstance().getUserInfo());
                    MainActivity.go(activity);
                    activity.finish();
                    return;
                }
                //注册
                Intent login = new Intent(activity, LoginActivity.class);
                activity.startActivity(login);
                activity.finish();
            }
        };
        return timerTask;
    }

    public static void register(Context context, String userName, String avatarPath) {
        Identifier identifier = Identifier.getInstance();
        UserInfoBean userInfo = new UserInfoBean();
        userInfo.setIdentifier(identifier.getSerialNumber(context));
        userInfo.setName(userName);
        userInfo.setAvatarIdentifier(
                TextUtils.isEmpty(avatarPath) ?
                        identifier.getDefaultAvatarIdentifier() :
                        identifier.getFileIdentifier(avatarPath));
        userInfo.setStatus(Config.STATUS_ONLINE);
        login(userInfo);

        MineInfoManager infoManager = MineInfoManager.getInstance();
        infoManager.setName(userName);
        infoManager.setAvatar(avatarPath);
        infoManager.setIdentifier(userInfo.getIdentifier());
        infoManager.setStatus(Config.STATUS_ONLINE);
    }

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
            UserInfoBean userInfoBean = MineInfoManager.getInstance().getUserInfo();
            IntranetChatApplication.sAidlInterface.broadcastUserInfo(GsonTools.toJson(userInfoBean));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastChangeAvatar(){
        try {
            SendFile sendFile = new SendFile();
            String path = MineInfoManager.getInstance().getAvatarPath();
            UserInfoBean mineUserInfo = MineInfoManager.getInstance().getUserInfo();
            FileBean fileBean = sendFile.generatorFileBean(new File(path), mineUserInfo.getAvatarIdentifier(), mineUserInfo.getIdentifier(), mineUserInfo.getIdentifier(), Config.FILE_AVATAR);
            mineUserInfo.setRemark(fileBean.getMd5());
            IntranetChatApplication.
                    sAidlInterface.
                    addResourceManagerBean(
                            GsonTools.toJson(fileBean),
                            path,
                            true,
                            false);
            IntranetChatApplication.
                    sAidlInterface.
                    broadcastUserInfo(
                            GsonTools.toJson(mineUserInfo));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void notifyChangeAvatar(String host){
        try {
            SendFile sendFile = new SendFile();
            String path = MineInfoManager.getInstance().getAvatarPath();
            UserInfoBean mineUserInfo = MineInfoManager.getInstance().getUserInfo();
            FileBean fileBean = sendFile.
                    generatorFileBean(
                            new File(path),
                            mineUserInfo.getAvatarIdentifier(),
                            mineUserInfo.getIdentifier(),
                            mineUserInfo.getIdentifier(),
                            Config.FILE_AVATAR);

            mineUserInfo.setRemark(fileBean.getMd5());
            IntranetChatApplication.
                    sAidlInterface.
                    addResourceManagerBean(
                            GsonTools.toJson(fileBean),
                            path,
                            true,
                            false);
            IntranetChatApplication.
                    sAidlInterface.
                    sendUserInfo(
                            GsonTools.toJson(
                                    MineInfoManager.getInstance().getUserInfo()),host);
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

    public static void broadcastUserOutLine(String identifier){
        try {
            IntranetChatApplication.sAidlInterface.broadcastUserOutLine(identifier);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
