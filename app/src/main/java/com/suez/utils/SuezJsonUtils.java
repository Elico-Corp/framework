package com.suez.utils;


import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.utils.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 18-5-2.
 */

public class SuezJsonUtils extends JSONUtils {
    private static final String TAG = SuezJsonUtils.class.getSimpleName();

    /**
     * Format string to json array.
     * Change the relation fields to jason array with 2 element: id and name.
     *
     * @param str the Relation field name read by jsonrpc
     * @return the json array [id, name]
     */
    public static JSONArray formatStringToJSON(String str) {
        try {
            if (str == null || str.equals("false")) {
                JSONArray jsonArray = new JSONArray("[\"0\",\"false\"]");
                return jsonArray;
            }
            String strFormat = str.trim().replace("\"", "").replaceFirst("\\[", "[\"")
                    .replaceFirst(".$", "\"]").replaceFirst(",", "\",\"");
            if (strFormat.equals("false")) {
                JSONArray jsonArray = new JSONArray("[\"0\",\"false\"]");
                return jsonArray;
            }
            JSONArray jsonArray = new JSONArray(strFormat);
            return jsonArray;
        } catch (JSONException e) {
            e.printStackTrace();
            LogUtils.e(TAG, "JSON Error is " + e);
            return null;
        }
    }

    /**
     * Parse float to int for integer fields.
     * Parse many2one fields, add new field with a name of _name to show many2one field in the view.
     *
     * @param model the model of the records
     * @param rows  the records
     * @return records with right integer fields and names of man2one fields
     */
    public static List<ODataRow> parseRecords(OModel model, List<ODataRow> rows) {
        List<ODataRow> res = new ArrayList<>();
        try {
            for (ODataRow row : rows) {
                ODataRow resRow = new ODataRow();
                for (OColumn column: model.getColumns()) {
                    if (row.get(column.getName()) == null) {
                        continue;
                    }
                    // avoid error by gson transfers int to double
                    if (column.getType().equals(OInteger.class)) {
                        resRow.put(column.getName(), row.getFloat(column.getName()).intValue());
                    } else if (column.getType().equals(OFloat.class)) {
                        resRow.put(column.getName(), new BigDecimal(row.getString(column.getName()))
                                .setScale(4, BigDecimal.ROUND_HALF_UP).floatValue());
                    }
                    // TODO: M2M and O2M columns
                    else if (column.getRelationType() == OColumn.RelationType.ManyToOne) {
                        resRow.put(column.getName(), (int) Float.parseFloat((String) formatStringToJSON(row.getString(column.getName())).get(0)));
                        resRow.put(column.getName() + "_name", formatStringToJSON(row.getString(column.getName())).get(1));
                    } else {
                        resRow.put(column.getName(), row.getString(column.getName()));
                    }
                }
                res.add(resRow);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            LogUtils.e(TAG, e.getMessage());
        }
        return res;
    }
}
