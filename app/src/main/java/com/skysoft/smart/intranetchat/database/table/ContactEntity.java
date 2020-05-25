/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-48][Intranet Chat] [APP][Database] Create Database
 */
package com.skysoft.smart.intranetchat.database.table;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "contact",
        indices = {@Index(value = "identifier",unique = true)})
public class ContactEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo
    private int avatar = -1;
    @ColumnInfo
    private String name;
    @ColumnInfo(name = "identifier")
    @NonNull
    private String identifier;

    @Ignore
    private int status = 3;
    @Ignore
    private int notifyId;
    @Ignore
    private long heartbeat = -1;
    @Ignore
    private String host;
    @Ignore
    private boolean check = false;
    @Ignore
    private boolean showCheck = true;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAvatar() {
        return avatar;
    }

    public void setAvatar(int avatar) {
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(@NonNull String identifier) {
        this.identifier = identifier;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(long heartbeat) {
        this.heartbeat = heartbeat;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isCheck() {
        return check;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    public int getNotifyId() {
        return notifyId;
    }

    public void setNotifyId(int notifyId) {
        this.notifyId = notifyId;
    }

    public boolean isShowCheck() {
        return showCheck;
    }

    public void setShowCheck(boolean showCheck) {
        this.showCheck = showCheck;
    }

    @Override
    public String toString() {
        return "ContactEntity{" +
                "id=" + id +
                ", avatar=" + avatar +
                ", name='" + name + '\'' +
                ", identifier='" + identifier + '\'' +
                ", status=" + status +
                ", notifyId=" + notifyId +
                ", heartbeat=" + heartbeat +
                ", host='" + host + '\'' +
                ", check=" + check +
                ", showCheck=" + showCheck +
                '}';
    }
}
