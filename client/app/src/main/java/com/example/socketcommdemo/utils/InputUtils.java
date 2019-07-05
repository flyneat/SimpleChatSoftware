package com.example.socketcommdemo.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.example.socketcommdemo.App;

public class InputUtils {
    private static InputMethodManager sImm;


    public static void hideInputMethod(View target) {
        if (sImm == null) {
            sImm = (InputMethodManager) App.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        if (sImm.isActive(target)) {
            sImm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
    }
}
