package com.example.testsms;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionInfo {
    public String balanceType; // "Карта" или "Наличные"
    public String type;
    public String amount;
    public String balance;
    public String date;
    public String category;

    // Форматтер для парсинга даты
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    public TransactionInfo() {}

    public TransactionInfo(String type, String amount, String balance, String date, String category) {
        this.type = type;
        this.amount = amount;
        this.balance = balance;
        this.date = date;
        this.category = category;
    }

    // Добавьте этот метод
    public Date getDateObject() {
        try {
            return DATE_FORMAT.parse(this.date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Или выбросьте RuntimeException, если дата всегда должна быть валидной
        }
    }
}