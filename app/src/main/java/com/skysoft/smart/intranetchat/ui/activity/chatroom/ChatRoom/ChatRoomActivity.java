/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/30
 * Description: [Intranet Chat] [APP][UI] Chat Room
 */
package com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;

import com.google.gson.Gson;
import com.skysoft.smart.intranetchat.app.BaseActivity;
import com.skysoft.smart.intranetchat.bean.base.DeviceInfoBean;
import com.skysoft.smart.intranetchat.bean.signal.AvatarSignal;
import com.skysoft.smart.intranetchat.bean.signal.RecordSignal;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.model.chat.Message;
import com.skysoft.smart.intranetchat.model.chat.record.Code;
import com.skysoft.smart.intranetchat.model.chat.record.RecordAdapter;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.filemanager.FileManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.ChatRoom.KeyBoardUtils;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skysoft.smart.intranetchat.MainActivity;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.model.camera.manager.MyMediaPlayerManager;
import com.skysoft.smart.intranetchat.model.camera.videocall.Sender;
import com.skysoft.smart.intranetchat.tools.CreateNotifyBitmap;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;
import com.skysoft.smart.intranetchat.tools.toastutil.ToastUtil;
import com.skysoft.smart.intranetchat.ui.activity.camera.CameraActivity;
import com.skysoft.smart.intranetchat.ui.activity.camera.ShowPictureActivity;
import com.skysoft.smart.intranetchat.ui.activity.camera.VideoActivity;
import com.skysoft.smart.intranetchat.model.camera.entity.EventMessage;
import com.skysoft.smart.intranetchat.model.camera.manager.MyAudioManager;
import com.skysoft.smart.intranetchat.model.camera.widget.AudioRecordMicView;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.tools.ContentUriUtil;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.EstablishGroup.EstablishGroupActivity;
import com.skysoft.smart.intranetchat.ui.activity.login.OnSoftKeyboardStateChangedListener;
import com.skysoft.smart.intranetchat.ui.activity.videocall.LaunchVideoCallActivity;
import com.skysoft.smart.intranetchat.ui.activity.voicecall.LaunchVoiceCallActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

public class ChatRoomActivity extends BaseActivity implements View.OnClickListener,GestureDetector.OnGestureListener {

    private static long inputVoiceStartTime;
    private static long inputVoiceStopTime;
    private boolean sendVoice = true;
    private static String TAG = ChatRoomActivity.class.getSimpleName();
    private String mEntity;

    private ContactEntity mContact;
    private GroupEntity mGroup;

    private TextView mRoomName;
    private TextView mSendMessage;
    private TextView mEstablishGroup;
    private TextView mInputVoiceBox;
    private TextView mReplayReceiverMessage;

    private EditText mInputMessage;

    private ImageView mIconInputVoice;
    private ImageView mIconMoreFunction;
    private ImageView mIconBackImage;
    private ImageView mIconReplayImage;
    private ImageView mIconReplayCancel;

    private RecordAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private LinearLayout mMoreFunctionBox;
    private LinearLayout mBlankFunctionBox;

    private ConstraintLayout mCameraBox;
    private ConstraintLayout mPhotoBox;
    private ConstraintLayout mVoiceCallBox;
    private ConstraintLayout mVideoCallBox;
    private ConstraintLayout mFileBox;
    private ConstraintLayout mCameraShootingBox;
    private ConstraintLayout mReplayMessageBox;

    private GestureDetector sGestureDetector;

    private MyAudioManager myAudioManager;
    private AudioRecordMicView mAudioView;

    private int mReceiver;
    private int mNotifyId;
    private int mSendMessageType = 0;       //0 普通文字消息,1 @消息,2 回复消息
    private int level;
    private int mHeightDifference = 0;

    private Handler handler;

    private boolean isClickMoreFunction = false;
    private boolean isKeyboardOpened = false;

    private boolean isGroup = false;
    private boolean isRefresh = false;
    private boolean isUp = false;
    private boolean isStartVoiceAnimation;
    private boolean isAudioRecording = false;
    public static boolean sIsAudioRecording = false;

