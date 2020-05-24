/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-48][Intranet Chat] [APP][Database] Create Database
 */
package com.skysoft.smart.intranetchat.database.table;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "latest")
public class LatestEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    //0为不置顶，1为置顶。
    @ColumnInfo
    private int top = 0;    //是否置顶
    @ColumnInfo
    private int user;     //消息发送者，个人/群组
    @ColumnInfo
    private int group;      //0:contact, 1:group
    @ColumnInfo
    private long time;      //时间戳
    @ColumnInfo
    private String content; //内容
    @ColumnInfo(name = "un_read_number")
    private int unReadNumber;   //未读消息数

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getUnReadNumber() {
        return unReadNumber;
    }

    public void setUnReadNumber(int unReadNumber) {
        this.unReadNumber = unReadNumber;
    }

    public void addUnReadNumber() {
        this.unReadNumber += 1;
    }

    @Override
    public String toString() {
        return "LatestEntity{" +
                "id=" + id +
                ", top=" + top +
                ", user=" + user +
                ", group=" + group +
                ", time=" + time +
                ", content='" + content + '\'' +
                ", unReadNumber=" + unReadNumber +
                '}';
    }
}
