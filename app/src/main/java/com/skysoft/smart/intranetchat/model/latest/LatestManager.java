package com.skysoft.smart.intranetchat.model.latest;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.chat.TransmitBean;
import com.skysoft.smart.intranetchat.bean.signal.LatestSignal;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.LatestDao;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.group.GroupManager;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity;

import org.greenrobot.eventbus.EventBus;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LatestManager {
    private static final String TAG = "LatestManager";
    private static LatestManager sInstance;
    private LatestManager() {
        mLatestSortList = new ArrayList<>();
        mLatestMap = new HashMap<>();
        mLatestIndex = new HashMap<>();
        mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }
    public static LatestManager getInstance() {
        if (sInstance == null) {
            synchronized (LatestManager.class) {
                if (sInstance == null) {
                    sInstance = new LatestManager();
                }
            }
        }
        return sInstance;
    }

    private List<Integer> mLatestSortList;
    private Map<Integer, LatestEntity> mLatestMap;
    private Map<Integer, Integer> mLatestIndex;
    private LatestListAdapter mAdapter;
    private LatestSignal mSignal = new LatestSignal();
    private final String HANDLER_THREAD_NAME = "LatestThread";
    private Handler mHandler;
    private HandlerThread mHandlerThread;

    public void initLatestMap(List<LatestEntity> entities) {
        int unRead = 0;
        for (LatestEntity latest:entities) {
            mLatestSortList.add(latest.getId());
            mLatestMap.put(latest.getId(),latest);
            mLatestIndex.put(latest.getUser(),latest.getId());
            unRead += latest.getUnReadNumber();
        }
        mSignal.code = Code.INIT_UNREAD;
        mSignal.unRead = unRead;
        EventBus.getDefault().post(mSignal);
    }

    public LatestEntity getLatest(int sender) {
        return mLatestMap.get(sender);
    }

    public LatestEntity getLatest(String sender) {
        ContactEntity contact = ContactManager.getInstance().getContact(sender);
        return mLatestMap.get(contact.getId());
    }

    public LatestEntity getLatestBySort(int position) {
        Integer sender = mLatestSortList.get(position);
        return getLatest(sender);
    }

    /**
     * @param user 联系人（群）在数据库的id
     * @param content 内容
     * @param group 是否为群*/
    public void receive(int user, String content, boolean group) {
        Runnable receive = new Runnable() {
            @Override
            public void run() {
                int base = group ? 1000 : 0;
                int u = user + base;
                LatestEntity latest = mLatestMap.get(mLatestIndex.get(u));
                LatestDao latestDao = MyDataBase.getInstance().getLatestDao();
                boolean inRoom = RecordManager.getInstance().isInRoom();

                if (latest == null) {
                    latest = new LatestEntity();
                    latest.setUser(u);

                    latestDao.insert(latest);
                    int id = latestDao.getNewInsertId();
                    latest.setId(id);

                    mLatestIndex.put(u,id);
                    mLatestMap.put(id,latest);
                    mLatestSortList.add(id);
                    if (!group && !inRoom) {
                        latest.addUnReadNumber();
                    }
                } else if (!inRoom) {
                    latest.addUnReadNumber();
                }

                latest.setContent(content);
                latest.setTime(System.currentTimeMillis());
                latest.setGroup(base);

                latestDao.update(latest);
                sortLatest();
                EventBus.getDefault().post(mSignal);
            }
        };

        mHandler.post(receive);
    }

    public void receive(String user, String content) {
        ContactEntity contact = ContactManager.getInstance().getContact(user);
        GroupEntity group = GroupManager.getInstance().getGroup(user);
        if (contact != null) {
            receive(contact.getId(),content,false);
        } else if (group != null){
            receive(group.getId(),content,true);
        }

    }

    public void receive(String user, int type) {
        switch (type) {
            case Config.FILE_VOICE:
                receive(user,"[语音]");
                break;
            case Config.FILE_VIDEO:
                receive(user,"[视频]");
                break;
            case Config.FILE_PICTURE:
                receive(user,"[图片]");
                break;
            case Config.FILE_COMMON:
                receive(user,"[文件]");
                break;
        }
    }

    /**
     * @param user 联系人（群）在数据库的id
     * @param content 内容
     * @param group 是否为群*/
    public void send(int user, String content, boolean group) {
        Runnable send = new Runnable() {
            @Override
            public void run() {
                int base = group ? 1000 : 0;
                int u = user + base;
                TLog.d(TAG,"==========<> User : " + u + ", id : " + mLatestIndex.get(u));
                LatestEntity latest = mLatestMap.get(mLatestIndex.get(u));
                if (latest == null) {
                    TLog.d(TAG,"=====Null-----");
                    latest = new LatestEntity();
                    latest.setId(-1);
                    latest.setUser(u);
                }
                latest.setTime(System.currentTimeMillis());
                latest.setContent(content);
                latest.setGroup(base);

                LatestDao latestDao = MyDataBase.getInstance().getLatestDao();
                if (latest.getId() != -1) {
                    latestDao.update(latest);
                } else {
                    latest.setId(0);
                    latestDao.insert(latest);
                    int id = latestDao.getNewInsertId();
                    latest.setId(id);

                    mLatestSortList.add(id);
                    mLatestMap.put(id,latest);
                    mLatestIndex.put(u,id);
                }

                sortLatest();
                EventBus.getDefault().post(mSignal);
            }
        };

        mHandler.post(send);
    }

    public void send(int user, FileEntity file, boolean group) {
        send(user,fileContent(file),group);
    }

    public void update(int user, String content, boolean group) {
        Runnable update = new Runnable() {
            @Override
            public void run() {
                int base = group ? 1000 : 0;
                int u = base + user;
                LatestEntity latest = mLatestMap.get(u);
                if (latest != null) {
                    latest.setContent(content);
                    MyDataBase.getInstance().getLatestDao().update(latest);
                }
            }
        };

        mHandler.post(update);
    }

    public void update(LatestEntity latest) {
        Runnable update = new Runnable() {
            @Override
            public void run() {
                MyDataBase.getInstance().getLatestDao().update(latest);
            }
        };
        mHandler.post(update);
    }

    public void clickLatest(Context context, LatestEntity latest, int position) {
        refreshUnRead(latest);
        String entity = null;
        if (latest.getGroup() == 0) {
            entity = GsonTools.toJson(
                    ContactManager.getInstance().getContact(
                            latest.getUser()));
        } else {
            entity = GsonTools.toJson(
                    GroupManager.getInstance().getGroup(
                            latest.getUser()));
        }

        ChatRoomActivity.go(context,entity,latest.getGroup() == 1);
    }

    private void refreshUnRead(LatestEntity latest) {
        if (latest == null) {
            return;
        }
        mSignal.unRead = latest.getUnReadNumber();
        mSignal.code = Code.CLICK_ITEM;
        latest.setUnReadNumber(0);
//        IntranetChatApplication.setTotalUnReadNumber(
//                LatestManager.getInstance().totalUnReadNumber());
        update(latest);
        EventBus.getDefault().post(mSignal);
        mAdapter.notifyDataSetChanged();
    }

    public void refreshUnRead(int contact) {
        refreshUnRead(mLatestMap.get(contact));
    }

    public void delete(int position) {
        int id = mLatestSortList.get(position);
        LatestEntity latest = mLatestMap.get(id);
        mLatestMap.remove(id);
        mLatestIndex.remove(latest.getUser());
        mLatestSortList.remove(position);

        mAdapter.notifyDataSetChanged();
        IntranetChatApplication.setTotalUnReadNumber(totalUnReadNumber());

        new Thread(new Runnable() {
            @Override
            public void run() {
                LatestDao latestDao = MyDataBase.getInstance().getLatestDao();
                latestDao.delete(latest);
            }
        }).start();
    }

    public void top(int position) {
        LatestEntity latest = mLatestMap.get(mLatestSortList.get(position));
        latest.setTop(latest.getTop() == 0 ? 1 : 0);
        sortLatest();

        mAdapter.notifyDataSetChanged();
        new Thread(new Runnable() {
            @Override
            public void run() {
                LatestDao latestDao = MyDataBase.getInstance().getLatestDao();
                latestDao.update(latest);
            }
        }).start();
    }

    public void sortLatest() {
        Collections.sort(mLatestSortList, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                LatestEntity latest1 = mLatestMap.get(o1);
                LatestEntity latest2 = mLatestMap.get(o2);
                int i = latest1.getTop() - latest1.getTop();
                if (i == 0) {
                    return latest1.getTime() - latest2.getTime() > 0 ? -1 : 1;
                }
                return -i;
            }
        });
    }

    public int totalUnReadNumber() {
        int n = 0;
        for (int id:mLatestMap.keySet()) {
            n += mLatestMap.get(id).getUnReadNumber();
        }
        return n;
    }

    public String fileContent(FileEntity file) {
        switch (file.getType()) {
            case Config.FILE_VOICE:
                return "[语音]";
            case Config.FILE_VIDEO:
                return "[视频]";
            case Config.FILE_PICTURE:
                return "[图片]";
            case Config.FILE_COMMON:
                return "[文件]";
            default:
                return null;
        }
    }

    public List<TransmitBean> getTransmits(int receiver) {
        List<TransmitBean> transmits = new ArrayList<>();
        for (int l:mLatestSortList) {
            LatestEntity latest = mLatestMap.get(l);
            if (latest.getUser() == receiver) {
                continue;
            }

            TransmitBean bean = new TransmitBean(l,latest.getGroup() == 1);
            transmits.add(bean);
        }
        return transmits;
    }

    public LatestListAdapter initAdapter(Context context) {
        mAdapter = new LatestListAdapter(context,mLatestSortList);
        return mAdapter;
    }

    public void setAdapter(LatestListAdapter adapter) {
        mAdapter = adapter;
    }

    public LatestListAdapter getAdapter() {
        return mAdapter;
    }

    public void notifyDataChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }
}
