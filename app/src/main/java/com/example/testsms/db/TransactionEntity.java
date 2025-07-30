package com.example.testsms.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String type;
    public String amount;
    public String balance;
    public String date;
    public String balanceType;
    public String category;
}
