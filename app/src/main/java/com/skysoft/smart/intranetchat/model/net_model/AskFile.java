/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication]Create a file service for file recipients to download files.
 */
package com.skysoft.smart.intranetchat.model.net_model;

import android.os.RemoteException;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.AskResourceBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

public class AskFile {
    private static final String TAG = "AskFile";

    public static void askFile(String identifier,String host){
        TLog.d(TAG,"---------Ask File-------" + identifier);
        AskResourceBean askResourceBean = new AskResourceBean();
        askResourceBean.setResourceUniqueIdentifier(identifier);
        askResourceBean.setResourceType(Config.RESOURCE_FILE);
        try {
            IntranetChatApplication.sAidlInterface.askResource(GsonTools.toJson(askResourceBean),host);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
