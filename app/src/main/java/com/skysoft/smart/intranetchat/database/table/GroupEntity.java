/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/11/6
 * Description: [Intranet Chat] [APP][UI] Group chat
 */
package com.skysoft.smart.intranetchat.database.table;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "_group",
        indices = @Index(value = "identifier",unique = true))
public class GroupEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo
    private int avatar = -1;
    @ColumnInfo
    private String name;
    @ColumnInfo(name = "identifier")
    @NonNull
    private String identifier;
    @ColumnInfo
    private String holder;

    @Ignore
    private int status = 3;
    @Ignore
    private String host = "255.255.255.255";
    @Ignore
    private boolean check = false;
    @Ignore
    private int notifyId;
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

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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
        return "GroupEntity{" +
                "id=" + id +
                ", avatar=" + avatar +
                ", name='" + name + '\'' +
                ", identifier='" + identifier + '\'' +
                ", holder='" + holder + '\'' +
                ", status=" + status +
                ", host='" + host + '\'' +
                ", check=" + check +
                ", notifyId=" + notifyId +
                ", showCheck=" + showCheck +
                '}';
    }
}
