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
    private static final long DEVELOPMENT_KEY = 4007770876L;
    public static final int OFFLINE_MD5_CHECK_FAIL_KEY = 0;
    public static final int INCR_MD5_CHECK_FAIL_KEY = 1;
    public static final int OFFLINE_DOWNLOAD_MAX_RETRY = 1;
    public static final int INCR_DOWNLOAD_MAX_RETRY = 3;
    public static final int SEARCH_RECORD_DEFAULT_LIMIT = 100;
    public static final String COMMON_KEY = "key";
    public static final String TANK_TRUCK_KEY = "tank_trunk";
    public static final String WAC_INFO_WAC_KEY = "wac_info_wac";
    public static final String WAC_INFO_KEY = "wac_info";
    public static final String CREATE_BLENDING_KEY = "create_blending";
    public static final String ADD_BLENDING_KEY = "add_blending";
    public static final String PRODLOT_ID_KEY = "prodlot_id";
    public static final String WAC_ID_KEY = "wac_id";
    public static final String DELIVERY_ROUTE_LINE_ID_KEY = "delivery_route_line_id";
    public static final String REPACKING_RESULT_KEY = "repacking_result_ids";
    public static final String STOCK_QUANT_ID_KEY = "quant_id";
    public static final String PRODLOT_NAME_KEY = "prodlot_name";
    public static final String IS_FINISHED_KEY = "is_finished";
    public static final String PRETREATMENT_KEY = "processing";
    public static final String REPACKING_KEY = "repacking";
    public static final String DIRECT_BURN_KEY = "direct_burn";
    public static final String REPACKING_LABEL_PRINT_KEY = "repacking_label_print";
    public static final String SCAN_BLENDING_KEY = "scan_blending";
    public static final String WAC_MOVE_KEY = "wac_move";
    public static final String MD5_FILE_NAME = "md5_checksum.txt";
    public static final String SYNC_DONE_ACTION = "Sync Done";
    public static final String SYNC_FAIL_ACTION = "Sync Failed";
    public static final int RPC_MAX_RETRY = 99;
}
