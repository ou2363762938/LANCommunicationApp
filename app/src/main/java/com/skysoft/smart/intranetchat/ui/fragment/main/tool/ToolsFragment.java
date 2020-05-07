/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/15
 * Description: [PT-40][Intranet Chat] [APP][UI] Home page ui
 */
package com.skysoft.smart.intranetchat.ui.fragment.main.tool;

import android.content.Intent;
import android.provider.AlarmClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.BaseFragment;
import com.skysoft.smart.intranetchat.ui.activity.calendar.MyCalendarView;
import com.skysoft.smart.intranetchat.ui.activity.weather.WeatherActivity;

public class ToolsFragment extends BaseFragment implements View.OnClickListener {

    private LinearLayout mWeather;
    private LinearLayout mClock;
    private LinearLayout mMemo;
    private TextView mPageTitle;

    @Override
    protected int getLayout() {
        return R.layout.fragment_main_tools;
    }

    @Override
    protected void initView(View view) {
        super.initView(view);
        mWeather =  view.findViewById(R.id.tools_linear_weather);
        mWeather.setOnClickListener(this::onClick);
        mClock = view.findViewById(R.id.tools_linear_clock);
        mClock.setOnClickListener(this::onClick);
        mMemo = view.findViewById(R.id.tools_linear_calendar);
        mMemo.setOnClickListener(this::onClick);
        mPageTitle = view.findViewById(R.id.page_title);
    }

    @Override
    protected void initData() {
        super.initData();
        mPageTitle.setText("工具");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tools_linear_weather:
                Intent weather = new Intent(getActivity(), WeatherActivity.class);
                startActivity(weather);
                break;
            case R.id.tools_linear_clock:
                Intent clock = new Intent(AlarmClock.ACTION_SET_ALARM);
                startActivity(clock);
                break;
            case R.id.tools_linear_calendar:
                Intent intent = new Intent(getActivity(), MyCalendarView.class);
                startActivity(intent);
                break;
                default:
                    break;
        }
    }
}