package com.skysoft.smart.intranetchat.ui.activity.chatroom.PopupWindow;

import android.content.Context;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.model.group.GroupManager;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.bean.chat.TransmitBean;
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
        mInflater = LayoutInflater.from(mSoftContext.get());
    }

    public void onInputSearchKeyChange(String key){
        TLog.d(TAG, "onInputSearchKeyChange: key = " + key);
        if (TextUtils.isEmpty(key)){
            mSearchResults.clear();
            notifyDataSetChanged();
            return;
        }

        mSearchResults.addAll(ContactManager.getInstance().findContact(key));
        mSearchResults.addAll(GroupManager.getInstance().findGroup(key));
        notifyDataSetChanged();
    }

    /**
     * 匹配key和contactEntity.getName()
     * @param key 关键词
     * @param contactEntity 联系人
     * @return null：key和contactEntity不匹配*/
    private TransmitBean matchingContactEntity(String key, ContactEntity contactEntity){
        TransmitBean bean = baseMatchingContactEntity(key,contactEntity);
        //a~z:97~122
        //A~Z:65~90
        if (null == bean){
            for (int i = 1; i < key.length(); i++){
                bean = baseMatchingContactEntity(key.substring(0,i),contactEntity);
                if (null != bean){
                    TLog.d(TAG, "matchingContactEntity: key.substring(0,i)" + key.substring(0,i));
                    break;
                }
            }
        }

        if (null == bean){
            for (int i = 0; i < key.length() ; i++){
                bean = baseMatchingContactEntity(""+key.charAt(i),contactEntity);
                if (null != bean){
                    TLog.d(TAG, "matchingContactEntity: key.charAt(i)" + ""+key.charAt(i));
                    break;
                }else {
                    //忽视大小写
                    if (key.charAt(i)>=65 && key.charAt(i)<=90){
                        bean = baseMatchingContactEntity(""+(char) (key.charAt(i)+32),contactEntity);
                    }else if (key.charAt(i) >= 97 && key.charAt(i)<=122){
                        bean = baseMatchingContactEntity(""+(char) (key.charAt(i)-32),contactEntity);
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
     * @param contactEntity 联系人
     * @return null：key和contactEntity不匹配*/
    private TransmitBean baseMatchingContactEntity(String key, ContactEntity contactEntity){
        if (!TextUtils.isEmpty(key) && contactEntity.getName().contains(key)){
            return new TransmitBean(contactEntity.getId()
                    ,false);
        }
        return null;
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
            holder = new SearchResultViewHolder();
            holder.mAvatar = convertView.findViewById(R.id.contact_head);
            holder.mName = convertView.findViewById(R.id.contact_name);
            holder.mBox = convertView.findViewById(R.id.contact_list_item);
            convertView.findViewById(R.id.contact_state).setVisibility(View.GONE);

            convertView.setTag(holder);
        }else {
            holder = (SearchResultViewHolder) convertView.getTag();
        }

        AvatarManager.getInstance().loadContactAvatar(mSoftContext.get(),holder.mAvatar,bean.getAvatar());
        holder.mName.setText(bean.getName());

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
