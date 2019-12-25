package com.suez.utils;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.Odoo;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.utils.gson.OdooRecord;
import com.odoo.core.rpc.helper.utils.gson.OdooResponse;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.notification.ONotificationBuilder;
import com.odoo.datas.OConstants;
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
import java.util.UUID;

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
    private DeliveryRoute deliveryRoute;
    private List<ODataRow> records;
    private List<ODataRow> conflictRecords;
    private int notificationCount = 1;

    public SuezSyncUtils(Context context, OUser user, String date) {
        mUser = user;
        mContext = context;
        syncDate = date;
        wizard = new OperationsWizard(context, user);
        stockProductionLot = new StockProductionLot(mContext, mUser);
        stockQuant = new StockQuant(mContext, mUser);
        deliveryRoute = new DeliveryRoute(mContext, mUser);
    }

    public void getRecords() {
        records = wizard.select(null, "_write_date > ? and synced = ? and has_conflict = ?",
                new String[] {syncDate, "false", "false"}, "create_date desc");
    }

    public void setRecords(List<ODataRow> rows) {
        records = rows;
    }

    public void syncProcessing() throws Exception {
        conflictRecords = new ArrayList<>();
        // Get records from the wizard model
        for (final ODataRow record: records) {
            final String[] quantLineIds = parseStringIds(record.getString("quant_line_ids"));
            if (conflictRecords.contains(record)) {
                onConflict(record);
                continue;
            }
            String[] quantLineQty = parseStringIds(record.getString("quant_line_qty"));
            String[] newQuantIds = parseStringIds(record.getString("new_quant_ids"));
            float remainQty = record.getFloat("remain_qty");
            int lotId = record.getM2ORecord("prodlot_id").browse().getInt("id");
            final String[] newLotId = parseStringIds(record.getString("new_prodlot_ids"));
            Float qty = record.getFloat("qty");
            HashMap<String, Object> map = new HashMap<>();
            map.put("action_uid", UUID.randomUUID().toString());
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
                    kwargs.put("quantity", qty);
                    kwargs.put("quant_id", stockQuant.browse(Integer.parseInt(quantLineIds[0])).getInt("id"));
                    kwargs.put("pretreatment_location_id", pretreatmentLocationId);
                    kwargs.put("location_dest_id", destinationLocationId);
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
                    kwargs.put("quantity", qty);
                    kwargs.put("repacking_location_id", repackingLocationId);
                    kwargs.put("location_dest_id", destinationLocationId);
                    kwargs.put("package_id", packageId);
                    kwargs.put("package_number", packageNumber);
                    map.put("action", SuezConstants.REPACKING_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.DIRECT_BURN_KEY:
                    pretreatmentLocationId = record.getM2ORecord("int_location_id").browse().getInt("id");
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("quant_id", stockQuant.browse(Integer.parseInt(quantLineIds[0])).getInt("id"));
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
            Object obj = null;
            int retry = 0;
            while (obj == null || String.valueOf(obj).equals("false") && retry <= SuezConstants.RPC_MAX_RETRY) {
                obj = stockProductionLot.getServerDataHelper().callMethod("get_flush_data", new OArguments(), null, map);
                retry ++ ;
            }
            OValues values = new OValues();
            values.put("synced", true);
            wizard.update(record.getInt("_id"), values);
            if (obj == null) {
                LogUtils.e(TAG, "Response null");
                throw new Exception("Resopn Null From Server");
            } else if (obj instanceof String && ((String) obj).contains("error")) {
                LogUtils.e(TAG, String.valueOf(obj));
                showNotification(String.valueOf(obj));
            } else if (obj instanceof String && ((String) obj).contains("conflict")) {
                handleConflict(record);
                showNotification(String.valueOf(obj));
            } else if (obj instanceof ArrayList) {
                List<LinkedTreeMap> results = new ArrayList<>();
                results.addAll((Collection<? extends LinkedTreeMap>) obj);
                for (int i = 0; i < results.size(); i++) {
                    if (newQuantIds != null) {
                        for (int n = results.get(i).getArray("quant_id").size() - 1; n >= 0; n--) {
                            OValues quantValues = new OValues();
                            quantValues.put("id", results.get(i).getArray("quant_id").get(n));
                            stockQuant.update(Integer.parseInt(newQuantIds[n + i]), quantValues);
                        }
                    }
                    if (newLotId != null && results.get(i).getArray("lot_id") != null && newLotId.length > 0) {
                        for (int m = results.get(i).getArray("lot_id").size() - 1; m >= 0; m--) {
                            OValues lotValues = new OValues();
                            lotValues.put("id", results.get(i).getArray("lot_id").get(m));
                            stockProductionLot.update(Integer.parseInt(newLotId[m + i]), lotValues);
                        }
                    }
                }
            } else if (obj instanceof Boolean) {
                if (!(Boolean) obj) {
                    showNotification(String.format(OResource.string(mContext, R.string.message_sync_failed),
                            record.getString("action"), record.getM2ORecord("prodlot_id").getName()));
                }
            }
//            });
//            syncUtils.callMethodOnServer(false);
        }
        records.removeAll(conflictRecords);
    }

    public void syncTankTrunk() {
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
        map.put("action_uid", UUID.randomUUID().toString());
        CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(deliveryRoute, "get_flush_data",
                new OArguments(), null, map);
        utils.callMethodOnServer(false);
    }

    private void handleConflict(ODataRow row) {
        String[] originIds = parseStringIds(row.getString("before_ids"));
        for (ODataRow record: records) {
            if (originIds != null && parseStringIds(row.getString("before_ids")) == originIds) {
                conflictRecords.add(record);
            }
        }
    }

    private void onConflict(ODataRow record) {
                OValues oValues = new OValues();
                oValues.put("has_conflict", true);
                wizard.update(record.getInt("_id"), oValues);
                Log.v(TAG, "Conflict record: " + record);
                showNotification(String.format(OResource.string(mContext, R.string.message_sync_conflict),
                        record.getString("action"), record.getM2ORecord("prodlot_id").getName()));
            }

    private List<Integer> getResponseRecords(OdooResult result) {
        List<OdooRecord> responseRecords = result.getRecords();
        List<Integer> responseIds = new ArrayList<>();
        for (OdooRecord responseRecord: responseRecords) {
            responseIds.add(responseRecord.getInt("id"));
        }
        return responseIds;
    }

    private void showNotification(@StringRes int resId) {
        ONotificationBuilder builder = new ONotificationBuilder(mContext, notificationCount);
        builder.setIcon(R.drawable.ic_odoo);
        builder.setTitle(OResource.string(mContext, R.string.title_sync_failed));
        builder.setText(OResource.string(mContext, resId));
        builder.setAutoCancel(false);
        builder.show();
        notificationCount++;
    }

    private void showNotification(String message) {
        ONotificationBuilder builder = new ONotificationBuilder(mContext, notificationCount);
        builder.setIcon(R.drawable.ic_odoo);
        builder.setTitle(OResource.string(mContext, R.string.title_sync_failed));
        builder.setText(message);
        builder.setAutoCancel(false);
        builder.show();
        notificationCount++;
    }

    private String[] parseStringIds(String recordString) {
        if (recordString.equals("false")) {
            return null;
        }
        return recordString.split(",");
    }
}
