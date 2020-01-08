/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Oliver Ou on 2019/10/14
 * Description: [Intranet Chat] [APP] [Communication] Create a new process service with message sending and receiving.
 */
package com.skysoft.smart.intranetchat.tools;

import android.util.Log;

import com.google.gson.Gson;

public class GsonTools {
    private static final String TAG = GsonTools.class.getSimpleName();
    private static Gson sGson = new Gson();

    public static String toJson(Object obj){
        String json = null;
        try{
            json = sGson.toJson(obj);
        }catch (Exception e){
            Log.e(TAG, "toJson: ", e);
        }
        return json;
    }

    public static Object formJson(String json, Class tClass){
        Object obj = null;
        try{
            obj = sGson.fromJson(json,tClass);
        }catch (Exception e){
            Log.e(TAG, "formJson: ", e);
        }
        return obj;
    }
}
