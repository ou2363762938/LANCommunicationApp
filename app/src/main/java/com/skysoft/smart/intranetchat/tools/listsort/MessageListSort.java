/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-60][Intranet Chat] [APP][UI] contact list page
 */
package com.skysoft.smart.intranetchat.tools.listsort;

import android.util.Log;

import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.database.table.LatestChatHistoryEntity;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessageListSort {

    public static boolean Today(List<LatestChatHistoryEntity> myBeanList){
        int i = 0;
        for (; i < myBeanList.size(); i++){
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = dateformat.format(myBeanList.get(i).getContentTimeMill());
            Log.d("dateformat", "onCreateView:  " + dateStr);
            myBeanList.get(i).setContentTime(dateStr);
            Log.d("ContentTimeMill", "onCreateView:  " + myBeanList.get(i).getContentTimeMill());
        }
        return true;
    }
    public static void CollectionsList(List<LatestChatHistoryEntity> myBeanList) {
        Today(myBeanList);
        Collections.sort(myBeanList, new Comparator<LatestChatHistoryEntity>() {
            @Override
            public int compare(LatestChatHistoryEntity o1, LatestChatHistoryEntity o2) {
                int i = o1.getTop() - o2.getTop();
                if(i == 0){
                    return o1.getContentTimeMill() - o2.getContentTimeMill() > 0 ? -1 : 1;
                }
                return -i;
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }
        });
    }
}