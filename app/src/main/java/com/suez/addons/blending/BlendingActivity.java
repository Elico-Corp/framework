package com.suez.addons.blending;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.internal.LinkedTreeMap;
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
import com.suez.addons.processing.ProcessingActivity;
import com.suez.addons.scan.ScanZbarActivity;
import com.suez.utils.LogUtils;
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
 * Created by joseph on 18-6-5.
 */

public class BlendingActivity extends SuezActivity implements CommonTextAdapter.OnItemClickListener {
    @BindView(R.id.blending_quant_list)
    XRecyclerView blendingQuantList;
    @BindView(R.id.blending_quant_lines)
    LinearLayout blendingQuantLines;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.exist_blending)
    OField existBlending;
    @BindView(R.id.blending_location)
    OField blendingLocation;
    @BindView(R.id.destination_location)
    OField destinationLocation;
    @BindView(R.id.blending_category)
    OField blendingCategory;
    @BindView(R.id.pretreatment_qty)
    OField pretreatmentQty;
    @BindView(R.id.remain_qty)
    OField remainQty;
    @BindView(R.id.blending_wizard_form)
    OForm blendingWizardForm;
    @BindView(R.id.btn_scan)
    Button btnScan;
    @BindView(R.id.btn_blending)
    Button btnBlending;
    @BindView(R.id.btn_blending_finish)
    Button btnBlendingFinish;

    private static final String TAG = BlendingActivity.class.getSimpleName();
    protected int prodlotId;
    protected String prodlotName;
    protected StockProductionLot stockProductionLot;
    protected StockLocation stockLocation;
    protected OperationsWizard wizard;
    protected StockQuant stockQuant;
    protected OValues wizardValues;
    protected List<ODataRow> records;
    protected CommonTextAdapter adapter;
    protected int clickPosition;
    protected List<Integer> lotIds;
    protected List<Object> locations;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_blending_activity);
        ButterKnife.bind(this);
        initToolbar(R.string.title_suez_mix_blending);
        prodlotId = getIntent().getIntExtra(SuezConstants.PRODLOT_ID_KEY, 0);
        prodlotName = getIntent().getStringExtra(SuezConstants.PRODLOT_NAME_KEY);
        stockProductionLot = new StockProductionLot(this, null);
        stockLocation = new StockLocation(this, null);
        stockQuant = new StockQuant(this, null);
        wizard = new OperationsWizard(this, null);
        records = new ArrayList<>();
        lotIds = new ArrayList<>();
        lotIds.add(prodlotId);
        locations = RecordUtils.getFieldList(stockLocation.select(new String[]{"_id"}, "usage = ?", new String[]{"internal"}), "_id");
    }

    protected void initForm() {
        adapter = new CommonTextAdapter(records, R.layout.suez_blending_quant_layout,
                new String[]{"lot_id_name", "location_id_name", "qty", "input_qty"},
                new int[]{R.id.txt_lot, R.id.txt_source_location, R.id.txt_blending_qty_available, R.id.txt_blending_qty});
        blendingQuantList.setLayoutManager(new LinearLayoutManager(this));
        blendingQuantList.setPullRefreshEnabled(false);
        blendingQuantList.setLoadingMoreEnabled(false);
        blendingQuantList.setAdapter(adapter);
        adapter.setmOnItemClickListener(this);
        wizardValues.put("remain_qty", 0.0f);
        blendingWizardForm.initForm(wizardValues.toDataRow());
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
                if (validateInput(blendingWizardForm)) {
                    blending(false);
                }
                break;
            case R.id.btn_blending_finish:
                if (validateInput(blendingWizardForm)) {
                    blending(true);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
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
            List<ODataRow> newQuantRows = stockQuant.select(null, "lot_id=? and location_id in " + locations.toString().replace("[", "(").replace("]", ")"),
                    new String[]{String.valueOf(lotId)});
            records.addAll(ProcessingActivity.initInputQty(new RecordUtils(stockQuant).parseMany2oneRecords(newQuantRows, new String[]{"lot_id", "location_id"},
                    new String[]{"name", "name"})));
            adapter.notifyDataSetChanged();
        }
    }

    protected void blending(boolean finish) {
        if (records.size() == 0) {
            ToastUtil.toastShow(R.string.toast_no_data, this);
            return;
        }
    }

    protected void postBlending(Object response) {
        if (response == null) {
            alertWarning(R.string.message_response_null);
        } else if (response instanceof String) {
            ToastUtil.toastShow(R.string.toast_processing_failed, this);
            LogUtils.e(TAG, String.valueOf(response));
        } else if (response instanceof ArrayList || response instanceof LinkedTreeMap) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.toast_successful)
                    .setMessage(R.string.message_processing_success)
                    .setCancelable(false)
                    .setNegativeButton(R.string.label_close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    }).create();
            dialog.show();
        }
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
                    positiveButton.setTextColor(OResource.color(BlendingActivity.this, R.color.colorAccent));
                } else {
                    positiveButton.setEnabled(false);
                    positiveButton.setTextColor(OResource.color(BlendingActivity.this, R.color.drawer_separator_text_color));
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

    @Override
    protected void createAction() {
        int id = wizard.insert(wizardValues);
        for (String quantId: wizardValues.getString("new_quant_ids").split(",")) {
            OValues values = new OValues();
            values.put("wizard_id", id);
            stockQuant.update(Integer.parseInt(quantId), values);
        }
        super.createAction();
        finish();
    }
}
