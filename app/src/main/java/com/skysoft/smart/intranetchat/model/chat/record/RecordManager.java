package com.skysoft.smart.intranetchat.model.chat.record;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.HandlerThread;

import com.skysoft.smart.intranetchat.bean.chat.RecordCallBean;
import com.skysoft.smart.intranetchat.bean.signal.RecordSignal;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.RecordDao;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.model.camera.entity.EventMessage;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.group.GroupManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.model.net_model.SendFile;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordManager {
    private static final String TAG = "RecordManager";
    private static RecordManager sInstance;
    private RecordManager() {
        mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public static RecordManager getInstance() {
        return sInstance;
    }

    public static RecordManager init() {
        sInstance = new RecordManager();
        return sInstance;
    }

    public RecordAdapter initAdapter(Context context,
                                     int receiver,
                                     boolean group) {
        isInRoom = true;
        mRecordList = new ArrayList<>();
        mReceiver = receiver;
        mGroup = group ? 1 : 0;
        mRecordAdapter = new RecordAdapter(context,receiver,group,mRecordList);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadRecord(receiver,group ? 1 : 0);
            }
        });
        return mRecordAdapter;
    }

    public void destroy() {
        isInRoom = false;
        mRecordAdapter = null;
        mRecordList = null;
    }

    private RecordAdapter mRecordAdapter;
    private List<RecordEntity> mRecordList;
    private RecordSignal mSignal = new RecordSignal();
    private final String HANDLER_THREAD_NAME = "RecordThread";
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private final int LOAD_RECORD_NUMBER = 12;
    private final int LOAD_MORE_NUMBER = 9;
    private int mReceiver;
    private int mGroup;
    private boolean isInRoom = false;

    public boolean isInRoom() {
        return isInRoom;
    }

    private void loadRecord(int receiver, int group) {
        RecordDao recordDao = MyDataBase.getInstance().getRecordDao();
        int number = recordDao.getNumber(receiver,group);
        TLog.d(TAG,">>>>>>>>> Record Number : " + number);
        int pageNum = LOAD_RECORD_NUMBER;
        List<RecordEntity> all = recordDao.getRecord(receiver,
                group,
                number-pageNum,
                pageNum);
        if (all != null && all.size() != 0){
            mRecordList.addAll(all);
            mSignal.start = 0;
            mSignal.count = pageNum;
            mSignal.code = Code.LOAD;
            EventBus.getDefault().post(mSignal);
        }
    }

    public void loadMoreRecord(int receiver, int group) {
        TLog.d(TAG,"<<<<<<<<<<loadMoreRecord Receiver : " + group);
        Runnable loadMore = new Runnable() {
            @Override
            public void run() {
                RecordDao recordDao = MyDataBase.getInstance().getRecordDao();
                int number = recordDao.getNumber(receiver,group);
                int size = mRecordList.size();
                if (number == size){
                    return;
                }else {
                    int more = LOAD_MORE_NUMBER;
                    int start = number - size - LOAD_MORE_NUMBER;
                    if (start < 0){
                        start = 0;
                        more = number - size;
                    }
                    List<RecordEntity> all = recordDao.getRecord(receiver,group, start, more);
                    if (all != null && all.size() > 0) {
                        mRecordList.addAll(0,all);
                        mSignal.code = Code.LOAD_MORE;
                        mSignal.start = 0;
                        mSignal.count = all.size();
                        EventBus.getDefault().post(mSignal);
                    }
                }
            }
        };

        mHandler.post(loadMore);
    }

    public void canLoadMoreRecord(boolean canUpScroll,
                                  boolean isStateDragging,
                                  boolean isUp) {
        if (!canUpScroll && isStateDragging && isUp) {
            loadMoreRecord(mReceiver,mGroup);
        }
    }

    private void notifyChanged() {
        if (mRecordList != null) {
            mSignal.code = Code.RS;
            mSignal.count = 1;
            mSignal.start = mRecordList.size() - 1;
            EventBus.getDefault().post(mSignal);
        }
    }

    private void recordTime(RecordDao recordDao, long time) {
        if (isRecordTime(time)) {
            RecordEntity recordTime = generatorRecord(ChatRoomConfig.RECORD_TIME, -1,time);
            recordDao.insert(recordTime);
            mRecordList.add(recordTime);
        }
    }

    private void recordTime(RecordDao recordDao, long time, long t2) {
        if (isRecordTime(time,t2)) {
            RecordEntity recordTime = generatorRecord(ChatRoomConfig.RECORD_TIME, -1,time);
            recordDao.insert(recordTime);
//            mRecordList.add(recordTime);
        }
    }

    private boolean isRecordTime(long time) {
        if (mRecordList.size() == 0) {
            return false;
        }

        RecordEntity latestRecord = mRecordList.get(mRecordList.size() - 1);
        if (latestRecord.getType() == ChatRoomConfig.RECORD_TIME) {
            return false;
        }

        return isRecordTime(time,latestRecord.getTime());
    }

    /**
     * @param t1 待加入的记录的时间
     * @param t2 上一条记录的时间*/
    private boolean isRecordTime(long t1, long t2) {
        if (t2 >0 && t1 - t2 > 2*60*1000) {
            return true;
        }
        return false;
    }

    private void record(RecordEntity record) {
        Runnable r = new Runnable(){
            @Override
            public void run() {
                RecordDao recordDao = MyDataBase.getInstance().getRecordDao();

                if (mRecordList == null) {
                    long time = recordDao.getLatestRecordTime(record.getReceiver(), record.getGroup());
                    recordTime(recordDao,record.getTime(),time);
                } else {
                    recordTime(recordDao,record.getTime());
                    mRecordList.add(record);
                }

                recordDao.insert(record);

                notifyChanged();
            }
        };

        mHandler.post(r);
    }

    public void recordText(String content, int sender) {
        RecordEntity record = generatorRecord(ChatRoomConfig.RECORD_TEXT, sender);
        record.setContent(content);
        record(record);
    }

    public void recordText(MessageBean bean) {
        RecordEntity record = generatorRecord(ChatRoomConfig.RECORD_TEXT, bean);
        record.setContent(bean.getMsg());

        record(record);
    }

    public void recordFile(FileEntity file,
                           int sender,
                           int receiver,
                           int group) {
        RecordEntity record = generatorRecord(ChatRoomConfig.RECORD_FILE, sender);
        record.setReceiver(receiver);
        record.setSender(sender);
        record.setGroup(group);
        record.setFileEntity(file);

        if (file.getType() == Config.FILE_VIDEO) {
            file.setThumbnail(thumbnailFile(file.getPath()));
        }
        record.setFile(GsonTools.toJson(file));
        record(record);
    }

    public void recordFile(FileEntity file, String sender, String receiver) {
        int s = ContactManager.getInstance().getContactId(sender);
        if (sender.equals(receiver)) {
            recordFile(file,s,s,0);
        } else {
            recordFile(file,
                    s,
                    GroupManager.getInstance().getGroupId(receiver),
                    1);
        }
    }

    public void recordFile(FileEntity file, int sender) {
        TLog.d(TAG,"------------> " + file.toString());
        RecordEntity record = generatorRecord(ChatRoomConfig.RECORD_FILE, sender);
        record.setFileEntity(file);
        if (file.getType() == Config.FILE_VIDEO ) {
            file.setThumbnail(thumbnailFile(file.getPath()));
            TLog.d(TAG,"----------> Thumbnail : " + file.getThumbnail());
        }
        record.setFile(GsonTools.toJson(file));
        record(record);
    }

    public void recordFile(FileEntity file, String sender) {
        int id = ContactManager.getInstance().getContact(sender).getId();
        recordFile(file, id);
    }

    public void recordCall(RecordCallBean bean, int sender) {
        RecordEntity record = generatorRecord(ChatRoomConfig.RECORD_CALL, sender);
        record.setContent(GsonTools.toJson(bean));
        record(record);
    }

    public void recordCall(RecordCallBean bean, String sender) {
        int id = ContactManager.getInstance().getContact(sender).getId();
        recordCall(bean,id);
    }

    public void recordCall(String content, long length, int type, String sender) {
        int id = ContactManager.getInstance().getContact(sender).getId();
        recordCall(content,length,type,id);
    }

    public void recordCall(String content, long length, int type, int sender) {
        RecordCallBean bean = new RecordCallBean();
        bean.setContent(content);
        bean.setLength(length);
        bean.setType(type);
        recordCall(bean,sender);
    }

    public void deleteRecord(final int position) {
        Runnable delete = new Runnable() {
            @Override
            public void run() {
                RecordDao recordDao = MyDataBase.getInstance().getRecordDao();
                int i = queryDelete(position);
                if (i == -1) {
                    if (position == mRecordList.size()-1 && position > 1) {     //更新消息列表
                        RecordEntity record = mRecordList.get(position - 2);
                        updateLatest(record);
                    }
                    deleteRecord(recordDao,position,position-1);
                } else if (i == 1){
                    deleteRecord(recordDao,position+1,position);
                } else {
                    recordDao.delete(mRecordList.get(position));
                    mRecordList.remove(position);
                }

                EventBus.getDefault().post(mSignal);
            }
        };

        mHandler.post(delete);
    }

    public void updateLatest(RecordEntity record) {
        LatestManager.getInstance().update(
                record.getReceiver(),
                latestContent(record),
                record.getGroup()==1);
    }

    /**
     * i1 > i2
     * @param i1 第一个被删除的记录的下标
     * @param i2 第二个被删除的记录的下标*/
    private void deleteRecord(RecordDao dao, int i1, int i2) {
        dao.delete(mRecordList.get(i1));
        dao.delete(mRecordList.get(i2));
        mRecordList.remove(i1);
        mRecordList.remove(i2);
    }

    private int queryDelete(int position) {
        int size = mRecordList.size() - 1;
        boolean m = false;
        boolean u = false;
        if (position == 0) {        //第一条
            m = isTime(1);
        } else {
            u = true;
            //  上一条是日期记录                最后一条             下一条是日期记录
            m = isTime(position-1) && ( position == size || isTime(position+1) );
        }
        if (m) {
            return u ? -1 : 1;
        }
        return 0;
    }

    private boolean isTime(int position) {
        return mRecordList.get(position).getType() == ChatRoomConfig.RECORD_TIME;
    }

    private String latestContent(RecordEntity record) {
        if (record.getType() == ChatRoomConfig.RECORD_FILE) {
            return LatestManager.
                    getInstance().
                    fileContent(record.getFileEntity());
        } else {
            return record.getContent();
        }
    }

    private RecordEntity generatorRecord(int type, MessageBean bean) {
        int sender = ContactManager.getInstance().getContactId(bean.getSender());
        int group = bean.getSender().equals(bean.getReceiver()) ? 0 : 1;
        int receiver = group == 0 ? sender : GroupManager.getInstance().getGroupId(bean.getReceiver());

        RecordEntity record = new RecordEntity();
        record.setSender(sender);
        record.setGroup(group);
        record.setReceiver(receiver);
        record.setContent(bean.getMsg());
        record.setType(type);
        record.setTime(System.currentTimeMillis());
        return record;
    }

    private RecordEntity generatorRecord(int type, int sender) {
        return generatorRecord(type, sender, System.currentTimeMillis());
    }

    private RecordEntity generatorRecord(int type, int sender, long time) {
        RecordEntity record = new RecordEntity();
        record.setSender(sender);
        record.setReceiver(mReceiver);
        record.setGroup(mGroup);
        record.setTime(time);
        record.setType(type);
        return record;
    }

    /**获得视屏文件的第一帧图*/
    private static Bitmap thumbnailBitmap(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    private static String thumbnailFile(String path) {
        Bitmap bitmap = thumbnailBitmap(path);
        File parentFile = SendFile.getFile(ChatRoomConfig.PATH_VIDEO_FIRST_FRAME);
        if (!parentFile.exists()){
            parentFile.mkdirs();
        }

        String name = new File(path).getName();
        name = name.substring(0,name.lastIndexOf("."));
        File firstFrameFile = new File(parentFile.getPath(),"first_frame" + name + ".jpg");
        try {
            firstFrameFile.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(firstFrameFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return firstFrameFile.getPath();
    }

    public int scrollToPosition() {
        TLog.d(TAG,">>>>>>>>>> position " + mRecordList.size());
        if (mRecordList == null || mRecordList.size() == 0) {
            return 0;
        }

        return mRecordList.size() - 1;
    }
}
