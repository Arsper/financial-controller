package com.example.testsms;

public class TransactionInfo {
    public String balanceType; // "Карта" или "Наличные"
    public String type;
    public String amount;
    public String balance;
    public String date;
    public String category;

    public TransactionInfo() {}

    public TransactionInfo(String type, String amount, String balance, String date, String category) {
        this.type = type;
        this.amount = amount;
        this.balance = balance;
        this.date = date;
        this.category = category;
    }
}
