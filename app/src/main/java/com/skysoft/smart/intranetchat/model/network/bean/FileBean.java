/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description:[Intranet Chat] [APP] [Communication]File message sending and receiving (excluding file data transfer).
 */
package com.skysoft.smart.intranetchat.model.network.bean;

public class FileBean {
    private String rid;
    private String name;
    private String md5;
    private String sender;
    private String receiver;
    private long fileLength;
    private int contentLength;
    private int type;

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "FileBean{" +
                "rid='" + rid + '\'' +
                ", name='" + name + '\'' +
                ", md5='" + md5 + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", fileLength=" + fileLength +
                ", contentLength=" + contentLength +
                ", type=" + type +
                '}';
    }
}
