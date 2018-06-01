package com.suez.addons.blending;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
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
import com.odoo.core.utils.OResource;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.adapters.CommonTextAdapter;
import com.suez.addons.models.OperationsWizard;
import com.suez.addons.models.StockLocation;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.StockQuant;
import com.suez.addons.pretreatment.ProcessingActivity;
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
 * Created by joseph on 18-5-29.
 */

public class AddBlendingActivity extends SuezActivity implements CommonTextAdapter.OnItemClickListener {
    @BindView(R.id.blending_quant_list)
    XRecyclerView blendingQuantList;
    @BindView(R.id.blending_wizard_form)
    OForm blendingWizardForm;
    @BindView(R.id.exist_blending)
    OField existBlending;

    private int prodlotId;
    private String prodlotName;
    private StockProductionLot stockProductionLot;
    private OperationsWizard wizard;
    private StockQuant stockQuant;
    private OValues wizardValues;
    private List<ODataRow> records;
    private CommonTextAdapter adapter;
    private int clickPosition;
    private List<Integer> lotIds;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_blending_activity);
        ButterKnife.bind(this);
        initToolbar(R.string.title_suez_mix_blending);
        prodlotId = getIntent().getIntExtra(SuezConstants.PRODLOT_ID_KEY, 0);
        prodlotName = getIntent().getStringExtra(SuezConstants.PRODLOT_NAME_KEY);
        stockProductionLot = new StockProductionLot(this, null);
        stockQuant = new StockQuant(this, null);
        wizard = new OperationsWizard(this, null);
        records = new ArrayList<>();
        lotIds = new ArrayList<>();
        initForm();
    }

    private void initForm() {
        adapter = new CommonTextAdapter(records, R.layout.suez_blending_quant_layout,
                new String[]{"lot_id_name", "location_id_name", "qty", "input_qty"},
                new int[]{R.id.txt_lot, R.id.txt_source_location, R.id.txt_blending_qty_available, R.id.txt_blending_qty});
        blendingQuantList.setAdapter(adapter);
        adapter.setmOnItemClickListener(this);
        existBlending.setVisibility(View.VISIBLE);
        wizardValues = new OValues();
        wizardValues.put("prodlot_id", prodlotId);
        wizardValues.put("blending_location_id", false);
        wizardValues.put("destination_location_id", false);
        wizardValues.put("action", SuezConstants.ADD_BLENDING_KEY);
        wizardValues.put("exist_location", prodlotName);
        blendingWizardForm.initForm(wizardValues.toDataRow());
        existBlending.setEditable(false);
    }

    @OnClick({R.id.btn_scan, R.id.btn_blending, R.id.btn_blending_finish})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                Intent intent = new Intent(this, ScanZbarActivity.class);
                intent.putExtra(SuezConstants.COMMON_KEY, SuezConstants.SCAN_BLENDING_KEY);
                startActivityForResult(intent, 1);
                break;
            case R.id.btn_blending:
                blending();
                break;
            case R.id.btn_blending_finish:
                blendingFinish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int resLotId = data.getIntExtra(SuezConstants.PRODLOT_ID_KEY, 0);
        if (resultCode == RESULT_OK) {
            if (lotIds.contains(resLotId)) {
                ToastUtil.toastShow(R.string.toast_existing_lot, this);
            } else if (resLotId != 0){
                lotIds.add(resLotId);
                addQuants(resLotId);
            }
        }
    }

    private void addQuants(int lotId) {
        if (isNetwork) {
            ODomain domain = new ODomain();
            domain.add("lot_id", "=", lotId);
            domain.add("location_id.usage", "=", "internal");
            BaseAbstractListener listener = new BaseAbstractListener(){
                @Override
                public void OnSuccessful(List<ODataRow> listRow) {
                    records.addAll(ProcessingActivity.initInputQty(SuezJsonUtils.parseRecords(stockQuant, listRow)));
                    adapter.notifyDataSetChanged();
                }
            };
            SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(stockQuant, domain).setListener(listener);
            utils.searchRecordsOnServer();
        } else {
            List<ODataRow> newQuantRows = stockQuant.select(null, "lot_id = ? and location_id in (select _id from stock_location where usage = ?",
                    new String[]{String.valueOf(lotId), "internal"});
            records.addAll(ProcessingActivity.initInputQty(new RecordUtils(stockQuant).parseMany2oneRecords(newQuantRows, new String[]{"lot_id", "location_id"},
                    new String[]{"name", "name"})));
            adapter.notifyDataSetChanged();
        }
    }

    private void blending() {
        if (isNetwork) {

        } else {
            for (ODataRow record: records) {
                Integer targetLocationId = stockQuant.browse(null, "lot_id=?", new String[]{String.valueOf(prodlotId)}).getInt("_id");
                int blendingLocationId = new StockLocation(this, null).browse(null, "is_blending=?", new String[]{"true"}).getInt("_id");
                if (record.getFloat("qty").equals(record.getFloat("input_qty"))) {
                    OValues values = new OValues();
                    values.put("location_id", blendingLocationId);
                    stockQuant.update(record.getInt("_id"), values);
                } else { // Part pretreatment
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

                wizardValues.put("quant_line_quantity", RecordUtils.getFieldString(records, "input_qty"));
                wizardValues.put("quant_line_ids", RecordUtils.getFieldString(records, "_id"));
                wizardValues.put("quant_line_location_ids", RecordUtils.getFieldString(records, "location_id"));

                wizard.insert(wizardValues);
            }
        }
    }

    private void blendingFinish() {
        blending();
        OValues oValues = new OValues();
        oValues.put("is_finished", true);
        stockProductionLot.update(prodlotId, oValues);
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
                    positiveButton.setTextColor(OResource.color(AddBlendingActivity.this, R.color.colorAccent));
                } else {
                    positiveButton.setEnabled(false);
                    positiveButton.setTextColor(OResource.color(AddBlendingActivity.this, R.color.drawer_separator_text_color));
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
