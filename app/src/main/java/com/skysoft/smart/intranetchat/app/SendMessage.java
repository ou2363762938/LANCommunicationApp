/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/11/1
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.app;

import android.os.RemoteException;
import android.util.Log;

import com.skysoft.smart.intranetchat.database.table.GroupMemberEntity;
import com.skysoft.smart.intranetchat.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;

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
}
