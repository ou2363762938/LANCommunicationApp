/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-48][Intranet Chat] [APP][Database] Create Database
 */
package com.skysoft.smart.intranetchat.database;

import android.content.Context;

import com.skysoft.smart.intranetchat.database.dao.AvatarDao;
import com.skysoft.smart.intranetchat.database.dao.LatestDao;
import com.skysoft.smart.intranetchat.database.dao.RecordDao;
import com.skysoft.smart.intranetchat.database.table.AvatarEntity;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import com.skysoft.smart.intranetchat.database.dao.ContactDao;
import com.skysoft.smart.intranetchat.database.dao.FileDao;
import com.skysoft.smart.intranetchat.database.dao.GroupDao;
import com.skysoft.smart.intranetchat.database.dao.GroupMemberDao;
import com.skysoft.smart.intranetchat.database.dao.RefuseGroupDao;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.GroupMemberEntity;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.database.table.RefuseGroupEntity;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {RecordEntity.class, AvatarEntity.class,
        ContactEntity.class, GroupEntity.class, GroupMemberEntity.class,
        LatestEntity.class, FileEntity.class, RefuseGroupEntity.class},
        version = 1,exportSchema = false)
public abstract class MyDataBase extends RoomDatabase {
    private static final String DB_NAME = "chat.db";
    public MyDataBase(){}
    public static MyDataBase sInstance;
    public static void init(Context context){
        TLog.d("MyDatabase", "init: ");
        sInstance = Room.databaseBuilder(context, MyDataBase.class,DB_NAME).build();
    }

    public static MyDataBase getInstance(){
        if (sInstance == null){
            throw new NullPointerException("sInstance == null");
        }
        return sInstance;
    }

    public abstract AvatarDao getAvatarDao();
    public abstract RecordDao getRecordDao();
    public abstract ContactDao getContactDao();
    public abstract LatestDao getLatestDao();
    public abstract FileDao getFileDao();
    public abstract GroupDao getGroupDao();
    public abstract GroupMemberDao getGroupMemberDao();
    public abstract RefuseGroupDao getRefuseGroupDao();
}
