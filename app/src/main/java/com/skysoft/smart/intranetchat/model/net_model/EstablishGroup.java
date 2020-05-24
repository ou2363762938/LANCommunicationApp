package com.skysoft.smart.intranetchat.model.net_model;

import android.os.RemoteException;

import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.GroupMemberEntity;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.EstablishGroupBean;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EstablishGroup {

    private static String TAG = EstablishGroup.class.getSimpleName();
    public static void establishGroup(EstablishGroupBean establishGroupBean,List<String> hostList){
        try {
            IntranetChatApplication.sAidlInterface.establishGroup(GsonTools.toJson(establishGroupBean),hostList);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void establishGroup(EstablishGroupBean establishGroupBean,String host){
        List<String> hostList = new ArrayList<>();
        hostList.add(host);
        establishGroup(establishGroupBean,hostList);
    }
}
