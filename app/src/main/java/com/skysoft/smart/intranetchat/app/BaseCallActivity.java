package com.skysoft.smart.intranetchat.app;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

public class BaseCallActivity extends AppCompatActivity {
    protected int mConfig;
    private int mUser;
    protected String mIdentifier;

    protected void init() {
        mUser = ContactManager.getInstance().getContactId(mIdentifier);
    }

    protected void endLaunchCall(String content) {
        RecordManager.getInstance().recordCall(
                content,
                0,
                mConfig,
                -1
        );
        LatestManager.getInstance().receive(mUser,content,false);
    }

    protected void endAnswerCall(String content) {
        RecordManager.getInstance().recordCall(
                content,
                0,
                mConfig,
                mIdentifier
        );
        LatestManager.getInstance().receive(mUser,content,false);
    }

    public void endLaunchCall(String contact, boolean answer) {
        long time = IntranetChatApplication.
                getStartCallTime() -
                IntranetChatApplication.
                        getEndCallTime();

        if (answer) {
            RecordManager.getInstance().recordCall(
                    "通话时长",
                    time,
                    ChatRoomConfig.RECEIVE_VIDEO_CALL,
                    mIdentifier);
        } else {
            RecordManager.getInstance().recordCall(
                    "通话时长",
                    time,
                    ChatRoomConfig.SEND_VIDEO_CALL,
                    -1);
        }
        LatestManager.getInstance().receive(mUser,"通话时长",false);
    }
}
