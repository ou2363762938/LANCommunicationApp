package com.skysoft.smart.intranetchat.model.transmit;

import android.text.TextUtils;

import com.skysoft.smart.intranetchat.bean.chat.TransmitBean;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.model.chat.Message;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.filemanager.FileManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

public class TransmitManager {
    private static final String TAG = "TransmitManager";
    private TransmitManager() {}

    public static TransmitManager getInstance() {
        return new TransmitManager();
    }

    public void transmit(int type,
                         String message,
                         String leave,
                         FileEntity file,
                         TransmitBean bean) {
        if (type == ChatRoomConfig.RECORD_TEXT){
            //转发文字
            transmitMessage(message,bean);
        }else if (type == ChatRoomConfig.RECORD_FILE){
            //转发图片
            transmitFile(file,bean);
        }
        if (!TextUtils.isEmpty(leave)){     //如果输入留言，转发留言
            transmitMessage(leave,bean);
        }
    }

    public void transmitMessage(String content, TransmitBean bean) {
        Message.getInstance().send(content,bean.getUser(),bean.isGroup());
        LatestManager.getInstance().send(bean.getUser(),content,bean.isGroup());
        RecordManager.getInstance().recordText(content,bean.getUser(),bean.isGroup() ? 1 : 0);
    }

    public void transmitFile(FileEntity file, TransmitBean bean) {
        TLog.d(TAG, ">>>>>>>>>>>> " + bean.toString());
        FileManager.getInstance().notify(bean.getUser(),file,bean.isGroup());
        LatestManager.getInstance().send(bean.getUser(),file,bean.isGroup());
        RecordManager.getInstance().recordFile(file,bean.getUser(),bean.isGroup() ? 1 : 0);
    }
}
