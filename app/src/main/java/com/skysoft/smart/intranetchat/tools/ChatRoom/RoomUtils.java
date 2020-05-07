package com.skysoft.smart.intranetchat.tools.ChatRoom;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;


public class RoomUtils {

    /**
     * 改变Recycler的滑动速度
     * @param recyclerView
     * @param velocity      //滑动速度默认是8000dp
     */
    public static void setMaxFlingVelocity(RecyclerView recyclerView, int velocity){
        try{
            Field field = recyclerView.getClass().getDeclaredField("mMaxFlingVelocity");
            field.setAccessible(true);
            field.set(recyclerView, velocity);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String millsToTime(long time) {
        Date date = new Date(time);
        return millsToTime(date);
    }

    public static boolean initMillToTmie(long time){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int nowDay = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.setTimeInMillis(time);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        if (nowDay < day){
            return false;
        }
        return true;
    }

    public static String millsToTime(Date date){
        int hours = date.getHours();
        int minutes = date.getMinutes();
        StringBuilder sb = new StringBuilder();
        sb.append(hours);
        sb.append(':');
        if (minutes < 10){
            sb.append(0);
        }
        sb.append(minutes);
        return sb.toString();
    }

    public static String millToFullTime(long time){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(time));
        String fullTime = String.valueOf(calendar.get(Calendar.MONTH) + 1) + "月" + (calendar.get(Calendar.DAY_OF_MONTH))+ "日" + millsToTime(time);
        calendar.clear();
        return fullTime;
    }
}
