package com.suez.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Created by joseph on 18-6-20.
 */

public class MD5Utils {
    private static final String TAG = MD5Utils.class.getSimpleName();

    @Nullable
    public static String getMD5(File file) {
        FileInputStream fis = null;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);
            byte[] buffer = new byte[2048];
            int len = -1;
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            return bytesToHexString(md.digest());
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, e.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtils.e(TAG, e.getMessage());
                }
            }
        }
        return null;
    }

    @NonNull
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b :bytes) {
            sb.append(String.format("%02x", (int) (b & 0xff)));
        }
        return sb.toString();
    }
}
