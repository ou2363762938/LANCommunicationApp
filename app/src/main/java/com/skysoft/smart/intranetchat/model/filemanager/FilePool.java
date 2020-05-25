package com.skysoft.smart.intranetchat.model.filemanager;

import android.text.TextUtils;

import com.skysoft.smart.intranetchat.model.network.bean.FileBean;

import java.util.HashMap;
import java.util.Map;

public class FilePool {
    private Map<String,FileDrops> mFileDrops;
    private static FilePool sInstance;

    private FilePool() {
        mFileDrops = new HashMap<>();
    }

    public static void init() {
        sInstance = new FilePool();
    }

    public static FilePool getInstance() {
        return sInstance;
    }

    public void put(FileDrops drops) {
        if (drops == null) {
            throw new NullPointerException();
        }

        mFileDrops.put(drops.getFileBean().getRid(),drops);
    }

    public FileDrops put (FileBean bean, String path, int step) {
        FileDrops drops = new FileDrops();
        drops.setFileBean(bean);
        drops.setPath(TextUtils.isEmpty(path) ? "" : path);
        drops.setTime(System.currentTimeMillis());
        drops.setStep(step);
        mFileDrops.put(bean.getRid(),drops);
        return drops;
    }

    public FileDrops get(String rid) {
        if (TextUtils.isEmpty(rid)) {
            throw new NullPointerException();
        }
        return mFileDrops.get(rid);
    }
}
