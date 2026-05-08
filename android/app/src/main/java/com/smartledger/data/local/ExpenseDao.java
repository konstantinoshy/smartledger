package com.smartledger.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartledger.models.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    List<Expense> getAllExpenses();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Expense> expenses);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Expense expense);

    @Query("DELETE FROM expenses")
    void clearAll();
}
