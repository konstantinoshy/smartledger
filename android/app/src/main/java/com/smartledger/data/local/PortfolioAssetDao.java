package com.smartledger.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartledger.models.PortfolioAsset;

import java.util.List;

@Dao
public interface PortfolioAssetDao {

    @Query("SELECT * FROM portfolio_assets ORDER BY symbol ASC")
    List<PortfolioAsset> getAllAssets();

    @Query("SELECT * FROM portfolio_assets WHERE symbol = :symbol LIMIT 1")
    PortfolioAsset getBySymbol(String symbol);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PortfolioAsset> assets);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PortfolioAsset asset);

    @Query("DELETE FROM portfolio_assets WHERE symbol = :symbol")
    void deleteBySymbol(String symbol);

    @Query("DELETE FROM portfolio_assets")
    void clearAll();
}
