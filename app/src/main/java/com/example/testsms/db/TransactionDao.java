package com.example.testsms.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    long insert(TransactionEntity transaction);

    @Update
    void update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<TransactionEntity> getAll();

    @Query("SELECT * FROM transactions WHERE currency = :currency ORDER BY date DESC")
    List<TransactionEntity> getTransactionsByCurrency(String currency);

    @Query("DELETE FROM transactions")
    void clearAll();
}
