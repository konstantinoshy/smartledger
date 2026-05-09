package com.smartledger.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.smartledger.models.Expense;
import com.smartledger.models.PortfolioAsset;
import com.smartledger.models.SplitExpense;
import com.smartledger.models.SplitGroup;

@Database(
    entities = {Expense.class, SplitGroup.class, SplitExpense.class, PortfolioAsset.class},
    version = 2,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ExpenseDao expenseDao();
    public abstract SplitGroupDao splitGroupDao();
    public abstract SplitExpenseDao splitExpenseDao();
    public abstract PortfolioAssetDao portfolioAssetDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "smartledger_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
