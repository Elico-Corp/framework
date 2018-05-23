package com.suez.addons.pretreatment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.OResource;
import com.suez.SuezConstants;
import com.suez.utils.CallMethodsOnlineUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-17.
 */

public class PretreatmentActivity extends ProcessingActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initToolbar(R.string.label_pretreatment);
    }

    @Override
    protected void initView() {
        super.initView();
        pretreatmentLocation.setVisibility(View.VISIBLE);
        destinationLocation.setVisibility(View.VISIBLE);
        pretreatmentType.setVisibility(View.VISIBLE);
        pretreatmentQty.setVisibility(View.VISIBLE);
        remainQty.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initForm() {
        super.initForm();
        OValues wizardValues = new OValues();
        wizardValues.put("pretreatment_location_id", false);
        wizardValues.put("destination_location_id", false);
        wizardValues.put("pretreatment_type_id", false);
        wizardValues.put("qty", 0.0f);
        wizardValues.put("remain_qty", 0.0f);
        wizardValues.put("action", SuezConstants.PRETREATMENT_KEY);
        wizardValues.put("prodlot_id", prodlot_id);
        ODataRow wizardRecord = wizard.browse(wizard.insert(wizardValues));
        pretreatmentWizardForm.initForm(wizardRecord);
        pretreatmentQty.setEditable(false);
        remainQty.setEditable(false);
        refreshQty();
    }

    @Override
    protected void performProcessing() {
        String pretreatmentLocationId = pretreatmentLocation.getValue().toString();
        String destinationLocationId = destinationLocation.getValue().toString();
        String treatmentTypeId = pretreatmentType.getValue().toString();
        String quantity = pretreatmentQty.getValue().toString();
        String remainQuantity = remainQty.getValue().toString();
        if (isNetwork) {
            OArguments args = new OArguments();
            args.add(new JSONArray().put(prodlot_id));
            HashMap<String, Object> kwargs = new HashMap();
            kwargs.put("lot_id", prodlot_id);
            kwargs.put("product_qty", Float.parseFloat(quantity));
            kwargs.put("date_planned_start", ODateUtils.getUTCDate(ODateUtils.DEFAULT_FORMAT));
            List<HashMap> quantLines  = new ArrayList<>();
            for (ODataRow record: records) {
                HashMap<String, Object> quantLine= new HashMap();
                quantLine.put("location_id", record.getInt("location_id"));
                quantLine.put("quantity", record.getFloat("input_qty"));
                quantLines.add(quantLine);
            }
            kwargs.put("quant_lines", quantLines);
            kwargs.put("pretreatment_location", stockLocation.browse(Integer.parseInt(pretreatmentLocationId)).getInt("id"));
            kwargs.put("dest_location", stockLocation.browse(Integer.parseInt(destinationLocationId)).getInt("id"));
            HashMap<String, Object> map = new HashMap();
            map.put("data", kwargs);
            map.put("action", SuezConstants.PRETREATMENT_KEY);
            CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data", new OArguments(), null, map);
            utils.callMethodOnServer();
        } else {
            ODataRow prodlot = stockProductionLot.browse(prodlot_id);
            OValues lotValues = prodlot.toValues();
            lotValues.put("product_qty", Float.parseFloat(quantity));
            lotValues.put("name", ODateUtils.getDate("yyMMdd") + stockProductionLot.count("", null));
            int newLotId = stockProductionLot.insert(lotValues);
            for (ODataRow record: records) {
                // All pretreatment
                if (record.getFloat("qty").equals(record.getFloat("input_qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", Integer.parseInt(pretreatmentLocationId));
                    stockQuant.update(record.getInt("_id"), values);
                } else { // Part pretreatment
                    // Remain
                    OValues remainValues = new OValues();
                    remainValues.put("lot_id", record.getInt("lot_id"));
                    remainValues.put("location_id", record.getInt("location_id"));
                    remainValues.put("qty", Float.parseFloat(remainQuantity));
                    stockQuant.update(record.getInt("_id"), remainValues);
                    OValues newValues = new OValues();
                    newValues.put("lot_id", record.getInt("lot_id"));
                    newValues.put("location_id", Integer.parseInt(pretreatmentLocationId));
                    newValues.put("qty", record.getFloat("input_qty"));
                    stockQuant.insert(newValues);
                }
                // New Quants with new lot
                OValues newQuantValues = new OValues();
                newQuantValues.put("lot_id", newLotId);
                newQuantValues.put("location_id", Integer.parseInt(destinationLocationId));
                newQuantValues.put("qty", record.getFloat("input_qty"));
                stockQuant.insert(newQuantValues);
            }
        }
    }
}