    private Map<ImageSpan,String> mNotifyReceivers = new HashMap<>();

    private ViewTreeObserver.OnGlobalLayoutListener mLayoutChangeListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Point screenSize = new Point();
            getWindowManager().getDefaultDisplay().getSize(screenSize);

            Rect rect = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            int heightDifference = screenSize.y - rect.bottom;
            boolean isKeyboardShowing = heightDifference > screenSize.y/3;
            mOSSCL.onSoftKeyboardStateChangedListener(isKeyboardShowing,heightDifference,screenSize.y);
        }
    };

    private OnSoftKeyboardStateChangedListener mOSSCL =
            new OnSoftKeyboardStateChangedListener() {

        @Override
        public void onSoftKeyboardStateChangedListener(boolean isKeyBoardShow, int keyboardHeight, int screenSize) {
            TLog.d(TAG,"---> isKeyBoardShow : " + isKeyBoardShow +
                    ", keyboardHeight : " + keyboardHeight +
                    ", screenSize : " + screenSize);
            isKeyboardOpened = isKeyBoardShow;

            //开启软键盘
            if (mHeightDifference == 0 &&
                    keyboardHeight != 0 &&
                    isKeyBoardShow) {
                isClickMoreFunction = false;
                //关闭软键盘
            } else if (mInputMessage.getVisibility() == View.VISIBLE &&
                    !isKeyBoardShow && mHeightDifference != 0 &&
                    keyboardHeight == 0) {

                if (!isClickMoreFunction && mMoreFunctionBox.getVisibility() != View.GONE) {
                    mMoreFunctionBox.setVisibility(View.GONE);
                }
                if (mBlankFunctionBox.getVisibility() != View.GONE) {
                    mBlankFunctionBox.setVisibility(View.GONE);
                }
                mInputMessage.clearFocus();
            }
            mHeightDifference = keyboardHeight;
        }
    };

    private OnClickReplayOrNotify mOnClickReplayOrNotify =
            new OnClickReplayOrNotify() {
        @Override
        public void onClickReplay(RecordEntity recordEntity, String name) {
//            mReplayMessageBox.setVisibility(View.VISIBLE);
//
//            if (recordEntity.getIsReceive() == ChatRoomConfig.RECEIVE_MESSAGE){
//                mIconReplayImage.setVisibility(View.GONE);
//                mReplayReceiverMessage.setText(name);
//                mReplayReceiverMessage.append(" :\n\t\t");
//                mReplayReceiverMessage.append(recordEntity.getContent());
//            }else if (recordEntity.getIsReceive() == ChatRoomConfig.RECEIVE_IMAGE){
//                mReplayReceiverMessage.setText(name);
//                mReplayReceiverMessage.append(" :");
//                mIconReplayImage.setVisibility(View.VISIBLE);
//                if (!TextUtils.isEmpty(recordEntity.getPath())){
//                    Glide.with(ChatRoomActivity.this).load(recordEntity.getPath()).into(mIconReplayImage);
//                }
//            }

//            onClickNotify(recordEntity,name);
//            mSendMessageType = 2;
        }

        @Override
        public void onClickNotify(RecordEntity recordEntity, String name) {
            if (!mNotifyReceivers.containsValue(recordEntity.getSender())){     //未被@
                mSendMessageType = 1;
                String notify = " @" + name + " ";
                int start = mInputMessage.getSelectionStart();       //当前光标位置
                SpannableStringBuilder spannableString = new SpannableStringBuilder(notify);    //构建SpannableStringBuilder
                Bitmap bitmap = CreateNotifyBitmap.notifyBitmap(ChatRoomActivity.this,notify);      //构建内容为notify的bitmap
                ImageSpan imageSpan = new ImageSpan(ChatRoomActivity.this,bitmap);      //构建内容为bitmap的ImageSpan
                spannableString.setSpan(imageSpan,
                        1,notify.length()-1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);        //SpannableStringBuilder天剑ImageSpan
                //原内容中插入SpannableStringBuilder
                Editable editable = mInputMessage.getEditableText();
                editable.insert(mInputMessage.getSelectionStart(),spannableString);
                mInputMessage.setText(editable);
                //移动光标到SpannableStringBuilder后
                mInputMessage.setSelection(start+notify.length());
                //记录插入的ImageSpan
                mNotifyReceivers.put(imageSpan, ContactManager.
                        getInstance().
                        getContact(recordEntity.getSender()).getName());
            }
        }
    };

    private TextWatcher mWatchInputMessage = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (after == 0){    //删除内容是after为0
                Editable editableText = mInputMessage.getEditableText();     //用于获得ImageSpan[]
                int remainImageSpan = 0;    //删除内容后剩余的ImageSpan数量

                if (start != 0){    //被删除内容的起始位置不是原内容的开端
                    //计算被删除内容之前的ImageSpan数量
                    ImageSpan[] spans = editableText.getSpans(0, start, ImageSpan.class);
                    if (null != spans && spans.length != 0){
                        remainImageSpan += spans.length;
                    }
                }else if (start+count == s.length()){       //所有内容内清空，清空mNotifyReceivers
                    mNotifyReceivers.clear();
                    return;
                }

                if (start+count != s.length()){     //被删除内容的末尾位置不是原内容结尾
                    //计算被删除内容之后的ImageSpan数量
                    ImageSpan[] spans = editableText.getSpans(start+count, s.length(), ImageSpan.class);
                    if (null != spans && spans.length != 0){
                        remainImageSpan += spans.length;
                    }
                }

                if (mNotifyReceivers.size() != remainImageSpan){    //值不同，有ImageSpan被删除
                    ImageSpan[] spans = editableText.getSpans(start, count, ImageSpan.class);   //被删除内容中的ImageSpan
                    for (int i = 0;i < spans.length; i++){
                        //mNotifyReceivers删除被删除的ImageSpan
                        mNotifyReceivers.remove(spans[i]);
                    }

                    if (remainImageSpan == 0){
                        mSendMessageType = 0;   //没有@任何人
                    }
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    public OnScrollToPosition onScrollToPosition = new OnScrollToPosition() {
        @Override
        public void onLoadViewOver(int size) {
            scroll();
        }
    };

    public static void go(Context context,String entity,boolean group){
        Intent intent = new Intent(context,ChatRoomActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatRoomConfig.NAME,entity);
        bundle.putBoolean(ChatRoomConfig.GROUP,group);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
//        IntranetChatApplication.getsCallback().setOnReceiveMessage(onReceiveMessage);
        EventBus.getDefault().register(this);
        sGestureDetector = new GestureDetector(ChatRoomActivity.this,this);
        initView();
        Intent intent = getIntent();
        initData(intent);
    }

    private void initData(Intent intent){
        Bundle bundle = intent.getExtras();
//        host = bundle.getString(ChatRoomConfig.HOST);
        mEntity = bundle.getString(ChatRoomConfig.NAME);
        isGroup = bundle.getBoolean(ChatRoomConfig.GROUP);
        if (isGroup) {
            mGroup = new Gson().fromJson(mEntity,GroupEntity.class);
            mReceiver = mGroup.getId();
            mNotifyId = mGroup.getNotifyId();
        } else {
            mContact = new Gson().fromJson(mEntity,ContactEntity.class);
            mReceiver = mContact.getId();
            mNotifyId = mContact.getNotifyId();
        }


        mAdapter = RecordManager.getInstance().initAdapter(this,mReceiver,isGroup);
        mAdapter.setHasStableIds(true);
        mAdapter.setOnScrollToPosition(onScrollToPosition);
        mAdapter.setOnClickReplayOrNotify(mOnClickReplayOrNotify);       //注册回复和@
        mRecyclerView.setAdapter(mAdapter);
        if (mAdapter.getItemCount() > 0) {
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount()-1);
        }

        mRoomName.setText(isGroup ? mGroup.getName() : mContact.getName());

        //单聊聊天室不允许建群
        if (!isGroup){
            mEstablishGroup.setVisibility(View.GONE);
        }else {
            findViewById(R.id.chat_room_more_function_voice_call_box).setVisibility(View.GONE);
            findViewById(R.id.chat_room_more_function_video_call_box).setVisibility(View.GONE);
            findViewById(R.id.chat_room_more_function_placeholder).setVisibility(View.VISIBLE);
        }

        //监听软键盘弹出
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(mLayoutChangeListener);

        //删除存在的通知
        if (IntranetChatApplication.getsNotificationManager() != null){
            IntranetChatApplication.getsNotificationManager().cancel(mNotifyId);
        }

        myAudioManager = new MyAudioManager();
    }

    private void initView() {
        mRecyclerView = findViewById(R.id.chat_room_recycler);
        mSendMessage = findViewById(R.id.chat_room_send_message);
        mInputMessage = findViewById(R.id.chat_room_input_message);
        mIconInputVoice = findViewById(R.id.chat_room_input_voice);
        mIconBackImage = findViewById(R.id.chat_room_back);

        mMoreFunctionBox = findViewById(R.id.chat_room_bottom_function);
        mBlankFunctionBox = findViewById(R.id.chat_room_bottom_blank_function);

        mCameraBox = findViewById(R.id.chat_room_more_function_camera_box);
        mPhotoBox = findViewById(R.id.chat_room_more_function_photo_box);
        mFileBox = findViewById(R.id.chat_room_more_function_file_box);
        mVoiceCallBox = findViewById(R.id.chat_room_more_function_voice_call_box);
        mVideoCallBox = findViewById(R.id.chat_room_more_function_video_call_box);
        mCameraShootingBox = findViewById(R.id.chat_room_more_function_video_box);
        mEstablishGroup = findViewById(R.id.chat_room_establish_group);
        mInputVoiceBox = findViewById(R.id.chat_room_input_voice_box);

        mIconMoreFunction = findViewById(R.id.chat_room_more_function);
        mRoomName = findViewById(R.id.chat_room_name);
        mAudioView = findViewById(R.id.chat_room_audio_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.getItemAnimator().setAddDuration(120);
        mRecyclerView.getItemAnimator().setChangeDuration(250);
        mRecyclerView.getItemAnimator().setMoveDuration(250);
        mRecyclerView.getItemAnimator().setRemoveDuration(120);
        //设置recyclerView的滑动惯性
        RoomUtils.setMaxFlingVelocity(mRecyclerView, 6000);

        ViewGroup.LayoutParams layoutParams = mMoreFunctionBox.getLayoutParams();
        layoutParams.height = DeviceInfoBean.getInstance().getKeyBroadHeight();
        mMoreFunctionBox.setLayoutParams(layoutParams);
        mBlankFunctionBox.setLayoutParams(layoutParams);

        mReplayReceiverMessage = findViewById(R.id.replay_receiver_message);
        mReplayMessageBox = findViewById(R.id.replay_box);
        mIconReplayImage = findViewById(R.id.replay_image);
        mIconReplayCancel = findViewById(R.id.replay_cancel);

        setListener();
    }

    private void setListener() {
        mSendMessage.setOnClickListener(this);
        mIconInputVoice.setOnClickListener(this);
        mInputMessage.setOnClickListener(this::onClick);
        mIconMoreFunction.setOnClickListener(this);
        mIconBackImage.setOnClickListener(this::onClick);
        //more function
        mCameraBox.setOnClickListener(this::onClick);
        mPhotoBox.setOnClickListener(this::onClick);
        mFileBox.setOnClickListener(this::onClick);
        mVoiceCallBox.setOnClickListener(this::onClick);
        mVideoCallBox.setOnClickListener(this::onClick);
        mCameraShootingBox.setOnClickListener(this::onClick);
        mEstablishGroup.setOnClickListener(this::onClick);

        mInputMessage.addTextChangedListener(mWatchInputMessage);        //监听输入框内容变化

        mIconReplayCancel.setOnClickListener(this::onClick);

        mInputMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && mMoreFunctionBox.getVisibility() == View.GONE){
                    isClickMoreFunction = false;
                    mBlankFunctionBox.setVisibility(View.VISIBLE);
                    scroll();
                }
            }
        });

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    if (!isRefresh){
                        isRefresh = true;
                        return;
                    }
                    if (mAdapter.getTopPosition() == 0 && isUp){
                        isRefresh = false;
                        RecordManager.getInstance().loadMoreRecord(mReceiver,isGroup ? 1 : 0);
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                isUp = dy < 0;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initData(intent);
        TLog.d(TAG, "onNewIntent: 调用了onNewIntent!");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveRecordSignal(RecordSignal signal) {
        mAdapter.notifyDataSetChanged();
        switch (signal.code) {
//            case Code.LOAD:
//                scroll();
//                break;
            case Code.LOAD_MORE:
                break;
            case Code.LOAD:
            case Code.RS:
                smoothScroll();
                break;

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveAvatarSignal(AvatarSignal signal) {
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveEventMessage(EventMessage event) {
        TLog.d(TAG,"-------- " + event.getMessage() + ", " + event.getType());
        if (event.getType() <= 4) {
            int type = 0;
            switch (event.getType()) {
                case 1:
                    type = Config.FILE_PICTURE;
                    break;
                case 2:
                    type = Config.FILE_VIDEO;
                    break;
                case 3:
                    type = Config.FILE_VOICE;
                    break;
            }
            FileEntity f = null;
            if (isGroup) {
                f = FileManager.getInstance().notify(mGroup,event.getMessage(),type, (int) event.getLength());
            } else {
                f = FileManager.getInstance().notify(mContact,event.getMessage(),type, (int) event.getLength());
            }
            RecordManager.getInstance().recordFile(f,-1);
            LatestManager.getInstance().send(mReceiver,f,isGroup);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        RecordManager.getInstance().destroy();
    }

    @Override
    public void onClick(View v) {
        isClickMoreFunction = false;
        switch (v.getId()) {
            case R.id.chat_room_send_message:
                if (!IntranetChatApplication.isNetWortState()) {
                    ToastUtil.toast(ChatRoomActivity.this, getString(R.string.Toast_text_non_lan));
                    break;
                }
                onClickSendMessage();
                break;
            case R.id.chat_room_input_voice:
//                if (QuickClickListener.isFastClick(100)) {
//                    clickVoice();
//                }
                clickVoice();
                break;
            case R.id.chat_room_more_function:
                clickMoreFunction();
                break;
            case R.id.chat_room_back:
                MainActivity.go(ChatRoomActivity.this);
                finish();
                break;
            case R.id.chat_room_more_function_voice_call_box:
                if (QuickClickListener.isFastClick()) {
                    if (!IntranetChatApplication.isNetWortState()) {
                        ToastUtil.toast(ChatRoomActivity.this, getString(R.string.Toast_text_non_lan));
                        break;
                    }
                    IntranetChatApplication.setInCall(true);
                    LaunchVoiceCallActivity.go(ChatRoomActivity.this,
                            mContact.getHost(),
                            mContact.getName(),
                            mContact.getAvatar(),
                            mContact.getIdentifier());
                }
                break;
            case R.id.chat_room_more_function_video_call_box:
                if (QuickClickListener.isFastClick()) {
                    if (!IntranetChatApplication.isNetWortState()) {
                        ToastUtil.toast(ChatRoomActivity.this, getString(R.string.Toast_text_non_lan));
                        break;
                    }
                    IntranetChatApplication.setInCall(true);
                    IntranetChatApplication.getmDatasQueue().clear();
                    Sender.mInputDatasQueue.clear();
                    LaunchVideoCallActivity.go(ChatRoomActivity.this,
                            mContact.getHost(),
                            mContact.getName(),
                            mContact.getAvatar(),
                            mContact.getIdentifier());
                }
                break;
            case R.id.chat_room_more_function_camera_box:
                if (QuickClickListener.isFastClick()) {
                    CameraActivity.goActivity(ChatRoomActivity.this);
                }
                break;
            case R.id.chat_room_more_function_photo_box:
                if (QuickClickListener.isFastClick()) {
                    goPhotoAlbum();
                }
                break;
            case R.id.chat_room_more_function_file_box:
                if (QuickClickListener.isFastClick()) {
                    goChoseFile();
                }
                break;
            case R.id.chat_room_more_function_video_box:
                if (QuickClickListener.isFastClick()) {
                    VideoActivity.goActivity(ChatRoomActivity.this);
                }
                break;
            case R.id.chat_room_establish_group:
                if (QuickClickListener.isFastClick(300)) {
                    TLog.d(TAG, "onClick: 查看群成员！！！");
                    EstablishGroupActivity.go(ChatRoomActivity.this, mGroup.getId());
                }
                break;
            case R.id.chat_room_input_message:
//                if (mMoreFunctionBox.getVisibility() != View.VISIBLE && mInputMessage.hasFocus()) {
//                    mMoreFunctionBox.setVisibility(View.INVISIBLE);
//                }
                clickInputMessage();
                break;
            case R.id.chat_room_input_voice_box:
//                if (mMoreFunctionBox.getVisibility() == View.VISIBLE) {
//                    fold();
//                }
                break;
            case R.id.replay_cancel:    //关闭回复消息展示框
                if (QuickClickListener.isFastClick()){
                    mReplayMessageBox.setVisibility(View.GONE);
                    mSendMessageType = 1;
                }
                break;
        }
    }

    private void clickInputMessage() {
        isClickMoreFunction = false;
    }

    /**
     * 开启多功能框，关闭空白框，关闭软键盘*/
    private void openMoreFunction() {
//        if (mMoreFunctionBox.getVisibility() != View.VISIBLE) {
//            mMoreFunctionBox.setVisibility(View.VISIBLE);
//        }
        mMoreFunctionBox.setVisibility(View.VISIBLE);
        mBlankFunctionBox.setVisibility(View.GONE);
        KeyBoardUtils.hintKeyBoard(this);
        scroll();
//        if (isKeyboardOpened) {
//            mBlankFunctionBox.setVisibility(View.GONE);
//            KeyBoardUtils.hintKeyBoard(this);
//        }
    }

    /**
     * 关闭多功能框，关闭软键盘
     * @param isClosingKeyboard 是否关闭软键盘*/
    private void closeMoreFunction(boolean isClosingKeyboard, boolean isOpeningKeyboard) {
//        if (mMoreFunctionBox.getVisibility() == View.VISIBLE) {
//            mMoreFunctionBox.setVisibility(View.INVISIBLE);
//        }
        if (isClosingKeyboard) {
//            mBlankFunctionBox.setVisibility(View.GONE);
            KeyBoardUtils.hintKeyBoard(this);
        } else if (isOpeningKeyboard) {
//            mBlankFunctionBox.setVisibility(View.VISIBLE);
//            mMoreFunctionBox.setVisibility(View.INVISIBLE);
            KeyBoardUtils.showInput(this,mInputMessage);
            scroll();
        }
    }

    private void switchVoice() {
        KeyBoardUtils.hintKeyBoard(this);
        onTouchInputMessageListener();
        mIconInputVoice.setImageResource(R.drawable.ic_keyboard);
        mInputMessage.setVisibility(View.GONE);
        mInputVoiceBox.setVisibility(View.VISIBLE);
//        KeyBoardUtils.hintKeyBoard(this);
        mMoreFunctionBox.setVisibility(View.GONE);
        mBlankFunctionBox.setVisibility(View.GONE);

    }

    private void switchMessage(boolean isOpenKeyboard) {
        mIconInputVoice.setImageResource(R.drawable.ic_voice_circle);
        mInputMessage.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        mInputMessage.setVisibility(View.VISIBLE);
        mInputVoiceBox.setVisibility(View.GONE);
        mInputVoiceBox.setOnTouchListener(null);
        if (isOpenKeyboard) {
            KeyBoardUtils.showInput(this,mInputMessage);
        }
    }

    private void clickMoreFunction() {
        //1. 输入文字，软键盘关闭 --》 打开多功能框
        //1.2. 输入文字，多功能框打开 --》 关闭多功能框，开启空白框，打开软键盘
        //2. 输入文字，软键盘打开 --》 开启多功能框，关闭空白框，关闭软键盘（区别主动关闭软键盘）
        //3. 输入语音，软键盘关闭 --》 切换为文字输入，打开多功能框
        isClickMoreFunction = true;
        int inputMessageVisibility = mInputMessage.getVisibility();
        int moreFunctionBoxVisibility = mMoreFunctionBox.getVisibility();
        int blankFunctionBoxVisibility = mBlankFunctionBox.getVisibility();
        if (inputMessageVisibility == 0) {      //输入文字
            if (moreFunctionBoxVisibility != 0 || isKeyboardOpened) {       //多功能框不可见
                openMoreFunction();
            } else {
                closeMoreFunction(false,true);
            }
        } else {
            switchMessage(false);
            openMoreFunction();
        }
    }

    private void clickVoice() {
        int inputMessageVisibility = mInputMessage.getVisibility();
        if (inputMessageVisibility == 0) {
            switchVoice();
        } else {
            switchMessage(true);
        }
    }

    private void scroll() {
        mRecyclerView.scrollToPosition(mAdapter.getItemCount()-1);
    }

    private void smoothScroll() {
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount()-1);
    }

    private void goChoseFile() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 3);
    }

    private void goPhotoAlbum() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }

    //输入框的按压事件
    public void onTouchInputMessageListener() {
        mInputVoiceBox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mInputMessage.getVisibility() == View.VISIBLE) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        inputVoiceStopTime = System.currentTimeMillis();
                        if (!isAudioRecording)
                            break;
                        if (isStartVoiceAnimation){
                            TLog.d(TAG, "onTouch: 手指松开被调用！");
                            if (inputVoiceStopTime - inputVoiceStartTime <= 999){
                                ToastUtil.toast(ChatRoomActivity.this, getString(R.string.ChatRoomActivity_inputVoiceTime_Toast));
                                sendVoice = false;
                            }else
                                sendVoice = true;
                            myAudioManager.stop();
                            stopAnimation();
                            isAudioRecording = false;
                            sIsAudioRecording = false;
                        }
                        break;
                }
                return sGestureDetector.onTouchEvent(event);
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        inputVoiceStartTime = System.currentTimeMillis();
//                        TLog.d(TAG, "onTouch: down");
//                        myAudioManager = new MyAudioManager();
//                        myAudioManager.startAudioRecordOnly(ChatRoomActivity.this);
//                        mAudioView.setVisibility(View.VISIBLE);
//                        audioAnimation();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        inputVoiceStopTime = System.currentTimeMillis();
//                        TLog.d(TAG, "onTouch: up");
////                        mAudioView.setVisibility(View.GONE);
//                        stopAnimation();
//                        if (inputVoiceStopTime - inputVoiceStartTime <= 999){
//                            ToastUtil.toast(ChatRoomActivity.this, getString(R.string.ChatRoomActivity_inputVoiceTime_Toast));
//                            sendVoice = false;
//                        }else
//                            sendVoice = true;
//                        myAudioManager.stop();
//                        break;
//                }
//                return true;
            }
        });
    }

    private void audioAnimation() {
        handler = new Handler();
        level = 0;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (level > mAudioView.getMaxLevel()) {
                    level = 0;
                }
                mAudioView.setLevel(level);
                level++;
                handler.postDelayed(this, 333);
            }
        });
    }

    public void stopAnimation() {
        mAudioView.setVisibility(View.GONE);
        handler.removeMessages(0);
    }

    private void onClickSendMessage() {
        if (mInputMessage.getVisibility() != View.VISIBLE) {
            return;
        }
        String message = mInputMessage.getText().toString();
        if (TextUtils.isEmpty(message)) {
            return;
        }
        if (message.length() > 30) {
            ToastUtil.toast(ChatRoomActivity.this, getString(R.string.chat_room_word_limit));
            return;
        }
        switch (mSendMessageType){
            case 0:     //普通消息
                sendCommonMessage(message);
                break;
//            case 1:     //@消息
//                sendAtMessage(message);
//                break;
//            case 2:     //回复消息
//                break;
        }
        mInputMessage.setText("");
    }

    /**
     * 发送@消息到指定用户*/
    private void sendAtMessage(String message) {
        Editable editableText = mInputMessage.getEditableText();
        ImageSpan[] spans = editableText.getSpans(0, editableText.length(), ImageSpan.class);
        String[] atIdentifiers = new String[spans.length];
        for (int i = 0; i<spans.length; i++){
            atIdentifiers[i] = mNotifyReceivers.get(spans[i]);
            String name = ContactManager.getInstance().getContact(atIdentifiers[i]).getName();
            int idx = message.indexOf('@' + name);
            atIdentifiers[i] = atIdentifiers[i] + '|' + idx + '|' + name.length();
        }

//        RecordEntity recordEntity = SendMessage.broadcastAtMessage(
//                new SendAtMessageBean(message, mReceiverIdentifier, host,
//                mReceiverAvatarPath, mEntity, isGroup, atIdentifiers));
//        mAdapter.add(recordEntity);
    }

    /**
     * 发送普通消息到指定用户
     * @param message 发送的消息*/
    private void sendCommonMessage(String message){
        //发送消息
        if (isGroup) {
            Message.getInstance().send(mGroup,message);
        } else {
            Message.getInstance().send(mContact,message);
        }

        //刷新聊天列表和消息列表
        RecordManager.getInstance().recordText(message,-1);
        LatestManager.getInstance().send(mReceiver,message,isGroup);
    }

    public void hidden(){
        if (mInputMessage.getVisibility() == View.VISIBLE){
            mMoreFunctionBox.setVisibility(View.GONE);
            KeyBoardUtils.hintKeyBoard(this);
        }
    }

    public void fold(){
        if (mMoreFunctionBox.getVisibility() != View.VISIBLE){
            mMoreFunctionBox.setVisibility(View.VISIBLE);
        }else {
            mMoreFunctionBox.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK) {
            ShowPictureActivity.goActivity(this,data.getData(),false);
        } else if (requestCode == 3 && resultCode == RESULT_OK) {
            //文件
            String realFilePathFromUri = ContentUriUtil.getPath(this,data.getData());
            String suffix = realFilePathFromUri.substring(realFilePathFromUri.lastIndexOf("."));
            EventMessage eventMessage = new EventMessage();
            eventMessage.setMessage(realFilePathFromUri);
            if (suffix.equals(".mp4")){
                eventMessage.setType(2);
            }else if (suffix.equals(".jpg") || suffix.equals(".png")){
                eventMessage.setType(1);
            }else {
                eventMessage.setType(4);
            }

//            FileManager.getInstance().notify();
//            SendMessage.sendCommonMessage(eventMessage, new SendMessageBean("", mReceiverIdentifier, host,
//                    mReceiverAvatarPath, mEntity, isGroup),false);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MainActivity.go(ChatRoomActivity.this);
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyMediaPlayerManager.getsInstance().stop();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        inputVoiceStartTime = System.currentTimeMillis();
        isStartVoiceAnimation = false;
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        if (MyMediaPlayerManager.getsInstance().isPlaying())MyMediaPlayerManager.getsInstance().stop();
        isStartVoiceAnimation = true;
        myAudioManager = new MyAudioManager();
        myAudioManager.startAudioRecordOnly(ChatRoomActivity.this);
        mAudioView.setVisibility(View.VISIBLE);
        audioAnimation();
        isAudioRecording = true;
        sIsAudioRecording = true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        TLog.d(TAG, "onLongPress: 被调用");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMoreFunctionBox.getVisibility() == View.VISIBLE && mInputMessage.getVisibility() == View.VISIBLE){
            hidden();
        }
    }
}
