package com.suez.addons.blending;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.OResource;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.adapters.CommonTextAdapter;
import com.suez.addons.models.OperationsWizard;
import com.suez.addons.models.StockLocation;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.StockQuant;
import com.suez.addons.processing.ProcessingActivity;
import com.suez.addons.scan.ScanZbarActivity;
import com.suez.utils.CallMethodsOnlineUtils;
import com.suez.utils.RecordUtils;
import com.suez.utils.SearchRecordsOnlineUtils;
import com.suez.utils.SuezJsonUtils;
import com.suez.utils.ToastUtil;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import odoo.controls.OField;
import odoo.controls.OForm;

/**
 * Created by joseph on 18-5-29.
 */

public class AddBlendingActivity extends BlendingActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initForm();
    }

    @Override
    protected void initForm() {
        wizardValues = new OValues();
        wizardValues.put("prodlot_id", prodlotId);
        wizardValues.put("blending_location_id", false);
        wizardValues.put("destination_location_id", false);
        wizardValues.put("action", SuezConstants.ADD_BLENDING_KEY);
        wizardValues.put("exist_location", prodlotName);
        existBlending.setVisibility(View.VISIBLE);
        super.initForm();
        existBlending.setEditable(false);
    }

    @Override
    protected void blending(boolean finish) {
        if (isNetwork) {
            HashMap<String, Object> kwargs = new HashMap<>();
            kwargs.put("lot_id", prodlotId);
            kwargs.put("is_finish", finish);
            kwargs.put("quantity", RecordUtils.sumField(records, "input_qty"));
            List<HashMap> quantLines  = new ArrayList<>();
            for (ODataRow record: records) {
                HashMap<String, Object> quantLine= new HashMap<>();
                quantLine.put("location_id", record.getInt("location_id"));
                quantLine.put("quantity", record.getFloat("input_qty"));
                quantLines.add(quantLine);
            }
            kwargs.put("quant_lines", quantLines);
            HashMap<String, Object> map = new HashMap<>();
            map.put("data", kwargs);
            map.put("action", SuezConstants.ADD_BLENDING_KEY);
            CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data", new OArguments(), null, map);
            utils.callMethodOnServer();
        } else {
            for (ODataRow record: records) {
                Integer targetLocationId = stockQuant.browse(null, "lot_id=?", new String[]{String.valueOf(prodlotId)}).getInt("_id");
                int blendingLocationId = new StockLocation(this, null).browse(null, "is_blending=?", new String[]{"true"}).getInt("_id");
                if (record.getFloat("qty").equals(record.getFloat("input_qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", blendingLocationId);
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
                    newValues.put("location_id", blendingLocationId);
                    newValues.put("qty", record.getFloat("input_qty"));
                    stockQuant.insert(newValues);
                }
                OValues newQuantValues = new OValues();
                newQuantValues.put("lot_id", prodlotId);
                newQuantValues.put("location_id", targetLocationId);
                newQuantValues.put("qty", record.getFloat("input_qty"));
                stockQuant.insert(newQuantValues);

                OValues lotValues = new OValues();
                lotValues.put("is_finished", finish);
                stockProductionLot.update(prodlotId, lotValues);

                wizardValues.put("qty", RecordUtils.sumField(records, "input_qty"));
                wizardValues.put("quant_line_quantity", RecordUtils.getFieldString(records, "input_qty"));
                wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
                wizardValues.put("quant_line_location_ids", RecordUtils.getFieldString(records, "location_id"));

                wizard.insert(wizardValues);
            }
        }
    }
}
