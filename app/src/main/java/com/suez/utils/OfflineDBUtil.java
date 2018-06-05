package com.suez.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 18-5-8.
 */

public class OfflineDBUtil {
    public static final String TAG = OfflineDBUtil.class.getSimpleName();
    protected App app;
    protected Context mContext;
    protected OUser mUser;
    private OPreferenceManager preferenceManager;

    public OfflineDBUtil(Context context, OUser user) {
        mContext = context;
        app = (App) context.getApplicationContext();
        mUser = user;
        preferenceManager = new OPreferenceManager(context);
    }

    public class DownloadDBTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog progressDialog = new ProgressDialog(mContext);
        private BaseAbstractListener listener;

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
            String offlineUrl = params[0];
            String incrUrl = params[1];
            String offlinePath = params[2];
            String incrPath = params[3];
            String offlineVersion = params[4];

            if (!checkUrl(offlineUrl)) {
                if (listener != null) {
                    listener.OnFail(SuezConstants.OFFLINE_URL_ERROR_KEY);
                }
                return null;
            }
            if (!checkUrl(incrUrl)){
                if (listener != null) {
                    listener.OnFail(SuezConstants.INCR_URL_ERROR_KEY);
                }
                return null;
            }

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
                    if (listener != null) {
                        if (error.equals("404")) {
                            listener.OnFail(SuezConstants.HTTP_RETURN_404);
                        } else if (error.equals("0")) {
                            listener.OnFail(SuezConstants.DB_SIZE_GET_ZERO);
                        } else {
                            listener.OnFail(error);
                        }
                    }
                }
            });
            String offline_version = DownloadUtils.get().downloadDB(offlineUrl, offlineVersion, offlinePath, new DownloadUtils.OnDownloadListener() {
                @Override
                public void onDownloadSuccess(Long size) {
                    Log.v(TAG, "Download Offline Success");
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
                    if (listener != null) {
                        if (error.equals("404")) {
                            listener.OnFail(SuezConstants.HTTP_RETURN_404);
                        } else if (error.equals("0")) {
                            listener.OnFail(SuezConstants.DB_SIZE_GET_ZERO);
                        } else {
                            listener.OnFail(error);
                        }
                    }
                }
            });
            preferenceManager.putString(SuezConstants.OFFLINE_DB_VERSION_KEY, offline_version);
            return null;
        }

        @Override
            protected void onPostExecute(Void v) {
                super.onPostExecute(v);
                progressDialog.dismiss();
                if (listener != null) {
                    listener.OnSuccessful("Success");
                }
            }


        public void setListener(BaseAbstractListener listener) {
            this.listener = listener;
        }

        private boolean checkUrl(String url) {
            if (url == null || !new File(url).getName().endsWith(".db")){
                return false;
            }
            return true;
        }

    }

    public String mergeSqlite (OSQLite sqLite) {
        SQLiteDatabase incrDB = sqLite.getReadableDatabase();
        String[] tableNames = new String[] {
                "delivery_route",
        };
        SQLiteDatabase db = new OModel(mContext, "res.partner", mUser).getWritableDatabase();
        db.beginTransaction();
        for (String tableName: tableNames) {
            try {
                Class<?> ormClass = Class.forName("com.suez.addons.models." + toClassName(tableName));
                Constructor constructor = ormClass.getConstructors()[0];
                OModel mModel = (OModel) constructor.newInstance(new Object[]{mContext, null});

                preferenceManager = new OPreferenceManager(mContext);
                List<ODataRow> dataRows = preferenceManager.getString(SuezConstants.LAST_SYNC_DATE_KEY, null) == null ?
                        queryList(incrDB, "select * from " + tableName, null) :
                        queryList(incrDB, "select * from " + tableName +  "where _write_date >= ?", new String[] {preferenceManager.getString(SuezConstants.LAST_SYNC_DATE_KEY, null)});
                if (dataRows.size() > 0) {
                    for (ODataRow dataRow : dataRows) {
                        OValues values = dataRow.toValues();
                        int odoo_id = values.getInt("_id");
                        values.put("id", odoo_id);
                        values.removeKey("_id");
                        checkValues(mModel, values);
                        int rowId = mergeRow(mModel, values, odoo_id);
                        mergeMany2one(mModel, values, rowId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.e(TAG, "Merge failed for table: " + tableName);
                LogUtils.e(TAG, e.toString());
            }
        }
        String lastSyncDate = updateSyncDate(incrDB, tableNames);
        db.setTransactionSuccessful();
        db.endTransaction();
        incrDB.close();
        return lastSyncDate;
    }

    private int mergeRow(OModel model, OValues values, int id){
        List<ODataRow> record = model.select(null, "id=?", new String[]{String.valueOf(id)});
        int _id;
        switch (record.size()) {
            case 0:
                Log.d(TAG, "Inserting into " + model.getModelName() + " Values " + values.toString());
                return model.insert(values);
            case 1:
                _id = record.get(0).getInt("_id");
                Log.d(TAG, "Updating " + model.getModelName() + " Values " + values.toString());
                model.update(_id, values);
                return _id;
            default:
                LogUtils.w(TAG, "More than 1 record found with id " + id);
                _id = record.get(0).getInt("_id");
                Log.d(TAG, "Updating " + model.getModelName() + " Values " + values.toString());
                model.update(_id, values);
                return _id;
        }
    }

    private void mergeMany2one(OModel model, OValues values, int rowId) {
        OValues many2oneValues = new OValues();
        for (OColumn column: model.getRelationColumns()) {
            if (values.get(column.getName()) == null) {continue;}
            String relatedId = values.getString(column.getName());
            if (!relatedId.equals("false")) {
                OModel relatedModel = model.createInstance(column.getType());
                List<ODataRow> records = relatedModel.select(null, "id=?", new String[]{relatedId});
                switch (records.size()) {
                    case 0:
                        LogUtils.e(TAG, String.format("Record {id %s, name %s} related record to model {%s} not found.", rowId, values.get("name"), relatedModel.getModelName()));
                        break;
                    case 1:
                        many2oneValues.put(column.getName(), records.get(0).getInt("_id"));
                        break;
                    default:
                        LogUtils.w(TAG, "More than 1 record found with id " + relatedId + " in " + relatedModel.getModelName());
                        many2oneValues.put(column.getName(), records.get(0).getInt("_id"));
                        break;
                }
            }
        }
        model.update(rowId, many2oneValues);
    }

    // FIXME: Remove the function, get the date from odoo.
    private String updateSyncDate(SQLiteDatabase incrDB, String[] tables) {
        String date = queryList(incrDB, "select max(_write_date) from quality_control", null).get(0).getString("max(_write_date)");
        if (date.equals("0") || date.equals("false")){
            date = new DeliveryRoute(mContext, null).query("select max(_write_date) from quality_control").get(0).getString("max(_write_date)");
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

    public class MergeTask extends AsyncTask<OSQLite, Void, Void> {
        private ProgressDialog progressDialog = new ProgressDialog(mContext);
        private BaseAbstractListener listener;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setTitle(R.string.title_merge_db);
            progressDialog.setMessage(OResource.string(mContext, R.string.message_merge_db));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(OSQLite... params) {
            // TODO: add param: date
            OSQLite incrSqlite = params[0];
            mergeSqlite(incrSqlite);
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            progressDialog.dismiss();
            if (listener != null) {
                listener.OnSuccessful("Success");
            }
        }

        public void setListener(BaseAbstractListener listener) {
            this.listener = listener;
        }
    }

}
