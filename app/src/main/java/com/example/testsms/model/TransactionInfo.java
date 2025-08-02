package com.example.testsms.model;

import com.example.testsms.db.TransactionEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionInfo {
    public int id;
    public String type;
    public String amount;
    public String balance;
    public String date;
    public String balanceType;
    public String category;
    public String currency = "BYN"; // ← добавлено

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    // Пустой конструктор
    public TransactionInfo() {}

    // Конструктор с параметрами (без balanceType и id)
    public TransactionInfo(String type, String amount, String balance, String date, String category) {
        this.type = type;
        this.amount = amount;
        this.balance = balance;
        this.date = date;
        this.category = category;
    }

    // Полный конструктор
    public TransactionInfo(int id, String type, String amount, String balance, String date,
                           String balanceType, String category, String currency) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.balance = balance;
        this.date = date;
        this.balanceType = balanceType;
        this.category = category;
        this.currency = currency;
    }

    // Конструктор из TransactionEntity (для загрузки из БД)
    public TransactionInfo(TransactionEntity e) {
        this.id = e.id;
        this.type = e.type;
        this.amount = e.amount;
        this.balance = e.balance;
        this.date = e.date;
        this.balanceType = e.balanceType;
        this.category = e.category;
        this.currency = e.currency; // ← добавлено
    }

    // Преобразование строки даты в Date
    public Date getDateObject() {
        if (date == null || date.isEmpty()) return null;
        try {
            return DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
