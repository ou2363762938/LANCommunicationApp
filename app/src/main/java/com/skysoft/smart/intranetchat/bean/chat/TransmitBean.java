package com.skysoft.smart.intranetchat.bean.chat;

/**
 * 转发消息时记载当时最近聊天*/
public class TransmitBean {
    private int user;
    private int avatar;
    private String name;
    private boolean group;
    private String host;

    public TransmitBean(int user, boolean group) {
        this.user = user;
        this.group = group;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
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

    @Override
    public String toString() {
        return "TransmitBean{" +
                "user=" + user +
                ", avatar=" + avatar +
                ", name='" + name + '\'' +
                ", group=" + group +
                ", host='" + host + '\'' +
                '}';
    }
}
