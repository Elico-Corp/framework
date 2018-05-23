package com.suez.addons.wac_info;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.LinearLayout;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.adapters.CommonTextAdapter;
import com.suez.addons.models.DeliveryRouteLine;
import com.suez.addons.models.ProductWac;
import com.suez.addons.models.StockProductionLot;
import com.suez.utils.SearchRecordsOnlineUtils;
import com.suez.utils.SuezJsonUtils;
import com.suez.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import odoo.controls.OForm;

/**
 * Created by joseph on 18-5-14.
 */

public class WacInfoDrlLIstActivity extends SuezActivity implements CommonTextAdapter.OnItemClickListener {

    @BindView(R.id.wacInfoForm)
    OForm wacInfoForm;
    @BindView(R.id.wacInfoFormOnLine)
    OForm wacInfoFormOnLine;
    @BindView(R.id.xRecyWACList)
    XRecyclerView xRecyWACList;
    @BindView(R.id.linear_drl_list)
    LinearLayout linearDrlList;

    private ProductWac productWac;
    private DeliveryRouteLine deliveryRouteLine;
    private int wac_id;
    private List<ODataRow> records;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_wacinfo_drl_list_activity);
        ButterKnife.bind(this);
        initToolbar(R.string.title_suez_scan_wac_list);
        wac_id = getIntent().getIntExtra(SuezConstants.WAC_ID_KEY, 0);
        productWac = new ProductWac(this, null);
        deliveryRouteLine = new DeliveryRouteLine(this, null);

        if (isNetwork) {
            wacInfoFormOnLine.setVisibility(View.VISIBLE);
            wacInfoForm.setVisibility(View.GONE);
            initDataOnline();
        } else {
            initDataOffline();
        }
    }

    private void initDataOnline() {
        OdooFields wacFields = new OdooFields(productWac.getColumns());
        ODomain wacDomain = new ODomain().add("id", "=", wac_id);
        BaseAbstractListener wacListener = new BaseAbstractListener(){
            @Override
            public void OnSuccessful(List<ODataRow> listRow) {
                if (listRow != null && listRow.size() > 0) {
                    wacInfoFormOnLine.initForm(SuezJsonUtils.parseRecords(productWac, listRow).get(0));
                    OdooFields drlFields = new OdooFields(deliveryRouteLine.getColumns());
                    ODomain drlDomain = new ODomain().add("wac_id", "=", wac_id);
                    BaseAbstractListener drlListener = new BaseAbstractListener(){
                        @Override
                        public void OnSuccessful(List<ODataRow> listRow) {
                            if (listRow != null && listRow.size() > 0) {
                                records = SuezJsonUtils.parseRecords(deliveryRouteLine, listRow);
                                loadData(records);
                            } else {
                                linearDrlList.setVisibility(View.GONE);
                                ToastUtil.toastShow(R.string.toast_no_delivery_route_line, WacInfoDrlLIstActivity.this);
                            }
                        }
                    };
                    SearchRecordsOnlineUtils drlSearchUtils = new SearchRecordsOnlineUtils(deliveryRouteLine, drlFields, drlDomain).setListener(drlListener);
                    drlSearchUtils.searchRecordsOnServer();
                }
            }
        };
        SearchRecordsOnlineUtils wacSearchUtils = new SearchRecordsOnlineUtils(productWac, wacFields, wacDomain).setListener(wacListener);
        wacSearchUtils.searchRecordsOnServer();
    }

    private void initDataOffline() {
        wacInfoForm.initForm(productWac.browse(wac_id));
        List<ODataRow> drlRecords = deliveryRouteLine.select(null, "wac_id = ?",
                new String[]{String.valueOf(wac_id)});
        if (drlRecords != null && drlRecords.size() > 0) {
            records = drlRecords;
            loadData(records);
        } else {
            linearDrlList.setVisibility(View.GONE);
            ToastUtil.toastShow(R.string.toast_no_delivery_route_line, this);
        }
    }

    private void loadData(List<ODataRow> records) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        xRecyWACList.setLayoutManager(layoutManager);
        xRecyWACList.setPullRefreshEnabled(false);
        xRecyWACList.setLoadingMoreEnabled(false);
        xRecyWACList.setNestedScrollingEnabled(false);

        CommonTextAdapter adapter = new CommonTextAdapter(records, R.layout.suez_wacinfo_drl_list_items,
                new String[]{"name"}, new int[]{R.id.txtProductName});
        xRecyWACList.setAdapter(adapter);
        adapter.setmOnItemClickListener(this);
    }

    @Override
    public void onItemLongClick(int position) {}

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(this, WacInfoActivity.class);
        if (isNetwork) {
            intent.putExtra(SuezConstants.DELIVERY_ROUTE_LINE_ID_KEY, records.get(position).getInt("id"));
        } else {
            intent.putExtra(SuezConstants.DELIVERY_ROUTE_LINE_ID_KEY, records.get(position).getInt("_id"));
        }
        startActivity(intent);
    }
}
