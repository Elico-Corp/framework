package com.suez.addons.processing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.utils.ODateUtils;
import com.suez.SuezConstants;
import com.suez.utils.CallMethodsOnlineUtils;
import com.suez.utils.RecordUtils;
import com.suez.utils.ToastUtil;

import org.json.JSONArray;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-20.
 */

public class RepackingActivity extends ProcessingActivity {
    private static final String TAG = RepackingActivity.class.getSimpleName();

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
        packagingNumber.setVisibility(View.VISIBLE);
        remainQty.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initForm() {
        wizardValues = new OValues();
        wizardValues.put("repacking_location_id", 0);
        wizardValues.put("destination_location_id", 0);
        wizardValues.put("package_id", 0);
        wizardValues.put("qty", 0.00f);
        wizardValues.put("package_number", 0);
        wizardValues.put("remain_qty", 0.00f);
        wizardValues.put("prodlot_id", prodlot_id);
        wizardValues.put("action", SuezConstants.REPACKING_KEY);

        super.initForm();
    }

    @Override
    protected void performProcessing() {
        OValues inputValues = pretreatmentWizardForm.getValues();
        int packageId = Integer.parseInt(packagingId.getValue().toString());
        Integer packageNumber = Integer.parseInt(packagingNumber.getValue().toString());
        Float repackingQty = records.get(0).getFloat("input_qty");
        Float remainQuantity = Float.parseFloat(remainQty.getValue().toString());

        if (isNetwork) {
            HashMap<String, Object> kwargs = new HashMap<>();
            kwargs.put("lot_id", prodlot_id);
            kwargs.put("quant_id", quant_id);
            kwargs.put("available_quantity", records.get(0).getFloat("qty"));
            kwargs.put("source_location_id", records.get(0).getInt("location_id"));
            kwargs.put("package_number", packageNumber);
            kwargs.put("quantity", repackingQty);
//            kwargs.put("date_planned_start", ODateUtils.getUTCDate(ODateUtils.DEFAULT_FORMAT));
//            List<HashMap> quantLines  = new ArrayList<>();
//            for (ODataRow record: records) {
//                HashMap<String, Object> quantLine= new HashMap<>();
//                quantLine.put("location_id", record.getInt("location_id"));
//                quantLine.put("quantity", record.getFloat("input_qty"));
//                quantLines.add(quantLine);
//            }
//            kwargs.put("quant_lines", quantLines);
            kwargs.put("repacking_location_id", stockLocation.browse(inputValues.getInt("repacking_location_id")).getInt("id"));
            kwargs.put("location_dest_id", stockLocation.browse(inputValues.getInt("destination_location_id")).getInt("id"));
            HashMap<String, Object> map = new HashMap<>();
            map.put("data", kwargs);
            map.put("action", SuezConstants.REPACKING_KEY);
            BaseAbstractListener listener = new BaseAbstractListener() {
                @Override
                public void OnSuccessful(Object obj) {
                    postProcessing(obj);
                }
            };
            CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data", new OArguments(), null, map)
                    .setListener(listener);
            utils.callMethodOnServer();
        } else {
            ODataRow prodlot = stockProductionLot.browse(prodlot_id);
            OValues prodlotValues = prodlot.toValues();
            prodlotValues.removeKey("_id");
            prodlotValues.removeKey("quant_ids");
            prodlotValues.removeKey("id");
            String[] newLotIds = new String[packageNumber];
            List<Integer> newQuantIds = new ArrayList<>();
            Float sum = 0.000f;
            // New lot ids and quant ids
            int lotCount = stockProductionLot.count("name like ?", new String[]{prodlot.getString("name").split("-")[0] + "-%"}) + 1;
            for (int i=0; i<packageNumber-1; i++) {
                Float packagingQty = new BigDecimal(repackingQty / packageNumber).setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
                prodlotValues.put("product_qty", packagingQty);
                prodlotValues.put("name", prodlot.getString("name").split("-")[0] + "-" + lotCount);
                int newLotId = stockProductionLot.insert(prodlotValues);
                sum += packagingQty;
                lotCount += 1;
                newLotIds[i] = String.valueOf(newLotId);
                OValues newQuantValues = new OValues();
                newQuantValues.put("lot_id", newLotId);
                newQuantValues.put("location_id", inputValues.getInt("destination_location_id"));
                newQuantValues.put("qty", packagingQty);
                int id = stockQuant.insert(newQuantValues);
                newQuantIds.add(id);
            }
            prodlotValues.put("product_qty", repackingQty - sum);
            int lastLotId = stockProductionLot.insert(prodlotValues);
            newLotIds[packageNumber-1] = String.valueOf(lastLotId);
            // Stock Quants
//            for (ODataRow record: records) {
                // No remaining
            ODataRow record = records.get(0);
                if (record.getFloat("input_qty").equals(record.getFloat("qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", inputValues.getInt("repacking_location_id"));
                    stockQuant.update(record.getInt("_id"), values);
                } else {
                    OValues remainValues = new OValues();
                    remainValues.put("lot_id", record.getInt("lot_id"));
                    remainValues.put("location_id", record.getInt("location_id"));
                    remainValues.put("qty", remainQuantity);
                    stockQuant.update(record.getInt("_id"), remainValues);
                    OValues newValues = new OValues();
                    newValues.put("lot_id", record.getInt("lot_id"));
                    newValues.put("location_id", inputValues.getInt("repacking_location_id"));
                    newValues.put("qty", record.getFloat("input_qty"));
                    stockQuant.insert(newValues);
                }
//            }

//            wizardValues.put("quant_line_quantity", RecordUtils.getFieldString(records, "input_qty"));
            wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
            wizardValues.put("new_quant_ids", RecordUtils.getArrayString(newQuantIds.toArray()));
            wizardValues.put("repacking_location_id", inputValues.getInt("repacking_location_id"));
            wizardValues.put("destination_location_id", inputValues.getInt("destination_location_id"));
            wizardValues.put("qty", repackingQty);
            wizardValues.put("remain_qty", remainQuantity);
            wizardValues.put("package_id", packageId);
            wizardValues.put("package_number", packageNumber);
            wizardValues.put("new_prodlot_ids", RecordUtils.getArrayString(newLotIds));

            super.performProcessing();
        }
    }
}
