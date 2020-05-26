package com.skysoft.smart.intranetchat.model.contact;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.chat.TransmitBean;
import com.skysoft.smart.intranetchat.bean.signal.ContactSignal;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.ContactDao;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.login.Login;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.net_model.SendRequest;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.AskResourceBean;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ContactManager {
    private static final String TAG = "ContactManager";
    private static ContactManager sInstance;
    private final int BASE_NOTIFY = 0;
    private ContactManager() {
        mContactSortList = new ArrayList<>();
        mWatchList = new ArrayList<>();
        mWatchMeList = new ArrayList<>();
        mContactMap = new HashMap<>();
        mContactIndex = new HashMap<>();

        mHandlerThread = new HandlerThread(CONTACT_THREAD);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mWatchTimer = new Timer();
        TimerTask watchTask = new TimerTask() {
            @Override
            public void run() {
                watchTask();
            }
        };
        mWatchTimer.schedule(watchTask,5000,2000);

        mWatchMeTimer = new Timer();
        TimerTask watchMeTask = new TimerTask() {
            @Override
            public void run() {
                watchMeTask();
            }
        };
        mWatchMeTimer.schedule(watchMeTask,5000,2000);
    }
    public static ContactManager getInstance() {
        if (sInstance == null) {
            synchronized (ContactManager.class) {
                if (sInstance == null) {
                    sInstance = new ContactManager();
                }
            }
        }
        return sInstance;
    }

    private List<Integer> mContactSortList;
    private List<String> mWatchList;
    private List<String> mWatchMeList;
    private Map<Integer, ContactEntity> mContactMap;
    private Map<String, Integer> mContactIndex;
    private ContactListAdapter mAdapter;
    private Timer mWatchTimer;
    private Timer mWatchMeTimer;
    private ContactSignal mSignal = new ContactSignal();
    private boolean isInUserInfoShowActivity = false;
    private final String CONTACT_THREAD = "ContactThread";
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    public void initContactMap(List<ContactEntity> entities) {
        int notify = 1;
        for (ContactEntity contact:entities) {
            contact.setNotifyId(notify++ + BASE_NOTIFY);
            mContactSortList.add(contact.getId());
            mContactMap.put(contact.getId(),contact);
            mContactIndex.put(contact.getIdentifier(),contact.getId());
        }
    }

    /**
     * 由联系人唯一码寻找联系人
     * @param identifier 联系人唯一码
     * @return 联系人，null：没有此联系人*/
    public ContactEntity getContact(String identifier) {
        return mContactMap.get(mContactIndex.get(identifier));
    }

    public String getContactJson(String identifier) {
        ContactEntity contact = mContactMap.get(mContactIndex.get(identifier));
        return GsonTools.toJson(contact);
    }

    /**
     * 由联系人唯一码寻找联系人，并刷新联系人的心跳
     * @param identifier 联系人唯一码
     * @return 联系人，null：没有此联系人*/
    public ContactEntity getContactAndHeartbeat(String identifier) {
        ContactEntity contact = mContactMap.get(mContactIndex.get(identifier));
        contact.setHeartbeat(System.currentTimeMillis());
        return contact;
    }

    /**
     * 获得联系人
     * @param id 联系人的id
     * @return 联系人，null：没有此联系人*/
    public ContactEntity getContact(int id) {
        return mContactMap.get(id);
    }

    public int getContactId(String contact) {
        return mContactIndex.get(contact);
    }

    public ContactEntity getContactBySort(int position) {
        Integer contact = mContactSortList.get(position);
        return getContact(contact);
    }

    /**
     * 获得联系人，并刷新心跳
     * @param id 联系人的id
     * @return 联系人，null：没有此联系人*/
    public ContactEntity getContactAndHeartbeat(int id) {
        ContactEntity contact = mContactMap.get(id);
        contact.setHeartbeat(System.currentTimeMillis());
        return contact;
    }

    public List<Integer> getContacts() {
        return this.mContactSortList;
    }

    public List<TransmitBean> findContact(String key) {
        List<TransmitBean> transmits = new ArrayList<>();
        for (int id:mContactMap.keySet()) {
            ContactEntity contact = mContactMap.get(id);
            TransmitBean bean = matchingContactEntity(key, contact);
            if (bean != null) {
                transmits.add(bean);
            }
        }
        return transmits;
    }

    public void receiveUserInfo(UserInfoBean userInfoBean, String host) {
        Log.d(TAG, "---------> Receive UserInfo " + userInfoBean.toString());
        if (MineInfoManager
                .getInstance()
                .getIdentifier()
                .equals(userInfoBean.getIdentifier())) {
            return;
        }

        ContactEntity contact = getContact(userInfoBean.getIdentifier());
        if (contact == null) {
            contact = generatorContact(userInfoBean,host);
        } else {
            TLog.d(TAG,"<<<<<<<<<<< " + contact.toString());
            if (isInUserInfoShowActivity) {
                //更换头像
                if (AvatarManager.getInstance().checkAvatar(contact.getAvatar(),userInfoBean.getAvatarIdentifier())) {
                    AvatarManager.getInstance().askAvatar(userInfoBean,host);
                }

                if (!contact.getName().equals(userInfoBean.getName())) {
                    contact.setName(userInfoBean.getName());
                }
            }
        }

        contact.setHost(host);
        if (userInfoBean.getStatus() == Config.STATUS_LOGIN) {
            contact.setStatus(Config.STATUS_ONLINE);
        } else {
            contact.setStatus(userInfoBean.getStatus());
        }

        saveContact(contact,userInfoBean.getAvatarIdentifier(),userInfoBean.getBeMonitored(),userInfoBean.getMonitor());
    }

    /**
     * 新建联系人
     * @param userInfo 联系人信息*/
    public ContactEntity generatorContact(UserInfoBean userInfo, String host) {
        ContactEntity contactEntity = new ContactEntity();
        contactEntity.setName(userInfo.getName());
        contactEntity.setIdentifier(userInfo.getIdentifier());
        contactEntity.setId(-1);

        AvatarManager.getInstance().askAvatar(userInfo.getAvatarIdentifier(),host);
        return contactEntity;
    }

    private void saveContact(ContactEntity contact, String avatar, int userMonitored, int userMonitor) {
        Runnable save = new Runnable() {
            @Override
            public void run() {
                ContactDao contactDao = MyDataBase.getInstance().getContactDao();
                if (contact.getId() == -1) {
                    contact.setAvatar(
                            AvatarManager.
                                    getInstance().
                                    insert(avatar)
                    );

                    contact.setId(0);
                    Log.d(TAG, "----------> " + contact.toString());
                    contactDao.insert(contact);
                    int id = contactDao.getNewInsertId();
                    Log.d(TAG, "---------Get ID : " + id);
                    contact.setId(id);

                    mContactMap.put(id,contact);
                    mContactSortList.add(id);
                    mContactIndex.put(contact.getIdentifier(),id);
                } else {
                    contactDao.update(contact);
                }

                watch(userMonitored, contact.getId(), contact.getHost());
                watchMe(userMonitor, contact.getId(), contact.getHost());
                sortContact();
                EventBus.getDefault().post(mSignal);
            }
        };
        mHandler.post(save);
    }

    /**
     * 对联系人进行排序*/
    public void sortContact() {
        sortContact(mContactSortList);
    }

    public void sortContact(List<Integer> contacts) {
        Collections.sort(contacts,new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                ContactEntity contact1 = mContactMap.get(o1);
                ContactEntity contact2 = mContactMap.get(o2);
                return contact1.getStatus() - contact2.getStatus();
            }
        });
    }

    public void contactOutLine(String user) {
        if (user.equals(MineInfoManager.getInstance().getIdentifier())) {
            Login.broadcastUserInfo();      //别人以为我下线，广播我的信息，告知其他用户我在线
            return;
        }

        ContactEntity contact = getContact(user);
        if (contact == null) {
            return;
        }

        contact.setStatus(Config.STATUS_OUT_LINE);
        String id = ""+contact.getId();
        if (mWatchList.contains(id)) {
            mWatchList.remove(id);
        }
        if (mWatchMeList.contains(id)) {
            mWatchMeList.remove(id);
        }

        sortContact();
        EventBus.getDefault().post(mSignal);
    }

    /**
     * 初始化adapter*/
    public ContactListAdapter initAdapter(Context context) {
        mAdapter = new ContactListAdapter(context,mContactSortList);
        return mAdapter;
    }

    public void setAdapter(ContactListAdapter adapter) {
        this.mAdapter = adapter;
    }

    public ContactListAdapter getAdapter() {
        return mAdapter;
    }

    public void addWatch(int watch) {
        mWatchList.add(""+watch);
    }

    public void addWatch(String contact) {
        Integer watch = mContactIndex.get(contact);
        addWatch(watch);
    }

    public void addWatchMe(int watch) {
        mWatchMeList.add(""+watch);
    }

    public void addWatchMe(String contact) {
        Integer watch = mContactIndex.get(contact);
        addWatchMe(watch);
    }

    public void removeWatch(int watch) {
        mWatchList.remove(""+watch);
    }

    public void removeWatch(String contact) {
        Integer watch = mContactIndex.get(contact);
        removeWatch(watch);
    }

    public void removeWatchMe(int watch) {
        mWatchMeList.remove(""+watch);
    }

    public void removeWatchMe(String contact) {
        Integer watch = mContactIndex.get(contact);
        removeWatchMe(watch);
    }

    public int watchNumber() {
        return mWatchList.size();
    }

    public int watchMeNumber() {
        return mWatchMeList.size();
    }

    public boolean isWatch(String w) {
        int contact = mContactIndex.get(w);
        return mWatchList.contains(contact);
    }

    public boolean isWatchMe(String w) {
        int contact = mContactIndex.get(w);
        return mWatchMeList.contains(contact);
    }

    private void watchTask() {
        long millis = System.currentTimeMillis();
        long difference = 0;
        boolean isSomeOneOutLine = false;

        for (int i = mWatchList.size()-1; i >= 0; i--) {
            ContactEntity contact = mContactMap.get(mWatchList.get(i));
            if (contact == null) {
                continue;
            }
            difference = millis - contact.getHeartbeat();
            if (difference > 6000) {
                Login.broadcastUserOutLine(contact.getIdentifier());
                contact.setStatus(Config.STATUS_OUT_LINE);
                isSomeOneOutLine = true;
                mWatchList.remove(i);
            } else if (difference > 5000) {
                SendRequest.sendRequestHeartbeat(
                        MineInfoManager.getInstance().getIdentifier(),
                        MineInfoManager.getInstance().getHost());
            }
        }

        if (isSomeOneOutLine) {
            sortContact();
            EventBus.getDefault().post(mSignal);
        }
    }

    private void watchMeTask() {
        String identifier = MineInfoManager.getInstance().getIdentifier();
        for (String id:mWatchList) {
            ContactEntity contact = mContactMap.get(Integer.parseInt(id));
            if (contact == null) {
                continue;
            }
            SendRequest.sendHeartbeat(identifier, contact.getHost());
        }
    }

    private void watch(int userBeMonitor, int contact, String host) {
        if (mWatchList.size() < 4
                && userBeMonitor < 4
                && !mWatchList.contains(contact)) {
            //请求对方同意被我监听
            mWatchList.add(""+contact);
            SendRequest.sendMonitorRequest(Config.REQUEST_MONITOR,
                    MineInfoManager.getInstance().getIdentifier(),
                    host);
        }
    }

    private void watchMe(int userMonitor, int contact, String host) {
        //请求被对方监视
        if (mWatchMeList.size() < 4     //监视我的人数小于4
                && userMonitor < 4                //对方监视的人数小于4
                //对方没有被添加到监视我的名单
                && !mWatchMeList.contains(contact)){
            //请求对方监听我
            mWatchMeList.add(""+contact);
            SendRequest.sendMonitorRequest(Config.REQUEST_BE_MONITOR,
                    MineInfoManager.getInstance().getIdentifier(),
                    host);
        }
    }

    public boolean isInUserInfoShowActivity() {
        return isInUserInfoShowActivity;
    }

    public void setInUserInfoShowActivity(boolean inUserInfoShowActivity) {
        isInUserInfoShowActivity = inUserInfoShowActivity;
    }

    public void heartbeat(String contact) {
        ContactEntity entity = mContactMap.get(mContactIndex.get(contact));
        entity.setHeartbeat(System.currentTimeMillis());
    }

    public void hearbeat(int contact) {
        ContactEntity entity = mContactMap.get(contact);
        entity.setHeartbeat(System.currentTimeMillis());
    }

    public void heartbeat(ContactEntity contact) {
        contact.setHeartbeat(System.currentTimeMillis());
    }

    /**
     * 匹配key和contactEntity.getName()
     * @param key 关键词
     * @param contact 联系人
     * @return null：key和contactEntity不匹配*/
    private TransmitBean matchingContactEntity(String key, ContactEntity contact){
        TransmitBean bean = baseMatchingContactEntity(key,contact);
        //a~z:97~122
        //A~Z:65~90
        if (null == bean){
            for (int i = 1; i < key.length(); i++){
                bean = baseMatchingContactEntity(key.substring(0,i),contact);
                if (null != bean){
                    break;
                }
            }
        }

        if (null == bean){
            for (int i = 0; i < key.length() ; i++){
                bean = baseMatchingContactEntity(""+key.charAt(i),contact);
                if (null != bean){
                    break;
                }else {
                    //忽视大小写
                    if (key.charAt(i)>=65 && key.charAt(i)<=90){
                        bean = baseMatchingContactEntity(""+(char) (key.charAt(i)+32),contact);
                    }else if (key.charAt(i) >= 97 && key.charAt(i)<=122){
                        bean = baseMatchingContactEntity(""+(char) (key.charAt(i)-32),contact);
                    }

                    if (null != bean){
                        break;
                    }
                }
            }
        }
        return bean;
    }

    /**
     * 匹配key和contactEntity.getName()
     * @param key 关键词
     * @param contact 联系人
     * @return null：key和contactEntity不匹配*/
    private TransmitBean baseMatchingContactEntity(String key, ContactEntity contact){
        if (!TextUtils.isEmpty(key) && contact.getName().contains(key)){
            TransmitBean bean = new TransmitBean(contact.getId()
                    ,false);
            bean.setName(contact.getName());
            return bean;
        }
        return null;
    }
}
