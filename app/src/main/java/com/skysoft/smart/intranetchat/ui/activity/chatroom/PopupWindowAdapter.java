package com.skysoft.smart.intranetchat.ui.activity.chatroom;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.database.table.ChatRecordEntity;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PopupWindowAdapter extends RecyclerView.Adapter<PopupWindowAdapter.PopupWindowHolder> {
    private static final String TAG = "PopupWindowAdapter";
    private Context mContext;
    private List<String> mItemList;
    private PopupWindow mPopupWindow;   //关闭popupWindow
    private ChatRecordEntity mChatRecordEntity;     //长按的消息记录
    private ChatRoomMessageAdapter mChatAdapter;

    public PopupWindowAdapter(Context mContext,ChatRecordEntity mChatRecordEntity, PopupWindow mPopupWindow, ChatRoomMessageAdapter mChatAdapter) {
        this.mContext = mContext;
        this.mChatRecordEntity = mChatRecordEntity;
        //判断长按的消息记录
        switch (mChatRecordEntity.getType()){
            case ChatRoomConfig.RECORD_TEXT:
                mItemList = Arrays.asList(mContext.getResources().getStringArray(R.array.popup_window_item_text));
                break;
            case ChatRoomConfig.RECORD_VOICE:
                mItemList = Arrays.asList(mContext.getResources().getStringArray(R.array.popup_window_item_voice));
                break;
            case ChatRoomConfig.RECORD_IMAGE:
            case ChatRoomConfig.RECORD_VIDEO:
            case ChatRoomConfig.RECORD_FILE:
                mItemList = Arrays.asList(mContext.getResources().getStringArray(R.array.popup_window_item_other));
                break;
        }
        this.mPopupWindow = mPopupWindow;
        this.mChatAdapter = mChatAdapter;
    }

    @NonNull
    @Override
    public PopupWindowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.chat_message_popup_window_item, parent, false);
        return new PopupWindowHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull PopupWindowHolder holder, int position) {
        holder.mItem.setText(mItemList.get(position));
        holder.mItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String clickContent = mItemList.get(position); //点击内容
                Log.d(TAG, "onClick: " + clickContent);
                if (clickContent.equals(mContext.getResources().getString(R.string.copy))){
                    //点击复制

                }else if (clickContent.equals(mContext.getResources().getString(R.string.transmit))){
                    //点击转发
                    TransmitActivity.startActivity(mContext);
                }else if (clickContent.equals(mContext.getResources().getString(R.string.delete))){
                    //点击删除

                }else if (clickContent.equals(mContext.getResources().getString(R.string.replay))){
                    //点击回复

                }
                mPopupWindow.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        return null == mItemList ? 0 : mItemList.size();
    }

    public static class PopupWindowHolder extends RecyclerView.ViewHolder{
        public TextView mItem;

        public PopupWindowHolder(@NonNull View itemView) {
            super(itemView);
            mItem = itemView.findViewById(R.id.chat_message_popup_window_item);
        }
    }
}
