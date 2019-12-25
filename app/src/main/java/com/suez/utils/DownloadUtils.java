package com.suez.utils;

import android.util.Log;

import com.odoo.datas.OConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by joseph on 18-5-4.
 */

public class DownloadUtils {
    private static final String TAG = DownloadUtils.class.getSimpleName();
    private static DownloadUtils downloadUtil;
    final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }};
    private SSLContext sslContext;

    public static DownloadUtils get() {
        if (downloadUtil == null) {
            downloadUtil = new DownloadUtils();
        }
        return downloadUtil;
    }

    public String downloadDB(String url, String path, OnDownloadListener listener) {
        String res = null;
        try {
            // Ignore SSL Warning
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            URL address = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) address.openConnection();
            connection.setConnectTimeout(OConstants.RPC_REQUEST_TIME_OUT);

            int response = connection.getResponseCode();
            if (response == 404){
                listener.onDownloadFailed("404");
                return res;
            }

            res = String.valueOf(connection.getHeaderFieldDate("Last-Modified", 0));

            createDownload(connection, path, listener);
        } catch (Exception e){
            e.printStackTrace();
        }
        return res;
    }

    public String downloadDB(String url, String version, String path, OnDownloadListener listener) {
        String res = null;
        try {
            // Ignore SSL Warning
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            URL address = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) address.openConnection();
            connection.setConnectTimeout(OConstants.RPC_REQUEST_TIME_OUT);

            int response = connection.getResponseCode();
            if (response == 404){
                listener.onDownloadFailed("404");
                return res;
            }

            res = String.valueOf(connection.getHeaderFieldDate("Last-Modified", 0));
            if (version != null && Long.parseLong(version) >= Long.parseLong(res)) {
                listener.onDownloadSuccess(0L);
                return res;
            }
            createDownload(connection, path, listener);
        } catch (Exception e){
            e.printStackTrace();
            LogUtils.e(TAG, e.getMessage());
        }
        return res;
    }

    public void createDownload(HttpURLConnection connection, String dbPath, OnDownloadListener listener) throws Exception{
            File file = new File(dbPath);
            if (!file.exists()) {
                LogUtils.w(TAG, dbPath);
                file.createNewFile();
            }
            byte[] bytes = new byte[4096];
            int len;
            InputStream in = connection.getInputStream();
            FileOutputStream fos = new FileOutputStream(file);
            long total = connection.getContentLength();
            int sum = 0;
            if (total == 0) {
                listener.onDownloadFailed("0");
                return;
            }
        try{
            while ((len = in.read(bytes)) > 0) {
                fos.write(bytes, 0, len);
                sum += len;
                int progress = (int) (sum * 1.0f / total * 100);
                listener.onDownloading(progress, total);
            }
            fos.flush();
            if (file.length() < total) {
                listener.onDownloadFailed("Failed");
            }
            listener.onDownloadSuccess(total);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, e.getMessage());
            listener.onDownloadFailed(e.toString());
        } finally {
            if (in != null) {
                in.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public interface OnDownloadListener {
        void onDownloadSuccess(Long size);
        void onDownloading(int progress, Long size);
        void onDownloadFailed(String error);
    }
}
