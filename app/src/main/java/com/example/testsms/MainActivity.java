package com.example.testsms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Dao;
import androidx.room.Room;

import com.example.testsms.db.AppDatabase;
import com.example.testsms.db.TransactionEntity;
import com.example.testsms.model.TransactionInfo;
import com.example.testsms.sms.SmsHelper;
import com.example.testsms.sms.SmsParser;
import com.example.testsms.ui.BalanceDialog;
import com.example.testsms.ui.ConverDialog;
import com.example.testsms.ui.StaticDialog;
import com.example.testsms.ui.TransactionAdapter;
import com.example.testsms.ui.TransactionDialog;
import com.example.testsms.util.TransactionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private static final long UPDATE_INTERVAL =240_000;

    private AppDatabase db;
    private SmsHelper smsHelper;
    private Handler handler = new Handler();

    private ListView lvTransactions;
    private TextView tvBalanceTotal;
    private ImageButton btnAdd;
    private ImageButton btnStatic;
    private ImageButton btnConvert;
    private LinearLayout balanceContainer;

    private ArrayList<TransactionInfo> allTransactions = new ArrayList<>();
    private ArrayList<TransactionInfo> filteredTransactions = new ArrayList<>();
    private TransactionAdapter adapter;

    private double balanceCart = 0.0;
    private double balanceCash = 0.0;
    private double balanceTotal = 0.0;
    private String lastSmsId = "";

    private TextView textCurrency;
    private String[] currencies;

    private String currentCurrency = "BYN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currencies = getResources().getStringArray(R.array.currency_array);

        textCurrency = findViewById(R.id.textCurrency);
        lvTransactions = findViewById(R.id.lvTransactions);

        lvTransactions.setOnItemClickListener((parent, view, position, id) -> {
            TransactionInfo selectedTransaction = filteredTransactions.get(position);
            double selectedBalance = "Карта".equals(selectedTransaction.balanceType)
                    ? balanceCart
                    : balanceCash;

            TransactionDialog.show(this, selectedTransaction, selectedBalance, this::onTransactionSaved);
        });

        tvBalanceTotal = findViewById(R.id.tvBalanceTotal);
        btnAdd = findViewById(R.id.buttonAdd);
        btnStatic = findViewById(R.id.btnStatic);
        btnConvert = findViewById(R.id.btnConvector);
        balanceContainer = findViewById(R.id.balanceContainer);

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "transaction-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        smsHelper = new SmsHelper(this);
        adapter = new TransactionAdapter(this, filteredTransactions);
        lvTransactions.setAdapter(adapter);

        initViews();
        loadTransactionsFromDb();
        checkSmsPermission();
        setupCurrencySelector();
    }

    private void initViews() {
        btnAdd.setOnClickListener(v ->
                TransactionDialog.show(this, null, 0.0, this::onTransactionSaved));

        btnStatic.setOnClickListener(v ->{
            StaticDialog.show(this, this, db);
        });

        btnConvert.setOnClickListener(v ->{
            ConverDialog.show(this, this);
        });

        balanceContainer.setOnClickListener(v -> {
            BalanceDialog.show(
                    this,
                    balanceTotal,
                    balanceCart,
                    balanceCash,
                    currentCurrency,
                    newCurrency -> {
                        currentCurrency = newCurrency;
                        textCurrency.setText(newCurrency);
                        filterTransactionsByCurrency();
                    },
                    this::getConvertedBalance
            );
        });
    }

    private double[] getConvertedBalance(String currency) {
        return new double[]{balanceTotal, balanceCart, balanceCash};
    }

    private void setupCurrencySelector() {
        textCurrency.setText(currentCurrency);

        textCurrency.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Выберите валюту");
            builder.setItems(currencies, (dialog, which) -> {
                currentCurrency = currencies[which];
                textCurrency.setText(currentCurrency);
                filterTransactionsByCurrency();
            });
            builder.show();
        });
    }

    private void loadTransactionsFromDb() {
        List<TransactionEntity> saved = db.transactionDao().getAll();
        for (TransactionEntity e : saved) {
            Log.d("DB_DEBUG", "Loaded from DB: id=" + e.id + ", desc=" + e.description);
        }
        allTransactions.clear();
        for (TransactionEntity e : saved) {
            allTransactions.add(new TransactionInfo(e));
        }
        filterTransactionsByCurrency();
        lastSmsId = smsHelper.loadLastSmsId();
    }

    private void filterTransactionsByCurrency() {
        filteredTransactions.clear();
        for (TransactionInfo t : allTransactions) {
            if (t.currency != null && t.currency.equals(currentCurrency)) {
                filteredTransactions.add(t);
            }
        }
        recalculateBalancesAndRefreshUI();
    }

    private void recalculateBalancesAndRefreshUI() {
        balanceCart = 0.0;
        balanceCash = 0.0;
        Collections.sort(filteredTransactions, (a, b) -> a.date.compareTo(b.date));

        for (TransactionInfo t : filteredTransactions) {
            double amount = Double.parseDouble(t.amount);
            if (t.type.equals("Пополнение") || t.type.equals("Начисление")) {
                if ("Карта".equals(t.balanceType)) {
                    balanceCart += amount;
                } else {
                    balanceCash += amount;
                }
            } else if (t.type.equals("Списание") && "Карта".equals(t.balanceType) && "Перевод на Наличку".equals(t.category)) {
                balanceCart -= amount;
                balanceCash += amount;
            } else {
                if ("Карта".equals(t.balanceType)) {
                    balanceCart -= amount;
                } else {
                    balanceCash -= amount;
                }
            }
            if ("Карта".equals(t.balanceType)) {
                t.balance = String.format(Locale.US, "%.2f", balanceCart);
            } else {
                t.balance = String.format(Locale.US, "%.2f", balanceCash);
            }
        }

        balanceTotal = balanceCart + balanceCash;
        tvBalanceTotal.setText(String.format(Locale.US, "%.2f %s", balanceTotal, currentCurrency));

        TransactionUtils.sortTransactions(filteredTransactions);
        adapter.notifyDataSetChanged();
    }

    private void onTransactionSaved(TransactionInfo info, boolean isNew) {
        if (!isNew) {
            TransactionEntity entity = new TransactionEntity(info);
            Log.d("DB_DEBUG", "Updating entity id=" + entity.id + ", desc=" + entity.description);
            db.transactionDao().update(entity);
            for (int i = 0; i < allTransactions.size(); i++) {
                if (allTransactions.get(i).id == info.id) {
                    allTransactions.set(i, info);
                    break;
                }
            }
        } else {
            Log.d("DEBUG_DESC_MAIN", "Received description in MainActivity: " + info.description);
            // Дополнительно: можно вывести весь объект
            Log.d("DEBUG_DESC_MAIN", "Full info: " + info.toString());
            TransactionEntity entity = new TransactionEntity(info);
            long newId = db.transactionDao().insert(entity);
            info.id = (int) newId;
            allTransactions.add(info);
        }
        filterTransactionsByCurrency();
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

    private void loadLatestSms() {
        List<TransactionInfo> newTransactions = new ArrayList<>();
        String maxSmsId = lastSmsId;

        try (Cursor cursor = smsHelper.getNewMessages(lastSmsId)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String smsId = cursor.getString(0);
                    String address = cursor.getString(1);

                    if (smsHelper.isAllowedSender(address)) {
                        String body = cursor.getString(2);
                        TransactionInfo info = SmsParser.parse(body);
                        info.balanceType = "Карта";
                        if (info.currency == null || info.currency.isEmpty()) {
                            info.currency = currentCurrency;
                        }

                        boolean isDuplicate = false;
                        for (TransactionInfo t : allTransactions) {
                            if (t.date.equals(info.date) && t.amount.equals(info.amount)) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (!isDuplicate) {
                            TransactionEntity entity = new TransactionEntity(info);
                            long newId = db.transactionDao().insert(entity);
                            info.id = (int) newId;
                            newTransactions.add(info);
                        }
                    }

                    // Запоминаем максимальный ID
                    if (smsId.compareTo(maxSmsId) > 0) {
                        maxSmsId = smsId;
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        if (!newTransactions.isEmpty()) {
            allTransactions.addAll(newTransactions);
            lastSmsId = maxSmsId;
            smsHelper.saveLastSmsId(lastSmsId);
            filterTransactionsByCurrency();
        }
    }


    private void startPeriodicUpdates() {
        handler.postDelayed(() -> {
            checkForNewMessages();
            handler.postDelayed(this::startPeriodicUpdates, UPDATE_INTERVAL);
        }, UPDATE_INTERVAL);
    }

    private void checkForNewMessages() {
        List<TransactionInfo> newTransactions = new ArrayList<>();
        String newLastSmsId = lastSmsId;

        try (Cursor cursor = smsHelper.getNewMessages(lastSmsId)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(1);
                    if (smsHelper.isAllowedSender(address)) {
                        String body = cursor.getString(2);
                        TransactionInfo info = SmsParser.parse(body);
                        info.balanceType = "Карта";
                        if (info.currency == null || info.currency.isEmpty()) {
                            info.currency = currentCurrency;
                        }

                        // Проверяем на дубликаты
                        boolean isDuplicate = false;
                        for (TransactionInfo t : allTransactions) {
                            if (t.date.equals(info.date) && t.amount.equals(info.amount)) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (!isDuplicate) {
                            TransactionEntity entity = new TransactionEntity(info);
                            long newId = db.transactionDao().insert(entity);
                            info.id = (int) newId;
                            newTransactions.add(info);
                        }
                    }
                    newLastSmsId = cursor.getString(0);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка проверки SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        if (!newTransactions.isEmpty()) {
            allTransactions.addAll(newTransactions);
            lastSmsId = newLastSmsId;
            smsHelper.saveLastSmsId(lastSmsId);
            filterTransactionsByCurrency();
        }
    }

    // Новый метод для обработки одного SMS
    private void processSingleSms(Cursor cursor) {
        String body = cursor.getString(2);
        TransactionInfo info = SmsParser.parse(body);

        info.balanceType = "Карта";
        if (info.currency == null || info.currency.isEmpty()) {
            info.currency = currentCurrency;
        }

        boolean isDuplicate = false;
        for (TransactionInfo t : allTransactions) {
            if (t.date.equals(info.date) && t.amount.equals(info.amount)) {
                isDuplicate = true;
                break;
            }
        }

        if (!isDuplicate) {
            TransactionEntity entity = new TransactionEntity(info);
            long newId = db.transactionDao().insert(entity);
            info.id = (int) newId;
            allTransactions.add(info);
            filterTransactionsByCurrency();
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
}