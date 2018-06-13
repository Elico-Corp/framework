package com.suez.addons.processing;

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
import com.suez.utils.RecordUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-6-4.
 */

public class WacMoveActivity extends ProcessingActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initToolbar(R.string.label_wac_move);
    }

    @Override
    protected void initView() {
        super.initView();
        destinationLocation.setVisibility(View.VISIBLE);
        remainQty.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initForm() {
        wizardValues = new OValues();
        wizardValues.put("destination_location_id", false);
        wizardValues.put("qty", 0.0f);
        wizardValues.put("remain_qty", 0.0f);
        wizardValues.put("action", SuezConstants.WAC_MOVE_KEY);
        wizardValues.put("prodlot_id", prodlot_id);
        super.initForm();
    }

    @Override
    protected void performProcessing() {
        int destinationLocationId = Integer.parseInt(destinationLocation.getValue().toString());
        Float quantity = records.get(0).getFloat("input_qty");
        Float remainQuantity = Float.parseFloat(remainQty.getValue().toString());

        if (isNetwork) {
            HashMap<String, Object> kwargs = new HashMap<>();
            kwargs.put("lot_id", prodlot_id);
            kwargs.put("quantity", quantity);
            kwargs.put("available_quantity", records.get(0).getFloat("qty"));
            // FIXME: 18-6-12
            kwargs.put("quant_id", quant_id);
//            List<HashMap> quantLines  = new ArrayList<>();
//            for (ODataRow record: records) {
//                HashMap<String, Object> quantLine= new HashMap<>();
//                quantLine.put("location_id", record.getInt("location_id"));
//                quantLine.put("quantity", record.getFloat("input_qty"));
//                quantLines.add(quantLine);
//            }
//            kwargs.put("quant_lines", quantLines);
            kwargs.put("location_dest_id", stockLocation.browse(destinationLocationId).getInt("id"));
            HashMap<String, Object> map = new HashMap<>();
            map.put("data", kwargs);
            map.put("action", SuezConstants.WAC_MOVE_KEY);
            CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data", new OArguments(), null, map);
            utils.callMethodOnServer();
        } else {
//            for (ODataRow record: records) {
                // All processing
            ODataRow record = records.get(0);
                if (record.getFloat("qty").equals(record.getFloat("input_qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", destinationLocationId);
                    stockQuant.update(record.getInt("_id"), values);
                } else { // Part processing
                    // Remain
                    OValues remainValues = new OValues();
                    remainValues.put("lot_id", record.getInt("lot_id"));
                    remainValues.put("location_id", record.getInt("location_id"));
                    remainValues.put("qty", record.getFloat("qty") - record.getFloat("input_qty"));
                    stockQuant.update(record.getInt("_id"), remainValues);
                    OValues newValues = new OValues();
                    newValues.put("lot_id", record.getInt("lot_id"));
                    newValues.put("location_id", destinationLocationId);
                    newValues.put("qty", record.getFloat("input_qty"));
                    stockQuant.insert(newValues);
                }

                // Create the wizard record
                wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
//                wizardValues.put("quant_line_location_ids", RecordUtils.getFieldString(records, "location_id"));
                wizardValues.put("destination_location_id", destinationLocationId);
                wizardValues.put("qty", quantity);
                wizardValues.put("remain_qty", remainQuantity);

                wizard.insert(wizardValues);
//            }
        }
    }
}