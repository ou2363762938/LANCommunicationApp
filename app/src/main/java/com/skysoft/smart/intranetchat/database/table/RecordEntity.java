/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-48][Intranet Chat] [APP][Database] Create Database
 */
package com.skysoft.smart.intranetchat.database.table;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "record")
public class RecordEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo
    private int receiver;
    @ColumnInfo
    private int sender;
    @ColumnInfo(name = "group_")
    private int group;      //1:group   0:one
    @ColumnInfo
    private int type;      //0:文字,1:文件,2:Time
    @ColumnInfo
    private long time;
    @ColumnInfo
    private String file;
    @ColumnInfo
    private String content;
    @Ignore
    private FileEntity fileEntity;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReceiver() {
        return receiver;
    }

    public void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public FileEntity getFileEntity() {
        return fileEntity;
    }

    public void setFileEntity(FileEntity fileEntity) {
        this.fileEntity = fileEntity;
    }

    @Override
    public String toString() {
        return "RecordEntity{" +
                "id=" + id +
                ", receiver=" + receiver +
                ", sender=" + sender +
                ", time=" + time +
                ", file='" + file + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
