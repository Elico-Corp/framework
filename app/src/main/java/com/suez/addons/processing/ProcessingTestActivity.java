package com.suez.addons.processing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;
import com.odoo.core.support.OUser;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.models.OperationsWizard;
import com.suez.addons.models.StockLocation;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.StockQuant;
import com.suez.utils.MD5Utils;
import com.suez.utils.RecordUtils;
import com.suez.utils.SuezSyncUtils;
import com.suez.utils.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by joseph on 18-6-21.
 */

public class ProcessingTestActivity extends SuezActivity {
    private static final String TAG = ProcessingTestActivity.class.getSimpleName();

    @BindView(R.id.testProgressBar)
    ProgressBar testProgressBar;
    @BindView(R.id.wac_move_count)
    EditText wacMoveCountView;
    @BindView(R.id.repacking_count)
    EditText repackingCountView;
    @BindView(R.id.blending_count)
    EditText blendingCountView;
    @BindView(R.id.btn_confirm_test)
    Button btnConfirmTest;
    @BindView(R.id.btn_cancel_test)
    Button btnCancelTest;

    private StockProductionLot stockProductionLot;
    private OperationsWizard operationsWizard;
    private StockLocation stockLocation;
    private StockQuant stockQuant;
    private List<ODataRow> records;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_test_layout);
        ButterKnife.bind(this);
        initToolbar(R.string.column_wac_processing);
        stockProductionLot = new StockProductionLot(this, null);
        operationsWizard = new OperationsWizard(this, null);
        stockQuant = new StockQuant(this, null);
        stockLocation = new StockLocation(this, null);
    }

    @OnClick({R.id.btn_confirm_test, R.id.btn_cancel_test})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_confirm_test:
                startFlushing();
                break;
            case R.id.btn_cancel_test:
                finish();
                break;
        }
    }

    private void startFlushing() {
        int wacMoveCount = Integer.parseInt(wacMoveCountView.getText().toString());
        int repackingCount = Integer.parseInt(repackingCountView.getText().toString());
        int blendingCount = Integer.parseInt(blendingCountView.getText().toString());
        List<Object> locations = RecordUtils.getFieldList(stockLocation.select(new String[]{"_id"}, "usage = ?", new String[]{"internal"}), "_id");
        records = stockQuant.select(null, "location_id " +
                "in " + locations.toString().replace("[", "(").replace("]", ")") + " limit " + (wacMoveCount + repackingCount + blendingCount), null);
        createActions(SuezConstants.WAC_MOVE_KEY, 0, wacMoveCount);
        createActions(SuezConstants.REPACKING_KEY, wacMoveCount, repackingCount);
        createActions(SuezConstants.CREATE_BLENDING_KEY, wacMoveCount + repackingCount, blendingCount);

    }

    private void createActions(String action, int offset, int count) {
        List<ODataRow> actions = new ArrayList<>();
        for (int i= offset;i<=count+offset;i++) {
            OValues values = new OValues();
            values.put("action", action);
            values.put("prodlot_id", records.get(i).getM2ORecord("lot_id").browse().getInt("id"));
            values.put("pretreatment_location_id", stockLocation.browse(null, "is_pretreatment = ?", new String[]{"true"}).getInt("id"));
            values.put("destination_location_id", stockLocation.browse(null, "is_int = ?", new String[]{"true"}).getInt("id"));
            values.put("quant_line_ids", records.get(i).getString("_id"));
            values.put("quant_line_qty", records.get(i).getString("qty"));
            values.put("remain_qty", 0.0f);
            values.put("repacking_location_id", stockLocation.browse(null, "is_repacking = ?", new String[]{"true"}).getInt("id"));
            values.put("package_number", 2);
            values.put("qty", records.get(i).getFloat("qty"));
            values.put("blending_location_id", stockLocation.browse(null, "is_blending = ?", new String[]{"false"}).getInt("id"));
            values.put("blending_waste_category_id", 1);
            values.put("is_finished", true);
            actions.add(values.toDataRow());
        }
        try {
            SuezSyncUtils utils = new SuezSyncUtils(this, OUser.current(this), null);
            utils.setRecords(actions);
            utils.syncProcessing(new OdooResult());
        } catch (Exception e){
            e.printStackTrace();
            ToastUtil.toastShow(e.getMessage(), this);
        }
    }
}
