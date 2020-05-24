/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-48][Intranet Chat] [APP][Database] Create Database
 */
package com.skysoft.smart.intranetchat.database.dao;

import com.skysoft.smart.intranetchat.database.table.LatestEntity;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface LatestDao {

    @Insert
    void insert(LatestEntity... entities);

    @Update
    void update(LatestEntity... entities);

    @Delete
    void delete(LatestEntity... entities);

    //B:[Intranet Chat] [APP][UI] Chat Room, Oliver Ou, 2019/10/31
    @Query("select * from latest")
    List<LatestEntity> getAllHistory();
    //E:[Intranet Chat] [APP][UI] Chat Room, Oliver Ou, 2019/10/31

    @Query("select * from latest where user =:user")
    LatestEntity getHistory(int user);

    @Query("select id from latest order by id desc limit 1")
    int getNewInsertId();
}
