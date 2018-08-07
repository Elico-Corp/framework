package com.suez.utils;

import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 18-5-23.
 */

public class RecordUtils {

    private OModel model;

    public RecordUtils(OModel model) {
        this.model = model;
    }

    public static List<Object> getFieldList(List<ODataRow> rows, String field) {
        List<Object> res = new ArrayList<>();
        for (ODataRow row: rows) {
            res.add(row.get(field));
        }
        return res;
    }

    public static String getFieldString(List<ODataRow> rows, String field, char sep) {
        StringBuilder builder = new StringBuilder();
        for (ODataRow row: rows) {
            builder.append(row.get(field));
            builder.append(sep);
        }
        builder.deleteCharAt(builder.lastIndexOf(String.valueOf(sep)));
        return builder.toString();
    }

    public static String getFieldString(List<ODataRow> rows, String field) {
        return getFieldString(rows, field, ',');
    }

    public static String getArrayString(Object[] objs, char sep) {
        StringBuilder builder = new StringBuilder();
        for (Object obj: objs) {
            builder.append(String.valueOf(obj));
            builder.append(sep);
        }
        builder.deleteCharAt(builder.lastIndexOf(String.valueOf(sep)));
        return builder.toString();
    }

    public static String getArrayString(Object[] objs) {
        return getArrayString(objs, ',');
    }

    public static float sumField(List<ODataRow> rows, String field) {
        float res = 0.00f;
        for (ODataRow row : rows) {
            res += row.getFloat(field);
        }
        return res;
    }

    /**
     * Parse many2one records. Add field named with many2one field + `_name` to show many2one fields in the view
     *
     * @param rows               the records
     * @param many2oneFields     the many2one fields to be parsed
     * @param many2oneFieldsName the name field of the many2one fields
     * @return the records with many2one field names
     */
    public List<ODataRow> parseMany2oneRecords(List<ODataRow> rows, String[] many2oneFields, String[] many2oneFieldsName) {
        for (ODataRow row : rows) {
            for (int i = 0; i < many2oneFields.length && i < many2oneFieldsName.length; i++) {
                if (model.getColumn(many2oneFields[i]).getRelationType().equals(OColumn.RelationType.ManyToOne)) {
                    if (row.getM2ORecord(many2oneFields[i]).browse() != null) {
                        row.put(many2oneFields[i] + "_name", row.getM2ORecord(many2oneFields[i]).browse().getString(many2oneFieldsName[i]));
                    } else {
                        row.put(many2oneFields[i] + "_name", "");
                    }
                }
            }
        }
        return rows;
    }

    public ODataRow parseMany2oneRecords(ODataRow row, String[] many2oneFields, String[] many2oneFieldsName) {
        for (int i = 0; i < many2oneFields.length && i < many2oneFieldsName.length; i++) {
            if (model.getColumn(many2oneFields[i]).getRelationType().equals(OColumn.RelationType.ManyToOne)) {
                if (row.getM2ORecord(many2oneFields[i]).browse() != null) {
                    row.put(many2oneFields[i] + "_name", row.getM2ORecord(many2oneFields[i]).browse().getString(many2oneFieldsName[i]));
                } else {
                    row.put(many2oneFields[i] + "_name", "");
                }
            }
        }
        return row;
    }

    public static float minusFloat(String f1, String f2) {
        BigDecimal bd1 = new BigDecimal(f1);
        BigDecimal bd2 = new BigDecimal(f2);
        return bd1.subtract(bd2).floatValue();
    }

    public static float minusFloat(float f1, float f2) {
        return minusFloat(String.valueOf(f1), String.valueOf(f2));
    }

    public static String fieldGet(ODataRow row, String[] fields, char sep) {
        StringBuilder builder = new StringBuilder();
        for (String field: fields) {
            if (!row.getString(field).equals("false")) {
                builder.append(row.getString(field));
            }
            builder.append(sep);
        }
        builder.deleteCharAt(builder.lastIndexOf(String.valueOf(sep)));
        return builder.toString();
    }

    public static List<ODataRow> nameGet(List<ODataRow> rows, String[] fields, char sep) {
        for (ODataRow row: rows) {
            row.put("name", fieldGet(row, fields, sep));
        }
        return rows;
    }
}
