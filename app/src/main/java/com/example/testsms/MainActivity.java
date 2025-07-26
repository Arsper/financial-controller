package com.example.testsms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;
    private static final long UPDATE_INTERVAL = 300_000; // 5 минут

    private ListView lvTransactions;
    private TextView tvBalanceCart;
    private TextView tvBalanceCash;
    private Button btnAdd;

    private TransactionAdapter adapter;
    private ArrayList<TransactionInfo> transactionInfos;
    private Handler handler = new Handler();
    private String lastSmsId = "";

    private double balanceCart = 0.0;
    private double balanceCash = 0.0;

    private final String[] ALLOWED_SENDERS = {"ASB.BY"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvTransactions   = findViewById(R.id.lvTransactions);
        tvBalanceCart    = findViewById(R.id.tvBalanceCart);
        tvBalanceCash    = findViewById(R.id.tvBalanceCash);
        btnAdd           = findViewById(R.id.button);

        transactionInfos = new ArrayList<>();
        adapter          = new TransactionAdapter(this, transactionInfos);
        lvTransactions.setAdapter(adapter);

        // Добавление новой транзакции
        btnAdd.setOnClickListener(v -> {
            // По умолчанию показываем баланс карты
            TransactionDialog.show(this, null, balanceCart, (info, isNew) -> onTransactionSaved(info, isNew));
        });


        // Редактирование по клику
        lvTransactions.setOnItemClickListener((p, view, pos, id) -> {
            TransactionInfo existing = transactionInfos.get(pos);
            double selectedBalance = "Карта".equals(existing.balanceType)
                    ? balanceCart
                    : balanceCash;

            TransactionDialog.show(this, existing, selectedBalance, (info, isNew) -> onTransactionSaved(info, isNew));
        });

        checkSmsPermission();
    }

    private void onTransactionSaved(TransactionInfo info, boolean isNew) {
        double balance = Double.parseDouble(info.balance);

        if ("Карта".equals(info.balanceType)) {
            balanceCart = balance;
            tvBalanceCart.setText("Карта: " + String.format("%.2f BYN", balanceCart));
        } else {
            balanceCash = balance;
            tvBalanceCash.setText("Наличные: " + String.format("%.2f BYN", balanceCash));
        }

        if (isNew) transactionInfos.add(0, info);
        adapter.notifyDataSetChanged();
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initSmsMonitoring();
        } else {
            Toast.makeText(this,
                    "Для работы нужно разрешение на чтение SMS",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void initSmsMonitoring() {
        loadLatestSms();
        startPeriodicUpdates();
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
                    String address = cursor.getString(1);
                    if (isAllowedSender(address)) {
                        lastSmsId = cursor.getString(0);
                        processAndStore(cursor);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    processAndStore(cursor);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка проверки SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndStore(Cursor cursor) {
        lastSmsId = cursor.getString(0);
        String address = cursor.getString(1);
        String body = cursor.getString(2);
        if (!isAllowedSender(address)) return;

        TransactionInfo info = parseTransaction(body);

        // по умолчанию SMS считаем по "Карта"
        info.balanceType = "Карта";

        onTransactionSaved(info, true);
    }

    private boolean isAllowedSender(String address) {
        if (address == null) return false;
        for (String s : ALLOWED_SENDERS) {
            if (address.contains(s)) return true;
        }
        return false;
    }

    private TransactionInfo parseTransaction(String body) {
        TransactionInfo info = new TransactionInfo();

        // Тип
        if (body.contains("OPLATA"))        info.type = "Платеж";
        else if (body.contains("POPOLNENIE")) info.type = "Пополнение";
        else if (body.contains("SPISANIE"))   info.type = "Списание";
        else if (body.contains("ZACHISLENIE"))info.type = "Начисление";
        else info.type = "Другая";

        // Сумма и баланс
        Pattern p = Pattern.compile("(\\d+\\.\\d{2}) BYN");
        Matcher m = p.matcher(body);
        if (m.find()) info.amount = m.group(1);
        if (m.find()) info.balance = m.group(1);

        // Дата
        p = Pattern.compile("DATA (\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2})");
        m = p.matcher(body);
        info.date = m.find() ? m.group(1) : "";

        info.category = "Другое";
        return info;
    }
}
