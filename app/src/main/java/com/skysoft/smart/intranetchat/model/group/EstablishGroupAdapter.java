/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/11/6
 * Description: [Intranet Chat] [APP][UI] Group chat
 */
package com.skysoft.smart.intranetchat.model.group;

import android.content.Context;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.latest.LatestListAdapter;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.model.net_model.EstablishGroup;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.database.table.GroupMemberEntity;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;
import com.skysoft.smart.intranetchat.model.network.bean.EstablishGroupBean;
import com.skysoft.smart.intranetchat.model.network.bean.UserInfoBean;
import com.skysoft.smart.intranetchat.tools.Identifier;
import com.skysoft.smart.intranetchat.tools.QuickClickListener;
import com.skysoft.smart.intranetchat.tools.toastutil.ToastUtil;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomActivity;
import com.skysoft.smart.intranetchat.ui.activity.userinfoshow.UserInfoShowActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;

public class EstablishGroupAdapter extends BaseAdapter {
    private static final String TAG = "ContactListAdapter";

    private LayoutInflater mInflater;
    private List<Integer> mMembersList;
    private List<String> mSelectMember;
    private static Context mContext;
    private boolean isGroup = false;
    private boolean isEstablish = false;
    private int mSelectedNumber;

    public EstablishGroupAdapter(Context mContext) {
        this.mContext = mContext;
        this.mMembersList = new ArrayList<>();
        this.mSelectMember = new ArrayList<>();
        mInflater = LayoutInflater.from(mContext);
//        this.notifyDataSetChanged();
    }

    public void addGroupMembers(List<Integer> memberEntities, boolean isEstablish) {
        this.isEstablish = isEstablish;
        if (memberEntities != null) {
            mMembersList.addAll(memberEntities);
        }
    }

    public void setEstablish(boolean isEstablish) {
        this.isEstablish = isEstablish;
    }

    @Override
    public int getCount() {
        return mMembersList == null?0: mMembersList.size();
    }

    @Override
    public Object getItem(int position) {
        return mMembersList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ContactEntity contact = ContactManager
                .getInstance()
                .getContact(mMembersList.get(position));
        EstablishGroupAdapter.ViewHolder holder;
        if (convertView == null) {
            holder = new EstablishGroupAdapter.ViewHolder();
            convertView = mInflater.inflate(R.layout.listview_main_contact, null);

            /*得到各个控件的对象*/
            holder.name = (TextView) convertView.findViewById(R.id.contact_name);
            holder.contactItem = (ConstraintLayout) convertView.findViewById(R.id.contact_list_item) ;
            holder.userState = (View) convertView.findViewById(R.id.contact_state);
            holder.head = convertView.findViewById(R.id.contact_head);
            holder.checkBox = convertView.findViewById(R.id.contact_check);

            convertView.setTag(holder); //绑定ViewHolder对象
        }else {
            holder = (EstablishGroupAdapter.ViewHolder) convertView.getTag(); //取出ViewHolder对象
        }
        //不加载离线用户
        holder.checkBox.setVisibility(View.VISIBLE);
        holder.name.setText(String.valueOf(contact.getName()));

        AvatarManager.getInstance().loadContactAvatar(mContext,holder.head,contact.getAvatar());

        if (isEstablish){
            holder.checkBox.setVisibility(View.VISIBLE);
            contact.setCheck(false);
            contact.setShowCheck(true);
        }else if (!contact.isShowCheck()){
            holder.checkBox.setVisibility(View.GONE);
        }

        holder.checkBox.setChecked(contact.isCheck());

        holder.contactItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGroup) {
                    if (QuickClickListener.isFastClick(300)) {
                        UserInfoShowActivity.go(mContext, contact.getId());
                    }
                }
                if (contact.isShowCheck()){
                    contact.setCheck(!holder.checkBox.isChecked());
                    holder.checkBox.setChecked(!holder.checkBox.isChecked());
                }
            }
        });
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                holder.checkBox.setChecked(isChecked);
                contact.setCheck(isChecked);
                mSelectedNumber += isChecked ? 1 : -1;
            }
        });
        return convertView;
    }

    private class ViewHolder {
        TextView name;
        ImageView head;
        View userState;
        ConstraintLayout contactItem;
        CheckBox checkBox;
    }

    public int getSelectedNumber() {
        return mSelectedNumber;
    }

    public List<ContactEntity> getSelected() {
        List<ContactEntity> selected = new ArrayList<>();
        for (int id:mMembersList) {
            ContactEntity contact = ContactManager.getInstance().getContact(id);
            if (contact.isCheck()) {
                selected.add(contact);
            }
        }
        return selected;
    }
}
