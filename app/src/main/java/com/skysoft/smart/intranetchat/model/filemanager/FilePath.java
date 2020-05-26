package com.skysoft.smart.intranetchat.model.filemanager;

import android.content.Context;
import android.content.SharedPreferences;

import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import java.io.File;

public class FilePath {
    private static final String TAG = "FilePath";
    private static FilePath sInstance;
    private FilePath(Context context) {
        mContext = context;
        mShared = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        mEditor = mShared.edit();
        separator = File.separator;
    }
    private Context mContext;
    private SharedPreferences mShared;
    private SharedPreferences.Editor mEditor;
    private final String NAME = "FilePath";
    private final String VOICE = "voice";
    private final String VIDEO = "video";
    private final String IMAGE = "image";
    private final String AVATAR = "avatar";
    private final String COMMON = "common";
    private final String TEMP = "temp";

    public static FilePath init(Context context) {
        sInstance = new FilePath(context);
        return sInstance;
    }
    public static FilePath getInstance() {
        return sInstance;
    }

    private String voice;
    private String video;
    private String picture;
    private String avatar;
    private String common;
    private String temp;
    private String separator;

    public static FilePath getsInstance() {
        return sInstance;
    }

    public static void setsInstance(FilePath sInstance) {
        FilePath.sInstance = sInstance;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
        mEditor.putString(VOICE, voice);
        mEditor.commit();
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
        mEditor.putString(VIDEO,video);
        mEditor.commit();
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
        mEditor.putString(IMAGE, picture);
        mEditor.commit();
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
        mEditor.putString(AVATAR,avatar);
        mEditor.commit();
    }

    public String getCommon() {
        return common;
    }

    public void setCommon(String common) {
        this.common = common;
        mEditor.putString(COMMON,common);
        mEditor.commit();
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
        mEditor.putString(TEMP,temp);
        mEditor.commit();
    }

    public String getSeparator() {
        return separator;
    }

    public void init(String voice,
                     String video,
                     String image,
                     String avatar,
                     String common,
                     String temp) {
        this.video = video;
        mEditor.putString(VIDEO,video);
        this.voice = voice;
        mEditor.putString(VOICE,voice);
        this.picture = image;
        mEditor.putString(IMAGE,image);
        this.avatar = avatar;
        mEditor.putString(AVATAR,avatar);
        this.temp = temp;
        mEditor.putString(TEMP,temp);
        TLog.d(TAG,">>>>>>>>>> Temp : " + temp);
        this.common = common;
        mEditor.putString(COMMON,common);
        mEditor.commit();
    }
}
