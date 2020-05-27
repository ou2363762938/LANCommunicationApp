package com.skysoft.smart.intranetchat.model.avatar;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.signal.AvatarSignal;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.AvatarDao;
import com.skysoft.smart.intranetchat.database.table.AvatarEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.AskResourceBean;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvatarManager {
    private static final String TAG = "AvatarManager";
    private static AvatarManager sInstance;
    private AvatarManager() {
        mAvatarMap = new HashMap<>();
        mAvatarIndex = new HashMap<>();
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public static AvatarManager getInstance() {
        if (sInstance == null) {
            synchronized (AvatarManager.class) {
                if (sInstance == null) {
                    sInstance = new AvatarManager();
                }
            }
        }

        return sInstance;
    }

    private Map<Integer, AvatarEntity> mAvatarMap;
    private Map<String, Integer> mAvatarIndex;
    private final String sDefaultAvatarId = Identifier
            .getInstance()
            .getDefaultAvatarIdentifier();
    private AvatarSignal mSignal = new AvatarSignal();
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    public void initAvatarMap(List<AvatarEntity> avatarEntities) {
        for (AvatarEntity avatar:avatarEntities) {
            mAvatarMap.put(avatar.getId(),avatar);
            mAvatarIndex.put(avatar.getIdentifier(),avatar.getId());
        }
    }

    /**
     * 获得头像的地址
     * @param avatar 头像在数据库的id
     * @return 头像的地址，为null则使用默认头像*/
    public String getAvatarPath(int avatar) {
        AvatarEntity avatarEntity = mAvatarMap.get(avatar);
        if (avatarEntity == null) {
            return null;
        }
        if (avatarEntity.getIdentifier().equals(sDefaultAvatarId)) {
            return null;
        }
        return avatarEntity.getPath();
    }

    public String getDefaultAvatar() {
        return sDefaultAvatarId;
    }

    public int getAvatar(String avatar) {
        return mAvatarIndex.get(avatar);
    }

    public String getAvatar(int avatar) {
        return mAvatarMap.get(avatar).getIdentifier();
    }

    public void insertAvatar(ContactEntity contact, String identifier) {
        if (contact.getAvatar() == -1) {
            Runnable insert = new Runnable() {
                @Override
                public void run() {
                    contact.setAvatar(insert(identifier));
                }
            };

            mHandler.post(insert);
        }
    }

    public void insertAvatar(GroupEntity group, String identifier) {
        if (group.getAvatar() == -1) {
            Runnable insert = new Runnable() {
                @Override
                public void run() {
                    group.setAvatar(insert(identifier));
                }
            };
            mHandler.post(insert);
        }
    }

    public int insert(String identifier) {
        if (!sDefaultAvatarId.equals(identifier)) {
            AvatarEntity avatar = mAvatarMap.get(identifier);
            if (avatar != null) {
                return avatar.getId();
            }
        }

        AvatarEntity avatarEntity = new AvatarEntity();

        avatarEntity.setIdentifier(identifier);
        avatarEntity.setPath(null);
        if (!identifier.equals(sDefaultAvatarId)) {
            //请求头像
        }

        AvatarDao avatarDao = MyDataBase.getInstance().getAvatarDao();
        avatarDao.insert(avatarEntity);
        int id = avatarDao.getNewInsertId();
        avatarEntity.setId(id);
        mAvatarMap.put(id,avatarEntity);
        return id;
    }

    public void update(AvatarEntity avatar) {
        Runnable update = new Runnable() {
            @Override
            public void run() {
                AvatarDao avatarDao = MyDataBase.getInstance().getAvatarDao();
                avatarDao.update(avatar);
            }
        };

        mHandler.post(update);
    }

    public void loadContactAvatar(Context context, ImageView view, int avatar) {
        String path = null;
        if (avatar == -1) {
            path = MineInfoManager.getInstance().getAvatarPath();
        } else {
            AvatarEntity avatarEntity = mAvatarMap.get(avatar);
            path = avatarEntity.getPath();
        }

        if (TextUtils.isEmpty(path)) {
            Glide.with(context).load(R.drawable.default_head).into(view);
        } else {
            Glide.with(context).load(path).into(view);
        }
    }

    public void loadContactAvatar(Context context, ImageView view, String contact) {
        int avatar = ContactManager.getInstance().getContact(contact).getAvatar();
        loadContactAvatar(context,view,avatar);
    }

    public boolean checkAvatar(int id, String identifier) {
        AvatarEntity avatar = mAvatarMap.get(id);
        TLog.d(TAG,"<<<<<<< " + avatar.toString() + ", ID : " + identifier);

        if (!avatar.getIdentifier().equals(identifier)) {
//            avatar.setIdentifier(identifier);
            if (identifier.equals(sDefaultAvatarId)) {
                avatar.setIdentifier(sDefaultAvatarId);
                update(avatar);
                return false;
            }
            return true;
        }
        return false;
//        return !avatar.equals(identifier) && !identifier.equals(sDefaultAvatarId);
    }

    public boolean isDefault(String avatar) {
        return !TextUtils.isEmpty(avatar) && avatar.equals(sDefaultAvatarId);
    }

    public void askAvatar(UserInfoBean userInfo, String host) {
        AskResourceBean askResourceBean = new AskResourceBean();
        askResourceBean.setResourceType(Config.RESOURCE_AVATAR);
        askResourceBean.setResourceUniqueIdentifier(userInfo.getAvatarIdentifier());
        try {
            IntranetChatApplication.sAidlInterface.askResource(GsonTools.toJson(askResourceBean),host);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void askAvatar(String identifier, String host) {
        AskResourceBean askResourceBean = new AskResourceBean();
        askResourceBean.setResourceUniqueIdentifier(identifier);
        askResourceBean.setResourceType(Config.RESOURCE_AVATAR);
        try {
            IntranetChatApplication.sAidlInterface.askResource(GsonTools.toJson(askResourceBean), host);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void receiveAvatar(String receiver, String rid, String path) {
        Runnable receive = new Runnable() {
            @Override
            public void run() {
                AvatarDao avatarDao = MyDataBase.getInstance().getAvatarDao();
                AvatarEntity avatar = mAvatarMap.get(
                        ContactManager.
                                getInstance().
                                getContact(receiver).
                                getAvatar());
                TLog.d(TAG,"<<<<<<<< " + avatar.toString() + " >>>>>>>>>>>>");
                avatar.setIdentifier(rid);
                avatar.setPath(path);
                avatarDao.update(avatar);

                mSignal.receiver = receiver;
                EventBus.getDefault().post(mSignal);
            }
        };

        mHandler.post(receive);
    }
}
