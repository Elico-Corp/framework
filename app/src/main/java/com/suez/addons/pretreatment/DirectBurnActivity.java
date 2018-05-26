package com.suez.addons.pretreatment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.utils.RecordUtils;

/**
 * Created by joseph on 18-5-25.
 */

public class DirectBurnActivity extends ProcessingActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initToolbar(R.string.label_direct_burn);
    }

    @Override
    protected void initView() {
        super.initView();
        pretreatmentLocation.setVisibility(View.VISIBLE);
        pretreatmentQty.setVisibility(View.VISIBLE);
        remainQty.setVisibility(View.VISIBLE);
    }

    @Override
    protected void initForm() {
        wizardValues = new OValues();
        wizardValues.put("pretreatment_location_id", false);
        wizardValues.put("qty", 0.00f);
        wizardValues.put("remain_qty", 0.00f);
        wizardValues.put("action", SuezConstants.DIRECT_BURN_KEY);
        super.initForm();
    }

    @Override
    protected void performProcessing() {
        int pretreatmentLocationId = Integer.parseInt(pretreatmentLocation.getValue().toString());
        float qty = Float.parseFloat(pretreatmentQty.getValue().toString());
        float remainQuantity = Float.parseFloat(remainQty.getValue().toString());
        if (isNetwork) {

        } else {
            for (ODataRow record : records) {
                if (record.getFloat("qty").equals(record.getFloat("input_qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", pretreatmentLocationId);
                    stockQuant.update(record.getInt("_id"), values);
                } else {
                    OValues remainValues = new OValues();
                    remainValues.put("lot_id", record.getInt("lot_id"));
                    remainValues.put("location_id", record.getInt("location_id"));
                    remainValues.put("qty", remainQuantity);
                    stockQuant.update(record.getInt("_id"), remainValues);
                    OValues newValues = new OValues();
                    newValues.put("lot_id", record.getInt("lot_id"));
                    newValues.put("location_id", pretreatmentLocationId);
                    newValues.put("qty", record.getFloat("input_qty"));
                    stockQuant.insert(newValues);
                }
            }
        }

        wizardValues.put("quant_line_quantity", RecordUtils.getFieldString(records, "input_qty"));
        wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
        wizardValues.put("quant_line_location_ids", RecordUtils.getFieldString(records, "location_id"));
        wizardValues.put("pretreatment_location_id", pretreatmentLocationId);
        wizardValues.put("qty", qty);
        wizardValues.put("remain_qty", remainQuantity);
    }
}
