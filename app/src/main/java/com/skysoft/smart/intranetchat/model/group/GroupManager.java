package com.skysoft.smart.intranetchat.model.group;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.chat.TransmitBean;
import com.skysoft.smart.intranetchat.bean.network.GroupMemberList;
import com.skysoft.smart.intranetchat.bean.signal.GroupSignal;
import com.skysoft.smart.intranetchat.database.MyDataBase;
import com.skysoft.smart.intranetchat.database.dao.GroupDao;
import com.skysoft.smart.intranetchat.database.dao.GroupMemberDao;
import com.skysoft.smart.intranetchat.database.dao.RefuseGroupDao;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.GroupEntity;
import com.skysoft.smart.intranetchat.database.table.GroupMemberEntity;
import com.skysoft.smart.intranetchat.database.table.RefuseGroupEntity;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.latest.LatestManager;
import com.skysoft.smart.intranetchat.model.mine.MineInfoManager;
import com.skysoft.smart.intranetchat.model.net_model.EstablishGroup;
import com.skysoft.smart.intranetchat.model.network.bean.EstablishGroupBean;
import com.skysoft.smart.intranetchat.tools.DialogUtil;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.tools.toastutil.ToastUtil;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupManager {
    private static final String TAG = "GroupManager";
    private static GroupManager sInstance;
    private final int BASE_NOTIFY = 10000;

    private GroupManager() {
        mGroupMap = new HashMap<>();
        mGroupIndex = new HashMap<>();

        mRefuseMap = new HashMap<>();
        mRefuseIndex = new ArrayList<>();

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }
    public static GroupManager getInstance() {
        if (sInstance == null) {
            synchronized (GroupManager.class) {
                if (sInstance == null) {
                    sInstance = new GroupManager();
                }
            }
        }
        return sInstance;
    }

    private Map<Integer, GroupEntity> mGroupMap;
    private Map<String, Integer> mGroupIndex;
    private EstablishGroupAdapter mAdapter;

    private Map<Integer, RefuseGroupEntity> mRefuseMap;
    private List<String> mRefuseIndex;
    private GroupSignal mSignal = new GroupSignal();
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    public void initGroup(List<GroupEntity> entities) {
        int notify = 1;
        for (GroupEntity group:entities) {
            group.setNotifyId(notify++ + BASE_NOTIFY);
            mGroupMap.put(group.getId(), group);
            mGroupIndex.put(group.getIdentifier(), group.getId());
        }
    }

    public void initRefuse(List<RefuseGroupEntity> entities) {
        for (RefuseGroupEntity refuse:entities) {
            mRefuseMap.put(refuse.getId(),refuse);
            mRefuseIndex.add(refuse.getIdentifier());
        }
    }

    public GroupEntity getGroup(int group) {
        return mGroupMap.get(group);
    }

    public GroupEntity getGroup(String group) {
        return mGroupMap.get(mGroupIndex.get(group));
    }

    public String getGroupJson(String group) {
        GroupEntity g = mGroupMap.get(mGroupIndex.get(group));
        return GsonTools.toJson(g);
    }

    public List<TransmitBean> findGroup(String key) {
        List<TransmitBean> transmits = new ArrayList<>();
        for (int id:mGroupMap.keySet()) {
            GroupEntity group = mGroupMap.get(id);
            TransmitBean bean = matchingContactEntity(key, group);
            if (bean != null) {
                transmits.add(bean);
            }
        }
        return transmits;
    }

    public int getGroupId(String group) {
        return mGroupIndex.get(group);
    }

    public EstablishGroupAdapter initAdapter(Context context, int groupId) {
        mAdapter = new EstablishGroupAdapter(context);
        setAdapterData(groupId);
        return mAdapter;
    }

    public void setAdapterData(int groupId) {
        if (groupId == -1) {        //新建群
            mAdapter.addGroupMembers(
                    ContactManager.getInstance().getContacts(),
                    true
            );
            mAdapter.notifyDataSetChanged();
        } else {                //查看群
            Runnable set = new Runnable() {
                @Override
                public void run() {
                    List<GroupMemberEntity> allGroupMember = MyDataBase.
                            getInstance().
                            getGroupMemberDao().
                            getMember(groupId);
                    GroupMemberList groupMembersBean = new GroupMemberList(allGroupMember);
                    EventBus.getDefault().post(groupMembersBean);
                }
            };
            mHandler.post(set);
            mAdapter.setEstablish(false);
        }
    }

    public void receiveMembers(GroupMemberList membersBean) {
        List<GroupMemberEntity> memberEntities = membersBean.getMemberEntities();
        List<Integer> contactList = new ArrayList<>();
        for (GroupMemberEntity member:memberEntities) {
            contactList.add(member.getContact());
        }
        ContactManager.getInstance().sortContact(contactList);
        mAdapter.addGroupMembers(contactList,false);
        mAdapter.notifyDataSetChanged();
    }

    public EstablishGroupAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(EstablishGroupAdapter mAdapter) {
        this.mAdapter = mAdapter;
    }

    public void establishGroup(Context context) {
        List<ContactEntity> select = mAdapter.getSelected();

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_input_name, null);
        TextView inputNewName = view.findViewById(R.id.dialog_input);
        String defaultGroupName = generatorGroupName(select);
        inputNewName.setHint(defaultGroupName);

        AlertDialog dialog = DialogUtil.createDialog(context, view, "输入群名"
                , "确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = inputNewName.getText().toString();
                        if (!checkGroupName(context,newName)) {
                            dialog.dismiss();
                            newName = inputNewName.getHint().toString();
//                            return;
                        }

                        List<String> contactId = new ArrayList<>();
                        List<String> contactHost = new ArrayList<>();
                        GroupEntity group = generatorGroup(context
                                , select
                                , contactHost
                                , contactId
                                , newName);

                        if (group.getId() != -1) {
                            ChatRoomActivity.go(context, GsonTools.toJson(group),true);
                        } else {
                            EstablishGroupBean establishGroupBean = generatorEstablishGroupBean(group, contactId);
                            TLog.d(TAG,">>>>>>>>>> " + establishGroupBean.toString());
                            EstablishGroup.establishGroup(establishGroupBean,contactHost);
                            saveGroupAndMembers(context,group,select);
                        }

                    }
                }, "取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private String generatorGroupName(List<ContactEntity> select) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MineInfoManager.getInstance().getName());
        stringBuilder.append("、");
        int i = 0;
        for (ContactEntity contact:select) {
            if (stringBuilder.length() + contact.getName().length() > 20) {
                stringBuilder.append("....");
                break;
            }

            if (i++ == 3) {
                break;
            }

            stringBuilder.append(contact.getName());
            stringBuilder.append("、");
        }
        stringBuilder.delete(stringBuilder.length() - 1,stringBuilder.length());
        return stringBuilder.toString();
    }

    private boolean checkGroupName(Context context, String newName) {
        if (TextUtils.isEmpty(newName)
                || newName.equals(
                        MineInfoManager.getInstance().getName())){
            return false;
        }

        if (newName.length() <= 4 || newName.length() > 23){
            ToastUtil.toast(context, context.getString(R.string.limit_name_length));
            return false;
        }

        if (!IntranetChatApplication.isNetWortState()){
            ToastUtil.toast(context, context.getString(R.string.Toast_text_non_lan));
        }
        return true;
    }

    private GroupEntity generatorGroup(Context context
            , List<ContactEntity> select
            , List<String> contactHosts
            , List<String> contactIdentifiers
            , String name) {
        GroupEntity group = new GroupEntity();
        for (ContactEntity contact:select) {
            contactIdentifiers.add(contact.getIdentifier());
            contactHosts.add(contact.getHost());
        }
        contactIdentifiers.add(MineInfoManager.getInstance().getIdentifier());

        String groupIdentifier = Identifier.getInstance().getGroupIdentifier(contactIdentifiers);
        group = mGroupMap.get(groupIdentifier);
        if (group != null) {
            ToastUtil.toast(context,context.getString(R.string.repeated_construction_group));
            return group;
        }

        group = new GroupEntity();
        group.setId(-1);
        group.setIdentifier(groupIdentifier);
        group.setNotifyId((int) (System.currentTimeMillis() - IntranetChatApplication.getsBaseTimeLine()));
        group.setAvatar(-1);
        group.setHolder(MineInfoManager.getInstance().getIdentifier());
        group.setName(name);
        group.setHost("255.255.255.255");

        return group;
    }

    private EstablishGroupBean generatorEstablishGroupBean(GroupEntity group
            , List<String> contactIdentifier) {
        EstablishGroupBean bean = new EstablishGroupBean();
        bean.setmName(group.getName());
        bean.setmUsers(contactIdentifier);
        for (String c:contactIdentifier) {
            TLog.d(TAG,">>>>>>>>>>>>>>>>>>> " + c);
        }
        bean.setmHolderIdentifier(group.getHolder());
        bean.setmGroupAvatarIdentifier(AvatarManager.getInstance().getDefaultAvatar());
        bean.setmGroupIdentifier(group.getIdentifier());
        return bean;
    }

    private List<GroupMemberEntity> generatorMembers(int group, List<ContactEntity> contacts) {
        List<GroupMemberEntity> members = new ArrayList<>();
        for (ContactEntity contact:contacts) {
            GroupMemberEntity member = new GroupMemberEntity();
            member.setContact(contact.getId());
            member.setGroup(group);
            members.add(member);
        }
        return members;
    }

    private void saveGroupAndMembers(Context context
            , GroupEntity group
            , List<ContactEntity> select) {
        Runnable save = new Runnable() {
            @Override
            public void run() {
                GroupDao groupDao = MyDataBase.getInstance().getGroupDao();
                GroupMemberDao memberDao = MyDataBase.getInstance().getGroupMemberDao();

                group.setId(0);
                groupDao.insert(group);
                int id = groupDao.getNewInsertId();
                group.setId(id);
                mGroupMap.put(id,group);
                mGroupIndex.put(group.getIdentifier(),id);

                List<GroupMemberEntity> members = generatorMembers(id,select);
                memberDao.insert(members.toArray(new GroupMemberEntity[members.size()]));

                LatestManager.getInstance().send(id,"快点开始聊天吧！",true);
                ChatRoomActivity.go(context, GsonTools.toJson(group),true);
            }
        };
        mHandler.post(save);
    }

    public void receiveGroup(EstablishGroupBean bean, String host) {
        GroupEntity group = generatorGroup(bean, host);
        if (group.getId() == -1 && bean.getmUsers().contains(
                MineInfoManager.getInstance().getIdentifier())) {
            saveGroupAndMembers(group,bean);
        } else {
            addRefuse(bean);
        }

    }

    private GroupEntity generatorGroup(EstablishGroupBean bean, String host) {
        GroupEntity group = mGroupMap.get(mGroupIndex.get(bean.getmGroupIdentifier()));
        if (group != null) {
            group.setName(bean.getmGroupName());
            return group;
        }

        group = new GroupEntity();
        group.setHost(host);
        group.setName(bean.getmGroupName());
        group.setHolder(bean.getmHolderIdentifier());
        group.setNotifyId((int) (System.currentTimeMillis() - IntranetChatApplication.getsBaseTimeLine()));
        group.setIdentifier(bean.getmGroupIdentifier());
        group.setAvatar(-1);
        group.setId(-1);
        return group;
    }

    private List<GroupMemberEntity> generatorMembers(int group, EstablishGroupBean bean) {
        List<String> contacts = bean.getmUsers();
        List<GroupMemberEntity> members = new ArrayList<>();
        ContactManager contactManager = ContactManager.getInstance();
        for (String id:contacts) {
            ContactEntity contact = contactManager.getContact(id);
            if (contact == null) {
                TLog.d(TAG,">>>>>>>>>>> Is Mine : " + MineInfoManager.getInstance().getIdentifier().equals(id));
                continue;
            }
            GroupMemberEntity member = new GroupMemberEntity();
            member.setContact(contact.getId());
            member.setGroup(group);
            members.add(member);
        }
        return members;
    }

    private void saveGroupAndMembers(GroupEntity group, EstablishGroupBean bean) {
        Runnable save = new Runnable() {
            @Override
            public void run() {
                GroupDao groupDao = MyDataBase.getInstance().getGroupDao();
                GroupMemberDao memberDao = MyDataBase.getInstance().getGroupMemberDao();

                if (group.getId() == -1) {
                    group.setId(0);
                    groupDao.insert(group);
                    int id = groupDao.getNewInsertId();
                    group.setId(id);
                    mGroupMap.put(id,group);
                    mGroupIndex.put(group.getIdentifier(),id);

                    List<GroupMemberEntity> members = generatorMembers(id, bean);
                    memberDao.insert(members.toArray(new GroupMemberEntity[members.size()]));

                    LatestManager.getInstance().receive(id,"快点开始聊天吧！",true);
                } else {
                    groupDao.update(group);

                    LatestManager.getInstance().receive(group.getId(),"更改群名为：" + group.getName(),true);
                }
            }
        };
        mHandler.post(save);
    }

    private void addRefuse(EstablishGroupBean bean) {
        int idx = mRefuseIndex.indexOf(bean.getmGroupIdentifier());
        if (idx != -1) {
            return;
        }

        mRefuseIndex.add(bean.getmGroupIdentifier());
        Runnable refuse = new Runnable() {
            @Override
            public void run() {
                RefuseGroupEntity refuse = new RefuseGroupEntity();
                refuse.setIdentifier(bean.getmGroupIdentifier());

                RefuseGroupDao refuseDao = MyDataBase.getInstance().getRefuseGroupDao();
                refuseDao.insert(refuse);
                int id = refuseDao.getNewInsertId();
                refuse.setId(id);
                mRefuseMap.put(id,refuse);
            }
        };
        mHandler.post(refuse);
    }

    private void getRefuse() {

    }

    /**
     * 匹配key和contactEntity.getName()
     * @param key 关键词
     * @param group 群
     * @return null：key和contactEntity不匹配*/
    private TransmitBean matchingContactEntity(String key, GroupEntity group){
        TransmitBean bean = baseMatchingContactEntity(key,group);
        //a~z:97~122
        //A~Z:65~90
        if (null == bean){
            for (int i = 1; i < key.length(); i++){
                bean = baseMatchingContactEntity(key.substring(0,i),group);
                if (null != bean){
                    break;
                }
            }
        }

        if (null == bean){
            for (int i = 0; i < key.length() ; i++){
                bean = baseMatchingContactEntity(""+key.charAt(i),group);
                if (null != bean){
                    break;
                }else {
                    //忽视大小写
                    if (key.charAt(i)>=65 && key.charAt(i)<=90){
                        bean = baseMatchingContactEntity(""+(char) (key.charAt(i)+32),group);
                    }else if (key.charAt(i) >= 97 && key.charAt(i)<=122){
                        bean = baseMatchingContactEntity(""+(char) (key.charAt(i)-32),group);
                    }

                    if (null != bean){
                        break;
                    }
                }
            }
        }
        return bean;
    }

    /**
     * 匹配key和contactEntity.getName()
     * @param key 关键词
     * @param group 群
     * @return null：key和contactEntity不匹配*/
    private TransmitBean baseMatchingContactEntity(String key, GroupEntity group){
        if (!TextUtils.isEmpty(key) && group.getName().contains(key)){
            TransmitBean bean = new TransmitBean(group.getId()
                    ,true);
            bean.setName(group.getName());
            return bean;
        }
        return null;
    }
}
