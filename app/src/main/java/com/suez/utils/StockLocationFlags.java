package com.suez.utils;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.utils.OResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 18-11-2.
 */

public class StockLocationFlags {
    private static StockLocationFlags ourInstance = null;

    public static synchronized StockLocationFlags getInstance() {
        if (ourInstance == null) {
            ourInstance = new StockLocationFlags();
        }
        return ourInstance;
    }

    private List<ODataRow> locations;

    public List<String> getLocations(Context mContext) {
        List<String> res = new ArrayList<>();
        if (locations != null && locations.size() > 0) {
            for (ODataRow location: locations) {
                res.add(location.getString("name"));
            }
        }
        res.add("RPT");
        res.add("INT");
        res.add(OResource.string(mContext, R.string.label_others));
        return res;
    }

    public void setLocations(List<ODataRow> flagLocations) {
        this.locations = flagLocations;
    }
}
