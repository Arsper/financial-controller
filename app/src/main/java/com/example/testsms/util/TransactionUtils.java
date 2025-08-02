package com.example.testsms.util;

import com.example.testsms.model.TransactionInfo;

import java.util.Collections;
import java.util.List;

public class TransactionUtils {
    public static void sortTransactions(List<TransactionInfo> list) {
        Collections.sort(list, (t1, t2) -> {
            if (t1.getDateObject() == null && t2.getDateObject() == null) return 0;
            if (t1.getDateObject() == null) return 1;
            if (t2.getDateObject() == null) return -1;
            return t2.getDateObject().compareTo(t1.getDateObject());
        });
    }
}
