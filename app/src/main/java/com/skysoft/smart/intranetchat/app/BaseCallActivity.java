package com.skysoft.smart.intranetchat.app;

import androidx.appcompat.app.AppCompatActivity;

import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

public class BaseCallActivity extends AppCompatActivity {
    protected int mConfig;
    protected String mIdentifier;
    protected void endCall(String content) {
        RecordManager.getInstance().recordCall(
                content,
                0,
                mConfig,
                mIdentifier
        );
    }

    public void endCall(String contact, int type) {
        long time = IntranetChatApplication.
                getStartCallTime() -
                IntranetChatApplication.
                        getEndCallTime();
        RecordManager.getInstance().recordCall(
                "通话时长",
                time,
                type,
                contact);
    }

    public void endCall(String contact, boolean answer) {
        endCall(contact,
                answer
                        ? ChatRoomConfig.RECEIVE_VIDEO_CALL
                        : ChatRoomConfig.SEND_VIDEO_CALL);
    }
}
