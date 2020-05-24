package com.skysoft.smart.intranetchat.model.chat.record;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.HandlerThread;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.bean.chat.RecordCallBean;
import com.skysoft.smart.intranetchat.bean.signal.ChatSignal;
import com.skysoft.smart.intranetchat.bean.signal.ContactSignal;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.GroupMemberDao;
import com.skysoft.smart.intranetchat.database.dao.RecordDao;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.GroupMemberEntity;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.model.net_model.SendFile;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordManager {
    private static RecordManager sInstance;
    private RecordManager(Context context, int receiver, boolean group) {
        mRecordList = new ArrayList<>();
        mReceiver = receiver;
        mGroup = group ? 1 : 0;
        mRecordAdapter = new RecordAdapter(context,receiver,group,mRecordList);

        mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                loadRecord(receiver,group ? 1 : 0);
                EventBus.getDefault().post(mSignal);
            }
        });
    }
    public static RecordManager getInstance() {
        return sInstance;
    }

    public static RecordAdapter init(Context context,
                                           int receiver,
                                           boolean group) {
        if (sInstance != null) {
            sInstance = null;
        }
        sInstance = new RecordManager(context,receiver,group);
        return sInstance.mRecordAdapter;
    }

    public static void destroy() {
        if (sInstance != null) {
            sInstance.mRecordAdapter = null;
            sInstance.mRecordList = null;
            sInstance = null;
        }
    }

    private RecordAdapter mRecordAdapter;
    private List<RecordEntity> mRecordList;
    private ChatSignal mSignal = new ChatSignal();
    private final String HANDLER_THREAD_NAME = "RecordThread";
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private final int LOAD_RECORD_NUMBER = 35;
    private final int LOAD_MORE_NUMBER = 20;
    private int mReceiver;
    private int mGroup;

    private void loadRecord(int receiver, int group) {
        RecordDao recordDao = MyDataBase.getInstance().getRecordDao();
        int number = recordDao.getNumber(receiver,group);
        int pageNum = LOAD_RECORD_NUMBER;
        List<RecordEntity> all = recordDao.getRecord(receiver,
                group,
                number-pageNum,
                pageNum);
        if (all != null && all.size() != 0){
            mRecordList.addAll(all);
        }
    }

    public void loadMoreRecord(int receiver, int group) {
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
                    mRecordList.addAll(all);
                    EventBus.getDefault().post(mSignal);
                }
            }
        };

        mHandler.post(loadMore);
    }

    private void notifyChanged() {
        EventBus.getDefault().post(mSignal);
    }

    private void recordTime(RecordDao recordDao, long time) {
        if (isRecordTime(time)) {
            RecordEntity recordTime = generatorRecord(ChatRoomConfig.RECORD_TIME, -1,time);
            recordDao.insert(recordTime);
            mRecordList.add(recordTime);
        }
    }

    private boolean isRecordTime(long time) {
        if (mRecordList.size() == 0) {
            return true;
        }

        RecordEntity latestRecord = mRecordList.get(mRecordList.size() - 1);
        if (latestRecord.getType() == ChatRoomConfig.RECORD_TIME) {
            return false;
        }

        if (time - latestRecord.getTime() > 2*60*1000) {
            return true;
        }

        return false;
    }

    private void record(RecordEntity record) {
        Runnable recordText = new Runnable(){
            @Override
            public void run() {
                RecordDao recordDao = MyDataBase.getInstance().getRecordDao();

                recordTime(recordDao,record.getTime());

                mRecordList.add(record);
                recordDao.insert(record);

                notifyChanged();
            }
        };

        mHandler.post(recordText);
    }

    public void recordText(String content, int sender) {
        RecordEntity record = generatorRecord(ChatRoomConfig.RECORD_TEXT, sender);
        record.setContent(content);
        record(record);
    }

    public void recordText(String content, String sender) {
        int id = ContactManager.getInstance().getContact(sender).getId();
        recordText(content,id);
    }

    public void recordFile(FileEntity file, int sender) {
        RecordEntity record = generatorRecord(ChatRoomConfig.RECORD_FILE, sender);
        record.setFileEntity(file);
        record.setFile(GsonTools.toJson(file));
        if (file.getType() == Config.FILE_VIDEO && file.getStep() == Config.STEP_SUCCESS) {
            file.setThumbnail(thumbnailFile(file.getPath()));
        }
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
}
