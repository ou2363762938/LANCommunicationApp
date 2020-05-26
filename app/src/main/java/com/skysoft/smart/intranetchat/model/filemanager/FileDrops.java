package com.skysoft.smart.intranetchat.model.filemanager;

import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.model.network.bean.FileBean;

public class FileDrops {
    private FileBean fileBean;
    private String path;
    private long time;
    private int step;

    public FileBean getFileBean() {
        return fileBean;
    }

    public void setFileBean(FileBean fileBean) {
        this.fileBean = fileBean;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public FileEntity getFileEntity() {
        FileEntity entity = new FileEntity();
        entity.fromFileBean(fileBean);
        entity.setStep(step);
        entity.setPath(path);
        entity.setTime(time);
        return entity;
    }

    @Override
    public String toString() {
        return "FileDrops{" +
                "fileBean=" + fileBean +
                ", path='" + path + '\'' +
                ", time=" + time +
                ", step=" + step +
                '}';
    }
}
