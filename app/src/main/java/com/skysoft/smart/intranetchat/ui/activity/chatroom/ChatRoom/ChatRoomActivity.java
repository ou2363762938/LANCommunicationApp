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

import com.skysoft.smart.intranetchat.app.BaseActivity;
import com.skysoft.smart.intranetchat.bean.SendAtMessageBean;
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

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.MainActivity;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.SendMessageBean;
import com.skysoft.smart.intranetchat.model.net_model.SendMessage;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveMessage;
import com.skysoft.smart.intranetchat.bean.GroupMembersBean;
import com.skysoft.smart.intranetchat.bean.LoadResourceBean;
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
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.ChatRecordDao;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.LatestChatHistoryEntity;
import com.skysoft.smart.intranetchat.model.network.bean.FileBean;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.server.IntranetChatServer;
import com.skysoft.smart.intranetchat.tools.ContentUriUtil;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.EstablishGroup.EstablishGroupActivity;
import com.skysoft.smart.intranetchat.ui.activity.login.OnSoftKeyboardStateChangedListener;
import com.skysoft.smart.intranetchat.ui.activity.videocall.LaunchVideoCallActivity;
import com.skysoft.smart.intranetchat.ui.activity.voicecall.LaunchVoiceCallActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.skysoft.smart.intranetchat.MainActivity.CALL_FROM_OTHER;

public class ChatRoomActivity extends BaseActivity implements View.OnClickListener,GestureDetector.OnGestureListener {

    private static long inputVoiceStartTime;
    private static long inputVoiceStopTime;
    private boolean sendVoice = true;
    private static String TAG = ChatRoomActivity.class.getSimpleName();
    private String host;
    private String myHost;
    private String mReceiverName;
    private String mReceiverAvatarPath;
    private String mReceiverIdentifier;

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

    private ChatRoomMessageAdapter mAdapter;
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

    private int mNotifyId;
    private int mSendMessageType = 0;       //0 普通文字消息,1 @消息,2 回复消息
    private int level;

    private Handler handler;

