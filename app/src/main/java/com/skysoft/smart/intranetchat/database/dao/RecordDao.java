/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-48][Intranet Chat] [APP][Database] Create Database
 */
package com.skysoft.smart.intranetchat.database.dao;


import com.skysoft.smart.intranetchat.database.table.RecordEntity;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RecordDao {
    @Insert
    void insert(RecordEntity... entities);

    @Update
    void update(RecordEntity... entities);

    @Delete
    void delete(RecordEntity... entities);

    @Query("select * from record where receiver = :receiver and group_ = :group limit :page,:num")
    List<RecordEntity> getRecord(int receiver, int group, int page, int num);

    @Query("select count(*) from record where receiver = :receiver and group_ = :group")
    int getNumber(int receiver, int group);

    @Query("select time from record where receiver = :receiver and group_ =:group order by time desc limit 1")
    long getLatestRecordTime(int receiver, int group);

    @Query("select * from record where receiver =:receiver and group_ = :group and time <=:time and id < :id order by time desc limit 1")
    RecordEntity getLatestRecordBeforeTime(int receiver, int group, long time, int id);
}
