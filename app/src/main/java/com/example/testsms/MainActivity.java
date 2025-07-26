package com.example.testsms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private static final long UPDATE_INTERVAL = 300_000; // 5 минут
    private static final String TAG = "MainActivity";

    private ListView lvTransactions;
    private TextView tvBalance;
    private ArrayList<String> transactionsList;
    private ArrayAdapter<String> adapter;
    private Handler handler = new Handler();
    private String lastSmsId = "";

    // Список отправителей (можно содержать часть строки)
    private final String[] ALLOWED_SENDERS = {"ASB.BY", "+37529"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvTransactions = findViewById(R.id.lvTransactions);
        tvBalance = findViewById(R.id.tvBalance);
        transactionsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, transactionsList);
        lvTransactions.setAdapter(adapter);

        checkSmsPermission();
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            initSmsMonitoring();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    SMS_PERMISSION_CODE);
        }
    }

    private void initSmsMonitoring() {
        loadLatestSms();
        startPeriodicUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initSmsMonitoring();
            } else {
                Toast.makeText(this,
                        "Для работы приложения нужно разрешение на чтение SMS",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startPeriodicUpdates() {
        handler.postDelayed(() -> {
            checkForNewMessages();
            handler.postDelayed(this::startPeriodicUpdates, UPDATE_INTERVAL);
        }, UPDATE_INTERVAL);
    }

    private void loadLatestSms() {
        try (Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[]{"_id", "address", "body", "date"},
                null, null,
                "date DESC")) {

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(1); // column "address"
                    if (isAllowedSender(address)) {
                        lastSmsId = cursor.getString(0); // column "_id"
                        processSms(cursor);
                        break; // нашли нужное сообщение — выходим
                    }
                }
            }
        } catch (Exception e) {
            showToast("Ошибка чтения SMS: " + e.getMessage());
        }
    }


    private void checkForNewMessages() {
        try (Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[]{"_id", "address", "body", "date"},
                null, null,
                "date DESC")) {

            if (cursor != null && cursor.moveToFirst()) {
                String currentId = cursor.getString(0);
                if (!currentId.equals(lastSmsId)) {
                    processSms(cursor);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки SMS", e);
            showToast("Ошибка проверки SMS: " + e.getMessage());
        }
    }

    private void processSms(Cursor cursor) {
        lastSmsId = cursor.getString(0);
        String address = cursor.getString(1);
        String body = cursor.getString(2);
        Log.d(TAG, "Получено SMS от " + address + ": " + body);

        if (isAllowedSender(address)) {
            TransactionInfo info = parseTransaction(body);
            updateUI(info);
        }
    }

    private boolean isAllowedSender(String address) {
        if (address == null) return false;
        for (String sender : ALLOWED_SENDERS) {
            if (address.contains(sender)) {
                return true;
            }
        }
        return false;
    }

    private TransactionInfo parseTransaction(String body) {
        TransactionInfo info = new TransactionInfo();

        // Тип операции
        if (body.contains("OPLATA")) {
            info.type = "Платеж";
        } else if (body.contains("POPOLNENIE")) {
            info.type = "Пополнение";
        } else if (body.contains("SPISANIE")) {
            info.type = "Списание";
        } else if (body.contains("ZACHISLENIE")) {
            info.type = "Начисление";
        } else {
            info.type = "Другая операция";
        }

        // Парсинг суммы и баланса
        Pattern p = Pattern.compile("(\\d+\\.\\d{2}) BYN");
        Matcher m = p.matcher(body);
        if (m.find()) {
            info.amount = m.group(1);
        }
        if (m.find()) {
            info.balance = m.group(1);
        }

        // Парсинг даты из текста, если есть
        p = Pattern.compile("DATA (\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2})");
        m = p.matcher(body);
        if (m.find()) {
            info.date = m.group(1);
        }

        return info;
    }

    private void updateUI(TransactionInfo info) {
        // Обновление баланса
        if (info.balance != null) {
            tvBalance.setText("Баланс: " + info.balance + " BYN");
        }

        // Добавление транзакции в список
        if (info.date != null && info.type != null && info.amount != null) {
            String transaction = String.format("%s\n%s: %s BYN",
                    info.date, info.type, info.amount);
            transactionsList.add(0, transaction);
            adapter.notifyDataSetChanged();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private static class TransactionInfo {
        String type;
        String amount;
        String balance;
        String date;
    }
}
