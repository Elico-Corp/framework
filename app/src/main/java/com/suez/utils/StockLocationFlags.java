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
        res.add("Aisle");
        res.add("Area");
        res.add("Standby");
        res.add("A-01");
        res.add("A-02");
        res.add("A-03");
        res.add("A-04");
        res.add("A-05");
        res.add("A-06");
        res.add("B-01");
        res.add("B-02");
        res.add("B-03");
        res.add("B-04");
        res.add("B-05");
        res.add("B-06");
        res.add("C-01");
        res.add("C-02");
        res.add("C-03");
        res.add("C-04");
        res.add("C-05");
        res.add("C-06");
        res.add(OResource.string(mContext, R.string.label_others));
        return res;
    }

    public void setLocations(List<ODataRow> flagLocations) {
        this.locations = flagLocations;
    }
}
