package com.suez.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.odoo.App;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OSQLite;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OCursorUtils;
import com.odoo.core.utils.OPreferenceManager;
import com.odoo.core.utils.OResource;
import com.suez.SuezConstants;
import com.suez.addons.models.DeliveryRoute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by joseph on 18-5-8.
 */

public class OfflineDBUtil {
    public static final String TAG = OfflineDBUtil.class.getSimpleName();
    protected App app;
    protected Context mContext;
    protected OUser mUser;
    private String offlineUrl;
    private String incrUrl;
    private String offlinePath;
    private String incrPath;
    private OPreferenceManager preferenceManager;
    private BaseAbstractListener listener;
    private String offlineVersion;
    private ProgressDialog progressDialog;

    public OfflineDBUtil(Context context, String offlineUrl, String incrUrl) {
        mContext = context;
        app = (App) context.getApplicationContext();
        mUser = OUser.current(context);
        preferenceManager = new OPreferenceManager(context);
        this.offlineUrl = offlineUrl;
        this.incrUrl = incrUrl;
        offlinePath = new OModel(mContext, "res.partner", mUser).getDatabaseLocalPath();
        incrPath = new OSQLite(mContext, mUser, mUser.getIncrDBName()).databaseLocalPath();

        offlineVersion = preferenceManager.getString(SuezConstants.OFFLINE_DB_VERSION_KEY, "0");
        progressDialog = new ProgressDialog(mContext);
    }

    public void download() {
        if (!checkUrl(offlineUrl)) {
            ToastUtil.toastShow(String.format(OResource.string(mContext, R.string.toast_input_right_url),
                    OResource.string(mContext, R.string.label_offline)), mContext);
            if (listener != null) {
                listener.OnFail("offline");
            }
            return;
        }
        if (!checkUrl(incrUrl)) {
            ToastUtil.toastShow(String.format(OResource.string(mContext, R.string.toast_input_right_url),
                    OResource.string(mContext, R.string.label_incremental_db)), mContext);
            if (listener != null) {
                listener.OnFail("incr");
            }
            return;
        }
        DownloadDBTask task = new DownloadDBTask();
        task.execute();
    }

    public void checkMD5(boolean check) {
        MD5Task task = new MD5Task();
        task.execute(check);
    }

    public void mergeDB() {
        MergeTask task = new MergeTask();
        task.execute(new OSQLite(mContext, mUser, mUser.getIncrDBName()));
    }

