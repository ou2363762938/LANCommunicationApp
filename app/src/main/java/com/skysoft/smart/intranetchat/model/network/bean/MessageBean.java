/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication] Create a new process service with message sending and receiving.
 */
package com.skysoft.smart.intranetchat.model.network.bean;

public class MessageBean{
    private String sender;
    private String receiver;
    private String msg;
    private long timeStamp;

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
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

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "MessageBean{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", msg='" + msg + '\'' +
                ", timeStamp=" + timeStamp +
                '}';
    }
}
