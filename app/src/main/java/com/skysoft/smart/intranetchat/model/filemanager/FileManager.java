package com.skysoft.smart.intranetchat.model.filemanager;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.FileDao;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.FileEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.camera.entity.EventMessage;
import com.skysoft.smart.intranetchat.model.chat.record.RecordManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.group.GroupManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.net_model.AskFile;
import com.skysoft.smart.intranetchat.model.net_model.SendResponse;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.model.network.bean.FileBean;
import com.skysoft.smart.intranetchat.model.network.bean.ReceiveFileContentBean;
import com.skysoft.smart.intranetchat.model.network.bean.ResponseBean;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class FileManager {
    private static final String TAG = "FileManager";
    private static FileManager sInstance;
    private Context mContext;
    private HandlerThread mHandlerThread;
    private Handler mHandler;


    private FileManager(Context context) {
        mContext = context;
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public static void init(Context context) {
        sInstance = new FileManager(context);
    }

    public static FileManager getInstance() {
        return sInstance;
    }

    public FileEntity notify(ContactEntity contact, String path, int type, int contentLength) {
        String receiver = MineInfoManager.getInstance().getIdentifier();
        return notify(contact.getHost(),receiver,receiver,path,type,contentLength);
    }

    public FileEntity notify(GroupEntity group, String path, int type, int contentLength) {
        return notify("255.255.255.255",
                group.getIdentifier(),
                MineInfoManager.getInstance().getIdentifier(),
                path,
                type,
                contentLength);
    }

    public void notify(GroupEntity group, FileEntity file) {
        notify(group,file.getPath(),file.getType(),file.getContentLength());
    }

    public void notify(ContactEntity contact, FileEntity file) {
        notify(contact,file.getPath(),file.getType(),file.getContentLength());
    }

    public void notify(int id, FileEntity file, boolean group) {
        if (group) {
            notify(GroupManager.getInstance().getGroup(id),file);
        } else {
            notify(ContactManager.getInstance().getContact(id),file);
        }
    }

    public void notifyAvatar(String host, String rid) {
        TLog.d(TAG,">>>>>Notify Avatar <<<<<< " + rid);
        String avatar = MineInfoManager.getInstance().getAvatarIdentifier();
        if (avatar.equals(rid)) {
            String receiver = MineInfoManager.getInstance().getIdentifier();
            String path = MineInfoManager.getInstance().getAvatarPath();
            FileBean bean = generatorFileBean(path, Config.FILE_AVATAR, 0);
            bean.setRid(rid);
            generatorFileDrops(bean,path,receiver,receiver);
            sendNotify(bean,path,host);
        }
    }

    private FileEntity notify(String host,
                              String receiver,
                              String sender,
                              String path,
                              int type,
                              int contentLength) {
        FileBean bean = generatorFileBean(path,type,contentLength);
        bean.setSender(sender);
        bean.setReceiver(receiver);
        FileDrops drops = generatorFileDrops(bean,path,receiver,sender);

        sendNotify(bean,path,host);
        return drops.getFileEntity();
    }

    private FileDrops generatorFileDrops(FileBean bean, String path, String receiver, String sender) {
        bean.setSender(sender);
        bean.setReceiver(receiver);
        return FilePool.getInstance().put(bean,path,Config.STEP_NOTIFY);
    }

    private void sendNotify(FileBean bean, String path, String host) {
        TLog.d(TAG,"=========> notify " + bean.toString());
        try {
            TLog.d(TAG,"----------> notify " + host);
            IntranetChatApplication.sAidlInterface.sendFile(GsonTools.toJson(bean),path,host);
        } catch (RemoteException e) {
            e.printStackTrace();
            TLog.d(TAG,"========== Exception =========");
        }
    }

    public void requestFile(FileBean bean, String host) {
        TLog.d(TAG,"requestFile----------> " + bean.toString());
        TLog.d(TAG,"<<<<<<<<<<>>>>>>> " + MineInfoManager.getInstance().toString());
        if (host.equals(MineInfoManager.getInstance().getHost()) ||
            bean.getSender().equals(MineInfoManager.getInstance().getIdentifier())) {
            TLog.d(TAG,"========>>>>>>>>> repeat");
            return;
        }

        FilePool.getInstance().put(bean,"",Config.STEP_REQUEST);
        AskFile.askFile(bean.getRid(), host);
    }

    public void receiveRequest(String rid, String host) {
        Runnable receiveRequest = new Runnable() {
            @Override
            public void run() {
                TLog.d(TAG,"receiveRequest--------> " + rid);
                FileDrops drops = FilePool.getInstance().get(rid);
                TLog.d(TAG,".....>>>><<<<<<< " + drops.toString());
                drops.setStep(Config.STEP_SEND);
                try {
                    IntranetChatApplication.
                            sAidlInterface.
                            sendFileContent(rid, drops.getPath(), host);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };

        mHandler.post(receiveRequest);
    }

    public void receiveFile(ReceiveFileContentBean bean, String host) {
        FileDrops drops = FilePool.getInstance().get(bean.getRid());
        TLog.d(TAG,"<><><>>><>< " + drops.toString() + bean.toString());
        if (drops.getFileBean().getMd5().equals(bean.getMd5())) {
            TLog.d(TAG,",,,,,,,,,,,,,,,,,,,,,,,,,,,,,1");
            String path = generatorFilePath(drops);
//            bean.setPath(path);
            TLog.d(TAG,",,,,,,,,,,,,,,,,,,,,,,,,,,,,, " + path);
            File temp = new File(bean.getPath());
            TLog.d(TAG,",,,,,,,,,,,,,,,,,,,,,,,,,,,,,2");
            temp.renameTo(new File(path));

            drops.setStep(Config.STEP_SUCCESS);
            drops.setPath(path);
            FileEntity entity = drops.getFileEntity();
            saveFileEntity(entity);
            TLog.d(TAG,",,,,,,,,,,,,,,,,,,,,,,,,,5");

            if (entity.getType() == Config.FILE_AVATAR) {
                AvatarManager.
                        getInstance().
                        receiveAvatar(
                                drops.getFileBean().getReceiver(),
                                entity.getRid(),
                                path);
                TLog.d(TAG,",,,,,,,,,,,,,,,,,,,,,,,,,,,,,Avatar");
            } else {
                RecordManager.
                        getInstance().
                        recordFile(entity,
                                drops.getFileBean().getSender(),
                                drops.getFileBean().getReceiver());
                LatestManager.
                        getInstance().
                        receive(drops.getFileBean().getReceiver(),
                                entity.getType());
                TLog.d(TAG,",,,,,,,,,,,,,,,,,,,,,,,,,,,,,UI");
            }
        } else {
            FileEntity entity = drops.getFileEntity();
            saveFileEntity(entity);
            drops.setStep(Config.STEP_FAILURE);
        }

        ResponseBean response = new ResponseBean(drops.getStep(),bean.getRid());
        try {
            IntranetChatApplication.sAidlInterface.sendResponse(GsonTools.toJson(response), host);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void receiveResponse(int type, String rid) {
        FileDrops drops = FilePool.getInstance().get(rid);
        drops.getFileEntity();
    }

    public void saveFileEntity(FileEntity entity) {
        FileDao fileDao = MyDataBase.getInstance().getFileDao();
        if (entity.getId() == -1) {
            fileDao.insert(entity);
            entity.setId(fileDao.getNewInsertId());
        } else {
            fileDao.update(entity);
        }
    }

    private File getFile(String path){
        if (TextUtils.isEmpty(path)) {
            throw new NullPointerException();
        }
        return new File(path);
    }

    private FileBean generatorFileBean(String path, int type, int contentLength) {
        File file = getFile(path);
        FileBean bean = new FileBean();
        bean.setType(type);
        bean.setName(file.getName());
        bean.setFileLength(file.length());
        bean.setContentLength(contentLength);
        bean.setMd5(getFileMd5(file));
        bean.setRid(Identifier.getInstance().getFileIdentifier(bean.getMd5()));
        return bean;
    }

    private String getFileMd5(File file){
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());

        return bigInt.toString(Config.RADIX_16);
    }

    private String generatorFilePath(FileDrops drops) {
        String name = drops.getFileBean().getName();
        String suffix = name.substring(name.lastIndexOf("."));
        TLog.d(TAG,"=======Suffix : " + suffix);
        FilePath filePath = FilePath.getInstance();
        switch (drops.getFileBean().getType()) {
            case Config.FILE_VOICE:
                return generatorFilePath(filePath.getVoice(),"Voice",suffix);
            case Config.FILE_VIDEO:
                return generatorFilePath(filePath.getVideo(),"Video",suffix);
            case Config.FILE_PICTURE:
                return generatorFilePath(filePath.getPicture(),"Picture",suffix);
            case Config.FILE_AVATAR:
                return generatorFilePath(filePath.getAvatar(),"Avatar",suffix);
            case Config.FILE_COMMON:
                return generatorFilePath(filePath.getCommon(),name,"",true);
                default:
                    return "";
        }
    }

    private String generatorFilePath(String parent, String name, String suffix) {
        return generatorFilePath(parent,name,suffix,false);
    }

    private String generatorFilePath(String parent,
                                     String name,
                                     String suffix,
                                     boolean common) {
        TLog.d(TAG,">>>>>>> Parent : " + parent +
                ", name " + name +
                ", suffix " + suffix +
                ", common " + common);
        StringBuilder sb = new StringBuilder();
        sb.append(parent).
                append(FilePath.getInstance().getSeparator()).
                append(name);

        if (!common) {
            sb.append(System.currentTimeMillis()).append(suffix);
        }
        return sb.toString();
    }

}
