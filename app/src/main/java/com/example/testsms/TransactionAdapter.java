package com.example.testsms;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class TransactionAdapter extends ArrayAdapter<TransactionInfo> {

    public TransactionAdapter(Context ctx, List<TransactionInfo> items) {
        super(ctx, android.R.layout.simple_list_item_1, items);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        TextView tv = (TextView) super.getView(pos, convertView, parent);
        TransactionInfo info = getItem(pos);

        tv.setText(String.format("%s\n[%s] %s: %s BYN",
                info.date, info.category, info.type, info.amount));
        return tv;
    }
}
