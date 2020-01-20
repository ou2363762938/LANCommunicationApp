package com.skysoft.smart.intranetchat.tools.toastutil;

import android.util.Log;

public class TLog {
    public static boolean sOpen = true;

    public static void d(String TAG,String message){
        if (sOpen){
            TLog.d(TAG, "d: " + message);
        }
    }

    public static void e(String TAG, String message){
        if (sOpen){
            TLog.e(TAG, "e: " + message);
        }
    }

    public static void e(String TAG, String messsage, Throwable tr){
        Log.e(TAG, "e: ", tr);
    }

    public static void w(String TAG, String message){
        if (sOpen){
            TLog.w(TAG, "w: " + message );
        }
    }

    public static void i(String TAG, String message){
        if (sOpen){
            TLog.i(TAG, "i: " + message);
        }
    }

    public static void m(String TAG, String message){
        if (sOpen){
            TLog.d(TAG, "m() called with: TAG = [" + TAG + "], message = [" + message + "]");
        }
    }
}