    private boolean isInputVoice = false;
    private boolean isClosing = true;
    private boolean isGroup = false;
    private boolean isInputMessage = false;
    private boolean isRefresh = false;
    private boolean isUp = false;
    private boolean isStartVoiceAnimation;
    private boolean isHiddenSoftKeyboard = false;
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
            if (!isInputMessage && isKeyBoardShow && !isHiddenSoftKeyboard){
                isInputMessage = true;
                mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
            }
            if (!isKeyBoardShow && !isClosing && isInputMessage){
                mMoreFunctionBox.setVisibility(View.GONE);
                isClosing = true;
                isInputMessage = false;
                mInputMessage.clearFocus();
            }
            if (isClosing){
                isInputMessage = false;
            }
        }
    };

    private OnClickReplayOrNotify mOnClickReplayOrNotify =
            new OnClickReplayOrNotify() {
        @Override
        public void onClickReplay(ChatRecordEntity recordEntity, String name) {
            mReplayMessageBox.setVisibility(View.VISIBLE);

            if (recordEntity.getIsReceive() == ChatRoomConfig.RECEIVE_MESSAGE){
                mIconReplayImage.setVisibility(View.GONE);
                mReplayReceiverMessage.setText(name);
                mReplayReceiverMessage.append(" :\n\t\t");
                mReplayReceiverMessage.append(recordEntity.getContent());
            }else if (recordEntity.getIsReceive() == ChatRoomConfig.RECEIVE_IMAGE){
                mReplayReceiverMessage.setText(name);
                mReplayReceiverMessage.append(" :");
                mIconReplayImage.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(recordEntity.getPath())){
                    Glide.with(ChatRoomActivity.this).load(recordEntity.getPath()).into(mIconReplayImage);
                }
            }

            onClickNotify(recordEntity,name);
            mSendMessageType = 2;
        }

        @Override
        public void onClickNotify(ChatRecordEntity recordEntity, String name) {
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
                mNotifyReceivers.put(imageSpan,recordEntity.getSender());
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

    private OnReceiveMessage onReceiveMessage = new OnReceiveMessage() {
        @Override
        public void onReceiveMessage(MessageBean message, String host) {
            if (message.getReceiver().equals(mReceiverIdentifier)){
                ChatRoomActivity.this.host = host;
            }
        }

        @Override
        public void onReceiveFile(FileBean fileBean, String host) {
            TLog.d(TAG, "onReceiveFile: ");
            ChatRoomActivity.this.host = host;
        }

        @Override
        public void onReceiveAndSaveFile(String sender, String receiver, String identifier, String path) {

        }
    };

    public OnScrollToPosition onScrollToPosition = new OnScrollToPosition() {
        @Override
        public void onLoadViewOver(int size) {
            mRecyclerView.scrollToPosition(size - 1);
        }
    };

    public static void go(Context context,String name,String avatar,String host,String identifier,boolean group){
        Intent intent = new Intent(context,ChatRoomActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatRoomConfig.NAME,name);
        bundle.putString(ChatRoomConfig.AVATAR,avatar);
        bundle.putString(ChatRoomConfig.HOST,host);
        bundle.putString(ChatRoomConfig.IDENTIFIER,identifier);
        bundle.putBoolean(ChatRoomConfig.GROUP,group);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        IntranetChatApplication.getsCallback().setOnReceiveMessage(onReceiveMessage);
        EventBus.getDefault().register(this);
        sGestureDetector = new GestureDetector(ChatRoomActivity.this,this);
        initView();
        Intent intent = getIntent();
        initData(intent);
    }

    private void initData(Intent intent){
        Bundle bundle = intent.getExtras();
        host = bundle.getString(ChatRoomConfig.HOST);
        mReceiverName = bundle.getString(ChatRoomConfig.NAME);
        mReceiverAvatarPath = bundle.getString(ChatRoomConfig.AVATAR);
        mReceiverIdentifier = bundle.getString(ChatRoomConfig.IDENTIFIER);
        isGroup = bundle.getBoolean(ChatRoomConfig.GROUP);

        //获得历史聊天记录
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatRecordDao chatRecordDao = MyDataBase.getInstance().getChatRecordDao();
                int number = chatRecordDao.getNumber(mReceiverIdentifier);
                int pageNum = 20;
                List<ChatRecordEntity> all = chatRecordDao.getAll(mReceiverIdentifier,number-pageNum,pageNum);
                if (all != null && all.size() != 0){
                    EventBus.getDefault().post(all);
                }
            }
        }).start();

        //加载历史聊天记录
        IntranetChatApplication.
                setsChatRoomMessageAdapter(
                        new ChatRoomMessageAdapter(
                                this,
                                IntranetChatApplication.getsMineAvatarPath(),
                                mReceiverIdentifier,isGroup)
                );
        mAdapter = IntranetChatApplication.getsChatRoomMessageAdapter();
        mAdapter.setHasStableIds(true);
        mAdapter.setHasStableIds(true);
        mAdapter.setOnScrollToPosition(onScrollToPosition);
        mAdapter.setOnClickReplayOrNotify(mOnClickReplayOrNotify);       //注册回复和@
        mRecyclerView.setAdapter(mAdapter);

        GroupMembersBean bean = new GroupMembersBean();
        bean.setmMemberName(mReceiverName);
        bean.setmMemberAvatarPath(mReceiverAvatarPath);
        mRoomName.setText(mReceiverName);

        myHost = IntranetChatServer.getHostIP();

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

        //刷新未读消息数
        LatestChatHistoryEntity item = IntranetChatApplication.sLatestChatHistoryMap.get(mReceiverIdentifier);
        if (null != item){
            int number = IntranetChatApplication.getmTotalUnReadNumber() - item.getUnReadNumber();
            item.setUnReadNumber(0);
            if (number == 0){
                //B: [PT-80][Intranet Chat] [APP][UI] TextBadgeItem 一直为红色,Allen Luo,2019/11/12
                IntranetChatApplication.getmTextBadgeItem().hide();
            }else {
                IntranetChatApplication.getmTextBadgeItem().show().setText(String.valueOf(number));
                //E: [PT-80][Intranet Chat] [APP][UI] TextBadgeItem 一直为红色,Allen Luo,2019/11/12
            }
            IntranetChatApplication.setmTotalUnReadNumber(number);
            if (IntranetChatApplication.getsMessageListAdapter() != null){
                IntranetChatApplication.getsMessageListAdapter().notifyDataSetChanged();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MyDataBase.getInstance().getLatestChatHistoryDao().update(item);
                }
            }).start();
        }

        //删除存在的通知
        ContactEntity next = null;
        if (isGroup){
            next = IntranetChatApplication.sGroupContactMap.get(mReceiverIdentifier);
        }else {
            next = IntranetChatApplication.sContactMap.get(mReceiverIdentifier);
        }
        if (null != next){
            mNotifyId = next.getNotifyId();
            if (IntranetChatApplication.getsNotificationManager() != null){
                IntranetChatApplication.getsNotificationManager().cancel(mNotifyId);
            }
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
        layoutParams.height = IntranetChatApplication.getsEquipmentInfoEntity().getSoftInputHeight();
        mMoreFunctionBox.setLayoutParams(layoutParams);

        mInputMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && isClosing){
                    mMoreFunctionBox.setVisibility(View.INVISIBLE);
                    isClosing = false;
                    mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                }
            }
        });

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    TLog.d(TAG, "onScrollStateChanged: 底部 TopPosition = " + mAdapter.getTopPosition());
                    if (!isRefresh){
                        isRefresh = true;
                        return;
                    }
                    if (mAdapter.getTopPosition() == 0 && isUp){
                        isRefresh = false;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ChatRecordDao chatRecordDao = MyDataBase.getInstance().getChatRecordDao();
                                int number = chatRecordDao.getNumber(mReceiverIdentifier);
                                if (number == mAdapter.getItemCount()){
                                    List<ChatRecordEntity> all = new ArrayList<>();
                                    EventBus.getDefault().post(all);
                                    return;
                                }else {
                                    int more = 15;
                                    int start = number - mAdapter.getItemCount() - 15;
                                    if (start < 0){
                                        start = 0;
                                        more = number - mAdapter.getItemCount();
                                    }
                                    List<ChatRecordEntity> all = chatRecordDao.getAll(mReceiverIdentifier, start, more);
                                    if (start == 0){
                                        Iterator<ChatRecordEntity> iterator = all.iterator();
                                        while (iterator.hasNext()){
                                            TLog.d(TAG, "run: " + iterator.next().toString());
                                        }
                                    }
                                    if (all != null && all.size() != 0){
                                        EventBus.getDefault().post(all);
                                    }
                                }
                            }
                        }).start();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                isUp = dy < 0;
            }
        });

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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initData(intent);
        TLog.d(TAG, "onNewIntent: 调用了onNewIntent!");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void loadChatHistory(List<ChatRecordEntity> chatHistory) {
        if (chatHistory.size() == 0){
            ToastUtil.toast(ChatRoomActivity.this, getString(R.string.ChatRoomActivity_loadChatHistory_toast_text));
            return;
        }
        mAdapter.addAll(chatHistory);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleReceiveAndSaveFile(LoadResourceBean loadResourceBean) {
        TLog.d(TAG, "onReceiveAndSaveFile: run: receiver = " + loadResourceBean.getRecordEntity());
        mAdapter.add(loadResourceBean.getRecordEntity(),true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveEventMessage(EventMessage eventMessage){
        TLog.d(TAG, "onReceiveEventMessage: eventMessage.getType() = " + eventMessage.getType());
        if (eventMessage.getType() == CALL_FROM_OTHER && isAudioRecording){
            myAudioManager.stopAndNotSend();
            stopAnimation();
            isAudioRecording = false;
            sIsAudioRecording = false;
            return;
        }
        if (!(eventMessage.getType() == 1 || eventMessage.getType() == 2 || eventMessage.getType() == 3)){
            return;
        }
        if (!sendVoice && eventMessage.getType() == 3){
            return;
        }
        ChatRecordEntity recordEntity = SendMessage.sendCommonMessage(eventMessage, new SendMessageBean("", mReceiverIdentifier, host,
                mReceiverAvatarPath, mReceiverName, isGroup),true);
        if (null != recordEntity){
            mAdapter.add(recordEntity);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        IntranetChatApplication.setsChatRoomMessageAdapter(null);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.chat_room_send_message:
                if (!IntranetChatApplication.isNetWortState()) {
                    ToastUtil.toast(ChatRoomActivity.this, getString(R.string.Toast_text_non_lan));
                    break;
                }
                onClickSendMessage();
                break;
            case R.id.chat_room_input_voice:
                if (QuickClickListener.isFastClick(100)) {
                    onClickInputVoice();
                }
                break;
            case R.id.chat_room_more_function:
                if (isInputVoice) {
                    TLog.d(TAG, "onClick: 2");
                    mMoreFunctionBox.setVisibility(View.VISIBLE);
                    isClosing = false;
                    mIconInputVoice.setImageResource(R.drawable.ic_voice_circle);
                    mInputMessage.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                    mInputMessage.setVisibility(View.VISIBLE);
                    mInputVoiceBox.setVisibility(View.GONE);
                    mInputVoiceBox.setOnTouchListener(null);
                    isInputVoice = false;
                    mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                    break;
                }
                if (isClosing && !isInputMessage) {
                    TLog.d(TAG, "onClick: 4");
                    mMoreFunctionBox.setVisibility(View.VISIBLE);
                    isClosing = false;
                    mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                    break;
                }
                if (isInputMessage) {
                    isHiddenSoftKeyboard = true;
                    hintKeyBoard();
                    mMoreFunctionBox.setVisibility(View.VISIBLE);
                } else {
                    isHiddenSoftKeyboard = false;
                    isInputMessage = true;
                    KeyBoardUtils.showInput(this,mInputMessage);
                    mMoreFunctionBox.setVisibility(View.INVISIBLE);
                }
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
                    LaunchVoiceCallActivity.go(ChatRoomActivity.this, host, mReceiverName, mReceiverAvatarPath, mReceiverIdentifier);
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
                    LaunchVideoCallActivity.go(ChatRoomActivity.this, host, mReceiverName, mReceiverAvatarPath, mReceiverIdentifier);
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
                    EstablishGroupActivity.go(ChatRoomActivity.this, mReceiverIdentifier, isGroup);
                }
                break;
            case R.id.chat_room_input_message:
                isHiddenSoftKeyboard = false;
                if (!isInputMessage && mInputMessage.hasFocus()) {
                    if (isClosing) {
                        mMoreFunctionBox.setVisibility(View.INVISIBLE);
                        isClosing = false;
                    }
                }
                break;
            case R.id.chat_room_input_voice_box:
                if (!isClosing) {
                    fold();
                }
                break;
            case R.id.replay_cancel:    //关闭回复消息展示框
                if (QuickClickListener.isFastClick()){
                    mReplayMessageBox.setVisibility(View.GONE);
                    mSendMessageType = 1;
                }
                break;
        }
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
                TLog.d(TAG, "onTouch: isInputVoice = " + isInputVoice);
                if (!isInputVoice) {
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

    private void onClickInputVoice() {
        if (!isClosing){
            mMoreFunctionBox.setVisibility(View.GONE);
            isClosing = true;
        }
        if (isInputMessage){
            hintKeyBoard();
        }
        if (isInputVoice) {
            //输入文字
            mIconInputVoice.setImageResource(R.drawable.ic_voice_circle);
            mInputMessage.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            mInputMessage.setVisibility(View.VISIBLE);
            mInputVoiceBox.setVisibility(View.GONE);
            mInputVoiceBox.setOnTouchListener(null);
        } else {
            //输入语音
            onTouchInputMessageListener();
            mIconInputVoice.setImageResource(R.drawable.ic_keyboard);
            mInputMessage.setVisibility(View.GONE);
            mInputVoiceBox.setVisibility(View.VISIBLE);
            isInputMessage = false;
        }
        isInputVoice = !isInputVoice;
    }

    private void onClickSendMessage() {
        if (isInputVoice) {
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
            case 1:     //@消息
                sendAtMessage(message);
                break;
            case 2:     //回复消息
                break;
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
            String name = IntranetChatApplication.sContactMap.get(atIdentifiers[i]).getName();
            int idx = message.indexOf('@' + name);
            atIdentifiers[i] = atIdentifiers[i] + '|' + idx + '|' + name.length();
        }

        ChatRecordEntity recordEntity = SendMessage.broadcastAtMessage(new SendAtMessageBean(message, mReceiverIdentifier, host,
                mReceiverAvatarPath, mReceiverName, isGroup, atIdentifiers));
        mAdapter.add(recordEntity);
    }

    /**
     * 发送普通消息到指定用户
     * @param message 发送的消息*/
    private void sendCommonMessage(String message){
        ChatRecordEntity recordEntity = SendMessage.sendCommonMessage(new SendMessageBean(message, mReceiverIdentifier, host,
                mReceiverAvatarPath, mReceiverName, isGroup));
        mAdapter.add(recordEntity);
    }

    public void hidden(){
        if (!isInputVoice){
            isHiddenSoftKeyboard = true;
            mMoreFunctionBox.setVisibility(View.GONE);
            isClosing = true;
            hintKeyBoard();
        }
    }

    //关闭软键盘
    public void hintKeyBoard() {
        isInputMessage = false;
        KeyBoardUtils.hintKeyBoard(this);
    }

    public void fold(){
        if (isClosing){
            mMoreFunctionBox.setVisibility(View.VISIBLE);
        }else {
            mMoreFunctionBox.setVisibility(View.GONE);
        }
        isClosing = !isClosing;
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
            SendMessage.sendCommonMessage(eventMessage, new SendMessageBean("", mReceiverIdentifier, host,
                    mReceiverAvatarPath, mReceiverName, isGroup),false);
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
        if (!isClosing && isInputMessage){
            hidden();
        }
    }
}
