/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication]Create a file service for file recipients to download files.
 */
package com.skysoft.smart.intranetchat.app;

import android.os.RemoteException;

import com.skysoft.smart.intranetchat.network.Config;
import com.skysoft.smart.intranetchat.network.bean.AskResourceBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;

public class AskFile {
    public void askFile(String identifier,String host){
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
