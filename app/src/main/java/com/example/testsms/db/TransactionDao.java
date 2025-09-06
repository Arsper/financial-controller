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
    void update(TransactionEntity entity);

    @Delete
    void delete(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<TransactionEntity> getAll();

    @Query("SELECT * FROM transactions WHERE currency = :currency ORDER BY date DESC")
    List<TransactionEntity> getTransactionsByCurrency(String currency);

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate and :endDate ORDER BY date DESC")
    List<TransactionEntity> getTransactionsByDateRange(String startDate, String endDate);

    @Query("SELECT * FROM transactions WHERE currency = :currency " +
            "AND (:category IS NULL OR :category = '' OR category = :category ) " +
            "AND date BETWEEN :startDate and :endDate " +
            " ORDER BY date DESC")
    List<TransactionEntity> getTransactionsStaticWithStringDates(String currency, String category, String startDate, String endDate);

    @Query("DELETE FROM transactions")
    void clearAll();
}
