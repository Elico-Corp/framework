package com.suez;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.account.BaseSettings;
import com.odoo.core.account.OdooLogin;
import com.odoo.core.orm.OSQLite;
import com.odoo.core.support.OUser;
import com.odoo.core.support.sync.SyncUtils;
import com.odoo.core.utils.OPreferenceManager;
import com.odoo.core.utils.OResource;
import com.suez.addons.models.DeliveryRoute;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by joseph on 18-5-3.
 */

public class SuezSettings extends BaseSettings {
    public static final String TAG = SuezSettings.class.getSimpleName();
    private App app;
    private Context mContext;
    private OUser mUser;
    private OPreferenceManager preferenceManager;
    private ListPreference keyLanguagePreference;
    private EditTextPreference offlineDBUrlPreference;
    private EditTextPreference incrementalDBUrlPreference;
    private SwitchPreference workModePreference;
    public ProgressDialog progressDialog;
    private OSQLite offlineSqlite;
    private OSQLite incrSqlite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceCategory syncIntervalPref = (PreferenceCategory) findPreference(SuezConstants.SYNC_DATA_INTERVAL_KEY);
        PreferenceCategory syncLimitPref = (PreferenceCategory) findPreference(SuezConstants.SYNC_DATA_LIMIT_KEY);
        PreferenceCategory syncSettingsPref = (PreferenceCategory) findPreference(SuezConstants.ACCOUNT_SYNC_SETTING_KEY);
        getPreferenceScreen().removePreference(syncIntervalPref);
        getPreferenceScreen().removePreference(syncLimitPref);
        getPreferenceScreen().removePreference(syncSettingsPref);
        addPreferencesFromResource(R.xml.suez_preference);
        app = (App) getActivity().getApplication();
        mContext = getActivity();
        preferenceManager = new OPreferenceManager(mContext);
        mUser = OUser.current(mContext);
        offlineSqlite = app.getSQLite(mUser.getName());
        if (offlineSqlite == null) {
            offlineSqlite = new OSQLite(mContext, mUser);
            app.setSQLite(mUser.getName(), offlineSqlite);
        }
        incrSqlite = new OSQLite(mContext, mUser, mUser.getIncrDBName());
        progressDialog = new ProgressDialog(mContext);

        keyLanguagePreference = (ListPreference) findPreference(SuezConstants.LANGUAGE_KEY);
        keyLanguagePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((ListPreference) preference).setValue((String) newValue);
                Intent intent = new Intent(mContext, OdooLogin.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
                return true;
            }
        });

        offlineDBUrlPreference = (EditTextPreference) findPreference(SuezConstants.OFFLINE_DB_URL_KEY);
        offlineDBUrlPreference.setSummary(String.format(OResource.string(mContext, R.string.label_database_size), getFileSize(new File(mContext.getDatabasePath("db").getParent(), mUser.getDBName()))));
        offlineDBUrlPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((EditTextPreference) preference).setText((String) newValue);
                return true;
            }
        });

        incrementalDBUrlPreference = (EditTextPreference) findPreference(SuezConstants.INCREMENTAL_DB_URL_KEY);
        incrementalDBUrlPreference.setSummary(String.format(OResource.string(mContext, R.string.label_database_size), getFileSize(new File(mContext.getDatabasePath("db"), mUser.getIncrDBName()))));
        incrementalDBUrlPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((EditTextPreference) preference).setText((String) newValue);
                return true;
            }
        });

        workModePreference = (SwitchPreference) findPreference(SuezConstants.WORK_MODE_KEY);
        workModePreference.setDefaultValue(app.inNetwork());
        if (!app.inNetwork()) {
            workModePreference.setEnabled(false);
        }
        workModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!app.inNetwork()) {
                    workModePreference.setEnabled(false);
                }
                if ((boolean) newValue) {
                    app.networkState = true;
                    Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
                    startActivity(intent);
                } else {
//                     Close auto sync
                    new SyncUtils(mContext, mUser).setAutoSync(DeliveryRoute.AUTHORITY, false);
                    String offline_url = offlineDBUrlPreference.getText();
                    String incr_url = incrementalDBUrlPreference.getText();
                    BaseAbstractListener listener = new BaseAbstractListener(){
                        @Override
                        public void OnSuccessful(String str){
                            toast(R.string.toast_successful);
//                            OfflineDBUtil.MergeTask mergeDB = layer.new MergeTask();
//                            BaseAbstractListener listener = new BaseAbstractListener() {
//                                @Override
//                                public void OnSuccessful(String str) {
//                                    toast(R.string.toast_successful);
//                                }
//                            };
//                            mergeDB.setListener(listener);
//                            mergeDB.execute(incrSqlite);
                        }
                        @Override
                        public void OnFail(int i) {
                            switch (i) {
                                case SuezConstants.OFFLINE_URL_ERROR_KEY:
                                    toast(String.format(OResource.string(mContext, R.string.toast_input_right_url), OResource.string(mContext, R.string.label_offline)));
                                    workModePreference.setChecked(true);
                                    break;
                                case SuezConstants.INCR_URL_ERROR_KEY:
                                    toast(String.format(OResource.string(mContext, R.string.toast_input_right_url), OResource.string(mContext, R.string.label_incremental_db)));
                                    workModePreference.setChecked(true);
                                    break;
                                case SuezConstants.HTTP_RETURN_404:
                                    toast(R.string.toast_db_not_found);
                                    break;
                                case SuezConstants.DB_SIZE_GET_ZERO:
                                    toast(R.string.toast_db_zero);
                                    break;
                                default:
                                    toast(R.string.toast_fail);
                            }
                        }

                        @Override
                        public void OnFail(String str) {
                            toast(str);
                        }
                    };
//                    OfflineDBUtil.DownloadDBTask downloadTask = layer.new DownloadDBTask();
//                    downloadTask.setListener(listener);
//                    downloadTask.execute(offline_url, incr_url, offlineSqlite.databaseLocalPath(), incrSqlite.databaseLocalPath(), preferenceManager.getString(SuezConstants.OFFLINE_DB_VERSION_KEY, "0"));
                    app.networkState = false;
                }
                return true;
            }
        });
    }

    private float getFileSize(File file) {
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                float size = (float) (Math.round(fis.available() / 1024f / 1024f * 100f) /100f);
                return size;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0f;
    }

