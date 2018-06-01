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
    public static final int OFFLINE_URL_ERROR_KEY = 0;
    public static final int INCR_URL_ERROR_KEY = 1;
    public static final int HTTP_RETURN_404 = 2;
    public static final int DB_SIZE_GET_ZERO = 3;
    public static final int SEARCH_RECORD_DEFAULT_LIMIT = 100;
    public static final String COMMON_KEY = "key";
    public static final String WAC_INFO_PRODLOT_KEY = "PRODLOT INFO";
    public static final String WAC_INFO_KEY = "WAC INFO";
    public static final String CREATE_BLENDING_KEY = "new_blending";
    public static final String ADD_BLENDING_KEY = "add_blending";
    public static final String PRODLOT_ID_KEY = "prodlot_id";
    public static final String WAC_ID_KEY = "wac_id";
    public static final String DELIVERY_ROUTE_LINE_ID_KEY = "delivery_route_line_id";
    public static final String PRODLOT_NAME_KEY = "prodlot_name";
    public static final String PRETREATMENT_KEY = "pretreatment";
    public static final String REPACKING_KEY = "repacking";
    public static final String DIRECT_BURN_KEY = "direct_burn";
    public static final String SCAN_BLENDING_KEY = "scan_blending";
}
