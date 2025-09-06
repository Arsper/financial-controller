package com.example.testsms.sms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class SmsHelper {
    private final Context context;
    private final ContentResolver resolver;
    private final String[] allowedSenders = {"ASB.BY"};

    public SmsHelper(Context context) {
        this.context = context;
        this.resolver = context.getContentResolver();
    }

    public boolean isAllowedSender(String address) {
        if (address == null) return false;
        for (String s : allowedSenders) {
            if (address.contains(s)) return true;
        }
        return false;
    }

    public Cursor getLatestSms() {
        return resolver.query(
                Uri.parse("content://sms/inbox"),
                new String[]{"_id", "address", "body", "date"},
                null, null,
                "_id DESC LIMIT 1"
        );
    }

    public Cursor getNewMessages(String lastSmsId) {
        return resolver.query(
                Uri.parse("content://sms/inbox"),
                new String[]{"_id", "address", "body", "date"},
                "_id > ?", new String[]{lastSmsId},
                "_id ASC"
        );
    }

    public void saveLastSmsId(String id) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_sms_id", id)
                .apply();
    }

    public String loadLastSmsId() {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("last_sms_id", "");
    }
}
