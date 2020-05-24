package com.skysoft.smart.intranetchat.bean.chat;

public class RecordCallBean {
    private int type;
    private long length;
    private String content;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "RecordCallBean{" +
                "type=" + type +
                ", length=" + length +
                ", content='" + content + '\'' +
                '}';
    }
}