//    private void downloadDB(final HashMap<String, String> version, final String offline_url, final String incr_url, final BaseAbstractListener listener) {
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                if (!checkUrl(offline_url)) {
//                    toast(String.format(OResource.string(mContext, R.string.toast_input_right_url), OResource.string(mContext, R.string.label_offline)));
//                    workModePreference.setChecked(true);
//                    return;
//                }
//                if (!checkUrl(incr_url)){
//                    toast(String.format(OResource.string(mContext, R.string.toast_input_right_url), OResource.string(mContext, R.string.label_incremental_db)));
//                    workModePreference.setChecked(true);
//                    return;
//                }
//                progressDialog.setTitle(R.string.title_progress_downloading);
//                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                progressDialog.setMessage(OResource.string(mContext, R.string.message_progress_downloading));
//                progressDialog.setMax(100);
//                progressDialog.setCancelable(false);
//                progressDialog.show();
//            }
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                DownloadUtils.get().downloadDB(incr_url, incrSqlite, new DownloadUtils.OnDownloadListener() {
//                    @Override
//                    public void onDownloadSuccess(Long size) {
//                        Log.v(TAG, "Download incremental DB success");
//                    }
//
//                    @Override
//                    public void onDownloading(int progress, Long size) {
//                        Log.v(TAG, "Downloading incremental progress = " + progress + " size = " + size);
//                        progressDialog.setProgress(progress);
//                    }
//
//                    @Override
//                    public void onDownloadFailed(String error) {
//                        Log.v(TAG, "Download failed: " + error);
//                        progressDialog.dismiss();
//                        if (error.equals("404")) {
//                            toast(R.string.toast_db_not_found);
//                        } else if (error.equals("0")) {
//                            toast(R.string.toast_db_zero);
//                        } else {
//                            toast(error);
//                        }
//                    }
//                });
//                String offline_version = DownloadUtils.get().downloadDB(offline_url, version.get(SuezConstants.OFFLINE_DB_VERSION_KEY), offlineSqlite, new DownloadUtils.OnDownloadListener() {
//                    @Override
//                    public void onDownloadSuccess(Long size) {
//                        Log.v(TAG, "Download Offline Success");
//                    }
//
//                    @Override
//                    public void onDownloading(int progress, Long size) {
//                        Log.v(TAG, "Downloading offline progress = " + progress + " size = " + size);
//                        progressDialog.setProgress(progress);
//                    }
//
//                    @Override
//                    public void onDownloadFailed(String error) {
//                        Log.v(TAG, "Download failed: " + error);
//                        progressDialog.dismiss();
//                        if (error.equals("404")) {
//                            toast(R.string.toast_db_not_found);
//                        } else if (error.equals("0")) {
//                            toast(R.string.toast_db_zero);
//                        } else {
//                            toast(error);
//                        }
//                    }
//                });
//                preferenceManager.putString(SuezConstants.OFFLINE_DB_VERSION_KEY, offline_version);
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void v) {
//                super.onPostExecute(v);
//                progressDialog.dismiss();
//                if (listener != null) {
//                    listener.OnSuccessful("Success");
//                }
//            }
//        }.execute();
//    }
//
//    private class MergeTask extends AsyncTask<Void, Void, Void> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            progressDialog.setTitle(R.string.title_merge_db);
//            progressDialog.setMessage(OResource.string(mContext, R.string.message_merge_db));
//            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//            progressDialog.setCancelable(false);
//            progressDialog.show();
//        }
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            // TODO: add param: date
//            offlineSqlite.mergeSqlite(incrSqlite);
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void v) {
//            super.onPostExecute(v);
//            progressDialog.dismiss();
//            toast(R.string.toast_successful);
//        }
//
//    }

    private void toast(final int id) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(app, id, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(app, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
