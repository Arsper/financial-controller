package com.example.testsms;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionDialog {

    public interface Callback {
        void onSaved(TransactionInfo info, boolean isNew);
    }

    public static void show(Context ctx,
                            TransactionInfo existing,
                            double currentBalance,
                            Callback cb) {

        View dialogView = LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_edit, null);

        Spinner spinnerType     = dialogView.findViewById(R.id.spinnerType);
        EditText etAmount       = dialogView.findViewById(R.id.etAmount);
        TextView tvCategoryLabel= dialogView.findViewById(R.id.tvCategoryLabel);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        Spinner spinnerBalanceType = dialogView.findViewById(R.id.spinnerBalanceType);

        // Адаптеры
        ArrayAdapter<CharSequence> balanceAdapter = ArrayAdapter.createFromResource(
                ctx, R.array.balance_types, android.R.layout.simple_spinner_item);
        balanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBalanceType.setAdapter(balanceAdapter);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                ctx, R.array.operation_types, android.R.layout.simple_spinner_item
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> catAdapterDefault = ArrayAdapter.createFromResource(
                ctx, R.array.categories_default, android.R.layout.simple_spinner_item);
        catAdapterDefault.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> catAdapterIncome = ArrayAdapter.createFromResource(
                ctx, R.array.categories_income, android.R.layout.simple_spinner_item);
        catAdapterIncome.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Показывать нужный список категорий в зависимости от типа
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                String sel = (String) parent.getItemAtPosition(position);
                boolean isIncome = sel.equals("Пополнение") || sel.equals("Начисление");

                // Просто меняем адаптер без скрытия
                spinnerCategory.setAdapter(isIncome
                        ? catAdapterIncome
                        : catAdapterDefault);

                tvCategoryLabel.setVisibility(View.VISIBLE);
                spinnerCategory.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        final boolean isNew = (existing == null);
        final TransactionInfo info = isNew
                ? new TransactionInfo()
                : new TransactionInfo(
                existing.type, existing.amount,
                existing.balance, existing.date,
                existing.category
        );

        // При редактировании сразу установим тип и категорию
        if (!isNew) {
            int typePos = typeAdapter.getPosition(info.type);
            spinnerType.setSelection(typePos);
            etAmount.setText(info.amount);

            boolean isIncome = info.type.equals("Пополнение") || info.type.equals("Начисление");
            spinnerCategory.setAdapter(isIncome ? catAdapterIncome : catAdapterDefault);

            int catPos = (isIncome ? catAdapterIncome : catAdapterDefault)
                    .getPosition(info.category);
            spinnerCategory.setSelection(catPos);
        }

        String title = isNew ? "Новая транзакция" : "Редактировать транзакцию";

        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(isNew ? "Добавить" : "Сохранить", (dlg, which) -> {
                    String type     = spinnerType.getSelectedItem().toString();
                    String amtText  = etAmount.getText().toString().trim();
                    if (amtText.isEmpty()) amtText = "0.00";

                    String category = spinnerCategory.getSelectedItem().toString();

                    double amount = Double.parseDouble(amtText);
                    boolean expense = type.equals("Платеж") || type.equals("Списание");

                    double newBal;
                    if (isNew) {
                        newBal = currentBalance + (expense ? -amount : amount);
                        info.date = new SimpleDateFormat(
                                "dd.MM.yyyy HH:mm:ss", Locale.getDefault()
                        ).format(new Date());
                    } else {
                        double oldAmt = Double.parseDouble(existing.amount);
                        boolean wasExp = existing.type.equals("Платеж") || existing.type.equals("Списание");
                        newBal = currentBalance
                                + (wasExp ? oldAmt : -oldAmt)
                                + (expense ? -amount : amount);
                    }

                    info.type     = type;
                    info.amount   = String.format(Locale.US, "%.2f", amount);
                    info.balance  = String.format(Locale.US, "%.2f", newBal);
                    info.category = category;
                    info.balanceType = spinnerBalanceType.getSelectedItem().toString();

                    cb.onSaved(info, isNew);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
