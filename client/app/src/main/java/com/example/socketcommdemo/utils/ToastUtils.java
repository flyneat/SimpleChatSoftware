package com.example.socketcommdemo.utils;

import android.widget.Toast;

import com.example.socketcommdemo.App;

public class ToastUtils {
    private static Toast sToast;

    static void show(String msg, int durationType) {
        if (sToast == null) {
            sToast = Toast.makeText(App.getInstance(), msg, durationType);
        } else {
            sToast.setText(msg);
        }
        sToast.show();
    }

    public static void show(String msg) {
        show(msg, Toast.LENGTH_SHORT);
    }

    public static void showLong(String msg) {
        show(msg, Toast.LENGTH_LONG);
    }

}
