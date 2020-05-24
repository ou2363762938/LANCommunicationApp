package com.skysoft.smart.intranetchat.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.skysoft.smart.intranetchat.database.table.AvatarEntity;

import java.util.List;

@Dao
public interface AvatarDao {
    @Query("select * from avatar")
    List<AvatarEntity> getAllAvatar();

    @Query("select * from avatar where identifier = :identifier")
    AvatarEntity getAvatar(String identifier);

    @Query("select * from avatar where id = :id")
    AvatarEntity getAvatar(int id);

    @Query("select avatar.id from avatar order by id desc limit 1")
    int getNewInsertId();

    @Update
    void update(AvatarEntity... avatars);

    @Delete
    void delete(AvatarEntity... avatars);

    @Insert
    void insert(AvatarEntity... avatars);
}
