package com.skysoft.smart.intranetchat.database.table;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "avatar")
public class AvatarEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo
    private String identifier;
    @ColumnInfo
    private String path;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "AvatarEntity{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
