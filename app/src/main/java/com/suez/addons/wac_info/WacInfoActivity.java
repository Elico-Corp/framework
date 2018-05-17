package com.suez.addons.wac_info;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;

import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.adapters.CommonTextAdapter;
import com.suez.addons.models.ActualPackaging;
import com.suez.addons.models.DeliveryRouteLine;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.WmdsParameterMainComponent;
import com.suez.utils.SearchRecordsOnlineUtils;
import com.suez.utils.SuezJsonUtils;
import com.suez.utils.ToastUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import odoo.controls.OField;
import odoo.controls.OForm;

/**
 * Created by joseph on 18-5-11.
 */

public class WacInfoActivity extends SuezActivity implements View.OnClickListener {

    @BindView(R.id.wac_info_customer)
    OField wacInfoCustomer;
    @BindView(R.id.wac_info_customer_zh)
    OField wacInfoCustomerZh;
    @BindView(R.id.xr_packaging_list)
    XRecyclerView xrPackagingList;
    @BindView(R.id.xr_component_list)
    XRecyclerView xrComponentList;
    @BindView(R.id.hide_form)
    TextView hideForm;
    @BindView(R.id.delivery_route_line_form_offline)
    OForm deliveryRouteLineFormOffline;
    @BindView(R.id.delivery_route_line_form_offline_hide)
    OForm deliveryRouteLineFormOfflineHide;
    @BindView(R.id.delivery_route_line_form_online)
    OForm deliveryRouteLineFormOnline;
    @BindView(R.id.delivery_route_line_form_online_hide)
    OForm deliveryRouteLineFormOnlineHide;
    @BindView(R.id.prodlot)
    OField prodlot;
    @BindView(R.id.txt_component)
    TextView txtComponent;
    @BindView(R.id.txt_min)
    TextView txtMin;
    @BindView(R.id.txt_average)
    TextView txtAverage;
    @BindView(R.id.txt_max)
    TextView txtMax;


