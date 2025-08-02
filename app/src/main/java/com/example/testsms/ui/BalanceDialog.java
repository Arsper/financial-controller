package com.example.testsms.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.testsms.R;

public class BalanceDialog {

    public interface OnCurrencyChangedListener {
        void onCurrencyChanged(String newCurrency);
    }

    public interface BalanceProvider {
        double[] getBalanceForCurrency(String currency); // [total, card, cash]
    }

    public static void show(Activity activity,
                            double total,
                            double card,
                            double cash,
                            String currentCurrency,
                            OnCurrencyChangedListener listener,
                            BalanceProvider balanceProvider) {

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = activity.getLayoutInflater().inflate(R.layout.balance_menu, null);
        dialog.setContentView(view);

        // UI элементы
        LinearLayout backButton = view.findViewById(R.id.backButton);
        TextView tvBalanceTotal = view.findViewById(R.id.tvBalanceTotalB);
        TextView tvBalanceCart = view.findViewById(R.id.tvBalanceCartB);
        TextView tvBalanceCash = view.findViewById(R.id.tvBalanceCashB);
        TextView textCurrencyBalance = view.findViewById(R.id.currency);
        ImageView imgChengBalance = view.findViewById(R.id.mailIcon);

        // Установка значений
        tvBalanceTotal.setText(String.format("%.2f", total));
        tvBalanceCart.setText(String.format("%.2f", card));
        tvBalanceCash.setText(String.format("%.2f", cash));
        textCurrencyBalance.setText(currentCurrency);

        if (backButton != null) {
            backButton.setOnClickListener(v -> dialog.dismiss());
        }

        // Загрузка валют
        String[] currencies = activity.getResources().getStringArray(R.array.currency_array);

        imgChengBalance.setOnClickListener(v -> {
            new AlertDialog.Builder(activity)
                    .setTitle("Выберите валюту")
                    .setItems(currencies, (dialogInterface, which) -> {
                        String selectedCurrency = currencies[which];
                        textCurrencyBalance.setText(selectedCurrency);

                        if (listener != null) {
                            listener.onCurrencyChanged(selectedCurrency);
                        }

                        // Обновляем баланс
                        if (balanceProvider != null) {
                            double[] newBalance = balanceProvider.getBalanceForCurrency(selectedCurrency);
                            tvBalanceTotal.setText(String.format("%.2f", newBalance[0]));
                            tvBalanceCart.setText(String.format("%.2f", newBalance[1]));
                            tvBalanceCash.setText(String.format("%.2f", newBalance[2]));
                        }
                    })
                    .show();
        });

        dialog.show();
    }
}
