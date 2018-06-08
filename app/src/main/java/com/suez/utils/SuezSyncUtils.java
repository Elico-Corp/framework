package com.suez.utils;

import android.content.Context;

import com.odoo.BaseAbstractListener;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;
import com.odoo.core.support.OUser;
import com.suez.SuezConstants;
import com.suez.addons.models.OperationsWizard;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.StockQuant;

import java.util.ArrayList;
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
    private List<ODataRow> records;

    public SuezSyncUtils(Context context, OUser user, String date) {
        mUser = user;
        mContext = context;
        syncDate = date;
        wizard = new OperationsWizard(context, user);
        stockProductionLot = new StockProductionLot(mContext, mUser);
        stockQuant = new StockQuant(mContext, mUser);
    }

    public void sync() {
        // Get records from the wizard model
        records = wizard.select(null, "create_date > ? and synced = ?",
                new String[] {syncDate, "false"}, "create_date desc");
        for (ODataRow record: records) {
            final String[] quantLineIds = record.getString("quant_line_ids").split(",");
            String[] quantLineQty = record.getString("quant_line_qty").split(",");
            String[] quantLineLocationIds = record.getString("quant_line_location_ids").split(",");
            int lotId = record.getM2ORecord("prodlot_id").browse().getInt("id");
            final int newLotId = record.getM2ORecord("new_prodlot_id").getId();
            Float qty = record.getFloat("qty");
            String datetime = record.getString("create_date");
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
                    kwargs.put("date_planned_start", datetime);
                    quantLines = new ArrayList<>();
                    for (int i=0; i<quantLineLocationIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("location_id", Integer.parseInt(quantLineLocationIds[i]));
                        quantLine.put("quantity", Float.parseFloat(quantLineQty[i]));
                        quantLines.add(quantLine);
                    }
                    kwargs.put("quant_lines", quantLines);
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
                    kwargs.put("product_qty", qty);
                    kwargs.put("date_planned_start", datetime);
                    kwargs.put("repacking_location", repackingLocationId);
                    kwargs.put("dest_location", destinationLocationId);
                    kwargs.put("package_id", packageId);
                    kwargs.put("package_number", packageNumber);
                    quantLines = new ArrayList<>();
                    for (int i=0; i<quantLineLocationIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("location_id", Integer.parseInt(quantLineLocationIds[i]));
                        quantLine.put("quantity", Float.parseFloat(quantLineQty[i]));
                        quantLines.add(quantLine);
                    }
                    kwargs.put("quant_lines", quantLines);
                    map.put("action", SuezConstants.REPACKING_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.DIRECT_BURN_KEY:
                    pretreatmentLocationId = record.getM2ORecord("pretreatment_location_id").browse().getInt("id");
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("product_qty", qty);
                    kwargs.put("date_planned_start", datetime);
                    kwargs.put("pretreatment_location", pretreatmentLocationId);
                    quantLines = new ArrayList<>();
                    for (int i=0; i<quantLineLocationIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("location_id", Integer.parseInt(quantLineLocationIds[i]));
                        quantLine.put("quantity", Float.parseFloat(quantLineQty[i]));
                        quantLines.add(quantLine);
                    }
                    kwargs.put("quant_lines", quantLines);
                    map.put("action", SuezConstants.DIRECT_BURN_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.ADD_BLENDING_KEY:
                    kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("product_qty", qty);
                    quantLines = new ArrayList<>();
                    for (int i=0; i<quantLineLocationIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("location_id", Integer.parseInt(quantLineLocationIds[i]));
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
                    for (int i=0; i<quantLineLocationIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("location_id", Integer.parseInt(quantLineLocationIds[i]));
                        quantLine.put("quantity", Float.parseFloat(quantLineQty[i]));
                        quantLines.add(quantLine);
                    }
                    kwargs = new HashMap<>();
                    kwargs.put("blending_location", blendingLocationId);
                    kwargs.put("dest_location", destinationLocationId);
                    kwargs.put("category_id", blendingWasteCategoryId);
                    kwargs.put("quant_lines", quantLines);
                    map.put("action", SuezConstants.CREATE_BLENDING_KEY);
                    map.put("data", kwargs);
                    break;
                case SuezConstants.WAC_MOVE_KEY:
                    destinationLocationId = record.getM2ORecord("destination_location_id").browse().getInt("id");
                    quantLines = new ArrayList<>();
                    for (int i=0; i<quantLineLocationIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("location_id", Integer.parseInt(quantLineLocationIds[i]));
                        quantLine.put("quantity", Float.parseFloat(quantLineQty[i]));
                        quantLines.add(quantLine);
                    }
                    kwargs = new HashMap<>();
                    kwargs.put("dest_location", destinationLocationId);
                    kwargs.put("quant_lines", quantLines);
                    map.put("action", SuezConstants.WAC_MOVE_KEY);
                    map.put("data", kwargs);
                    break;
            }
            CallMethodsOnlineUtils syncUtils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data",
                    new OArguments(), null, map).setListener(new BaseAbstractListener(){
                @Override
                public void OnSuccessful(Object obj) {
                    // Write back the ids
                    OdooResult result = (OdooResult) obj;
                    if (result != null && result.has("result") && !result.getString("result").contains("error")) {
                        // Quant ids
                        for (int i=0; i<quantLineIds.length && i<result.getMap("result").getArray("quant_id").size(); i++) {
                            OValues quantValues = new OValues();
                            quantValues.put("id", (int) result.getMap("result").getArray("quant_id").get(i));
                            stockQuant.update(Integer.parseInt(quantLineIds[i]), quantValues);
                        }
                        // Lot 
                        OValues lotValues = new OValues();
                        lotValues.put("id", Integer.parseInt(result.getMap("result").getString("lot_id")));
                        stockProductionLot.update(newLotId, lotValues);
                    }
                }
            });
            syncUtils.callMethodOnServer();
        }
    }

}
