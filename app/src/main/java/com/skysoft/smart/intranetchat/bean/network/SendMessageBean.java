package com.skysoft.smart.intranetchat.bean.network;

import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;

/**封装发送消息时需要的数据*/
public class SendMessageBean {
    private String message;
    private String receiver;
    private String host;
    private String avatar;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String mName) {
        this.name = mName;
    }

    private boolean isGroup;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReciever() {
        return receiver;
    }

    public void setReciever(String reciever) {
        this.receiver = reciever;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public SendMessageBean(String message, String receiver, String host, String avatar, String name, boolean isGroup) {
        this.message = message;
        this.receiver = receiver;
        this.host = host;
        this.avatar = avatar;
        this.name = name;
        this.isGroup = isGroup;
    }

    public SendMessageBean(ContactEntity contact, String message) {
        this(message,
                contact.getIdentifier(),
                contact.getHost(),
                AvatarManager.getInstance().getAvatar(contact.getAvatar()),
                contact.getName(),
                false);
    }

    public SendMessageBean(GroupEntity group, String message) {
        this(message,
                group.getIdentifier(),
                "255.255.255.255",
                AvatarManager.getInstance().getDefaultAvatar(),
                group.getName(),
                true);
    }
}
