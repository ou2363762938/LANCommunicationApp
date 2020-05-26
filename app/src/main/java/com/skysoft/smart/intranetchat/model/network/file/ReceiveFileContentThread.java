/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication]Create a file service for file recipients to download files.
 */
package com.skysoft.smart.intranetchat.model.network.file;

import android.text.TextUtils;

import com.skysoft.smart.intranetchat.model.net_model.SendFile;
import com.skysoft.smart.intranetchat.model.network.bean.FileBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReceiveFileContentBean;
import com.skysoft.smart.intranetchat.model.network.manager.ResourceManager;
import com.skysoft.smart.intranetchat.model.network.receive.MonitorTcpReceivePortThread;
import com.skysoft.smart.intranetchat.model.network.receive.MonitorUdpReceivePortThread;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.ResourceManagerBean;
import com.skysoft.smart.intranetchat.model.network.manager.FileWaitToSend;
import com.skysoft.smart.intranetchat.model.network.manager.ResponseSender;
import com.skysoft.smart.intranetchat.model.network.bean.ResponseBean;
import com.skysoft.smart.intranetchat.model.network.manager.Sender;
import com.skysoft.smart.intranetchat.model.network.manager.SocketManager;
import com.skysoft.smart.intranetchat.tools.GsonTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ReceiveFileContentThread extends Thread {
    private String TAG = ReceiveFileContentThread.class.getSimpleName();

    private String mHost;
    private String mRid;
    private Socket mSocket;

    public ReceiveFileContentThread(Socket socket){
        this.mSocket = socket;
        this.mHost = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        super.run();

        File f = new File(Config.PATH_TEMP,
                Thread.currentThread().getName()
                        + System.currentTimeMillis());
        TLog.d(TAG,"=<><<>>< Path : " + f.getPath());
        FileOutputStream fos = null;
        InputStream is = null;
        MessageDigest digest = null;

        int bl = 8096*16;
        byte[] buf = new byte[bl];
        try {
            f.createNewFile();
            fos = new FileOutputStream(f);
            is = mSocket.getInputStream();
            digest = MessageDigest.getInstance("MD5");
            int count = 0;
            int start = 0;

            count = is.read(buf,0,bl);
            int rid = buf[0];
            mRid = new String(buf,1,rid);
            start = 1 + rid;
            count -= start;
            fos.write(buf,start,count);
            digest.update(buf,start,count);

            while ((count = is.read(buf,0,bl)) != -1) {
                fos.write(buf,0,count);
                digest.update(buf,0,count);
            }

            BigInteger bi = new BigInteger(1,digest.digest());
            String md5 = bi.toString(Config.RADIX_16);
            TLog.d(TAG,"==========MD5=Rid==Over====== " + md5 + ", " + mRid);

            ReceiveFileContentBean bean = new ReceiveFileContentBean(mRid,md5,f.getPath());
            MonitorUdpReceivePortThread.
                    broadcastReceive(Config.STEP_RECEIVE,
                            GsonTools.toJson(bean),
                            mHost);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }

    private void receive() {
        InputStream is = null;
        byte[] buf = new byte[8096];
        int count = 0;
        String resourceUniqueIdentifier = null;
        File file = null;
        FileOutputStream fos = null;
        String resourceMd5 = null;
        boolean isSuccess = false;
        try {
            is = mSocket.getInputStream();
            if ((count = is.read(buf)) != -1){
                int multiple = buf[0];
                int remainder = buf[1];
                int length = multiple*128 + remainder;
                resourceUniqueIdentifier = new String(buf,2,length);
                /*该资源不在请求下载列表中*/
                if (!ResourceManager.getInstance().exist(resourceUniqueIdentifier)){
                    ResponseSender.response(Config.RESPONSE_NOT_REQUEST_THIS_RESOURCE,resourceUniqueIdentifier, mHost);
                    return;
                }
                ResourceManagerBean resource = ResourceManager.getInstance().getResource(resourceUniqueIdentifier);
                if (!resource.isReceive()){
                    TLog.d(TAG, "run: resource.isReceive() = false");
                    return;
                }
                resourceMd5 = resource.getFileBean().getMd5();

                File parentFile = SendFile.getFile(resource.getPath());
                if (!parentFile.exists()){
                    parentFile.mkdirs();
                }
                int i = 1;
                file = new File(parentFile,resource.getFileBean().getName());
                while (true){
                    if (!file.exists()){
                        break;
                    }
                    file = new File(parentFile,i + "_" + resource.getFileBean().getName());
                    i ++;
                }
                file.createNewFile();
                resource.setPath(file.getPath());
                //通知主进程开始接收文件数据
                MonitorUdpReceivePortThread.broadcastReceive(Config.STEP_REQUEST,resourceUniqueIdentifier,file.getPath());
                fos = new FileOutputStream(file);
                fos.write(buf,length+2,count - length - 2);
                fos.flush();
            }
            while ((count = is.read(buf)) != -1){
                fos.write(buf,0,count);
                fos.flush();
            }
            String md5 = SendFile.getFileMd5(file);
            ResponseBean responseBean = null;
            if (TextUtils.isEmpty(md5) || !md5.equals(resourceMd5)){
                /*接收文件失败*/
                file.deleteOnExit();
                TLog.d(TAG, "run: requestFile file failure");
                responseBean = new ResponseBean(Config.RESPONSE_RECEIVE_FILE_FAILURE,resourceUniqueIdentifier);
            }else {
                TLog.d(TAG, "run: requestFile file success");
                /*call back*/
                ResourceManagerBean resource = ResourceManager.getInstance().getResource(resourceUniqueIdentifier);
                FileBean fileBean = resource.getFileBean();
                MonitorTcpReceivePortThread.
                        broadcastSaveFile(
                                fileBean.getSender(),
                                fileBean.getReceiver(),
                                resourceUniqueIdentifier,
                                file.getPath(),
                                mHost);

                /*remove resource record*/
                responseBean = new ResponseBean(Config.RESPONSE_RECEIVE_FILE_SUCCESS,resourceUniqueIdentifier);

                //记录文件接收成功
                isSuccess = true;
            }
            Sender.sender(GsonTools.toJson(responseBean),Config.CODE_RESPONSE, mHost);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //如果接受失败（原因在对方），通知主进程
            if (!isSuccess){
                MonitorUdpReceivePortThread.broadcastReceive(Config.STEP_FAILURE,resourceUniqueIdentifier,null);
            }

            if (mSocket != null){
                try {
                    mSocket.close();
                    SocketManager.getInstance().reduce();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void send() {
        ResourceManagerBean waitToSend = FileWaitToSend.getInstance().getWaitToSend(mHost);

        if (waitToSend == null){
            ResponseSender.response(Config.RESPONSE_NOTHING_ASK,null, mHost);
            return;
        }
        String path = waitToSend.getPath();
        String resourceUniqueIdentifier = waitToSend.getFileBean().getRid();

        File file = new File(path);
        if (!file.exists() || !file.isFile()){
            ResponseBean responseBean = new ResponseBean(Config.RESPONSE_NOT_FOUND_FILE,resourceUniqueIdentifier);
            Sender.sender(GsonTools.toJson(responseBean),Config.CODE_RESPONSE, mHost);
            TLog.d(TAG, "run: 文件不存在");
            return;
        }

        FileInputStream fis = null;
        OutputStream os = null;
        int count = 0;
        byte[] buf = new byte[8096];
        byte[] header = resourceUniqueIdentifier.getBytes();
        int multiple = header.length / 128;
        int remainder = header.length % 128;
        buf[0] = (byte) multiple;
        buf[1] = (byte) remainder;

        //填充唯一标识符到数据包中
        for (int i = 0; i < header.length; i++){
            buf[i + 2] = header[i];
        }
        try {
            fis = new FileInputStream(file);
            os = mSocket.getOutputStream();
            //读取文件
            if ((count = fis.read(buf,header.length+2,buf.length - header.length - 2)) != -1){
                os.write(buf,0,count + header.length + 2);
                os.flush();
            }
            while ((count = fis.read(buf)) != -1){
                os.write(buf,0,count);
                os.flush();
            }
            TLog.d(TAG, "run: 发送文件结束");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (mSocket != null){
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SocketManager.getInstance().reduce();
            }
            if (fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
