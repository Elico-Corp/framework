package com.suez.utils;

import android.content.Context;

import com.google.gson.internal.LinkedTreeMap;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OPreferenceManager;
import com.suez.SuezConstants;
import com.suez.addons.models.OfflineAction;
import com.suez.addons.models.OperationsWizard;

import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-9.
 */

public class SuezSyncUtils {
    private static final String TAG = SuezSyncUtils.class.getSimpleName();
    private Context mContext;
    private OUser mUser;
    private String syncDate;
    private OperationsWizard wizard;
    private List<ODataRow> records;

    public SuezSyncUtils(Context context, OUser user, String date) {
        mUser = user;
        mContext = context;
        syncDate = date;
        wizard = new OperationsWizard(context, user);
    }

    public void sync() {
        records = wizard.select(null, "_create_date > ? and synced = ?",
                new String[] {syncDate, "false"}, "_create_date desc");
        for (ODataRow record: records) {
            switch (record.getString("action")) {
                // TODO
                case SuezConstants.PRETREATMENT_KEY:
                    break;
            }
        }
    }


}