    private int delivery_route_line_id;
    private DeliveryRouteLine deliveryRouteLine;
    private WmdsParameterMainComponent component;
    private StockProductionLot stockProductionLot;
    private ActualPackaging actualPackaging;
    private CommonTextAdapter componentAdapter;
    private CommonTextAdapter packagingAdapter;
    private OForm deliveryRouteLineForm;
    private OForm deliveryRouteLineFormHide;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_wacinfo_activity);
        initToolbar(R.string.title_suez_wacinfo);
        ButterKnife.bind(this);
        delivery_route_line_id = getIntent().getIntExtra(SuezConstants.DELIVERY_ROUTE_LINE_ID_KEY, 0);
        initView();

        deliveryRouteLine = new DeliveryRouteLine(this, null);
        component = new WmdsParameterMainComponent(this, null);
        actualPackaging = new ActualPackaging(this, null);
        stockProductionLot = new StockProductionLot(this, null);
        if (isNetwork) {
            initViewOnline();
            initDataOnline();
        } else {
            initViewOffline();
            initDataOffline();
        }
    }

    private void initView() {
        xrComponentList.setLayoutManager(new LinearLayoutManager(this));
        xrComponentList.setLoadingMoreEnabled(false);
        xrComponentList.setPullRefreshEnabled(false);
        xrPackagingList.setLayoutManager(new LinearLayoutManager(this));
        xrPackagingList.setLoadingMoreEnabled(false);
        xrPackagingList.setPullRefreshEnabled(false);
    }

    private void initViewOnline() {
        deliveryRouteLineForm = deliveryRouteLineFormOnline;
        deliveryRouteLineFormHide = deliveryRouteLineFormOnlineHide;
        deliveryRouteLineForm.setVisibility(View.VISIBLE);
    }

    private void initViewOffline() {
        deliveryRouteLineForm = deliveryRouteLineFormOffline;
        deliveryRouteLineFormHide = deliveryRouteLineFormOfflineHide;
        deliveryRouteLineForm.setVisibility(View.VISIBLE);
        if (app.getLanguage().equals("zh")) {
            wacInfoCustomerZh.setVisibility(View.VISIBLE);
        } else
            wacInfoCustomer.setVisibility(View.VISIBLE);
    }


    @OnClick(R.id.hide_form)
    public void onClick(View view) {
        if (view.getId() == R.id.hide_form) {
            if (deliveryRouteLineFormHide.getVisibility() == View.GONE) {
                deliveryRouteLineFormHide.setVisibility(View.VISIBLE);
                hideForm.setText(R.string.label_hide);
            } else {
                deliveryRouteLineFormHide.setVisibility(View.GONE);
                hideForm.setText(R.string.label_more);
            }
        }
    }

    private void initDataOnline() {
        if (delivery_route_line_id == 0) {
            return;
        }
        OdooFields drlFields = new OdooFields(deliveryRouteLine.getColumns());
        ODomain drlDomain = new ODomain();
        drlDomain.add("id", "=", delivery_route_line_id);
        BaseAbstractListener drlListener = new BaseAbstractListener() {
            @Override
            public void OnSuccessful(List<ODataRow> listRow) {
                if (listRow != null && !listRow.isEmpty()) {
                    ODataRow row = SuezJsonUtils.parseRecords(deliveryRouteLine, listRow).get(0);
                    deliveryRouteLineForm.initForm(row);
                    deliveryRouteLineFormHide.initForm(row);
                    OdooFields componentFields = new OdooFields(component.getColumns());
                    ODomain componentDomain = new ODomain();
                    componentDomain.add("wac_id", "=", row.getInt("wac_id"));
                    BaseAbstractListener componentListener = new BaseAbstractListener() {
                        @Override
                        public void OnSuccessful(List<ODataRow> listRow) {
                            initComponentList(SuezJsonUtils.parseRecords(component, listRow));
                            OdooFields packagingFields = new OdooFields(actualPackaging.getColumns());
                            ODomain packagingDomain = new ODomain();
                            packagingDomain.add("route_line_id", "=", delivery_route_line_id);
                            BaseAbstractListener packagingListener = new BaseAbstractListener() {
                                @Override
                                public void OnSuccessful(List<ODataRow> listRow) {
                                    initPackagingList(SuezJsonUtils.parseRecords(actualPackaging, listRow));
                                }
                            };
                            SearchRecordsOnlineUtils packagingSearchUtil = new SearchRecordsOnlineUtils(actualPackaging, packagingFields, packagingDomain).setListener(packagingListener);
                            packagingSearchUtil.searchRecordsOnServer();
                        }
                    };
                    SearchRecordsOnlineUtils componentSearchUtil = new SearchRecordsOnlineUtils(component, componentFields, componentDomain).setListener(componentListener);
                    componentSearchUtil.searchRecordsOnServer();
                } else {
                    ToastUtil.toastShow(R.string.toast_no_data, WacInfoActivity.this);
                    deliveryRouteLineForm.initForm(null);
                    deliveryRouteLineFormHide.initForm(null);
                    return;
                }
            }
        };
        SearchRecordsOnlineUtils drlSearchUtil = new SearchRecordsOnlineUtils(deliveryRouteLine, drlFields, drlDomain).setListener(drlListener);
        drlSearchUtil.searchRecordsOnServer();
    }

    private void initDataOffline() {
        ODataRow drlRow = deliveryRouteLine.browse(delivery_route_line_id);
        if (drlRow == null) {
            ToastUtil.toastShow(R.string.toast_no_data, this);
            deliveryRouteLineForm.initForm(null);
            deliveryRouteLineFormHide.initForm(null);
            return;
        }
        drlRow.put("address_name_zh", drlRow.getM2ORecord("wac_id").browse().getString("partner_name_cn"));
        deliveryRouteLineForm.initForm(drlRow);
        deliveryRouteLineFormHide.initForm(drlRow);
        List<ODataRow> wmdsRows = component.query("select wm.name as component, wpm.min as min, wpm.max as max, wpm.average " +
                "as average from wmds_main_component as wm left outer join wmds_parameter_main_component as wpm where wm._id " +
                "= wpm.component and wpm.wac_id = " + drlRow.getString("wac_id"));
        initComponentList(wmdsRows);
        List<ODataRow> packagingRows = actualPackaging.query("select ap.qty, ap.remark, pp.name as package_ids from actual_packaging as ap " +
                "left outer join product_packaging as pp where ap.package_ids = pp._id and ap.route_line_id = " + drlRow.getString("_id"));
        initPackagingList(packagingRows);
    }

    private void initComponentList(List<ODataRow> rows) {
        if (rows == null || rows.size() == 0) {
            return;
        }
        componentAdapter = new CommonTextAdapter(rows, R.layout.suez_wac_info_component_list_items,
                new String[]{"component", "min", "max", "average"}, new int[]{R.id.txt_component, R.id.txt_min, R.id.txt_max, R.id.txt_average});
        xrComponentList.setAdapter(componentAdapter);
    }

    private void initPackagingList(List<ODataRow> rows) {
        if (rows == null || rows.size() == 0) {
            return;
        }
        String str = String.format("%s * %s%s", rows.get(0).getString("package_ids"), rows.get(0).getString("qty"),
                rows.get(0).getString("remark").equals("false") ? "" : "(" + rows.get(0).getString("remark") + ")");
        HashMap map = new HashMap<String, Object>();
        map.put("res_id", R.id.txt_packaging_info);
        map.put("text", str);
        List<HashMap<String, Object>> specifiedFields = new ArrayList<>();
        specifiedFields.add(map);
        packagingAdapter = new CommonTextAdapter(rows, R.layout.suez_wac_info_packaging_list_items, new String[]{}, new int[]{}, specifiedFields);
        xrPackagingList.setAdapter(packagingAdapter);
    }

}
