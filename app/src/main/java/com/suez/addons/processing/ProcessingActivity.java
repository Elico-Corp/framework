package com.suez.addons.processing;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.RelativeLayout;

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
import com.suez.utils.RecordUtils;
import com.suez.utils.SearchRecordsOnlineUtils;
import com.suez.utils.SuezJsonUtils;
import com.suez.utils.ToastUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import odoo.controls.OField;
import odoo.controls.OForm;

import static com.suez.utils.RecordUtils.sumField;

/**
 * Created by joseph on 18-5-17.
 */

public class ProcessingActivity extends SuezActivity implements CommonTextAdapter.OnItemClickListener {
    @BindView(R.id.stock_quant_list)
    XRecyclerView stockQuantList;
    @BindView(R.id.stock_quant_lines)
    LinearLayout stockQuantLines;
    @BindView(R.id.pretreatment_location)
    OField pretreatmentLocation;
    @BindView(R.id.repacking_location)
    OField repackingLocation;
    @BindView(R.id.destination_location)
    OField destinationLocation;
    @BindView(R.id.packaging_id)
    OField packagingId;
    @BindView(R.id.pretreatment_type)
    OField pretreatmentType;
    @BindView(R.id.pretreatment_qty)
    OField pretreatmentQty;
    @BindView(R.id.packaging_number)
    OField packagingNumber;
    @BindView(R.id.pretreatment_wizard_form)
    OForm pretreatmentWizardForm;
    @BindView(R.id.remain_qty)
    OField remainQty;

    protected int prodlot_id;
    protected int quant_id;
    protected int clickPosition;
    protected StockQuant stockQuant;
    protected OperationsWizard wizard;
    protected StockProductionLot stockProductionLot;
    protected StockLocation stockLocation;
    protected CommonTextAdapter adapter;
    protected List<ODataRow> records;
    protected OValues wizardValues;
    protected ODataRow lot;

    @BindView(R.id.btn_confirm)
    Button btnConfirm;
    @BindView(R.id.relayoutList)
    RelativeLayout relayoutList;
    @BindView(R.id.btn_cancel)
    Button btnCancel;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_pretreatment_activity);
        ButterKnife.bind(this);
        prodlot_id = getIntent().getIntExtra(SuezConstants.PRODLOT_ID_KEY, 0);
        quant_id = getIntent().getIntExtra(SuezConstants.STOCK_QUANT_ID_KEY, 0);
        stockQuant = new StockQuant(this, null);
        stockProductionLot = new StockProductionLot(this, null);
        wizard = new OperationsWizard(this, null);
        stockLocation = new StockLocation(this, null);
        initView();
        if (isNetwork) {
            initDataOnline();
        } else {
            initDataOffline();
        }
    }

    protected void initView() {
        stockQuantList.setLayoutManager(new LinearLayoutManager(this));
        stockQuantList.setLoadingMoreEnabled(false);
        stockQuantList.setPullRefreshEnabled(false);
    }

    protected void initDataOnline() {
        ODomain stockQuantDomain = new ODomain();
        stockQuantDomain.add("id", "=", quant_id);
        BaseAbstractListener listener = new BaseAbstractListener() {
            @Override
            public void OnSuccessful(List<ODataRow> listRow) {
                if (listRow != null && listRow.size() > 0) {
                    records = initInputQty(SuezJsonUtils.parseRecords(stockQuant, listRow));
                    ODomain lotDomain = new ODomain();
                    lotDomain.add("id", "=", prodlot_id);
                    BaseAbstractListener lotListener = new BaseAbstractListener() {
                        @Override
                        public void OnSuccessful(List<ODataRow> listRow) {
                            lot = SuezJsonUtils.parseRecords(stockProductionLot, listRow).get(0);
                            initForm();
                        }
                    };
                    SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(stockProductionLot, lotDomain)
                            .setListener(lotListener);
                    utils.searchRecordsOnServer();
                } else {
                    alertWarning();
                }
            }
        };
        SearchRecordsOnlineUtils searchUtils = new SearchRecordsOnlineUtils(stockQuant, stockQuantDomain).setListener(listener);
        searchUtils.searchRecordsOnServer();
    }

    protected void initDataOffline() {
        List<ODataRow> stockQuantRecords = stockQuant.select(null, "id = ?", new String[]{String.valueOf(quant_id)});
        if (stockQuantRecords == null || stockQuantRecords.size() == 0) {
            alertWarning();
        }
        records = initInputQty(new RecordUtils(stockQuant).parseMany2oneRecords(stockQuantRecords, new String[]{"location_id"}, new String[]{"name"}));
        lot = new RecordUtils(stockProductionLot).parseMany2oneRecords(stockProductionLot.browse(prodlot_id),
                new String[]{"product_id", "delivery_route_line", "delivery_route", "customer_id", "pretreatment_id"},
                new String[]{"name", "sequence", "name", "name", "name"});
        initForm();
    }

    protected void initForm() {
        adapter = new CommonTextAdapter(records, R.layout.suez_stock_quant_layout,
                new String[]{"location_id_name", "qty", "input_qty"}, new int[]{R.id.txt_location, R.id.txt_qty_available, R.id.txt_qty});
        adapter.setmOnItemClickListener(this);
        stockQuantList.setAdapter(adapter);
        pretreatmentWizardForm.initForm(wizardValues.toDataRow());
//        pretreatmentQty.setEditable(false);
        remainQty.setEditable(false);
        refreshQty();
    }

    public static List<ODataRow> initInputQty(List<ODataRow> rows) {
        for (ODataRow row : rows) {
            row.put("input_qty", row.getFloat("qty"));
        }
        return rows;
    }

    private void alertWarning() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_warning)
                .setMessage(R.string.message_no_qty_available)
                .setNegativeButton(R.string.label_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
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
                refreshQty();
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
                    positiveButton.setTextColor(OResource.color(ProcessingActivity.this, R.color.colorAccent));
                } else {
                    positiveButton.setEnabled(false);
                    positiveButton.setTextColor(OResource.color(ProcessingActivity.this, R.color.drawer_separator_text_color));
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
                        refreshQty();
                        adapter.notifyItemRemoved(clickPosition);
                    }
                }).create();
        dialog.show();
    }

    @OnClick({R.id.btn_cancel, R.id.btn_confirm})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                finish();
                break;
            case R.id.btn_confirm:
                if (preProcessing()) {
                    performProcessing();
                }
                break;
        }
    }

    /**
     * Refresh qty according after the input quantity changed.
     */
    protected void refreshQty() {
//        pretreatmentQty.setValue(sumField(records, "input_qty"));
        remainQty.setValue(records.get(0).getFloat("qty") - records.get(0).getFloat("input_qty"));
    }

    protected boolean preProcessing() {
        // Validate Datas
        OValues values = pretreatmentWizardForm.getValues();
        for (String key: values.keys()) {
            if (values.get(key) == null || values.get(key).equals(false)) {
                ToastUtil.toastShow(String.format(OResource.string(this, R.string.toast_invalid_field), key), this);
                return false;
            }
        }
        return true;
    }

    /**
     * Processing actions, to be inherited in child class.
     */
    protected void performProcessing() {

    }

    /**
     * Actions after processing, to be inherited in child class.
     */
    protected void postProcessing() {

    }
}