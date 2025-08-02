package com.example.testsms.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;


@Database(entities = {TransactionEntity.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
}
