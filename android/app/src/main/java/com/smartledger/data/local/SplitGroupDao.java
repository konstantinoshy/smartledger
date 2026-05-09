package com.smartledger.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartledger.models.SplitGroup;

import java.util.List;

@Dao
public interface SplitGroupDao {

    @Query("SELECT * FROM split_groups ORDER BY createdAt DESC")
    List<SplitGroup> getAllGroups();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SplitGroup> groups);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SplitGroup group);

    @Query("DELETE FROM split_groups WHERE id = :groupId")
    void deleteById(String groupId);

    @Query("DELETE FROM split_groups")
    void clearAll();
}
