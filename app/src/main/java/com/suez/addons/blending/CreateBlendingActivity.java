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
import com.suez.utils.RecordUtils;
import com.suez.utils.SearchRecordsOnlineUtils;
import com.suez.utils.SuezJsonUtils;
import com.suez.utils.ToastUtil;

import java.util.ArrayList;
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
        List<ODataRow> stockQuantRecords = stockQuant.query("select * from stock_quant where lot_id=? and location_id in (select _id from stock_location where usage=?)",
                new String[]{String.valueOf(prodlotId), "internal"});
        records.addAll(ProcessingActivity.initInputQty(new RecordUtils(stockQuant).parseMany2oneRecords(stockQuantRecords,
                new String[]{"lot_id", "location_id"}, new String[]{"name", "name"})));
        initForm();
    }

    @Override
    protected void initForm() {
        wizardValues = new OValues();
        wizardValues.put("blending_location_id", false);
        wizardValues.put("destination_location_id", false);
        wizardValues.put("blending_waste_category_id", false);
        wizardValues.put("action", SuezConstants.CREATE_BLENDING_KEY);
        super.initForm();
    }

    @Override
    protected int blending(boolean finish) {
        int blendingLocationId = Integer.parseInt(blendingLocation.getValue().toString());
        int destinationLocationId = Integer.parseInt(destinationLocation.getValue().toString());
        int blendingWasteCategoryId = Integer.parseInt(blendingCategory.getValue().toString());

        OValues lotValues = new OValues();
        lotValues.put("name", "B" + ODateUtils.getDate("yyMMdd") + stockProductionLot.count("name like ?", new String[]{"B%"}) % 1000);
        lotValues.put("product_qty", RecordUtils.sumField(records, "input_qty"));
        lotValues.put("is_finished", finish);
        int newLotId = stockProductionLot.insert(lotValues);

        for (ODataRow record: records) {
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
            // New Quants
            OValues newQuantValues = new OValues();
            newQuantValues.put("lot_id", newLotId);
            newQuantValues.put("location_id", destinationLocationId);
            newQuantValues.put("qty", record.getFloat("input_qty"));
            stockQuant.insert(newQuantValues);

            wizardValues.put("quant_line_quantity", RecordUtils.getFieldString(records, "input_qty"));
            wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
            wizardValues.put("quant_line_location_ids", RecordUtils.getFieldString(records, "location_id"));
            wizardValues.put("blending_location_id", blendingLocationId);
            wizardValues.put("destination_location_id", destinationLocationId);
            wizardValues.put("blending_waste_category_id", blendingWasteCategoryId);
            wizardValues.put("new_prodlot_id", newLotId);

            wizard.insert(wizardValues);
        }
        return newLotId;
    }



    @Override
    public void onItemClick(int position) {
        clickPosition = position;
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);
        params.setMargins(100, 0, 54, 0);
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setLayoutParams(params);
        input.setTextColor(OResource.color(this, R.color.body_text_1));
        input.setText(records.get(clickPosition - 1).getString("qty"));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(input);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_change_quantity);
        builder.setView(layout);
        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                records.get(clickPosition - 1).put("input_qty", input.getText().toString());
                adapter.notifyItemChanged(clickPosition);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty() && Float.compare(Float.parseFloat(s.toString()), records.get(clickPosition - 1).getFloat("qty")) != 1) {
                    positiveButton.setEnabled(true);
                    positiveButton.setTextColor(OResource.color(CreateBlendingActivity.this, R.color.colorAccent));
                } else {
                    positiveButton.setEnabled(false);
                    positiveButton.setTextColor(OResource.color(CreateBlendingActivity.this, R.color.drawer_separator_text_color));
                }
            }
        });
    }

    @Override
    public void onItemLongClick(int position) {
        clickPosition = position;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_warning)
                .setMessage(R.string.message_confirm_remove_record)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        records.remove(clickPosition - 1);
                        adapter.notifyItemRemoved(clickPosition);
                    }
                }).create();
        dialog.show();
    }
}
