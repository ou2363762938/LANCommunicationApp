package com.skysoft.smart.intranetchat.model.network.bean;

public class ReceiveFileContentBean {
    private String rid;
    private String md5;
    private String path;

    public ReceiveFileContentBean(String rid, String md5, String path) {
        this.rid = rid;
        this.md5 = md5;
        this.path = path;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ReceiveFileContentBean{" +
                "rid='" + rid + '\'' +
                ", md5='" + md5 + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
