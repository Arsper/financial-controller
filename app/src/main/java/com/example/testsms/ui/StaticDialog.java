package com.example.testsms.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import com.github.mikephil.charting.data.Entry;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.testsms.R;
import com.example.testsms.db.AppDatabase;
import com.example.testsms.db.TransactionDao;
import com.example.testsms.db.TransactionEntity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaticDialog {
    private static Activity activity;
    private static Context ctx;
    private static LineChart lineChart;
    private static PieChart pieChart;

    private static final String TAG = "StaticDialog";

    public static void show(Activity activityNew, Context ctx, AppDatabase db) {
        activity = activityNew;
        StaticDialog.ctx = ctx;

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = activity.getLayoutInflater().inflate(R.layout.static_menu, null);
        dialog.setContentView(view);

        LinearLayout backButton = view.findViewById(R.id.backButton);

        if (backButton != null) {
            backButton.setOnClickListener(v -> dialog.dismiss());
        }

        Spinner spinnerCurrence = view.findViewById(R.id.spinnerBalanceType);
        ArrayAdapter<CharSequence> typeCurrence = ArrayAdapter.createFromResource(
                ctx, R.array.currency_array, android.R.layout.simple_spinner_item
        );
        typeCurrence.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrence.setAdapter(typeCurrence);

        Spinner spinnerCategoryType = view.findViewById(R.id.spinnerCategoryType);
        ArrayAdapter<CharSequence> categoryType = ArrayAdapter.createFromResource(
                ctx, R.array.categories_static, android.R.layout.simple_dropdown_item_1line
        );
        categoryType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryType.setAdapter(categoryType);

        Spinner spinnerPeriod = view.findViewById(R.id.spinnerPeriod);
        ArrayAdapter<CharSequence> period = ArrayAdapter.createFromResource(
                ctx, R.array.period, android.R.layout.simple_dropdown_item_1line
        );
        period.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(period);

        lineChart = view.findViewById(R.id.lineChart);
        pieChart = view.findViewById(R.id.pieChart);

        Button btnGetStat = view.findViewById(R.id.buttonGetStatic);
        btnGetStat.setOnClickListener(v -> {
            String selectedCurrency = spinnerCurrence.getSelectedItem().toString();
            String selectedCategory = spinnerCategoryType.getSelectedItem().toString();
            String selectedPeriod = spinnerPeriod.getSelectedItem().toString();

            // Для отладки: получаем все транзакции без фильтров
            debugGetAllTransactions(db);

            // Получаем даты в формате, соответствующем базе данных
            String[] dateRange = getDateRangeForPeriod(selectedPeriod);
            String startDate = dateRange[0];
            String endDate = dateRange[1];

            Log.d(TAG, "Параметры запроса: currency=" + selectedCurrency +
                    ", category=" + selectedCategory +
                    ", startDate=" + startDate +
                    ", endDate=" + endDate);

            getStatic(selectedCurrency, selectedCategory, startDate, endDate, db);
        });

        dialog.show();
    }

    // Метод для отладки: получаем все транзакции
    private static void debugGetAllTransactions(AppDatabase db) {
        new Thread(() -> {
            try {
                TransactionDao transactionDao = db.transactionDao();
                List<TransactionEntity> allTransactions = transactionDao.getAll();

                Log.d(TAG, "Всего транзакций в базе: " + allTransactions.size());
                for (TransactionEntity transaction : allTransactions) {
                    Log.d(TAG, "Транзакция: id=" + transaction.id +
                            ", date=" + transaction.date +
                            ", amount=" + transaction.amount +
                            ", currency=" + transaction.currency +
                            ", category=" + transaction.category);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении всех транзакций: " + e.getMessage());
            }
        }).start();
    }

    private static String[] getDateRangeForPeriod(String period) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        String endDate = dateFormat.format(calendar.getTime());
        String startDate;

        switch (period) {
            case "Неделя":
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                startDate = dateFormat.format(calendar.getTime());
                break;

            case "Месяц":
                calendar.add(Calendar.MONTH, -1);
                startDate = dateFormat.format(calendar.getTime());
                break;

            case "Весь период":
                startDate = "01.01.1970 00:00:00"; // Минимальная дата в формате базы
                break;

            default:
                calendar.add(Calendar.MONTH, -1);
                startDate = dateFormat.format(calendar.getTime());
                break;
        }

        return new String[]{startDate, endDate};
    }

    private static void getStatic(String currency, String category, String startDate, String endDate, AppDatabase db) {
        new Thread(() -> {
            try {
                TransactionDao transactionDao = db.transactionDao();

                // Временный запрос без фильтров для отладки
                List<TransactionEntity> allTransactions = transactionDao.getAll();
                Log.d(TAG, "Все транзакции без фильтров: " + allTransactions.size());

                // Запрос с фильтром по дате
                List<TransactionEntity> dateFiltered = transactionDao.getTransactionsByDateRange(startDate, endDate);
                Log.d(TAG, "Транзакции по дате: " + dateFiltered.size());

                List<TransactionEntity> transactions;
                if(!category.equals("Все")){
                    transactions=transactionDao.getTransactionsStaticWithStringDates(currency, category, startDate, endDate);
                }else {
                    transactions = transactionDao.getTransactionsStaticWithStringDates(currency, "", startDate, endDate);
                }

                Log.d(TAG, "Результат полного запроса: " + transactions.size());

                activity.runOnUiThread(() -> {
                    processTransactions(transactions, lineChart, pieChart);;
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Ошибка при получении транзакций: " + e.getMessage());

                activity.runOnUiThread(() -> {
                    Toast.makeText(ctx, "Ошибка при получении данных", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private static void processTransactions(List<TransactionEntity> transactions, LineChart lineChart, PieChart pieChart) {
        if (transactions != null && !transactions.isEmpty()) {
            double totalAmount = 0;
            Map<String, Double> categoryMap = new HashMap<>();
            List<Entry> lineEntries = new ArrayList<>();

            int index = 0;
            for (TransactionEntity transaction : transactions) {
                try {
                    double amount = Double.parseDouble(transaction.amount.replace(",", "."));
                    totalAmount += amount;

                    // Линейный график (сумма по порядку)
                    lineEntries.add(new Entry(index++, (float) amount));

                    // Для PieChart (группировка по категориям)
                    String cat = transaction.category != null ? transaction.category : "Без категории";
                    categoryMap.put(cat, categoryMap.getOrDefault(cat, 0.0) + amount);

                } catch (NumberFormatException e) {
                    Log.e(TAG, "Ошибка преобразования суммы: " + transaction.amount);
                }
            }

            // --- LineChart ---
            LineDataSet dataSet = new LineDataSet(lineEntries, "Динамика расходов/доходов");
            dataSet.setColor(Color.YELLOW);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setCircleColor(Color.RED);

            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            lineChart.invalidate();

            // --- PieChart ---
            List<PieEntry> pieEntries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                pieEntries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }

            PieDataSet pieDataSet = new PieDataSet(pieEntries, "Категории");
            pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            pieDataSet.setValueTextColor(Color.WHITE);
            pieDataSet.setValueTextSize(12f);

            PieData pieData = new PieData(pieDataSet);
            pieChart.setData(pieData);
            pieChart.setUsePercentValues(true);
            pieChart.getDescription().setEnabled(false);
            pieChart.invalidate();

            Toast.makeText(ctx, "Найдено " + transactions.size() +
                    " транзакций на сумму " + totalAmount, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(ctx, "Транзакции не найдены", Toast.LENGTH_LONG).show();
        }
    }
}