package com.suez.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by joseph on 18-5-2.
 */

public class ToastUtil {
    private static Toast toast;

    public static void toastShow(String text, Context context) {
        if (toast == null){
            toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        } else {
            toast.setText(text);
        }
    }

    public static void toastShow(int resId, Context context){
        if (toast == null){
            toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        } else {
            toast.setText(resId);
        }
    }
}
