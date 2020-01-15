package com.skysoft.smart.intranetchat.model.network.bean;

public class ReplayMessageBean extends NotificationMessageBean {
    private String mReplayName;
    private String mReplayContent;
    private String mReplayType;

    public String getmReplayName() {
        return mReplayName;
    }

    public void setmReplayName(String mReplayName) {
        this.mReplayName = mReplayName;
    }

    public String getmReplayContent() {
        return mReplayContent;
    }

    public void setmReplayContent(String mReplayContent) {
        this.mReplayContent = mReplayContent;
    }

    public String getmReplayType() {
        return mReplayType;
    }

    public void setmReplayType(String mReplayType) {
        this.mReplayType = mReplayType;
    }
}
