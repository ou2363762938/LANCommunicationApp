/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication]Voice or video communication function
 */
package com.skysoft.smart.intranetchat.network.call;

import android.util.Log;

import com.skysoft.smart.intranetchat.network.Config;
import com.skysoft.smart.intranetchat.network.bean.VoiceCallDataBean;
import com.skysoft.smart.intranetchat.network.receive.MonitorTcpReceivePortThread;
import com.skysoft.smart.intranetchat.network.receive.MonitorUdpReceivePortThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class VoiceCallThread {
    private static String TAG = VoiceCallThread.class.getSimpleName();
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private boolean close = true;
    private ArrayBlockingQueue<byte[]> mDatasQueue = new ArrayBlockingQueue<byte[]>(1024);


    private static VoiceCallThread sInstance = new VoiceCallThread();
    private ReceiveVoiceCallData mReceiveData;
    private SendVoiceCallData mSendData;

    private VoiceCallThread() {
    }

    public static void init(Socket socket) {
        Log.d(TAG, "init: 1");
        if (sInstance.isClose()) {
            Log.d(TAG, "init: 2");
            sInstance.mDatasQueue.clear();
            sInstance.socket = socket;
            sInstance.close = false;
            try {
                sInstance.is = socket.getInputStream();
                sInstance.os = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sInstance.voiceCall();
            MonitorUdpReceivePortThread.broadcastReceive(Config.NOTIFY_CLEAR_QUEUE,null,null);
            MonitorUdpReceivePortThread.broadcastReceive(Config.ON_ESTABLISH_CALL_CONNECT, null, null);
        }
    }

    public static VoiceCallThread getInstance() {
        return sInstance;
    }

    public void setVoiceCallData(byte[] data) {
        mDatasQueue.offer(data);
    }

    private void voiceCall() {
        Log.d(TAG, "voiceCall: ");
        sInstance.mReceiveData = null;
        sInstance.mReceiveData = new ReceiveVoiceCallData();
        sInstance.mReceiveData.start();

        sInstance.mSendData = null;
        sInstance.mSendData = new SendVoiceCallData();
        sInstance.mSendData.start();
    }

    private class ReceiveVoiceCallData extends Thread {

        @Override
        public void run() {
            super.run();
            byte[] data = new byte[8096];
            int count = 0;
            try {
                while (!close && ((count = is.read(data)) != -1)) {
                    MonitorTcpReceivePortThread.broadcastReceive(data, count);
                    data = new byte[8096];
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
        }
    }

    private class SendVoiceCallData extends Thread {
        private long time = 0;

        @Override
        public void run() {
            super.run();
            while (!close) {
                if (mDatasQueue.size() != 0) {
                    byte[] data = mDatasQueue.poll();
                    try {
                        os.write(data);
                        os.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public boolean isClose() {
        return close;
    }

    public void close() {
        close = true;
        Log.d(TAG, "close = " + close);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isCurrentHost(String host) {
        return sInstance.socket.getInetAddress().getHostName().equals(host);
    }
}