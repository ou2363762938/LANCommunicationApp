package com.skysoft.smart.intranetchat.ui.activity.voicecall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.skysoft.smart.intranetchat.app.BaseCallActivity;
import com.skysoft.smart.intranetchat.bean.signal.AvatarSignal;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.model.net_model.VoiceCall;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveCallHungUp;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveRequestConsent;
import com.skysoft.smart.intranetchat.tools.customstatusbar.CustomStatusBarBackground;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

public class AnswerVoiceCallActivity extends BaseCallActivity {

    private String TAG = AnswerVoiceCallActivity.class.getSimpleName();
    private ImageView refuseCall;
    private ImageView consentCall;
    private TextView mName;
    private ImageView headImg;
    private String host;

    private String name;
    private int mAvatar;

    private Timer consentOutTimer;
    private long intervalTime = 550;
    private long lastRequestConsentTime;
    private Timer affirmRequestConsentTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);
        CustomStatusBarBackground.customStatusBarTransparent(this);
        CustomStatusBarBackground.colorViewStatusBar(this,R.color.color_black,findViewById(R.id.custom_status_bar_background));
        IntranetChatApplication.getsCallback().setHungUpInAnswer(hungUp);
        IntranetChatApplication.getsCallback().setOnReceiveRequestConsent(onReceiveRequestConsent);

        mConfig = ChatRoomConfig.RECEIVE_VOICE_CALL;
        EventBus.getDefault().register(this);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mIdentifier = bundle.getString("identifier");
        host = bundle.getString("host");
        name = bundle.getString("name");
        mAvatar = bundle.getInt("avatar");

        init();

        mName = findViewById(R.id.answer_voice_phone_name);
        mName.setText(name);
        headImg = findViewById(R.id.answer_voice_phone_img);
        AvatarManager.getInstance().loadContactAvatar(this,headImg,mAvatar);

        refuseCall = findViewById(R.id.answer_voice_phone_cancel);
        consentCall = findViewById(R.id.answer_voice_phone_accept);

        refuseCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoiceCall.refuseVoiceCall(host);
                IntranetChatApplication.setInCall(false);
                RecordManager.
                        getInstance().
                        recordCall(
                                getString(R.string.call_refuse_answer_mine),
                                0,
                                ChatRoomConfig.CALL_REFUSE_ANSWER_MINE,
                                mIdentifier
                        );
                AnswerVoiceCallActivity.this.finish();
            }
        });

        consentCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoiceCall.consentVoiceCall(host);
                VoiceCallActivity.go(AnswerVoiceCallActivity.this,
                        host,
                        name,
                        mAvatar,
                        mIdentifier,
                        true);
                finish();
            }
        });

        lastRequestConsentTime = System.currentTimeMillis();
        consentOutTime();
    }

    private void consentOutTime(){
        consentOutTimer = new Timer();
        TimerTask consentOutTask = new TimerTask() {
            @Override
            public void run() {
                TLog.d(TAG, "run: responseConsentOutTime");
                VoiceCall.responseConsentOutTime(host);
                IntranetChatApplication.setInCall(false);
                RecordManager.
                        getInstance().
                        recordCall(
                                getString(R.string.call_refuse_answer),
                                0,
                                ChatRoomConfig.CALL_OUT_TIME_ANSWER,
                                mIdentifier
                        );
                AnswerVoiceCallActivity.this.finish();
            }
        };
        consentOutTimer.schedule(consentOutTask,60*1000);

        affirmRequestConsentTimer = new Timer();
        TimerTask affirmRequestConsentTask = new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastRequestConsentTime > intervalTime){
                    TLog.d(TAG, "run: affirmRequestConsentTimer answer voice call");
                    VoiceCall.hungUpVoiceCall(host);
                    IntranetChatApplication.setInCall(false);
                    RecordManager.
                            getInstance().
                            recordCall(
                                    getString(R.string.call_die),
                                    0,
                                    ChatRoomConfig.CALL_DIE_ANSWER,
                                    mIdentifier
                            );
                    AnswerVoiceCallActivity.this.finish();
                }
            }
        };
        affirmRequestConsentTimer.schedule(affirmRequestConsentTask,100,300);
    }

    private OnReceiveCallHungUp hungUp = new OnReceiveCallHungUp() {
        @Override
        public void onReceiveHungUpVoiceCall(String host) {
            if (!AnswerVoiceCallActivity.this.host.equals(host)){
                return;
            }
            IntranetChatApplication.setInCall(false);
            endAnswerCall(getString(R.string.call_refuse_answer));
            AnswerVoiceCallActivity.this.finish();
        }
    };

    private OnReceiveRequestConsent onReceiveRequestConsent = new OnReceiveRequestConsent() {
        @Override
        public void onReceiveRequestConsent() {
            lastRequestConsentTime = System.currentTimeMillis();
            TLog.d(TAG, "onReceiveRequestConsent: " + lastRequestConsentTime);
        }
    };

    public static void go(Activity activity,
                          String host,
                          String name,
                          int avatar,
                          String identifier) {
        Intent intent = new Intent(activity, AnswerVoiceCallActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putInt("avatar", avatar);
        bundle.putString("host", host);
        bundle.putString("identifier",identifier);
        intent.putExtras(bundle);
        activity.startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        VoiceCall.hungUpVoiceCall(host);
        IntranetChatApplication.setInCall(false);
        endAnswerCall(getString(R.string.call_refuse_answer_mine));
        AnswerVoiceCallActivity.this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (consentOutTimer != null){
            consentOutTimer.cancel();
        }
        if (affirmRequestConsentTimer != null){
            affirmRequestConsentTimer.cancel();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveAvatarSignal(AvatarSignal signal) {
        if (mIdentifier.equals(signal.receiver)) {
            AvatarManager.getInstance().loadContactAvatar(
                    AnswerVoiceCallActivity.this,
                    headImg,
                    mAvatar
            );
        }
    }
}
