package com.suez.addons.tank_truck.utils;

import android.content.Context;

import com.google.gson.internal.LinkedTreeMap;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.ServerDataHelper;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.support.OUser;
import com.suez.addons.models.DeliveryRoute;
import com.suez.utils.SuezSyncUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-10.
 */

public class TankTruckSyncUtils extends SuezSyncUtils {
    private static final String TAG = SuezSyncUtils.class.getSimpleName();

    public TankTruckSyncUtils(Context context, OUser mUser, ODataRow record) {
        super(context, mUser, record);
    }

    @Override
    public HashMap getValues() {return null;}

    @Override
    public HashMap getContext() {return null;}

    @Override
    public OArguments getLoc() {
        OArguments args = new OArguments();
        args.add(new JSONArray().put(record.getInt("id")));
        args.add(new JSONObject());
        return args;
    }

    @Override
    public List<LinkedTreeMap> flushToServer(OArguments args, HashMap context, HashMap kwargs) {
        DeliveryRoute deliveryRoute = new DeliveryRoute(mContext, mUser);
        deliveryRoute.getServerDataHelper().callMethod("action_pumping", args);
        return null;
    }

    @Override
    public void writeBackRecord(List<LinkedTreeMap> ids) {
        return;
    }
}
