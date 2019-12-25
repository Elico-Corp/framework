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
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.SettingsActivity;
import com.odoo.core.account.BaseSettings;
import com.odoo.core.account.OdooLogin;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OSQLite;
import com.odoo.core.support.OUser;
import com.odoo.core.support.sync.SyncUtils;
import com.odoo.core.utils.OPreferenceManager;
import com.odoo.core.utils.OResource;
import com.suez.addons.models.DeliveryRoute;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.StockQuant;
import com.suez.utils.OfflineDBUtil;

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
    private OSQLite offlineSqlite;
    private OSQLite incrSqlite;
    private OfflineDBUtil util;
    private Handler handler;
    private int offlineRetry;
    private int incrRetry;


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
        handler = new Handler(Looper.getMainLooper());
        offlineSqlite = app.getSQLite(mUser.getName());
        if (offlineSqlite == null) {
            offlineSqlite = new OSQLite(mContext, mUser);
            app.setSQLite(mUser.getName(), offlineSqlite);
        }
        incrSqlite = new OSQLite(mContext, mUser, mUser.getIncrDBName());

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
        incrementalDBUrlPreference.setSummary(String.format(OResource.string(mContext, R.string.label_database_size), getFileSize(new File(mContext.getDatabasePath("db").getParent(), mUser.getIncrDBName()))));
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
                SyncUtils syncUtils = new SyncUtils(mContext, mUser);
                if ((boolean) newValue) {
                    app.networkState = true;
                    SettingsActivity.progressDialog.show();
                    cancelDialog();
                    syncUtils.requestSync(StockQuant.AUTHORITY);
//                    Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
//                    startActivity(intent);
                } else {
                    // Close auto sync
                    syncUtils.setAutoSync(StockQuant.AUTHORITY, false);
                    offlineRetry = 0;
                    incrRetry = 0;
                    String offline_url = offlineDBUrlPreference.getText();
                    String incr_url = incrementalDBUrlPreference.getText();
                    util = new OfflineDBUtil(mContext, offline_url, incr_url);
                    downloadDB();
                }
                return true;
            }
        });
    }

    public static float getFileSize(File file) {
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                return  Math.round(fis.available() / 1024f / 1024f * 100f) /100f;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0f;
    }

    private void downloadDB () {
        BaseAbstractListener downloadListener = new BaseAbstractListener(){
            @Override
            public void OnSuccessful(Boolean check){
                BaseAbstractListener md5Listener = new BaseAbstractListener() {

                    @Override
                    public void OnFail(String err) {
//                        switch (i) {
//                            case SuezConstants.OFFLINE_MD5_CHECK_FAIL_KEY:
//                                if (offlineRetry < SuezConstants.OFFLINE_DOWNLOAD_MAX_RETRY) {
//                                    preferenceManager.putString(SuezConstants.OFFLINE_DB_VERSION_KEY, "0");
//                                    downloadDB();
//                                    offlineRetry += 1;
//                                } else {
                                    onSwitchFail(R.string.toast_download_fail, true);
                                    preferenceManager.putString(SuezConstants.OFFLINE_DB_VERSION_KEY, "0");
//                                }
//                                break;
//                            case SuezConstants.INCR_MD5_CHECK_FAIL_KEY:
//                                if (incrRetry < SuezConstants.INCR_DOWNLOAD_MAX_RETRY) {
//                                    downloadDB();
//                                    incrRetry += 1;
//                                } else {
//                                    toast(R.string.toast_fail);
//                                    workModePreference.setChecked(true);
//                                }
//                                break;
//                        }
                    }
                    @Override
                    public void OnSuccessful(String obj) {
                        BaseAbstractListener mergeListener = new BaseAbstractListener() {
                            @Override
                            public void OnSuccessful(String str) {
                                if (str != null) {
                                    preferenceManager.putString("last_sync_date", str);
                                }
                                app.networkState = false;
                                toast(R.string.toast_successful);
                            }

                            @Override
                            public void OnFail(String str) {
                                onSwitchFail(R.string.toast_fail, true);
                            }
                        };
                        util.setListener(mergeListener).mergeDB();
                    }
                };
                util.setListener(md5Listener).checkMD5(check);
            }
            @Override
            public void OnFail(String str) {
                onSwitchFail(R.string.toast_fail, true);
            }
        };
        util.setListener(downloadListener).download();
    }

    private void cancelDialog() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((SettingsActivity) mContext).progressDialog.setCancelable(true);
            }
        }, 1000 * 60 * 5);
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

    private void onSwitchFail(@StringRes int id, final boolean pref) {
        toast(id);
        handler.post(new Runnable() {
            @Override
            public void run() {
                workModePreference.setChecked(pref);
            }
        });
        app.networkState = pref;
    }

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
