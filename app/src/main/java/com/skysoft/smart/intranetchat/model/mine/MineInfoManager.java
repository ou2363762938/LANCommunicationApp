package com.skysoft.smart.intranetchat.model.mine;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.server.IntranetChatServer;
import com.skysoft.smart.intranetchat.tools.Identifier;

public class MineInfoManager {
    private Context mContext;
    private SharedPreferences mShared;
    private SharedPreferences.Editor mEditor;
    private final String MINE = "mine";
    private final String NAME = "name";
    private final String IDENTIFIER = "identifier";
    private final String AVATAR_IDENTIFIER = "avatarIdentifier";
    private final String AVATAR_PATH = "avatarPath";


    private static MineInfoManager sInstance;
    private MineInfoManager(Context context) {
        mContext = context;
        mShared = context.getSharedPreferences(MINE,Context.MODE_PRIVATE);
        mEditor = mShared.edit();
        userInfo = new UserInfoBean();
    }

    public static void init(Context context) {
        sInstance = new MineInfoManager(context);
    }

    public static MineInfoManager getInstance() {
        return sInstance;
    }

    private String name;
    private String identifier;
    private String host;
    private String avatarIdentifier;
    private String avatarPath;
    private UserInfoBean userInfo;

    private int status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        mEditor.putString(NAME,name);
        mEditor.commit();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
        mEditor.putString(IDENTIFIER,identifier);
        mEditor.commit();
    }

    public String getAvatarIdentifier() {
        return avatarIdentifier;
    }

    public void setAvatarIdentifier(String avatarIdentifier) {
        this.avatarIdentifier = avatarIdentifier;
        mEditor.putString(AVATAR_IDENTIFIER,avatarIdentifier);
        mEditor.commit();
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
        mEditor.putString(AVATAR_PATH,avatarPath);
        mEditor.commit();
    }

    public void setAvatar(String avatarPath) {
        this.avatarPath = avatarPath;
        if (TextUtils.isEmpty(avatarPath)) {
            this.avatarIdentifier = Identifier.getInstance().getDefaultAvatarIdentifier();
        } else {
            this.avatarIdentifier = Identifier.getInstance().getFileIdentifier(avatarPath);
        }

        mEditor.putString(AVATAR_IDENTIFIER,this.avatarIdentifier);
        mEditor.putString(AVATAR_PATH,avatarPath);
        mEditor.commit();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public UserInfoBean getUserInfo() {
        if (TextUtils.isEmpty(name)) {
            return userInfo;
        }
        userInfo.setName(name);
        userInfo.setIdentifier(identifier);
        userInfo.setAvatarIdentifier(avatarIdentifier);
        userInfo.setStatus(status);
        userInfo.setBeMonitored(ContactManager.getInstance().watchMeNumber());
        userInfo.setMonitor(ContactManager.getInstance().watchNumber());
        return userInfo;
    }

    public MineInfoManager init() {
        name = mShared.getString(NAME,"");
        identifier = mShared.getString(IDENTIFIER,"");
        avatarIdentifier = mShared.getString(AVATAR_IDENTIFIER,"");
        avatarPath = mShared.getString(AVATAR_PATH,"");
        host = IntranetChatServer.getHostIP();
        return sInstance;
    }

    @Override
    public String toString() {
        return "MineInfoManager{" +
                "mContext=" + mContext +
                ", name='" + name + '\'' +
                ", identifier='" + identifier + '\'' +
                ", host='" + host + '\'' +
                ", status=" + status +
                '}';
    }
}
