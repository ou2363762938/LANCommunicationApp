/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/21
 * Description: [PT-40][Intranet Chat] [APP][UI] Home page ui
 */
package com.skysoft.smart.intranetchat.model.latest;

import android.content.Context;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.listener.AdapterOnClickListener;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.tools.ChatRoom.RoomUtils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.ui.SwipeLayout;
import com.skysoft.smart.intranetchat.database.table.LatestEntity;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class LatestListAdapter extends BaseAdapter {
    private final String TAG = "LatestListAdapter";

    private List<SwipeLayout> openList=new ArrayList<SwipeLayout>();
    private LayoutInflater mInflater;
    private List<Integer> mLatestList = null;
    private Context mContext;
    private AdapterOnClickListener mListener;

    public LatestListAdapter(Context mContext, List<Integer> mLatestList) {
        this.mLatestList = mLatestList;
        mInflater = LayoutInflater.from(mContext);
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mLatestList == null?0: mLatestList.size();
    }

    @Override
    public Object getItem(int position) {
        return mLatestList == null ? -1 : mLatestList.get(position);
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
        LatestEntity latest = LatestManager.
                getInstance().
                getLatest(mLatestList.get(position));
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.listview_main_message, null);

            /*得到各个控件的对象*/
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.message = (TextView) convertView.findViewById(R.id.message);
            holder.messageTime = (TextView) convertView.findViewById(R.id.message_time); // to ItemButton
            holder.swipeLayout = (SwipeLayout) convertView.findViewById(R.id.list_message_item);
            holder.delete = (TextView) convertView.findViewById(R.id.delete);
            holder.top = (TextView) convertView.findViewById(R.id.top);
            holder.messageItem = (LinearLayout) convertView.findViewById(R.id.message_list_item) ;
            holder.unreadNumber = convertView.findViewById(R.id.unread_number);
            holder.head = convertView.findViewById(R.id.head);
            convertView.setTag(holder); //绑定ViewHolder对象
        }
        else {
            holder = (ViewHolder) convertView.getTag(); //取出ViewHolder对象
        }

        initView(position,holder,convertView);
        setView(holder,latest);

        if (mListener != null) {
            holder.messageItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClickListener(v,latest,position);
                }
            });
        }

        return convertView;
    }

    private class ViewHolder {
        TextView delete;
        TextView top;
        TextView name;
        CircleImageView head;
        TextView message;
        TextView messageTime;
        //B: 去掉消息列表展示联系人状态，Allen Luo，2019/11/13
        //View userState;
        //E: 去掉消息列表展示联系人状态，Allen Luo，2019/11/13
        TextView unreadNumber;
        SwipeLayout swipeLayout;
        LinearLayout messageItem;
    }

    public void initView(int position, ViewHolder holder, View convertView){
        if (LatestManager.getInstance().getLatestBySort(position).getTop() == 0) {
            holder.messageItem.setBackground(convertView.getResources().getDrawable(R.drawable.fragment_message_list_item_selector_default));
        }else {
            holder.messageItem.setBackground(convertView.getResources().getDrawable(R.drawable.fragment_message_list_item_selector_top));
        }

        holder.swipeLayout.setSwipeChangeListener(new SwipeLayout.OnSwipeChangeListener() {

            @Override
            public void onStartOpen(SwipeLayout mSwipeLayout) {
                for(SwipeLayout layout:openList){
                    layout.close();
                }
                openList.clear();
            }

            @Override
            public void onStartClose(SwipeLayout mSwipeLayout) {

            }

            @Override
            public void onOpen(SwipeLayout mSwipeLayout) {
                openList.add(mSwipeLayout);
                if (mListener != null) {
                    holder.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onItemClickListener(v,null,position);
                            onStartOpen(mSwipeLayout);
                        }
                    });

                    holder.top.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onItemClickListener(v,null,position);
                            onStartOpen(mSwipeLayout);
                        }
                    });
                }
//
//                holder.top.setOnClickListener(new View.OnClickListener() {
//                    public void onClick(View v) {
//                        if (QuickClickListener.isFastClick()) {
//                            LatestEntity messageBean = IntranetChatApplication.sLatestChatHistoryMap.get(mLatestList.get(position));
//                            if (messageBean.getTop() == 0) {
//                                messageBean.setTop(1);
//                            } else {
//                                messageBean.setTop(0);
//                            }
//                            MessageListSort.CollectionsList(mLatestList);
//                            onStartOpen(mSwipeLayout);
//                            LatestListAdapter.this.notifyDataSetChanged();
//
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    MyDataBase.getInstance().getLatestDao().update(messageBean);
//                                }
//                            }).start();
//                        }
//                    }
//                });
            }

            @Override
            public void onDraging(SwipeLayout mSwipeLayout) {

            }

            @Override
            public void onClose(SwipeLayout mSwipeLayout) {
                openList.remove(mSwipeLayout);
            }
        });
    }

    public void setView(ViewHolder holder, LatestEntity latest){
        Log.d(TAG, "---------> " + latest.toString());
        ContactEntity contact = ContactManager
                .getInstance()
                .getContact(latest.getUser());
        holder.name.setText(contact.getName());
        holder.message.setText(latest.getContent());
        holder.messageTime.setText(RoomUtils.millToFullTime(latest.getTime()));

        if (latest.getUnReadNumber() == 0){
            holder.unreadNumber.setVisibility(View.GONE);
        }else {
            holder.unreadNumber.setText(String.valueOf(latest.getUnReadNumber()));
            holder.unreadNumber.setVisibility(View.VISIBLE);
        }

        String avatarPath = AvatarManager.getInstance().getAvatarPath(contact.getAvatar());
        if (!TextUtils.isEmpty(avatarPath)){
            Glide.with(mContext).load(avatarPath).into(holder.head);
        }else {
            Glide.with(mContext).load(R.drawable.default_head).into(holder.head);
        }

        holder.delete.setText(R.string.MessageListAdapter_delete);
    }
    //E: [PT-60][Intranet Chat] [APP][UI] contact list page,Oliver Ou,2019/10/31
//E: [PT-60][Intranet Chat] [APP][UI] contact list page,Allen Luo,2019/10/30
}