    private class DownloadDBTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setTitle(R.string.title_progress_downloading);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage(OResource.string(mContext, R.string.message_progress_downloading));
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            DownloadUtils.get().downloadDB(incrUrl, incrPath, new DownloadUtils.OnDownloadListener() {
                @Override
                public void onDownloadSuccess(Long size) {
                    Log.v(TAG, "Download incremental DB success");
                }

                @Override
                public void onDownloading(int progress, Long size) {
                    Log.v(TAG, "Downloading incremental progress = " + progress + " size = " + size);
                    progressDialog.setProgress(progress);
                }

                @Override
                public void onDownloadFailed(String error) {
                    Log.v(TAG, "Download failed: " + error);
                    progressDialog.dismiss();
                    toastError(error);
                }
            });
            String offline_version = DownloadUtils.get().downloadDB(offlineUrl, offlineVersion, offlinePath, new DownloadUtils.OnDownloadListener() {
                @Override
                public void onDownloadSuccess(Long size) {
                    Log.v(TAG, "Download Offline Success");
                    if (listener != null) {
                        listener.OnSuccessful(size > 0);
                    }
                }

                @Override
                public void onDownloading(int progress, Long size) {
                    Log.v(TAG, "Downloading offline progress = " + progress + " size = " + size);
                    progressDialog.setProgress(progress);
                }

                @Override
                public void onDownloadFailed(String error) {
                    Log.v(TAG, "Download failed: " + error);
                    progressDialog.dismiss();
                    preferenceManager.putString(SuezConstants.OFFLINE_DB_VERSION_KEY, "0");
                    toastError(error);
                }
            });
            preferenceManager.putString(SuezConstants.OFFLINE_DB_VERSION_KEY, offline_version);
            return null;
        }

            private void toastError(String error) {
                if (error.equals("404")) {
                    ToastUtil.toastShow(R.string.toast_db_not_found, mContext);
                } else if (error.equals("0")) {
                    ToastUtil.toastShow(R.string.toast_db_zero, mContext);
                } else {
                    ToastUtil.toastShow(error, mContext);
                }
                if (listener != null) {
                    listener.OnFail(error);
                }
            }
    }

    public OfflineDBUtil setListener(BaseAbstractListener listener) {
        this.listener = listener;
        return this;
    }

    private boolean checkUrl(String url) {
        if (url == null || !new File(url).getName().endsWith(".db")) {
            return false;
        }
        return true;
    }

    private String mergeSqlite (OSQLite sqLite) {
        SQLiteDatabase incrDB = sqLite.getReadableDatabase();
        // Get tables
        List<Object> tableNames = RecordUtils.getFieldList(queryList(incrDB, "select name from sqlite_master where type = ? and name not in (?, ?, ?)", new String[] {
                "table", "sqlite_master", "android_metadata", "sqlite_sequence"
        }), "name");
        SQLiteDatabase db = new OModel(mContext, "res.partner", mUser).getWritableDatabase();
        try {
            // Attach Incr
            db.execSQL(String.format("ATTACH DATABASE '%s' AS incr", incrPath));
            db.beginTransaction();
            for (Object table: tableNames) {
            Log.v(TAG,"Merge start for table: " + table);
            db.execSQL("INSERT OR REPLACE INTO " + table + " SELECT * FROM incr." + table);
            Log.v(TAG, "Merge success for table: " + table);
        }
            db.setTransactionSuccessful();
            db.endTransaction();
            // Detach
            db.execSQL("DETACH DATABASE incr");
            String lastSyncDate = updateSyncDate(incrDB, db, tableNames);
            incrDB.close();
            return lastSyncDate;
//        for (String tableName: tableNames) {
//            try {
//                Class<?> ormClass = Class.forName("com.suez.addons.models." + toClassName(tableName));
//                Constructor constructor = ormClass.getConstructors()[0];
//                OModel mModel = (OModel) constructor.newInstance(new Object[]{mContext, null});
//
//                preferenceManager = new OPreferenceManager(mContext);
//                List<ODataRow> dataRows = preferenceManager.getString(SuezConstants.LAST_SYNC_DATE_KEY, null) == null ?
//                        queryList(incrDB, "select * from " + tableName, null) :
//                        queryList(incrDB, "select * from " + tableName +  "where _write_date >= ?", new String[] {preferenceManager.getString(SuezConstants.LAST_SYNC_DATE_KEY, null)});
//                if (dataRows.size() > 0) {
//                    for (ODataRow dataRow : dataRows) {
//                        OValues values = dataRow.toValues();
//                        int odoo_id = values.getInt("_id");
//                        values.put("id", odoo_id);
//                        values.removeKey("_id");
//                        checkValues(mModel, values);
//                        int rowId = mergeRow(mModel, values, odoo_id);
//                        mergeMany2one(mModel, values, rowId);
//                    }
//                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.e(TAG, "Merge failed");
                LogUtils.e(TAG, e.getMessage());
                if (listener != null) {
                    listener.OnFail("db");
                }
            }
            return null;
    }

