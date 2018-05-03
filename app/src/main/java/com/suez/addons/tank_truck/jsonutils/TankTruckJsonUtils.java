package com.suez.addons.tank_truck.jsonutils;


import com.odoo.core.orm.ODataRow;
import com.suez.utils.LogUtils;
import com.suez.utils.SuezJsonUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 18-5-2.
 */

public class TankTruckJsonUtils extends SuezJsonUtils {
    private static final String TAG = TankTruckJsonUtils.class.getSimpleName();

    public static List<ODataRow> setTankTruck(List<ODataRow> listRow) {
        try {
            List<ODataRow> list = new ArrayList<>();
            ODataRow row;
            for (int i = 0; i < listRow.size(); i++) {
                row = new ODataRow();
                row.put("id", listRow.get(i).getFloat("id").intValue());
                row.put("name", listRow.get(i).getString("name"));
                row.put("plate_number", listRow.get(i).getString("plate_number").equals("false")
                        ? "false" : formatStringToJSON(listRow.get(i).getString("plate_number"))
                        .get(1));
                row.put("state", listRow.get(i).getString("state"));
                list.add(row);
            }
            return list;
        } catch (JSONException e) {
            e.printStackTrace();
            LogUtils.e(TAG, "setTankTruck error is " + e);
            return null;
        }
    }
}
