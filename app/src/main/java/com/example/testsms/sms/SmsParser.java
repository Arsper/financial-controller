package com.example.testsms.sms;

import com.example.testsms.model.TransactionInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    public static TransactionInfo parse(String body) {
        TransactionInfo info = new TransactionInfo();

        if (body.contains("OPLATA")) info.type = "Платеж";
        else if (body.contains("POPOLNENIE")) info.type = "Пополнение";
        else if (body.contains("SPISANIE")) info.type = "Списание";
        else if (body.contains("ZACHISLENIE")) info.type = "Начисление";
        else info.type = "Другая";

        Matcher m = Pattern.compile("(Сумма|AMOUNT)[:\\s]+(\\d+\\.\\d{2})").matcher(body);
        if (m.find()) info.amount = m.group(2);
        else {
            m = Pattern.compile("(\\d+\\.\\d{2})").matcher(body);
            if (m.find()) info.amount = m.group(1);
        }

        m = Pattern.compile("(Баланс|OSTATOK|DOSTUPNO|BALANCE)[:\\s]+(\\d+\\.\\d{2})").matcher(body);
        if (m.find()) info.balance = m.group(2);
        else {
            m = Pattern.compile("(\\d+\\.\\d{2})").matcher(body);
            if (m.find()) m.find(); // skip first
            if (m.find()) info.balance = m.group(1);
        }

        m = Pattern.compile("DATA (\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2})").matcher(body);
        info.date = m.find() ? m.group(1) : "";

        info.category = "Другое";
        return info;
    }
}
