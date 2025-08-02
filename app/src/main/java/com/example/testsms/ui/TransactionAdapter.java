package com.example.testsms.ui;

import android.content.Context;
import android.graphics.Color; // Импорт для работы с цветами
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.testsms.R;
import com.example.testsms.model.TransactionInfo;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends ArrayAdapter<TransactionInfo> {

    private int resourceLayout;
    private Context mContext;

    private static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    public TransactionAdapter(Context context, List<TransactionInfo> items) {
        super(context, R.layout.list_item_transaction, items);
        this.resourceLayout = R.layout.list_item_transaction;
        this.mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(mContext);
            view = vi.inflate(resourceLayout, null);
        }

        TransactionInfo transaction = getItem(position);

        if (transaction != null) {
            TextView tvDateHeader = view.findViewById(R.id.tvTransactionDateHeader);
            TextView tvAmount = view.findViewById(R.id.tvTransactionAmount);
            TextView tvCategory = view.findViewById(R.id.tvTransactionCategory); // Новый ID

            // --- Логика отображения даты (без изменений) ---
            String currentDay = "";
            if (transaction.getDateObject() != null) {
                currentDay = DAY_FORMAT.format(transaction.getDateObject());
            }

            String previousDay = "";
            if (position > 0) {
                TransactionInfo previousTransaction = getItem(position - 1);
                if (previousTransaction != null && previousTransaction.getDateObject() != null) {
                    previousDay = DAY_FORMAT.format(previousTransaction.getDateObject());
                }
            }

            if (!currentDay.equals(previousDay) || position == 0) {
                tvDateHeader.setText(currentDay);
                tvDateHeader.setVisibility(View.VISIBLE);
            } else {
                tvDateHeader.setVisibility(View.GONE);
            }
            // --- Конец логики отображения даты ---

            // Устанавливаем текст для типа и категории
            tvCategory.setText(transaction.category);

            // Форматируем сумму и устанавливаем цвет в зависимости от типа транзакции
            String amountText;
            int amountColor;
            try {
                double amount = Double.parseDouble(transaction.amount);
                if (transaction.type.equals("Пополнение") || transaction.type.equals("Начисление")) {
                    amountText = String.format(Locale.US, "+%.2f", amount); // Используем Locale.US для точки в дроби
                    amountColor = Color.parseColor("#4CAF50"); // Зеленый для прихода
                } else {
                    amountText = String.format(Locale.US, "-%.2f", amount);
                    amountColor = Color.parseColor("#F44336"); // Красный для расхода
                }
            } catch (NumberFormatException e) {
                amountText = transaction.amount; // Если не число, выводим как есть
                amountColor = Color.WHITE; // Цвет по умолчанию
            }
            tvAmount.setText(amountText);
            tvAmount.setTextColor(amountColor); // Устанавливаем цвет суммы
        }
        return view;
    }
}