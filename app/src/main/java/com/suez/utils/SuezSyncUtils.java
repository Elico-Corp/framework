package com.suez.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.utils.gson.OdooRecord;
import com.odoo.core.rpc.helper.utils.gson.OdooResponse;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OResource;
import com.suez.SuezConstants;
import com.suez.addons.models.DeliveryRoute;
import com.suez.addons.models.OperationsWizard;
import com.suez.addons.models.StockLocation;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.StockQuant;

import java.util.ArrayList;
import java.util.Collection;
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
    private StockProductionLot stockProductionLot;
    private StockQuant stockQuant;
    private StockLocation stockLocation;
    private List<ODataRow> records;

    public SuezSyncUtils(Context context, OUser user, String date) {
        mUser = user;
        mContext = context;
        syncDate = date;
        wizard = new OperationsWizard(context, user);
        stockProductionLot = new StockProductionLot(mContext, mUser);
        stockQuant = new StockQuant(mContext, mUser);
        stockLocation = new StockLocation(mContext, mUser);
    }

    public void getRecords() {
        records = wizard.select(null, "_write_date > ? and synced = ? and has_conflict = ?",
                new String[] {syncDate, "false", "false"}, "create_date desc");
    }

    public void setRecords(List<ODataRow> rows) {
        records = rows;
    }

    public void syncProcessing() throws Exception {
        // Get records from the wizard model
        for (final ODataRow record: records) {
            final String[] quantLineIds = record.getString("quant_line_ids").split(",");
            String[] quantLineQty = record.getString("quant_line_qty").split(",");
            String[] newQuantIds = record.getString("new_quant_ids").split(",");
            float remainQty = record.getFloat("remain_qty");
            int lotId = record.getM2ORecord("prodlot_id").browse().getInt("id");
            final String[] newLotId = record.getString("new_prodlot_ids").split(",");
            Float qty = record.getFloat("qty");
            HashMap<String, Object> map = new HashMap<>();
            int pretreatmentLocationId;
            int destinationLocationId;
            HashMap<String, Object> kwargs;
            List<HashMap> quantLines;
            // Sync according to the action
            switch (record.getString("action")) {
                case SuezConstants.PRETREATMENT_KEY:
                    pretreatmentLocationId = record.getM2ORecord("pretreatment_location_id").browse().getInt("id");
                    destinationLocationId = record.getM2ORecord("destination_location_id").browse().getInt("id");
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("product_qty", qty);
                    kwargs.put("available_qty", qty + remainQty);
                    kwargs.put("pretreatment_location", pretreatmentLocationId);
                    kwargs.put("dest_location", destinationLocationId);
                    map.put("action", SuezConstants.PRETREATMENT_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.REPACKING_KEY:
                    int repackingLocationId = record.getM2ORecord("repacking_location_id").browse().getInt("id");
                    int packageId = record.getM2ORecord("package_id").browse().getInt("id");
                    int packageNumber = record.getInt("package_number");
                    destinationLocationId = record.getM2ORecord("destination_location_id").browse().getInt("id");
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("quant_id", stockQuant.browse(Integer.parseInt(quantLineIds[0])).getInt("id"));
                    kwargs.put("available_quantity", qty + remainQty);
                    kwargs.put("source_location_id", stockQuant.browse(Integer.parseInt(quantLineIds[0])).getM2ORecord("location_id").browse().getInt("id"));
                    kwargs.put("quantity", qty);
                    kwargs.put("repacking_location_id", repackingLocationId);
                    kwargs.put("location_dest_id", destinationLocationId);
                    kwargs.put("package_id", packageId);
                    kwargs.put("package_number", packageNumber);
                    map.put("action", SuezConstants.REPACKING_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.DIRECT_BURN_KEY:
                    pretreatmentLocationId = record.getM2ORecord("pretreatment_location_id").browse().getInt("id");
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("quant_id", stockQuant.browse(Integer.parseInt(quantLineIds[0])).getInt("id"));
                    kwargs.put("source_location_id", stockQuant.browse(Integer.parseInt(quantLineIds[0])).getM2ORecord("location_id").browse().getInt("id"));
                    kwargs.put("quantity", qty);
                    kwargs.put("available_quantity", qty + remainQty);
                    kwargs.put("pretreatment_location", pretreatmentLocationId);
                    map.put("action", SuezConstants.DIRECT_BURN_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.ADD_BLENDING_KEY:
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("quantity", qty);
                    kwargs.put("is_finish", record.getBoolean("is_finished"));
                    quantLines = new ArrayList<>();
                    for (int i=0; i<quantLineIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("quant_id", stockQuant.browse(Integer.parseInt(quantLineIds[i])).getInt("id"));
                        quantLine.put("quantity", Float.parseFloat(quantLineQty[i]));
                        quantLines.add(quantLine);
                    }
                    kwargs.put("quant_lines", quantLines);
                    map.put("action", SuezConstants.ADD_BLENDING_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.CREATE_BLENDING_KEY:
                    int blendingLocationId = record.getM2ORecord("blending_location_id").browse().getInt("id");
                    destinationLocationId = record.getM2ORecord("destination_location_id").browse().getInt("id");
                    int blendingWasteCategoryId = record.getM2ORecord("blending_waste_category_id").browse().getInt("id");
                    quantLines = new ArrayList<>();
                    for (int i=0; i<quantLineIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("quant_id", stockQuant.browse(Integer.parseInt(quantLineIds[i])).getInt("id"));
                        quantLine.put("quantity", Float.parseFloat(quantLineQty[i]));
                        quantLines.add(quantLine);
                    }
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("blending_location_id", blendingLocationId);
                    kwargs.put("location_dest_id", destinationLocationId);
                    kwargs.put("category_id", blendingWasteCategoryId);
                    kwargs.put("quantity", qty);
                    kwargs.put("is_finish", record.getBoolean("is_finished"));
                    kwargs.put("quant_lines", quantLines);
                    map.put("action", SuezConstants.CREATE_BLENDING_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.WAC_MOVE_KEY:
                    destinationLocationId = record.getM2ORecord("destination_location_id").browse().getInt("id");
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("quant_id", stockQuant.browse(Integer.parseInt(quantLineIds[0])).getInt("id"));
                    kwargs.put("location_dest_id", destinationLocationId);
                    kwargs.put("quantity", qty);
                    kwargs.put("available_quantity", qty + remainQty);
                    map.put("action", SuezConstants.WAC_MOVE_KEY);
                    map.put("data", kwargs);
                    break;
            }
//            CallMethodsOnlineUtils syncUtils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data",
//                    new OArguments(), null, map).setListener(new BaseAbstractListener(){
//                @Override
//                public void OnSuccessful(Object obj) {
                    // Write back the ids
                    Object obj = stockProductionLot.getServerDataHelper().callMethod("get_flush_data", new OArguments(), null, map);
                    if (obj == null) {
                        LogUtils.e(TAG, "Response null");
                        throw new Exception("Resopn Null From Server");
                    } else if (obj instanceof String) {
                        LogUtils.e(TAG, String.valueOf(obj));
                    } else if (obj instanceof ArrayList) {
                        List<LinkedTreeMap> results = new ArrayList<>();
                        results.addAll((Collection<? extends LinkedTreeMap>) obj);
                        if (results.size() == 0) {
                            OValues values = new OValues();
                            values.put("synced", true);
                            wizard.update(record.getInt("_id"), values);
                            continue;
                        }
                        for (int i=0; i<results.size(); i++) {
                            for (int n=results.get(i).getArray("quant_id").size()-1; n>=0; n--) {
                                OValues quantValues = new OValues();
                                quantValues.put("id", results.get(i).getArray("quant_id").get(n));
                                stockQuant.update(Integer.parseInt(newQuantIds[n+i]), quantValues);
                            }
                            if (results.get(i).getArray("lot_id") != null && newLotId.length >0 && !newLotId[0].equals("false")) {
                                for (int m=results.get(i).getArray("lot_id").size()-1; m>=0; m--) {
                                    OValues lotValues = new OValues();
                                    lotValues.put("id", results.get(i).getArray("lot_id").get(m));
                                    stockProductionLot.update(Integer.parseInt(newLotId[m+i]), lotValues);
                                }
                            }
                        }
                        OValues values = new OValues();
                        values.put("synced", true);
                        wizard.update(record.getInt("_id"), values);
                }
//            });
//            syncUtils.callMethodOnServer(false);
        }
    }

    public void syncTankTrunk() {
        DeliveryRoute deliveryRoute = new DeliveryRoute(mContext, mUser);
        records = wizard.select(new String[]{"delivery_route_id"}, "create_date > ? and synced = ?",
                new String[]{syncDate, "false"});
        List<Integer> ids = new ArrayList<>();
        for (ODataRow record: records) {
            ids.add(record.getM2ORecord("delivery_route_id").browse().getInt("id"));
        }
        HashMap<String, Object> map = new HashMap<>();
        HashMap<String, Object> kwargs = new HashMap<>();
        kwargs.put("ids", ids);
        map.put("action", SuezConstants.TANK_TRUCK_KEY);
        map.put("data", kwargs);
        CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(deliveryRoute, "get_flush_data",
                new OArguments(), null, map);
        utils.callMethodOnServer(false);
    }

    public void verifyConflicts(OdooResult response) {
        List<OdooRecord> responseRecords = response.getRecords();
        List<Integer> responseIds = new ArrayList<>();
        for (OdooRecord responseRecord: responseRecords) {
            responseIds.add(responseRecord.getInt("id"));
        }
        List<ODataRow> conflictRecords = new ArrayList<>();
        for (ODataRow record: records) {
            if (responseIds.contains(record.getM2ORecord("prodlot_id").browse().getInt("id"))) {
                conflictRecords.add(record);
                OValues value = new OValues();
                value.put("has_conflict", true);
                wizard.update(record.getInt("_id"), value);
            }
        }
        Log.v(TAG, "Conflict records: " + conflictRecords);
        records.removeAll(conflictRecords);
    }

}
