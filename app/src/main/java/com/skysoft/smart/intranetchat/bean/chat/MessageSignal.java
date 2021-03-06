package com.skysoft.smart.intranetchat.bean.chat;

import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;

public class MessageSignal {
    private boolean isIn;
    private boolean isNew;
    private MessageBean mBean;

    public boolean isIn() {
        return isIn;
    }

    public void setIn(boolean in) {
        isIn = in;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public MessageBean getBean() {
        return mBean;
    }

    public void setBean(MessageBean mBean) {
        this.mBean = mBean;
    }
}
