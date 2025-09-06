package com.example.testsms.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.example.testsms.R;
import com.example.testsms.db.AppDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConverDialog {

    public static void show(Activity activityNew, Context ctx) {

        Dialog dialog = new Dialog(activityNew, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = activityNew.getLayoutInflater().inflate(R.layout.convector_menu, null);
        dialog.setContentView(view);

        LinearLayout backButton = view.findViewById(R.id.backButton);

        if (backButton != null) {
            backButton.setOnClickListener(v -> dialog.dismiss());
        }

        Spinner spinnerCurrence1 = view.findViewById(R.id.spinnerBalanceType1);
        ArrayAdapter<CharSequence> typeCurrence1 = ArrayAdapter.createFromResource(
                ctx, R.array.currency_array, android.R.layout.simple_spinner_item
        );
        typeCurrence1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrence1.setAdapter(typeCurrence1);

        Spinner spinnerCurrence2 = view.findViewById(R.id.spinnerBalanceType2);
        ArrayAdapter<CharSequence> typeCurrence2 = ArrayAdapter.createFromResource(
                ctx, R.array.currency_array, android.R.layout.simple_spinner_item
        );
        typeCurrence2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrence2.setAdapter(typeCurrence2);

        Button btnGetStat = view.findViewById(R.id.buttonGetStatic);
        btnGetStat.setOnClickListener(v -> {
            // Получаем выбранные валюты
            String fromCurrency = spinnerCurrence1.getSelectedItem().toString();
            String toCurrency = spinnerCurrence2.getSelectedItem().toString();

            getRespons(fromCurrency, toCurrency, 5000);
        });

        dialog.show();
    }

    private static void getRespons(String fromCurrency, String toCurrency, double amount){
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // Формируем URL
                String urlString = "https://cash.rbc.ru/cash/json/converter_currency_rate/?" +
                        "currency_from=" + fromCurrency +
                        "&currency_to=" + toCurrency +
                        "&source=cbrf&sum=" + amount +
                        "&date=";

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    String responseBody = response.toString();
                    Log.d("API_RESPONSE", "Ответ: " + responseBody);
                    parseJsonResponse(responseBody);

                } else {
                    Log.e("API_ERROR", "HTTP ошибка: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("API_EXCEPTION", "Ошибка: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private static void parseJsonResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject data = jsonObject.getJSONObject("data");

            double result = data.getDouble("result");
            double rate = data.getDouble("rate");
            String date = data.getString("date");

            Log.d("PARSED_DATA", "Результат: " + result + ", Курс: " + rate + ", Дата: " + date);

        } catch (JSONException e) {
            Log.e("JSON_ERROR", "Ошибка парсинга JSON: " + e.getMessage());
        }
    }
}
