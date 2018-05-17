/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * <p/>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * <p/>
 * Created on 30/12/14 3:31 PM
 */
package com.odoo.core.orm;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.odoo.App;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OCursorUtils;
import com.odoo.core.utils.OPreferenceManager;
import com.odoo.datas.OConstants;
import com.suez.SuezConstants;
import com.suez.addons.models.DeliveryRoute;
import com.suez.utils.LogUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OSQLite extends SQLiteOpenHelper {
    public static final String TAG = OSQLite.class.getSimpleName();
    private Context mContext;
    private OUser mUser = null;
    private App odooApp;
    private OPreferenceManager preferenceManager;

    public OSQLite(Context context, OUser user) {
        super(context, (user != null) ? user.getDBName() : OUser.current(context).getDBName(), null
                , OConstants.DATABASE_VERSION);
        mContext = context;
        odooApp = (App) context.getApplicationContext();
        mUser = (user != null) ? user : OUser.current(context);
    }

    // Add by Joseph 18-05-04: to create incremental sqlite object
    public OSQLite(Context context, OUser user, String dbName) {
        super(context, dbName, null, OConstants.DATABASE_VERSION);
        mContext = context;
        odooApp = (App) context.getApplicationContext();
        mUser = (user != null) ? user : OUser.current(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "creating database.");
        ModelRegistryUtils registryUtils = odooApp.getModelRegistry();
        HashMap<String, Class<? extends OModel>> models = registryUtils.getModels();
        OSQLHelper sqlHelper = new OSQLHelper(mContext);

        for (String key : models.keySet()) {
            OModel model = App.getModel(mContext, key, mUser);
            sqlHelper.createStatements(model);
        }
        for (String key : sqlHelper.getStatements().keySet()) {
            String query = sqlHelper.getStatements().get(key);
            db.execSQL(query);
            Log.i(TAG, "Table Created : " + key);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "upgrading database.");
        ModelRegistryUtils registryUtils = odooApp.getModelRegistry();
        HashMap<String, Class<? extends OModel>> models = registryUtils.getModels();
        for (String key : models.keySet()) {
            OModel model = App.getModel(mContext, key, mUser);
            if (model != null) model.onModelUpgrade(db, oldVersion, newVersion);
        }
    }

    public void dropDatabase() {
        if (mContext.deleteDatabase(getDatabaseName())) {
            Log.i(TAG, getDatabaseName() + " database dropped.");
        }
    }

    public String databaseLocalPath() {
        App app = (App) mContext.getApplicationContext();
        return Environment.getDataDirectory().getPath() +
                "/data/" + app.getPackageName() + "/databases/" + getDatabaseName();
    }

    public String getUserAndroidName() {
        return (this.mUser != null) ? this.mUser.getAndroidName() : "";
    }

//    public String mergeSqlite (OSQLite sqLite) {
//        SQLiteDatabase incrDB = sqLite.getReadableDatabase();
//        String[] tableNames = new String[] {
//                "delivery_route",
//        };
//        SQLiteDatabase db = new OModel(mContext, "res.partner", mUser).getWritableDatabase();
//        db.beginTransaction();
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
//            } catch (Exception e) {
//                e.printStackTrace();
//                LogUtils.e(TAG, "Merge failed for table: " + tableName);
//                LogUtils.e(TAG, e.toString());
//            }
//        }
//        String lastSyncDate = updateSyncDate(incrDB, tableNames);
//        db.setTransactionSuccessful();
//        db.endTransaction();
//        incrDB.close();
//        return lastSyncDate;
//    }
//
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
//
//    // FIXME: Remove the function, get the date from odoo.
//    private String updateSyncDate(SQLiteDatabase incrDB, String[] tables) {
//        String date = queryList(incrDB, "select max(_write_date) from quality_control", null).get(0).getString("max(_write_date)");
//        if (date.equals("0") || date.equals("false")){
//            date = new DeliveryRoute(mContext, null).query("select max(_write_date) from quality_control").get(0).getString("max(_write_date)");
//        }
//        return date;
//    }
//
//    private String toClassName(String tableName){
//        String[] strs = tableName.split("_");
//        StringBuilder nameBuilder = new StringBuilder();
//        for (String str: strs){
//            nameBuilder.append(Character.toUpperCase(str.charAt(0))).append(str.substring(1));
//        }
//        return nameBuilder.toString();
//    }
//
//
//    private List<ODataRow> queryList(SQLiteDatabase db, String sql, String[] args) {
//        Cursor cr = db.rawQuery(sql, args);
//        List<ODataRow> res = new ArrayList<>();
//        while (cr.moveToNext()) {
//            res.add(OCursorUtils.toDatarow(cr));
//        }
//        cr.close();
//        return res;
//    }
//
//    private OValues checkValues(OModel mModel, OValues values){
//        for (String key: values.keys()){
//            if (values.getString(key)==null || values.getString(key).equals(String.valueOf(mModel.getColumn(key).getDefaultValue())) || values.getString(key).equals("false")){
//                values.removeKey(key);
//                Log.v(TAG, key+" removed");
//            }
//        }
//        if (!values.keys().contains("_is_dirty")) {
//            values.put("_is_dirty", false);
//        }
//        return values;
//    }
}
