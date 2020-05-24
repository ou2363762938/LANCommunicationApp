/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/11/4
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.database.table;
//B:[Intranet Chat] [APP][UI] Chat Room,Oliver Ou,2019/11/4
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.skysoft.smart.intranetchat.model.network.bean.FileBean;

@Entity(tableName = "file",
        indices = @Index(value = {"rid"},unique = true))
public class FileEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo
    private int type;       //Video:40,Voice:41,picture:42,common:43,avatar:44
    @ColumnInfo(name = "content_length")
    private int contentLength;
    @ColumnInfo(name = "file_length")
    private long fileLength;
    @ColumnInfo
    private long time;
    @ColumnInfo
    private String name;
    @ColumnInfo
    @NonNull
    private String rid;
    @ColumnInfo
    private String path;
    @ColumnInfo
    private String thumbnail;
    @ColumnInfo
    private String md5;
    @ColumnInfo
    private int step;       // 0:please requestFile file
                            // 1:requestFile file
                            // 2:notify file
                            // 3:requestFile file success
                            // 4:notify file success

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    public String getRid() {
        return rid;
    }

    public void setRid(@NonNull String rid) {
        this.rid = rid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void fromFileBean(FileBean bean) {
        rid = bean.getRid();
        name = bean.getName();
        md5 = bean.getMd5();
        fileLength = bean.getFileLength();
        contentLength = bean.getContentLength();
        type = bean.getType();
    }

    @Override
    public String toString() {
        return "FileEntity{" +
                "id=" + id +
                ", type=" + type +
                ", contentLength=" + contentLength +
                ", fileLength=" + fileLength +
                ", time=" + time +
                ", name='" + name + '\'' +
                ", rid='" + rid + '\'' +
                ", path='" + path + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }
}
