package com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.database.table.RecordEntity;
import com.skysoft.smart.intranetchat.model.chat.record.RecordAdapter;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.PopupWindow.PopupWindowAdapter;

public class OnLongClickRecord implements View.OnLongClickListener {
    private static final String TAG = "OnLongClickRecord";
    private RecordEntity mRecordEntity;     //被长按的记录
    private ChatRoomMessageViewHolder holder;   //被长按的holder
    private RecordAdapter mAdapter;
    private View mView;
    private Context mContext;

    public OnLongClickRecord(Context context,
                             RecordEntity mRecordEntity,
                             ChatRoomMessageViewHolder holder,
                             RecordAdapter adapter) {
        this.mContext = context;
        this.mRecordEntity = mRecordEntity;
        this.holder = holder;
        this.mAdapter = adapter;
        this.mView = adapter.getInflate();
    }

    @Override
    public boolean onLongClick(View v) {
        showPopupMenu(v,
                mRecordEntity,
                holder.getBox().getTop() + holder.getBox().getScrollX(),  //计算view到聊天室顶部的距离
                holder.getSenderName().getVisibility() == View.VISIBLE ? holder.getSenderName().getHeight() : 0,
                holder.getAdapterPosition(),
                mAdapter);
        return false;
    }

    /**
     * 弹出popupWindow
     * @param view 被长按的控件，popupWindow围绕view显示
     * @param recordEntity 被长按的控件对应的聊天记录
     * @param relativeY 控件相对于聊天室顶部的相对距离
     * @param nameHeight 对方名字的高度*/
    public void showPopupMenu(View view,
                              RecordEntity recordEntity,
                              int relativeY, int nameHeight,
                              int position,
                              RecordAdapter chatRoomAdapter){
//        if (null == mView){
//            mView = LayoutInflater.from(mContext).inflate(R.layout.chat_message_popup_window, null);
//        }
//        mView = LayoutInflater.from(mContext).inflate(R.layout.chat_message_popup_window, null);

        PopupWindow popupWindow = new PopupWindow(mView, ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        RecyclerView recyclerView = chatRoomAdapter.getPopupRecyclerView();
        if (null == recyclerView){
            recyclerView = mView.findViewById(R.id.chat_message_popup_window);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext,LinearLayoutManager.HORIZONTAL,false);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.HORIZONTAL));
            chatRoomAdapter.setPopupRecyclerView(recyclerView);
        }

        //popupWindow弹出的内容
        PopupWindowAdapter adapter = new PopupWindowAdapter(
                mContext,
                recordEntity,
                popupWindow,
                mAdapter,
                position);
        recyclerView.setAdapter(adapter);

        //设置popupWindow点击外部消失
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);

        //测量popupWindow的长宽
        View contentView = popupWindow.getContentView();
        contentView.measure(makeDropDownMeasureSpec(popupWindow.getWidth()),
                makeDropDownMeasureSpec(popupWindow.getHeight()));

        int offsetX = 0;
        int offsetY = -(contentView.getMeasuredHeight() + view.getHeight());

        //如果是接收的消息，popupWindow相对于view靠左，反之靠右
        if (recordEntity.getSender() != -1){
            TLog.d(TAG, "showPopupMenu: right");
            offsetX = -(contentView.getMeasuredWidth() - view.getWidth());
        }

        //判断popupWindow在view上方或者下方
        if (relativeY < contentView.getMeasuredHeight()){
            offsetY = 0;
        }else {
            offsetY -= nameHeight;
        }

        //显示popupWindow
        popupWindow.showAsDropDown(view,offsetX,offsetY, Gravity.LEFT);
    }

    public static int makeDropDownMeasureSpec(int measureSpec){
        int mode = 0;
        if (measureSpec == ViewGroup.LayoutParams.WRAP_CONTENT){
            mode = View.MeasureSpec.UNSPECIFIED;
        }else {
            mode = View.MeasureSpec.EXACTLY;
        }
        return View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(measureSpec),mode);
    }
}
