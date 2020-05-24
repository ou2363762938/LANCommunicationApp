package com.skysoft.smart.intranetchat.bean.base;

import android.content.Context;
import android.content.SharedPreferences;

public class DeviceInfoBean {
    private Context mContext;
    private SharedPreferences mShared;
    private SharedPreferences.Editor mEditor;
    private final String DEVICE = "deice";
    private final String KEY_BROAD_HEIGHT = "keyBroadHeight";
    private final String SCREEN_SIZE = "screenSize";

    private static DeviceInfoBean sInstance;
    private DeviceInfoBean(Context context) {
        mContext = context;
        mShared = context.getSharedPreferences(DEVICE,Context.MODE_PRIVATE);
        mEditor = mShared.edit();
    }

    public static void init(Context context) {
        sInstance = new DeviceInfoBean(context);
    }

    public static DeviceInfoBean getInstance() {
        return sInstance;
    }

    private int keyBroadHeight;
    private int screenSize;

    public int getKeyBroadHeight() {
        return keyBroadHeight;
    }

    public void setKeyBroadHeight(int keyBroadHeight) {
        this.keyBroadHeight = keyBroadHeight;
        mEditor.putInt(KEY_BROAD_HEIGHT,keyBroadHeight);
        mEditor.commit();
    }

    public int getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(int screenSize) {
        this.screenSize = screenSize;
        mEditor.putInt(SCREEN_SIZE,screenSize);
        mEditor.commit();
    }

    public void set(int keyBroadHeight, int screenSize) {
        mEditor.putInt(KEY_BROAD_HEIGHT,keyBroadHeight);
        mEditor.putInt(SCREEN_SIZE,screenSize);
        mEditor.commit();
    }

    public void init() {
        keyBroadHeight = mShared.getInt(KEY_BROAD_HEIGHT, 0);
        screenSize = mShared.getInt(SCREEN_SIZE, 0);
    }
}
