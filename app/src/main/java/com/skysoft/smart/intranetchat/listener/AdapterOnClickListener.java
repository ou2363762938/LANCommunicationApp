package com.skysoft.smart.intranetchat.listener;

import android.view.View;

import com.skysoft.smart.intranetchat.tools.QuickClickListener;

public class AdapterOnClickListener{
    private Object obj;
    private int position;

    public void onItemClickListener(View view, Object obj, int position) {
        if (!QuickClickListener.isFastClick()) {
            return ;
        }
    }

    public void init(Object obj, int position) {
        this.obj = obj;
        this.position = position;
    }
}
