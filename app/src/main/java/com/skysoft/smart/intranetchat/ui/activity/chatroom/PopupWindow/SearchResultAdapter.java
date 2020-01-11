package com.skysoft.smart.intranetchat.ui.activity.chatroom.PopupWindow;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.TransmitBean;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import de.hdodenhof.circleimageview.CircleImageView;

public class SearchResultAdapter extends BaseAdapter {
    private static final String TAG = "SearchResultAdapter";

    private SoftReference<Context> mSoftContext;
    private LayoutInflater mInflater;
    private List<TransmitBean> mSearchResults = new ArrayList<>();
    private OnSelectSearchResultListener mOnSelectListener;

    public SearchResultAdapter(Context context, OnSelectSearchResultListener mOnSelectListener) {
        this.mSoftContext = new SoftReference<Context>(context);
        this.mOnSelectListener = mOnSelectListener;
    }

    public void onInputSearchKeyChange(String key){
        Log.d(TAG, "onInputSearchKeyChange: key = " + key);
        if (TextUtils.isEmpty(key)){
            mSearchResults.clear();
            notifyDataSetChanged();
            return;
        }

        List<TransmitBean> list = new ArrayList<>();
        Iterator<ContactEntity> contactIterator = IntranetChatApplication.getsContactList().iterator();
        Iterator<ContactEntity> groupIterator = IntranetChatApplication.getsGroupContactList().iterator();

        while (contactIterator.hasNext()){
            ContactEntity contactEntity = contactIterator.next();
            TransmitBean bean = matchingContactEntity(key, contactEntity);
            if (null != bean){
                list.add(bean);
            }
        }

        while (groupIterator.hasNext()){
            ContactEntity contactEntity = groupIterator.next();
            TransmitBean bean = matchingContactEntity(key, contactEntity);
            if (null != bean){
                bean.setGroup(true);
                list.add(bean);
            }
        }

        Log.d(TAG, "onInputSearchKeyChange: size = " + list.size());
        mSearchResults = list;
        notifyDataSetChanged();
    }

    private TransmitBean matchingContactEntity(String key, ContactEntity contactEntity){
        TransmitBean bean = null;
        if (contactEntity.getName().contains(key)){
            bean = new TransmitBean(contactEntity.getAvatarPath()
                    ,contactEntity.getName()
                    ,contactEntity.getIdentifier()
                    ,false
                    ,contactEntity.getHost());
        }
        return bean;
    }

    @Override
    public int getCount() {
        return null == mSearchResults ? 0 : mSearchResults.size();
    }

    @Override
    public Object getItem(int position) {
        return mSearchResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TransmitBean bean = mSearchResults.get(position);

        SearchResultViewHolder holder = null;
        if (convertView == null){
            convertView = mInflater.inflate(R.layout.listview_main_contact,null);
            holder.mAvatar = convertView.findViewById(R.id.contact_head);
            holder.mName = convertView.findViewById(R.id.contact_name);
            holder.mBox = convertView.findViewById(R.id.contact_list_item);
            convertView.findViewById(R.id.contact_state).setVisibility(View.GONE);

            convertView.setTag(holder);
        }else {
            holder = (SearchResultViewHolder) convertView.getTag();
        }

        if (!TextUtils.isEmpty(bean.getmAvatarPath())){
            Glide.with(mSoftContext.get()).load(bean.getmAvatarPath()).into(holder.mAvatar);
        }else {
            Glide.with(mSoftContext.get()).load(R.drawable.default_head).into(holder.mAvatar);
        }
        holder.mName.setText(bean.getmUseName());

        holder.mBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnSelectListener.onSelectSearchResultListener(bean);
            }
        });
        return convertView;
    }

    public static class SearchResultViewHolder{
        public CircleImageView mAvatar;
        public TextView mName;
        public ConstraintLayout mBox;
    }
}
