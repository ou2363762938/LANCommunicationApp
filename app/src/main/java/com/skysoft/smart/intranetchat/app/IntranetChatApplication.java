/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication] Create a new process service with message sending and receiving.
 */
package com.skysoft.smart.intranetchat.app;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.bean.base.DeviceInfoBean;
import com.skysoft.smart.intranetchat.bean.signal.LatestSignal;
import com.skysoft.smart.intranetchat.bean.signal.MessageSignal;
import com.skysoft.smart.intranetchat.customize.StatusBarLayout;
import com.skysoft.smart.intranetchat.customize.TitleLinearLayout;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.chat.Message;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.filemanager.FileManager;
import com.skysoft.smart.intranetchat.model.filemanager.FilePath;
import com.skysoft.smart.intranetchat.model.group.GroupManager;
import com.skysoft.smart.intranetchat.model.latest.Code;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;
import com.skysoft.smart.intranetchat.tools.customstatusbar.CustomStatusBarBackground;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;

import com.ashokvarma.bottomnavigation.TextBadgeItem;
import com.skysoft.smart.intranetchat.IIntranetChatAidlInterface;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.impl.OnReceiveCallBean;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.MessageBean;
import com.skysoft.smart.intranetchat.server.IntranetChatServer;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;


import static androidx.core.app.NotificationCompat.VISIBILITY_SECRET;

//B: [Intranet Chat] [APP] [Communication] Create a new process service with message sending and receiving,Oliver ou,2019/10/30

public class IntranetChatApplication extends Application {
    private final String TAG = IntranetChatApplication.class.getSimpleName();

    public static IIntranetChatAidlInterface sAidlInterface = null;

    private static IntranetChatCallback sCallback = new IntranetChatCallback();

    private static Context mAppContext;

    private static TextBadgeItem mTextBadgeItem;
    private static ArrayBlockingQueue<byte[]> mDatasQueue = new ArrayBlockingQueue<byte[]>(1024);
    private static int sTotalUnReadNumber = 0;
    private static long sBaseTimeLine;
    private static NotificationManager sNotificationManager;
    private static int sCurrentProgress;
    private static int sCreateProgress;
    private static boolean sInCall = false;
    private static long sStartCallTime = 0;
    private static long sEndCallTime = 0;
    private static boolean sNetWorkState = true;
    private static int sNetWorkType = 0;
    private static String sHostIp;

    public static Context getContext() {
        return mAppContext;
    }

    public static void setContext(Context context){
        mAppContext = context;
    }

    public static String getHostIp() {
        return sHostIp;
    }

    public static void setHostIp(String sHostIp) {
        IntranetChatApplication.sHostIp = sHostIp;
    }

    public static int getsNetWorkType() {
        return sNetWorkType;
    }

    public static void setsNetWorkType(int sNetWorkType) {
        IntranetChatApplication.sNetWorkType = sNetWorkType;
    }

    public static boolean isNetWortState() {
        return sNetWorkState;
    }

    public static void setNetWorkState(boolean sNewWortState) {
        IntranetChatApplication.sNetWorkState = sNewWortState;
    }

    public static long getStartCallTime() {
        return sStartCallTime;
    }

    public static void setStartCallTime(long mStartCallTime) {
        IntranetChatApplication.sStartCallTime = mStartCallTime;
    }

    public static long getEndCallTime() {
        return sEndCallTime;
    }

    public static void setEndCallTime(long mEndCallTime) {
        IntranetChatApplication.sEndCallTime = mEndCallTime;
    }

    public static boolean isInCall() {
        return sInCall;
    }

    public static void setInCall(boolean sInCall) {
        IntranetChatApplication.sInCall = sInCall;
    }


    public static void setsCreateProgress(int sCreateProgress) {
        IntranetChatApplication.sCreateProgress = sCreateProgress;
    }

    public static ArrayBlockingQueue<byte[]> getmDatasQueue() {
        return IntranetChatApplication.mDatasQueue;
    }

    public static void setmDatasQueue(ArrayBlockingQueue<byte[]> mDatasQueue) {
        IntranetChatApplication.mDatasQueue = mDatasQueue;
    }

    public static TextBadgeItem getmTextBadgeItem() {
        return mTextBadgeItem;
    }

    public static void setmTextBadgeItem(TextBadgeItem mTextBadgeItem) {
        IntranetChatApplication.mTextBadgeItem = mTextBadgeItem;
    }

    public static int getmTotalUnReadNumber() {
        return sTotalUnReadNumber;
    }

    public static void setTotalUnReadNumber(int mTotalUnReadNumber) {
        IntranetChatApplication.sTotalUnReadNumber = mTotalUnReadNumber;
    }

    public static long getsBaseTimeLine() {
        return sBaseTimeLine;
    }

