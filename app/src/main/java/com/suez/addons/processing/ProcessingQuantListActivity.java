package com.suez.addons.processing;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.rpc.helper.ODomain;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.adapters.CommonTextAdapter;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.StockQuant;
import com.suez.utils.RecordUtils;
import com.suez.utils.SearchRecordsOnlineUtils;
import com.suez.utils.SuezJsonUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import odoo.controls.OForm;

/**
 * Created by joseph on 18-6-12.
 */

public class ProcessingQuantListActivity extends SuezActivity implements CommonTextAdapter.OnItemClickListener {

    @BindView(R.id.stock_production_lot_form)
    OForm stockProductionLotForm;
    @BindView(R.id.xr_qc_list)
    XRecyclerView xrQcList;
    @BindView(R.id.linear_quant_list)
    LinearLayout linearQuantList;
    @BindView(R.id.main_view)
    NestedScrollView mainView;
    @BindView(R.id.no_item)
    ScrollView noItem;

    private int prodlotId;
    private StockProductionLot stockProductionLot;
    private StockQuant stockQuant;
    private CommonTextAdapter adapter;
    private String key;
    private List<ODataRow> rows;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_processing_quant_list_activity);
        ButterKnife.bind(this);
        prodlotId = getIntent().getIntExtra(SuezConstants.PRODLOT_ID_KEY, 0);
        key = getIntent().getStringExtra(SuezConstants.COMMON_KEY);
        initToolbar(R.string.column_wac_processing);
        initView();
        stockProductionLot = new StockProductionLot(this, null);
        stockQuant = new StockQuant(this, null);
        if (isNetwork) {
            initDataOnline();
        } else {
            initDataOffline();
        }
    }

    private void initView() {
        xrQcList.setLayoutManager(new LinearLayoutManager(this));
        xrQcList.setLoadingMoreEnabled(false);
        xrQcList.setPullRefreshEnabled(false);
    }

    private void initDataOnline() {
        ODomain domain = new ODomain();
        domain.add("id", "=", prodlotId);
        BaseAbstractListener listener = new BaseAbstractListener(){
            @Override
            public void OnSuccessful(List<ODataRow> listRow) {
                stockProductionLotForm.initForm(SuezJsonUtils.parseRecords(stockProductionLot, listRow).get(0));
                ODomain quantDomain = new ODomain();
                quantDomain.add("lot_id", "=", prodlotId);
                quantDomain.add("location_id.usage", "=", "internal");
                BaseAbstractListener quantListener = new BaseAbstractListener(){
                    @Override
                    public void OnSuccessful(List<ODataRow> listRow) {
                        if (listRow != null && !listRow.isEmpty()) {
                            rows = SuezJsonUtils.parseRecords(stockQuant, listRow);
                            initForm();
                        } else {
                            mainView.setVisibility(View.GONE);
                            noItem.setVisibility(View.VISIBLE);
                        }
                    }
                };
                SearchRecordsOnlineUtils quantUtils = new SearchRecordsOnlineUtils(stockQuant, quantDomain).setListener(quantListener);
                quantUtils.searchRecordsOnServer();
            }
        };
        SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(stockProductionLot, domain).setListener(listener);
        utils.searchRecordsOnServer();
    }

    private void initDataOffline() {
        ODataRow lot = stockProductionLot.browse(prodlotId);
        stockProductionLotForm.initForm(new RecordUtils(stockProductionLot).parseMany2oneRecords(lot,
                new String[]{"product_id", "delivery_route_line", "delivery_route", "customer_id", "pretreatment_id"},
                new String[]{"name", "sequence", "name", "name", "name"}));
        List<ODataRow> records = stockQuant.select(null, "lot_id = ? and location_id in (select _id from stock_location where usage = ?)", new String[]{String.valueOf(prodlotId), "internal"});
        if (records.isEmpty()) {
            mainView.setVisibility(View.GONE);
            noItem.setVisibility(View.VISIBLE);
        } else {
            rows = new RecordUtils(stockQuant).parseMany2oneRecords(records, new String[]{"location_id"}, new String[]{"name"});
            initForm();
        }
    }

    private void initForm() {
        adapter = new CommonTextAdapter(rows, R.layout.suez_quant_list_items, new String[]{"location_id_name", "qty"},
                new int[]{R.id.source_location_txt, R.id.qty_txt});
        adapter.setmOnItemClickListener(this);
        xrQcList.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
        int quant_id;
        if (isNetwork) {
            quant_id = rows.get(position - 1).getInt("id");
        } else {
            quant_id = rows.get(position - 1).getInt("_id");
        }
        Intent intent;
        switch (key) {
            case SuezConstants.WAC_MOVE_KEY:
                intent = new Intent(this, WacMoveActivity.class);
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.STOCK_QUANT_ID_KEY, quant_id);
                startActivityForResult(intent, 1);
                break;
            case SuezConstants.REPACKING_KEY:
                intent = new Intent(this, RepackingActivity.class);
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.STOCK_QUANT_ID_KEY, quant_id);
                startActivityForResult(intent, 1);
                break;
            case SuezConstants.PRETREATMENT_KEY:
                intent = new Intent(this, PretreatmentActivity.class);
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.STOCK_QUANT_ID_KEY, quant_id);
                startActivityForResult(intent, 1);
                break;
            case SuezConstants.DIRECT_BURN_KEY:
                intent = new Intent(this, DirectBurnActivity.class);
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.STOCK_QUANT_ID_KEY, quant_id);
                startActivityForResult(intent, 1);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (isNetwork) {
                initDataOnline();
            } else {
                initDataOffline();
            }
        }
    }

    @Override
    public void onItemLongClick(int position) {
    }
}
