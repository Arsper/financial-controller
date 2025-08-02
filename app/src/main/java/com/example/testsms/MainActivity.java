package com.example.testsms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.room.Room;

import com.example.testsms.db.AppDatabase;
import com.example.testsms.db.TransactionEntity;
import com.example.testsms.model.TransactionInfo;
import com.example.testsms.sms.SmsHelper;
import com.example.testsms.sms.SmsParser;
import com.example.testsms.ui.BalanceDialog;
import com.example.testsms.ui.TransactionAdapter;
import com.example.testsms.ui.TransactionDialog;
import com.example.testsms.util.TransactionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private static final long UPDATE_INTERVAL = 300_000;

    private AppDatabase db;
    private SmsHelper smsHelper;
    private Handler handler = new Handler();

    private ListView lvTransactions;
    private TextView tvBalanceTotal;
    private ImageButton btnAdd;
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

    // Текущая выбранная валюта
    private String currentCurrency = "BYN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currencies = getResources().getStringArray(R.array.currency_array);


        textCurrency = findViewById(R.id.textCurrency);
        lvTransactions = findViewById(R.id.lvTransactions);
        tvBalanceTotal = findViewById(R.id.tvBalanceTotal);
        btnAdd = findViewById(R.id.buttonAdd);
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
                        filterTransactionsByCurrency(); // если нужно
                    },
                    currency -> getConvertedBalance(currency) // реализация ниже
            );
        });
    }
    private double[] getConvertedBalance(String currency) {
        return new double[]{balanceTotal, balanceCart, balanceCash};
    }


    private void setupCurrencySelector() {
        // Устанавливаем начальную валюту
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

        allTransactions.clear();
        for (TransactionEntity e : saved) {
            allTransactions.add(new TransactionInfo(e));
        }

        filterTransactionsByCurrency();

        lastSmsId = smsHelper.loadLastSmsId();
    }

    // Фильтруем транзакции по выбранной валюте, пересчитываем баланс и обновляем UI
    private void filterTransactionsByCurrency() {
        filteredTransactions.clear();

        // Фильтрация по валюте
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

        // Сортируем по дате (от старой к новой)
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

        // Отображаем транзакции с самой новой сверху
        TransactionUtils.sortTransactions(filteredTransactions);
        adapter.notifyDataSetChanged();
    }

    private void onTransactionSaved(TransactionInfo info, boolean isNew) {
        if (!isNew) {
            // Обновляем существующую транзакцию
            for (TransactionInfo t : allTransactions) {
                if (t.date.equals(info.date)) {
                    info.id = t.id;
                    break;
                }
            }
        }

        TransactionEntity entity = new TransactionEntity(info);

        if (isNew) {
            long newId = db.transactionDao().insert(entity);
            info.id = (int) newId;
            allTransactions.add(info);
        } else {
            db.transactionDao().update(entity);
            for (int i = 0; i < allTransactions.size(); i++) {
                if (allTransactions.get(i).date.equals(info.date)) {
                    allTransactions.set(i, info);
                    break;
                }
            }
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
        try (Cursor cursor = smsHelper.getLatestSms()) {
            if (cursor != null && cursor.moveToFirst()) {
                String address = cursor.getString(1);
                String smsId = cursor.getString(0);
                if (smsHelper.isAllowedSender(address) && !lastSmsId.equals(smsId)) {
                    lastSmsId = smsId;
                    smsHelper.saveLastSmsId(lastSmsId);
                    processAndStore(cursor);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка чтения SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startPeriodicUpdates() {
        handler.postDelayed(() -> {
            checkForNewMessages();
            handler.postDelayed(this::startPeriodicUpdates, UPDATE_INTERVAL);
        }, UPDATE_INTERVAL);
    }

    private void checkForNewMessages() {
        try (Cursor cursor = smsHelper.getNewMessages(lastSmsId)) {
            if (cursor != null) {
                boolean hasNewMessages = false;
                while (cursor.moveToNext()) {
                    String address = cursor.getString(1);
                    if (smsHelper.isAllowedSender(address)) {
                        processAndStore(cursor);
                        hasNewMessages = true;
                    }
                }

                if (hasNewMessages && cursor.moveToLast()) {
                    lastSmsId = cursor.getString(0);
                    smsHelper.saveLastSmsId(lastSmsId);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка проверки SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndStore(Cursor cursor) {
        String body = cursor.getString(2);
        TransactionInfo info = SmsParser.parse(body);
        info.balanceType = "Карта"; // SMS-транзакции обычно относятся к карте

        // Проверяем на дубликаты по дате
        for (TransactionInfo t : allTransactions) {
            if (t.date.equals(info.date)) return;
        }

        // Если у транзакции нет валюты, ставим текущую валюту по умолчанию
        if (info.currency == null || info.currency.isEmpty()) {
            info.currency = currentCurrency;
        }

        db.transactionDao().insert(new TransactionEntity(info));
        allTransactions.add(info);

        filterTransactionsByCurrency();
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
