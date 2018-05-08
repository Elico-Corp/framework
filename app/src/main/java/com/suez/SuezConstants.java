package com.suez;

import android.os.Environment;
import android.preference.PreferenceCategory;

/**
 * Created by joseph on 18-5-2.
 */

public class SuezConstants {
    public static final String RESULT = "result";
    public static final long EXIT_TIME_GAP = 2000L;
    public static final String LOG_FILE_PATH = Environment.getExternalStorageDirectory() + "/suez-odoo-log.txt";
    public static final String LANGUAGE_KEY = "key_language";
    public static final String OFFLINE_DB_URL_KEY = "offline_db_url";
    public static final String INCREMENTAL_DB_URL_KEY = "incremental_db_url";
    public static final String FORCE_DOWNLOAD_DB_KEY = "download_db";
    public static final String WORK_MODE_KEY = "work_online";
    public static final String OFFLINE_DB_VERSION_KEY = "offline_version";
    public static final String INCREMENTAL_DB_VERSION_KEY = "incr_version";
    public static final String LAST_SYNC_DATE_KEY = "last_sync_date";
    public static final String ACCOUNT_SYNC_SETTING_KEY = "account_sync_settings";
    public static final String SYNC_DATA_INTERVAL_KEY = "sync_interval_settings";
    public static final String SYNC_DATA_LIMIT_KEY = "sync_data_limit_settings";
    public static final int OFFLINE_DB_INT_KEY = 1;
    public static final int INCREMENTAL_DB_INT_KEY = 2;
    private static final long DEVELOPMENT_KEY = 4007770876L;

}
