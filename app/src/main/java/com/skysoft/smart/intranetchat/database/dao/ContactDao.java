/***
 * Copyright (c) 2019 ASKEY Computer Corp. and/or its affiliates. All rights reserved.
 * Created by Allen Luo on 2019/10/30
 * Description: [PT-48][Intranet Chat] [APP][Database] Create Database
 */
package com.skysoft.smart.intranetchat.database.dao;

import com.skysoft.smart.intranetchat.database.table.ContactEntity;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface ContactDao {
    @Insert
    void insert(ContactEntity... entities);

    @Update
    void update(ContactEntity... entities);

    @Delete
    void delete(List<ContactEntity> entities);

    @Query("select * from contact")
    List<ContactEntity> getAllContact();

    @Query("select * from contact where identifier = :identifier")
    ContactEntity getContact(String identifier);

    @Query("select contact.id from contact order by id desc limit 1")
    int getNewInsertId();
}