    public static NotificationManager getsNotificationManager() {
        return sNotificationManager;
    }

    public static void setsNotificationManager(NotificationManager sNotificationManager) {
        IntranetChatApplication.sNotificationManager = sNotificationManager;
    }

    /**
     * 处理接受到的文字类消息*/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessageSignal(MessageSignal signal) {

        if (signal.getBean() != null) {
            TLog.d(TAG,"------> " + signal.toString());
            setUnReadNumber(signal.isIn(),
                    signal.getBean());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveLatestSignal(LatestSignal signal) {
        switch (signal.code) {
            case Code.INIT_UNREAD:
                initTotalUnReadNumber(signal.unRead);
                break;
            case Code.CLICK_ITEM:
                reduceTotalUnReadNumber(signal.unRead);
                break;
        }
    }

    private void setUnReadNumber(boolean isIn,
                                 MessageBean messageBean) {
        if (!isIn) {
            addTotalUnReadNumber();
            notification(messageBean,messageBean.getHost());
        }
    }

    private void initTotalUnReadNumber(int unRead) {
        TLog.d(TAG,"--------> Init UnRead " + unRead);
        sTotalUnReadNumber = unRead;
        if (sTotalUnReadNumber == 0) {
            mTextBadgeItem.hide();
        } else {
            mTextBadgeItem.show();
            mTextBadgeItem.setText(String.valueOf(sTotalUnReadNumber));
        }
    }

    private void reduceTotalUnReadNumber(int unRead) {
        if (sTotalUnReadNumber == 0) {
            return;
        }
        sTotalUnReadNumber -= unRead;
        if (sTotalUnReadNumber <= 0) {
            sTotalUnReadNumber = 0;
            mTextBadgeItem.hide();
        } else {
            mTextBadgeItem.show();
            mTextBadgeItem.setText(String.valueOf(sTotalUnReadNumber));
        }
    }

    private static void addTotalUnReadNumber() {
        sTotalUnReadNumber += 1;

        mTextBadgeItem.show();
        mTextBadgeItem.setText(String.valueOf(sTotalUnReadNumber));
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TLog.d(TAG, "onServiceConnected: ");
            sAidlInterface = IIntranetChatAidlInterface.Stub.asInterface(service);
            try {
                sAidlInterface.registerCallback(sCallback);
                sAidlInterface.initUserInfoBean(GsonTools.toJson(MineInfoManager.getInstance().getUserInfo()));
                sCallback.setOnReceiveCallBean(onReceiveCallBean);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Intent intent = new Intent(IntranetChatApplication.this, IntranetChatServer.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
//        MyDataBase.initAdapter(this);
        sBaseTimeLine = initBaseTimeLine();
        boolean isCurrentProcess = getApplicationContext().getPackageName().equals
                (getCurrentProcessName());
        int pid = android.os.Process.myPid();
        sCreateProgress = pid;
        if (isCurrentProcess) {
            Intent intent = new Intent(this, IntranetChatServer.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            sCurrentProgress = pid;
            init();
        }
    }

    public void init() {
        EventBus.getDefault().register(this);
        MyDataBase.init(this);
        DeviceInfoBean.init(this);
        MineInfoManager.init(this);
        RecordManager.init();
        Message.init(this);
        FileManager.init(this);
        FilePath.init(this);
        setContext(this);

        StatusBarLayout.sRootLayoutHeight = CustomStatusBarBackground.getStatusBarHeight(this);
        TitleLinearLayout.sLayoutHeight = getResources().getDimensionPixelOffset(R.dimen.dp_55);
    }

    private long initBaseTimeLine() {
        Date date = new Date();
        date.setYear(2019);
        date.setMonth(11);
        date.setDate(11);
        return date.getTime();
    }

    private String getCurrentProcessName() {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService
                (Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                processName = process.processName;
            }
        }
        return processName;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        try {
            sAidlInterface.unregisterCallback(sCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(mConnection);
        EventBus.getDefault().unregister(this);
    }

    public static IntranetChatCallback getsCallback() {
        return sCallback;
    }

    @SuppressLint("NewApi")
    public static boolean isNotificationEnabled(Context context) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = applicationInfo.uid;
        Class appOpsClass;
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod("checkOpNoThrow", Integer.TYPE, Integer.TYPE, String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField("OP_POST_NOTIFICATION");
            int value = (Integer) opPostNotificationValue.get(Integer.class);
            return ((Integer) checkOpNoThrowMethod.invoke(appOpsManager, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void toSettingPage(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
        }
        context.startActivity(intent);
    }

    public static void setsCallback(IntranetChatCallback sCallback) {
        IntranetChatApplication.sCallback = sCallback;
    }

    private OnReceiveCallBean onReceiveCallBean = new OnReceiveCallBean() {
        @Override
        public void onReceiveVoiceCallData(byte[] data) {
            mDatasQueue.offer(data);
        }
    };

    /**
     * 通知栏通知用户
     * @param message 通知的消息
     * @param host 对方IP地址*/
    public void notification(MessageBean message, String host) {
        if (sNotificationManager == null) {
            sNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        boolean group = !message.getReceiver().equals(message.getSender());

        String name = null;
        String avatar = null;
        int notifyId = 0;
        String entity = null;
        TLog.d(TAG, "notification: notifyId " + message.getReceiver());
        if (group) {
            GroupEntity groupEntity = GroupManager.getInstance().getGroup(message.getReceiver());
            entity = GsonTools.toJson(groupEntity);
            name = groupEntity.getName();
            avatar = AvatarManager.getInstance().getAvatarPath(groupEntity.getAvatar());
            notifyId = groupEntity.getNotifyId();
        } else {
            ContactEntity contact = ContactManager.getInstance().getContact(message.getSender());
            entity = GsonTools.toJson(contact);
            name = contact.getName();
            avatar = AvatarManager.getInstance().getAvatarPath(contact.getAvatar());
            notifyId = contact.getNotifyId();
        }

        Intent mainIntent = new Intent(IntranetChatApplication.this, ChatRoomActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(ChatRoomConfig.AVATAR, avatar);
        bundle.putString(ChatRoomConfig.NAME, entity);
        bundle.putString(ChatRoomConfig.HOST, host);
        bundle.putInt(ChatRoomConfig.UID, 0);
        bundle.putString(ChatRoomConfig.IDENTIFIER, message.getReceiver());
        bundle.putBoolean(ChatRoomConfig.GROUP, group);
        mainIntent.putExtras(bundle);

        RemoteViews mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_message);
        mRemoteViews.setTextViewText(R.id.notification_message, message.getMsg());
        mRemoteViews.setTextViewText(R.id.notification_name, name);
        mRemoteViews.setTextViewText(R.id.notification_message_time, RoomUtils.millsToTime(message.getTimeStamp()));
        if (TextUtils.isEmpty(avatar)) {
            mRemoteViews.setImageViewResource(R.id.notification_avatar, R.drawable.default_head);
        } else {
            Bitmap bitmap = BitmapFactory.decodeFile(avatar);
            mRemoteViews.setImageViewBitmap(R.id.notification_avatar, bitmap);
        }

        PendingIntent mainPendingIntent = PendingIntent.getActivity(IntranetChatApplication.this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notification = new Notification.Builder(IntranetChatApplication.this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.setSmallIcon(R.drawable.logo)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setCustomContentView(mRemoteViews);
        } else {
            notification.setSmallIcon(R.drawable.logo)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContent(mRemoteViews);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Config.Notification_Channel_Id, Config.Notification_Channel_Name, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            //锁屏显示通知
            notificationChannel.setLockscreenVisibility(VISIBILITY_SECRET);
            //闪关灯的灯光颜色
            notificationChannel.setLightColor(Color.RED);
            //桌面launcher的消息角标
            notificationChannel.canShowBadge();
            notificationChannel.setDescription("天軟通");
            //是否允许震动
            notificationChannel.enableVibration(true);
            notificationChannel.setSound(Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.yisell_sound)
                    , new AudioAttributes.Builder().build());
            //获取系统通知响铃声音的配置
            notificationChannel.getAudioAttributes();
            //设置可绕过  请勿打扰模式
            notificationChannel.setBypassDnd(true);
            //设置震动模式
            notificationChannel.setVibrationPattern(new long[]{100, 100, 200});
            sNotificationManager.createNotificationChannel(notificationChannel);
            notification.setChannelId(Config.Notification_Channel_Id);
        }else {
            notification.setDefaults(Notification.DEFAULT_ALL);
        }
        //B: Notification 完善(notifyId),Allen Luo,2019/11/12
        sNotificationManager.notify(notifyId, notification.build());
        TLog.d(TAG, "notification: notifyId 1 = " + notifyId + ", name = " + name);
        //E: Notification 完善(notifyId),Allen Luo,2019/11/12
    }

    /**
     * 将time转String类型
     * @param time 毫秒数时间*/
    public String chatTime(long time) {
        time /= 1000;
        int hour = (int) (time / 3600);
        int minute = (int) (time % 3600 / 60);
        int second = (int) (time % 60);
        StringBuilder sb = new StringBuilder();
        if (hour != 0) {
            sb.append(hour);
            sb.append(getString(R.string.hour));
        }
        if (minute != 0) {
            sb.append(minute);
            sb.append(getString(R.string.minute));
        }
        if (second != 0) {
            sb.append(second);
            sb.append(getString(R.string.second));
        }
        return sb.toString();
    }
}
