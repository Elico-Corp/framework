package com.suez.utils;


import com.odoo.core.utils.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by joseph on 18-5-2.
 */

public class SuezJsonUtils extends JSONUtils {
    private static final String TAG = SuezJsonUtils.class.getSimpleName();

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
}
