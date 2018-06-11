package com.suez.addons.processing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.utils.ODateUtils;
import com.suez.SuezConstants;
import com.suez.utils.CallMethodsOnlineUtils;
import com.suez.utils.RecordUtils;

import org.json.JSONArray;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-20.
 */

public class RepackingActivity extends ProcessingActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initToolbar(R.string.label_repacking);
    }

    @Override
    protected void initView() {
        super.initView();
        repackingLocation.setVisibility(View.VISIBLE);
        destinationLocation.setVisibility(View.VISIBLE);
        packagingId.setVisibility(View.VISIBLE);
        pretreatmentQty.setVisibility(View.VISIBLE);
        packagingNumber.setVisibility(View.VISIBLE);
        remainQty.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initForm() {
        wizardValues = new OValues();
        wizardValues.put("repacking_location_id", false);
        wizardValues.put("destination_location_id", false);
        wizardValues.put("package_id", false);
        wizardValues.put("qty", 0.00f);
        wizardValues.put("package_number", 0);
        wizardValues.put("remain_qty", 0.00f);
        wizardValues.put("action", SuezConstants.REPACKING_KEY);

        super.initForm();
    }

    @Override
    protected void performProcessing() {
        int repackingLocationId = Integer.parseInt(repackingLocation.getValue().toString());
        int destinationLocationId = Integer.parseInt(destinationLocation.getValue().toString());
        int packageId = Integer.parseInt(packagingId.getValue().toString());
        Integer packageNumber = Integer.parseInt(packagingNumber.getValue().toString());
        Float repackingQty = Float.parseFloat(pretreatmentQty.getValue().toString());
        Float remainQuantity = Float.parseFloat(remainQty.getValue().toString());

        if (isNetwork) {
            HashMap<String, Object> kwargs = new HashMap<>();
            kwargs.put("lot_id", prodlot_id);
            kwargs.put("product_qty", RecordUtils.sumField(records, "input_qty"));
            kwargs.put("date_planned_start", ODateUtils.getUTCDate(ODateUtils.DEFAULT_FORMAT));
            List<HashMap> quantLines  = new ArrayList<>();
            for (ODataRow record: records) {
                HashMap<String, Object> quantLine= new HashMap<>();
                quantLine.put("location_id", record.getInt("location_id"));
                quantLine.put("quantity", record.getFloat("input_qty"));
                quantLines.add(quantLine);
            }
            kwargs.put("quant_lines", quantLines);
            kwargs.put("repacking_location_id", stockLocation.browse(repackingLocationId).getInt("id"));
            kwargs.put("dest_location", stockLocation.browse(destinationLocationId).getInt("id"));
            HashMap<String, Object> map = new HashMap<>();
            map.put("data", kwargs);
            map.put("action", SuezConstants.PRETREATMENT_KEY);
            CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data", new OArguments(), null, map);
            utils.callMethodOnServer();
        } else {
            ODataRow prodlot = stockProductionLot.browse(prodlot_id);
            OValues prodlotValues = prodlot.toValues();
            String[] newLotIds = new String[packageNumber];
            Float sum = 0.000f;
            // New lot ids and quant ids
            for (int i=1; i<packageNumber; i++) {
                Float packagingQty = new BigDecimal(repackingQty / packageNumber).setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
                prodlotValues.put("product_qty", packagingQty);
                prodlotValues.put("name", prodlot.getString("name").split("-")[0] + "-" + stockProductionLot.count("name like ?", new String[]{prodlot.getString("name").split("-")[0] + "-%"}));
                sum += packagingQty;
                int newLotId = stockProductionLot.insert(prodlotValues);
                newLotIds[i-1] = String.valueOf(newLotId);
                OValues newQuantValues = new OValues();
                newQuantValues.put("lot_id", newLotId);
                newQuantValues.put("location_id", destinationLocationId);
                newQuantValues.put("qty", packagingQty);
                stockQuant.insert(newQuantValues);
            }
            prodlotValues.put("product_qty", repackingQty - sum);
            int lastLotId = stockProductionLot.insert(prodlotValues);
            newLotIds[packageNumber-1] = String.valueOf(lastLotId);
            // Stock Quants
            for (ODataRow record: records) {
                // No remaining
                if (record.getFloat("input_qty").equals(record.getFloat("qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", repackingLocationId);
                    stockQuant.update(record.getInt("_id"), values);
                } else {
                    OValues remainValues = new OValues();
                    remainValues.put("lot_id", record.getInt("lot_id"));
                    remainValues.put("location_id", record.getInt("location_id"));
                    remainValues.put("qty", remainQuantity);
                    stockQuant.update(record.getInt("_id"), remainValues);
                    OValues newValues = new OValues();
                    newValues.put("lot_id", record.getInt("lot_id"));
                    newValues.put("location_id", repackingLocationId);
                    newValues.put("qty", record.getFloat("input_qty"));
                    stockQuant.insert(newValues);
                }
            }

            wizardValues.put("quant_line_quantity", RecordUtils.getFieldString(records, "input_qty"));
            wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
            wizardValues.put("quant_line_location_ids", RecordUtils.getFieldString(records, "location_id"));
            wizardValues.put("repacking_location_id", repackingLocationId);
            wizardValues.put("destination_location_id", destinationLocationId);
            wizardValues.put("qty", repackingQty);
            wizardValues.put("remain_qty", remainQuantity);
            wizardValues.put("new_prodlot_ids", RecordUtils.getArrayString(newLotIds));

            wizard.insert(wizardValues);
        }
    }
}
