package com.suez.addons.blending;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import odoo.controls.OField;
import odoo.controls.OForm;

/**
 * Created by joseph on 18-5-31.
 */

public class CreateBlendingActivity extends BlendingActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        if (isNetwork) {
            initDataOnline();
        } else {
            initDataOffline();
        }
    }

    private void initView() {
        blendingLocation.setVisibility(View.VISIBLE);
        destinationLocation.setVisibility(View.VISIBLE);
        blendingCategory.setVisibility(View.VISIBLE);
    }

    private void initDataOnline() {
        ODomain domain = new ODomain();
        domain.add("lot_id", "=", prodlotId);
        domain.add("location_id.usage", "=", "internal");
        BaseAbstractListener listener = new BaseAbstractListener(){
            @Override
            public void OnSuccessful(List<ODataRow> listRow) {
                records.addAll(ProcessingActivity.initInputQty(SuezJsonUtils.parseRecords(stockQuant, listRow)));
                initForm();
            }
        };
        SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(stockQuant, domain).setListener(listener);
        utils.searchRecordsOnServer();
    }

    private void initDataOffline() {
        List<ODataRow> stockQuantRecords = stockQuant.select(null, "lot_id=? and location_id in " + locations.toString().replace("[", "(").replace("]", ")"),
                new String[]{String.valueOf(prodlotId)});
        records.addAll(ProcessingActivity.initInputQty(new RecordUtils(stockQuant).parseMany2oneRecords(stockQuantRecords,
                new String[]{"lot_id", "location_id"}, new String[]{"name", "name"})));
        initForm();
    }

    @Override
    protected void initForm() {
        wizardValues = new OValues();
        wizardValues.put("blending_location_id", 0);
        wizardValues.put("destination_location_id", 0);
        wizardValues.put("blending_waste_category_id", 0);
        wizardValues.put("action", SuezConstants.CREATE_BLENDING_KEY);
        super.initForm();
    }

    @Override
    protected void blending(boolean finish) {
        super.blending(finish);
        OValues inputValues = blendingWizardForm.getValues();
        int blendingWasteCategoryId = Integer.parseInt(blendingCategory.getValue().toString());
        if (isNetwork) {
            HashMap<String, Object> kwargs = new HashMap<>();
            kwargs.put("lot_id", lotIds.get(0));
            kwargs.put("blending_location_id", stockLocation.browse(inputValues.getInt("blending_location_id")).getInt("id"));
            kwargs.put("quantity", RecordUtils.sumField(records, "input_qty"));
            kwargs.put("location_dest_id", stockLocation.browse(inputValues.getInt("destination_location_id")).getInt("id"));
            kwargs.put("category_id", blendingWasteCategoryId);
            kwargs.put("is_finish", finish);
            List<HashMap> quantLines  = new ArrayList<>();
            for (ODataRow record: records) {
                HashMap<String, Object> quantLine= new HashMap<>();
                // FIXME: 18-6-12
                quantLine.put("quant_id", record.getInt("id"));
                quantLine.put("quantity", record.getFloat("input_qty"));
                quantLines.add(quantLine);
            }
            kwargs.put("quant_lines", quantLines);
            HashMap<String, Object> map = new HashMap<>();
            map.put("data", kwargs);
            map.put("action", SuezConstants.CREATE_BLENDING_KEY);
            BaseAbstractListener listener = new BaseAbstractListener() {
                @Override
                public void OnSuccessful(Object obj) {
                    postBlending(obj);
                }
            };
            CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data",
                    new OArguments(), null, map).setListener(listener);
            utils.callMethodOnServer();
        } else {
            OValues lotValues = new OValues();
            String prefix = "B" + ODateUtils.getDate("yyMMdd");
            lotValues.put("name", prefix + new DecimalFormat("000").format((stockProductionLot.count("name like ?", new String[]{prefix + "%"}) + 1) % 1000));
            lotValues.put("product_qty", RecordUtils.sumField(records, "input_qty"));
            lotValues.put("is_finished", finish);
            int newLotId = stockProductionLot.insert(lotValues);

            for (ODataRow record : records) {
                if (record.getFloat("qty").equals(record.getFloat("input_qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", inputValues.getInt("blending_location_id"));
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
                    newValues.put("location_id", inputValues.getInt("blending_location_id"));
                    newValues.put("qty", record.getFloat("input_qty"));
                    stockQuant.insert(newValues);
                }
                // New Quants
                OValues newQuantValues = new OValues();
                newQuantValues.put("lot_id", newLotId);
                newQuantValues.put("location_id", inputValues.getInt("destination_location_id"));
                newQuantValues.put("qty", record.getFloat("input_qty"));
                stockQuant.insert(newQuantValues);
            }
            wizardValues.put("qty", RecordUtils.sumField(records, "input_qty"));
            wizardValues.put("prodlot_id", lotIds.get(0));
            wizardValues.put("quant_line_qty", RecordUtils.getFieldString(records, "input_qty"));
            wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
            wizardValues.put("quant_line_location_ids", RecordUtils.getFieldString(records, "location_id"));
            wizardValues.put("blending_location_id", inputValues.getInt("blending_location_id"));
            wizardValues.put("destination_location_id", inputValues.getInt("destination_location_id"));
            wizardValues.put("blending_waste_category_id", blendingWasteCategoryId);
            wizardValues.put("new_prodlot_ids", String.valueOf(newLotId));
            wizardValues.put("is_finished", finish);
            createAction();
        }
    }

    @Override
    protected void createAction() {
        wizard.insert(wizardValues);
        super.createAction();
        finish();
    }
}
