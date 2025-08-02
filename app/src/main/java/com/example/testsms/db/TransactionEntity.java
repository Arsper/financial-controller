package com.example.testsms.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.testsms.model.TransactionInfo;

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
    public String currency = "BYN";

    // Конструктор из модели
    public TransactionEntity(TransactionInfo info) {
        this.id = info.id;
        this.type = info.type;
        this.amount = info.amount;
        this.balance = info.balance;
        this.date = info.date;
        this.balanceType = info.balanceType;
        this.category = info.category;
        this.currency = info.currency;
    }

    // Пустой конструктор
    public TransactionEntity() {}
}