//    private int mergeRow(OModel model, OValues values, int id){
//        List<ODataRow> record = model.select(null, "id=?", new String[]{String.valueOf(id)});
//        int _id;
//        switch (record.size()) {
//            case 0:
//                Log.d(TAG, "Inserting into " + model.getModelName() + " Values " + values.toString());
//                return model.insert(values);
//            case 1:
//                _id = record.get(0).getInt("_id");
//                Log.d(TAG, "Updating " + model.getModelName() + " Values " + values.toString());
//                model.update(_id, values);
//                return _id;
//            default:
//                LogUtils.w(TAG, "More than 1 record found with id " + id);
//                _id = record.get(0).getInt("_id");
//                Log.d(TAG, "Updating " + model.getModelName() + " Values " + values.toString());
//                model.update(_id, values);
//                return _id;
//        }
//    }
//
//    private void mergeMany2one(OModel model, OValues values, int rowId) {
//        OValues many2oneValues = new OValues();
//        for (OColumn column: model.getRelationColumns()) {
//            if (values.get(column.getName()) == null) {continue;}
//            String relatedId = values.getString(column.getName());
//            if (!relatedId.equals("false")) {
//                OModel relatedModel = model.createInstance(column.getType());
//                List<ODataRow> records = relatedModel.select(null, "id=?", new String[]{relatedId});
//                switch (records.size()) {
//                    case 0:
//                        LogUtils.e(TAG, String.format("Record {id %s, name %s} related record to model {%s} not found.", rowId, values.get("name"), relatedModel.getModelName()));
//                        break;
//                    case 1:
//                        many2oneValues.put(column.getName(), records.get(0).getInt("_id"));
//                        break;
//                    default:
//                        LogUtils.w(TAG, "More than 1 record found with id " + relatedId + " in " + relatedModel.getModelName());
//                        many2oneValues.put(column.getName(), records.get(0).getInt("_id"));
//                        break;
//                }
//            }
//        }
//        model.update(rowId, many2oneValues);
//    }

    private String updateSyncDate(SQLiteDatabase incrDB, SQLiteDatabase offlineDB, List<Object> tables) {
        String date = getMaxWriteDate(incrDB, tables);
        if (date == null || date.equals("") || date.equals("false") || date.equals(false)) {
            date = getMaxWriteDate(offlineDB, tables);
        }
        return date;
    }

    private String getMaxWriteDate(SQLiteDatabase db, List<Object> tables) {
        String date = "";
        for (Object table : tables) {
            String newDate = queryList(db, "select max(_write_date) from " + table, null).get(0).getString("max(_write_date)");
            if (!newDate.equals("false") && date.compareTo(newDate) <= 0) {
                date = newDate;
            }
        }
        return date;
    }

    private String toClassName(String tableName){
        String[] strs = tableName.split("_");
        StringBuilder nameBuilder = new StringBuilder();
        for (String str: strs){
            nameBuilder.append(Character.toUpperCase(str.charAt(0))).append(str.substring(1));
        }
        return nameBuilder.toString();
    }


    private List<ODataRow> queryList(SQLiteDatabase db, String sql, String[] args) {
        Cursor cr = db.rawQuery(sql, args);
        List<ODataRow> res = new ArrayList<>();
        while (cr.moveToNext()) {
            res.add(OCursorUtils.toDatarow(cr));
        }
        cr.close();
        return res;
    }

    private OValues checkValues(OModel mModel, OValues values){
        for (String key: values.keys()){
            if (values.getString(key)==null || values.getString(key).equals(String.valueOf(mModel.getColumn(key).getDefaultValue())) || values.getString(key).equals("false")){
                values.removeKey(key);
                Log.v(TAG, key+" removed");
            }
        }
        if (!values.keys().contains("_is_dirty")) {
            values.put("_is_dirty", false);
        }
        return values;
    }

    public class MergeTask extends AsyncTask<OSQLite, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setTitle(R.string.title_merge_db);
                    progressDialog.setMessage(OResource.string(mContext, R.string.message_merge_db));
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            });
        }

        @Override
        protected String  doInBackground(OSQLite... params) {
            OSQLite incrSqlite = params[0];
            return mergeSqlite(incrSqlite);
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
            progressDialog.dismiss();
            if (listener != null) {
                listener.OnSuccessful(res);
            }
        }
    }

    public class MD5Task extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setTitle(R.string.title_check_md5);
                    progressDialog.setMessage(OResource.string(mContext, R.string.message_check_md5));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMax(100);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            });
        }

        @Override
        protected Void doInBackground(Boolean... params) {
            Boolean check = params[0];
            if (!check) {
                if (listener != null) {
                    listener.OnSuccessful("Success");
                }
                return null;
            }
            String checksumUrl = offlineUrl.substring(0, offlineUrl.lastIndexOf('/') + 1) + SuezConstants.MD5_FILE_NAME;
            final String checksumPath = offlinePath.substring(0, offlinePath.lastIndexOf('/') + 1) + SuezConstants.MD5_FILE_NAME;
            DownloadUtils.get().downloadDB(checksumUrl, checksumPath, new DownloadUtils.OnDownloadListener() {
                @Override
                public void onDownloadSuccess(Long size) {
                    Log.v(TAG, "Download MD5 File Success");
                    try {
                        File md5File = new File(checksumPath);
                        BufferedReader br = new BufferedReader(new FileReader(md5File));
                        List<String> md5Digests = new ArrayList<>();
                        String line;
                        while ((line = br.readLine() )!= null) {
                            md5Digests.add(line);
                        }
                        br.close();
                        validateMD5(md5Digests);
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.e(TAG, e.getMessage());
                    }
                }

                @Override
                public void onDownloading(int progress, Long size) {
                    Log.v(TAG, "Downloading MD5 File progress = " + progress + " size = " + size);
                    progressDialog.setProgress(progress);
                }

                @Override
                public void onDownloadFailed(String error) {
                    LogUtils.e(TAG, error);
                    if (listener != null) {
                        listener.OnFail(error);
                        progressDialog.dismiss();
                    }
                }
            });
            return null;
        }

        private void validateMD5(List<String> digests) {
            String offlineDigest = MD5Utils.getMD5(new File(offlinePath));
//            String incrDigest = MD5Utils.getMD5(new File(incrPath));
            if (!offlineDigest.equals(digests.get(0))) {
                Log.w(TAG, "Verify MD5 For Offline DB Failed");
                if (listener != null) {
                    listener.OnFail("Failed");
                    progressDialog.dismiss();
                }
            } else {
                Log.d(TAG, "Verify MD5 Success");
                if (listener != null) {
                    listener.OnSuccessful("Success");
                }
            }
//            if (!incrDigest.equals(digests.get(1))) {
//                Log.w(TAG, "Verity MD5 For Incremental DB Failed");
//                if (listener != null) {
//                    listener.OnFail(SuezConstants.INCR_MD5_CHECK_FAIL_KEY);
//                }
//            } else {
//                if (listener != null) {
//                    listener.OnSuccessful(true);
//                }
//            }
        }
    }
}
