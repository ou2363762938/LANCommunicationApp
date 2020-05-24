/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication]Voice or video communication function
 */
package com.skysoft.smart.intranetchat.ui.activity.voicecall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.skysoft.smart.intranetchat.app.BaseCallActivity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.model.net_model.VoiceCall;
import com.skysoft.smart.intranetchat.app.impl.HandleVoiceCallResponse;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveCallHungUp;
import com.skysoft.smart.intranetchat.bean.network.InCallBean;
import com.skysoft.smart.intranetchat.tools.customstatusbar.CustomStatusBarBackground;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

public class LaunchVoiceCallActivity extends BaseCallActivity {

    private String TAG = LaunchVoiceCallActivity.class.getSimpleName();
    private ImageView hungVoiceCall;
    private ImageView mImgHead;
    private TextView mName;
    private String host;
    private String name;
    private int mAvatar;
    private long lastWaitingConsentCall;
    private long intervalTime = 550;
    private Timer affirmWaitingTimer;
    private Timer requestConsentTimer;
    private String mIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_voice_call);
        CustomStatusBarBackground.customStatusBarTransparent(this);
        CustomStatusBarBackground.colorViewStatusBar(this,R.color.color_black,findViewById(R.id.custom_status_bar_background));
        IntranetChatApplication.getsCallback().setHandleVoiceCallResponse(handleVoiceCallResponse);
        IntranetChatApplication.getsCallback().setHungUpInAnswer(onReceiveCallHungUp);
        hungVoiceCall = findViewById(R.id.activity_start_voice_call);

        mConfig = ChatRoomConfig.SEND_VOICE_CALL;
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        host = bundle.getString("host");
        name = bundle.getString("name");
        mAvatar = bundle.getInt("avatar");
        mIdentifier = bundle.getString("identifier");
        EventBus.getDefault().register(this);
        init();
        VoiceCall.startVoiceCall(MineInfoManager.getInstance().getUserInfo(), host);

        hungVoiceCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoiceCall.hungUpVoiceCall(host);
                IntranetChatApplication.setInCall(false);
                endCall(getString(R.string.call_refuse_launch_mine));
                LaunchVoiceCallActivity.this.finish();
            }
        });

        monitor();
    }

    public void monitor(){
        lastWaitingConsentCall = System.currentTimeMillis();
        affirmWaitingTimer = new Timer();
        TimerTask affirmWaitingTask = new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastWaitingConsentCall > intervalTime){
                    VoiceCall.hungUpVoiceCall(host);
                    IntranetChatApplication.setInCall(false);
                    endCall(getString(R.string.call_die));
                    LaunchVoiceCallActivity.this.finish();
                }
            }
        };
        affirmWaitingTimer.schedule(affirmWaitingTask,100,1000);

        requestConsentTimer = new Timer();
        TimerTask requestConsentTask = new TimerTask() {
            @Override
            public void run() {
                VoiceCall.requestConsentVoiceCall(host);
            }
        };
        requestConsentTimer.schedule(requestConsentTask,100,300);
    }

    private void init() {
        mImgHead = findViewById(R.id.activity_start_voice_img);
        mName = findViewById(R.id.activity_start_voice_name);
        mName.setText(name);
        AvatarManager.getInstance().loadContactAvatar(this,mImgHead,mAvatar);
    }

    public static void go(Activity activity, String host) {
        Intent intent = new Intent(activity, LaunchVoiceCallActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("host", host);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    public static void go(Activity activity,String host, String name, int avatar, String identifier) {
        Intent intent = new Intent(activity, LaunchVoiceCallActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putInt("avatar", avatar);
        bundle.putString("host", host);
        bundle.putString("identifier",identifier);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    private HandleVoiceCallResponse handleVoiceCallResponse = new HandleVoiceCallResponse() {
        @Override
        public void onReceiveConsentVoiceCall(String host) {
            TLog.d(TAG, "onReceiveConsentVoiceCall: " + host);
            VoiceCallActivity.go(LaunchVoiceCallActivity.this, host,name,mAvatar,mIdentifier,false);
            finish();
        }

        @Override
        public void onReceiveRefuseVoiceCall(String host) {
            TLog.d(TAG, "onReceiveRefuseVoiceCall: " + host);
            if (!LaunchVoiceCallActivity.this.host.equals(host)){
                return;
            }
            IntranetChatApplication.setInCall(false);
            endCall(getString(R.string.call_refuse_launch));
            LaunchVoiceCallActivity.this.finish();
        }

        @Override
        public void onReceiveWaitingConsentCall() {
            lastWaitingConsentCall = System.currentTimeMillis();
            TLog.d(TAG, "onReceiveWaitingConsentCall: " + lastWaitingConsentCall);
        }

        @Override
        public void onReceiveConsentOutTime() {
            TLog.d(TAG, "onReceiveConsentOutTime: ");
            IntranetChatApplication.setInCall(false);
            endCall(getString(R.string.call_refuse_answer));
            LaunchVoiceCallActivity.this.finish();
        }

        @Override
        public void onReceiveInCall(String host) {
            TLog.d(TAG, "onReceiveInCall: " + host);
            if (!LaunchVoiceCallActivity.this.host.equals(host)){
                return;
            }
//            Toast.makeText(LaunchVoiceCallActivity.this,"对方正在通话中",Toast.LENGTH_SHORT).show();
            endCall(getString(R.string.call_in_call));
//            EventBus.getDefault().post(new RecordCallBean(
//                    mIdentifier,ChatRoomConfig.CALL_IN_CALL,host,true));
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveInCall(InCallBean inCallBean){
        TLog.d(TAG, "onReceiveInCall: Main Thread");
        hungVoiceCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.start_voice_in_call).setVisibility(View.VISIBLE);
        Timer inCall = new Timer();
        TimerTask inCallTask = new TimerTask() {
            @Override
            public void run() {
                TLog.d(TAG, "run: finish");
                IntranetChatApplication.setInCall(false);
                finish();
            }
        };
        inCall.schedule(inCallTask,2*1000);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        VoiceCall.hungUpVoiceCall(host);
        IntranetChatApplication.setInCall(false);
        endCall(getString(R.string.call_refuse_launch_mine));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (affirmWaitingTimer != null){
            affirmWaitingTimer.cancel();
        }
        if(requestConsentTimer != null){
            requestConsentTimer.cancel();
        }
    }

    private OnReceiveCallHungUp onReceiveCallHungUp = new OnReceiveCallHungUp() {
        @Override
        public void onReceiveHungUpVoiceCall(String host) {
            TLog.d(TAG, "onReceiveHungUpVoiceCall: " + host);
            if (!LaunchVoiceCallActivity.this.host.equals(host)){
                return;
            }
            IntranetChatApplication.setInCall(false);
            endCall(getString(R.string.call_refuse_launch));
            finish();
        }
    };
}
