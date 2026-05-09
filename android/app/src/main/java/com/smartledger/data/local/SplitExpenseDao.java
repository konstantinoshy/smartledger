package com.smartledger.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartledger.models.SplitExpense;

import java.util.List;

@Dao
public interface SplitExpenseDao {

    @Query("SELECT * FROM split_expenses WHERE groupId = :groupId ORDER BY createdAt DESC")
    List<SplitExpense> getExpensesByGroup(String groupId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SplitExpense> expenses);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SplitExpense expense);

    @Query("UPDATE split_expenses SET settled = 1 WHERE groupId = :groupId")
    void settleAllByGroup(String groupId);

    @Query("DELETE FROM split_expenses WHERE groupId = :groupId")
    void clearByGroup(String groupId);

    @Query("DELETE FROM split_expenses")
    void clearAll();
}
