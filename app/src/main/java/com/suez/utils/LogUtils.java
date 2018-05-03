package com.suez.utils;

import android.util.Log;

import com.odoo.core.utils.ODateUtils;
import com.suez.SuezConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Created by joseph on 18-5-3.
 */

public class LogUtils {
    private static final String filePath = SuezConstants.LOG_FILE_PATH;
    private static final boolean isWrite = true;

    public static void i (String tag, String message, Throwable thr) {
        Log.i(tag, message, thr);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "INFO");
    }

    public static void i (String tag, String message){
        Log.i(tag, message);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "INFO");
    }

    public static void d (String tag, String message, Throwable thr) {
        Log.d(tag, message, thr);
    }

    public static void d (String tag, String message){
        Log.d(tag, message);
    }

    public static void v (String tag, String message, Throwable thr) {
        Log.v(tag, message, thr);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "VERBOSE");
    }

    public static void v (String tag, String message){
        Log.v(tag, message);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "VERBOSE");
    }

    public static void w (String tag, String message, Throwable thr) {
        Log.w(tag, message, thr);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "WARNING");
    }

    public static void w (String tag, String message){
        Log.w(tag, message);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "WARNING");
    }

    public static void e (String tag, String message, Throwable thr) {
        Log.e(tag, message, thr);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "ERROR");
    }

    public static void e (String tag, String message){
        Log.e(tag, message);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "ERROR");
    }

    public static void wtf (String tag, String message, Throwable thr) {
        Log.wtf(tag, message, thr);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "WTF");
    }

    public static void wtf (String tag, String message){
        Log.wtf(tag, message);
        writeToFile(tag, message, ODateUtils.getUTCDate(), "WTF");
    }

    public static void writeToFile(String tag, String message, String date, String type){
        if (!isWrite){
            return;
        }
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append(date).append(" ");
        logBuilder.append(type).append("/").append(tag).append(":").append(" ");
        logBuilder.append(message);
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(logBuilder.toString());
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
