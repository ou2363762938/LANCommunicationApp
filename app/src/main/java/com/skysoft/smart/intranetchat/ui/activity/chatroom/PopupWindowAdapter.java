package com.skysoft.smart.intranetchat.ui.activity.chatroom;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.skysoft.smart.intranetchat.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PopupWindowAdapter extends RecyclerView.Adapter<PopupWindowAdapter.PopupWindowHolder> {
    private static final String TAG = "PopupWindowAdapter";
    private Context mContext;
    private List<String> mItemList;
    private PopupWindow mPopupWindow;

    public PopupWindowAdapter(Context mContext, List<String> mItemList,PopupWindow mPopupWindow) {
        this.mContext = mContext;
        this.mItemList = mItemList;
        this.mPopupWindow = mPopupWindow;
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
                Log.d(TAG, "onClick: " + mItemList.get(position));
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
