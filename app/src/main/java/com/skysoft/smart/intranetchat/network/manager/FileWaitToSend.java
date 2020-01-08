/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication]Create a file service for file recipients to download files.
 */
package com.skysoft.smart.intranetchat.network.manager;

import com.skysoft.smart.intranetchat.network.bean.AskResourceBean;
import com.skysoft.smart.intranetchat.network.bean.ResourceManagerBean;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FileWaitToSend {
    private static Map<String , String> waitToSend = new HashMap<String, String>();
    private FileWaitToSend(){}
    private static FileWaitToSend sInstance = new FileWaitToSend();
    public static FileWaitToSend getInstance(){
        return sInstance;
    }

    public ResourceManagerBean getWaitToSend(String host){
        if (!waitToSend.containsKey(host)){
            return null;
        }
        String identifier = waitToSend.get(host);
        waitToSend.remove(host);
        return ResourceManager.getInstance().getResource(identifier);
    }

    public void setWaitToSend(String host, AskResourceBean askResourceBean){
        if (waitToSend.containsKey(host)){
            waitToSend.remove(host);
        }
        waitToSend.put(host,askResourceBean.getResourceUniqueIdentifier());
    }

    public void remove(String host){
        waitToSend.remove(host);
    }
}
