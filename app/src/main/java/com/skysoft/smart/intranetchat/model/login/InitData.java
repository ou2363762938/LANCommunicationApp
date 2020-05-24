package com.skysoft.smart.intranetchat.model.login;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.AvatarEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.database.table.RefuseGroupEntity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.group.GroupManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.server.IntranetChatServer;

import java.util.List;

public class InitData extends Thread {
    private static final String TAG = "InitData";

    @Override
    public void run() {
        super.run();
        MyDataBase dataBase = MyDataBase.getInstance();
        List<LatestEntity> latestList = dataBase.getLatestDao().getAllHistory();
        List<ContactEntity> contactList = dataBase.getContactDao().getAllContact();
        List<GroupEntity> groupList = dataBase.getGroupDao().getAllGroup();
        List<RefuseGroupEntity> refuseList = dataBase.getRefuseGroupDao().getAll();
        List<AvatarEntity> avatarList = dataBase.getAvatarDao().getAllAvatar();

        LatestManager.getInstance().initLatestMap(latestList);
        ContactManager.getInstance().initContactMap(contactList);
        GroupManager.getInstance().initGroup(groupList);
        GroupManager.getInstance().initRefuse(refuseList);
        AvatarManager.getInstance().initAvatarMap(avatarList);
    }
}
