package com.suez.utils;

import android.content.Context;

import com.google.gson.internal.LinkedTreeMap;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OPreferenceManager;
import com.suez.addons.models.OfflineAction;

import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-9.
 */

public abstract class SuezSyncUtils {
    private static final String TAG = SuezSyncUtils.class.getSimpleName();
    protected Context mContext;
    protected OUser mUser;
    protected ODataRow record;
    public SuezSyncUtils(Context context, OUser user, ODataRow dataRow) {
        mUser = user;
        mContext = context;
        record = dataRow;
    }

    public void sync() {
        HashMap values = getValues();
        List<LinkedTreeMap> res = flushToServer(getLoc(), getContext(), values);
        writeBackRecord(res);
    }

    public abstract HashMap getValues();
    public abstract OArguments getLoc();
    public abstract HashMap getContext();
    public abstract List<LinkedTreeMap> flushToServer(OArguments args, HashMap context, HashMap values);
    public abstract void  writeBackRecord(List<LinkedTreeMap> ids);
}
