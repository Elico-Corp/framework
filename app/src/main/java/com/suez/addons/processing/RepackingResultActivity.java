package com.suez.addons.processing;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Spinner;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.odoo.BaseAbstractListener;
import com.odoo.OdooActivity;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.rpc.helper.ODomain;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.adapters.CommonTextAdapter;
import com.suez.addons.models.StockProductionLot;
import com.suez.utils.SearchRecordsOnlineUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by joseph on 18-8-2.
 */

public class RepackingResultActivity extends SuezActivity {
    @BindView(R.id.xNewPackingList)
    XRecyclerView xNewPackingList;

    private ArrayList<Integer> ids;
    private StockProductionLot stockProductionLot;
    private List<ODataRow> records;
    private CommonTextAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_repacking_result_activity);
        ButterKnife.bind(this);
        initToolbar(R.string.title_repacking_result);
        records = new ArrayList<>();
        initView();
        ids = getIntent().getIntegerArrayListExtra(SuezConstants.REPACKING_RESULT_KEY);
        stockProductionLot = new StockProductionLot(this, null);
        initData();
    }

    private void initView() {
        xNewPackingList.setLayoutManager(new LinearLayoutManager(this));
        xNewPackingList.setLoadingMoreEnabled(false);
        xNewPackingList.setPullRefreshEnabled(false);
        adapter = new CommonTextAdapter(records, R.layout.suez_repacking_result_list_items,
                new String[]{"name", "product_qty"}, new int[]{R.id.txt_new_lot_id, R.id.txt_new_qty});
        xNewPackingList.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.suez_menu_repacking_print_label, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void initData() {
        ODomain domain = new ODomain();
        domain.add("id", "in", ids);
        SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(stockProductionLot, domain).setListener(new BaseAbstractListener(){
            @Override
            public void OnSuccessful(List<ODataRow> listRow) {
                records.addAll(listRow);
                adapter.notifyDataSetChanged();
            }
        });
        utils.searchRecordsOnServer();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            intentToHome();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void intentToHome() {
        Intent intent = new Intent(this, OdooActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                intentToHome();
                break;
            case R.id.menu_new_repacking_print:
                // TODO: 18-8-9 Print Repacking Label 
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
