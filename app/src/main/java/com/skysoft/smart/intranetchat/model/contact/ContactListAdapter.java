/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-60][Intranet Chat] [APP][UI] contact list page
 */
package com.skysoft.smart.intranetchat.model.contact;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.listener.AdapterOnClickListener;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactListAdapter extends BaseAdapter {
    private final String TAG = "ContactListAdapter";

    private LayoutInflater mInflater;
    private List<Integer> mContactList = null;
    private Context mContext;
    private AdapterOnClickListener mListener;
    public ContactListAdapter(Context mContext, List<Integer> contacts) {
        this.mContext = mContext;
        this.mContactList = contacts;
        mInflater = LayoutInflater.from(mContext);
//        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mContactList == null?0: mContactList.size();
    }

    @Override
    public Object getItem(int position) {
        return mContactList == null ? null : mContactList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void setListener(AdapterOnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int id = mContactList.get(position);
        ContactEntity contactBean = ContactManager.getInstance().getContact(id);

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.listview_main_contact, null);
            holder.name = (TextView) convertView.findViewById(R.id.contact_name);
            holder.contactItem = (ConstraintLayout) convertView.findViewById(R.id.contact_list_item);
            holder.userState = (View) convertView.findViewById(R.id.contact_state);
            holder.head = convertView.findViewById(R.id.contact_head);

            convertView.setTag(holder); //绑定ViewHolder对象
        }else {
            holder = (ContactListAdapter.ViewHolder) convertView.getTag(); //取出ViewHolder对象
        }

        switch (ContactManager.getInstance().getContactBySort(position).getStatus()){
            case 3:
                holder.userState.setBackgroundResource(R.drawable.bg_circle_gray);
                break;
            case 2:
                holder.userState.setBackgroundResource(R.drawable.bg_circle_red);
                break;
            case 1:
                holder.userState.setBackgroundResource(R.drawable.bg_circle_green);
                break;
        }

        holder.name.setText(String.valueOf(contactBean.getName()));

        String avatarPath = AvatarManager.
                getInstance().
                getAvatarPath(contactBean.getAvatar());
        if (!TextUtils.isEmpty(avatarPath)){
            Glide.with(mContext).load(avatarPath).into(holder.head);
        }else {
            Glide.with(mContext).load(R.drawable.default_head).into(holder.head);
        }

        if (mListener != null) {
            mListener.init(contactBean,position);
            holder.contactItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClickListener(v,contactBean,position);
                }
            });
            holder.head.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClickListener(v,contactBean,position);
                }
            });
        }

        return convertView;
    }

//    public void updateData(List<MessageBean> sessionList) {
//        sessionList.clear();
//        sessionList.addAll(sessionList);
//    }

    private class ViewHolder {
        TextView name;
        CircleImageView head;
        View userState;
//        SwipeLayout swipeLayout;
        ConstraintLayout contactItem;
    }
}
