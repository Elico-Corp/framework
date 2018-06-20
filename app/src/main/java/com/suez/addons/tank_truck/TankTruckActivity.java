package com.suez.addons.tank_truck;

import com.jcodecraeer.xrecyclerview.ProgressStyle;
import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.utils.OResource;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.adapters.CommonTextAdapter;
import com.suez.addons.models.DeliveryRoute;
import com.suez.addons.models.OperationsWizard;
import com.suez.utils.CallMethodsOnlineUtils;
import com.suez.utils.SearchRecordsOnlineUtils;
import com.suez.utils.SuezJsonUtils;
import com.suez.utils.ToastUtil;
import com.suez.view.ClearEditText;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-4-28.
 */

public class TankTruckActivity extends SuezActivity implements View.OnClickListener,
        TextWatcher {
    private Button btnSuezConfirm;
    private XRecyclerView mTankTruckList;
    private CommonTextAdapter adapter;
    private List<ODataRow> records;
    private ClearEditText mTankNUm;
    private DeliveryRoute deliveryRoute;
    private OperationsWizard wizard;
    private int loadNumber;
    private static final int NO_POSITION = 1;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.suez_tanktruck_activity);
        this.initToolbar(R.string.title_suez_tanktruck);
        this.initView();
        this.records = new ArrayList<>();
        this.deliveryRoute = new DeliveryRoute(this, null);
        wizard = new OperationsWizard(this, null);
        if (this.isNetwork){
            getDataOnServer(this.loadNumber, true);
        } else {
            getLocalData(this.loadNumber);
        }
        this.mTankTruckList.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                loadNumber = 0;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        records.clear();
                        if (isNetwork){
                            getDataOnServer(loadNumber, false);
                        } else {
                            getLocalData(loadNumber);
                        }
                        adapter.notifyDataSetChanged();
                        mTankTruckList.refreshComplete();
                    }
                }, 1000);
            }

            @Override
            public void onLoadMore() {
                loadNumber++;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isNetwork){
                            getDataOnServer(loadNumber, false);
                        } else {
                            getLocalData(loadNumber);
                        }
                    }
                }, 1000);
            }
        });
    }

    private void initView(){
        this.btnSuezConfirm = (Button) findViewById(R.id.btnSuezConfirm);
        this.mTankTruckList = (XRecyclerView) findViewById(R.id.xTankTrackList);
        this.mTankNUm = (ClearEditText) findViewById(R.id.txtTankNum);
        this.mTankNUm.addTextChangedListener(this);
        this.btnSuezConfirm.setOnClickListener(this);
    }

    private void getDataOnServer(int offset, Boolean showDialog){
        BaseAbstractListener listener = new BaseAbstractListener() {
            @Override
            public void OnSuccessful(List<ODataRow> row) {
                records.addAll(SuezJsonUtils.parseRecords(deliveryRoute, row));
                if (loadNumber == 0){
                    loadData();
                } else {
                    mTankTruckList.loadMoreComplete();
                    adapter.notifyDataSetChanged();
                }
            }
        };
        OdooFields fields = getCommonFields();
        ODomain domain = getCommonDomain();
        SearchRecordsOnlineUtils onlineUtils = new SearchRecordsOnlineUtils(deliveryRoute, fields, domain, 100, offset).setListener(listener).setShowDialog(showDialog);
        onlineUtils.searchRecordsOnServer();
    }

    private void getLocalData(int offset){
        records.addAll(this.deliveryRoute.select(new String[]{"id", "name", "state"},
                "state=? and truck_weight=? and gross_weight!=?", new String[]{"truck_in", "0", "0"}, "id desc limit 30 offset "+30*offset));
        if (loadNumber == 0){
            loadData();
        } else {
            this.mTankTruckList.loadMoreComplete();
            this.adapter.notifyDataSetChanged();
        }
    }

    private void loadData(){
        this.mTankTruckList.setLayoutManager(new LinearLayoutManager(this));
        this.mTankTruckList.setPullRefreshEnabled(true);
        this.mTankTruckList.setLoadingMoreEnabled(true);
        this.mTankTruckList.setRefreshProgressStyle(ProgressStyle.BallSpinFadeLoader);
        this.mTankTruckList.setArrowImageView(R.drawable.iconfont_downgrey);

        adapter = new CommonTextAdapter(this.records, R.layout.suez_tanktruck_list_items, new String[]{"name"}, new int[]{R.id.txtTankItemNum});
        mTankTruckList.setAdapter(adapter);
        adapter.setmOnItemClickListener(new CommonTextAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                dialogPumping(records.get(position - 1).getString("name"), records.get(position - 1).getInt("id"), position);
            }

            @Override
            public void onItemLongClick(int position) {}
        });
    }

    public void dialogPumping(String name, final int id, final int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.dialog_title_warning)
                .setMessage(String.format(OResource.string(this, R.string.message_tank_truck_pumping), name))
                .setNegativeButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isNetwork){
                            setState(id, position);
                        } else {
                            OValues values = new OValues();
                            values.put("state", "pumping");
                            Boolean row;
                            if (position == NO_POSITION){
                                row = deliveryRoute.update("id=?", new String[]{String.valueOf(id)},values)>0 ? true: false;
                            } else {
                                row = deliveryRoute.update(records.get(position).getInt("_id"), values);
                            }
                            if (row){
                                records.remove(position);
                                adapter.notifyDataSetChanged();
                                OValues wizardValues = new OValues();
                                wizardValues.put("delivery_route_id", records.get(position).getInt("_id"));
                                wizardValues.put("action", SuezConstants.TANK_TRUCK_KEY);
                                wizard.insert(wizardValues);
                                ToastUtil.toastShow(R.string.toast_successful, TankTruckActivity.this);
                            } else {
                                ToastUtil.toastShow(R.string.toast_fail, TankTruckActivity.this);
                                return;
                            }
                        }
                    }
                })
                .setPositiveButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        Button btnConfirm = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        btnConfirm.setTextColor(OResource.color(this, R.color.android_green));
        Button btnCancel = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnCancel.setTextColor(OResource.color(this, R.color.android_red));
    }

    private void setState(final int id, final int position){
        BaseAbstractListener listener = new BaseAbstractListener() {
            @Override
            public void OnSuccessful(Boolean flag){
                if (flag){
                    if (position > NO_POSITION){
                        records.remove(position);
                    }
                    adapter.notifyDataSetChanged();
                    ToastUtil.toastShow(R.string.toast_successful, TankTruckActivity.this);
                } else {
                    ToastUtil.toastShow(R.string.toast_fail, TankTruckActivity.this);
                    return;
                }
            }
        };
        HashMap<String, Object> kwargs = new HashMap<>();
        HashMap<String, Object> map = new HashMap<>();
        kwargs.put("id", id);
        map.put("action", SuezConstants.TANK_TRUCK_KEY);
        map.put("data", kwargs);
        CallMethodsOnlineUtils utils = new CallMethodsOnlineUtils(deliveryRoute, "get_flush_data", new OArguments(),
                null, map).setListener(listener);
        utils.callMethodOnServer();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btnSuezConfirm:
                List<ODataRow> rows;
                final String textTankNum = mTankNUm.getText().toString().trim();
                if (TextUtils.isEmpty(textTankNum)){
                    this.mTankNUm.setShakeAnimation();
                    return;
                }
                if (this.isNetwork){
                    BaseAbstractListener listener = new BaseAbstractListener() {
                        @Override
                        public void OnSuccessful(List<ODataRow> listRow){
                            confirmName(SuezJsonUtils.parseRecords(deliveryRoute, listRow), textTankNum);
                        }
                    };
                    OdooFields fields = getCommonFields();
                    ODomain domain = getCommonDomain();
                    domain.add("&");
                    domain.add("name", "=ilike", "%" + textTankNum + "%");
                    SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(deliveryRoute, fields, domain).setListener(listener);
                    utils.searchRecordsOnServer();
                } else {
                    rows = deliveryRoute.select(new String[]{"id", "name", "state"},
                            "name=? and state=? and truck_weight=? and gross_weight!=?",
                            new String[]{textTankNum, "truck_in", "0", "0"});
                    confirmName(rows, textTankNum);
                }
                break;
            default:
                break;
        }
    }

    private void confirmName(List<ODataRow> rows, String name) {
        if (rows == null || rows.size() == 0) {
            new AlertDialog.Builder(this).setTitle(OResource.string(this, R.string
                    .dialog_title_warning))
                    .setMessage(String.format(OResource.string(this, R.string
                            .message_no_tank_truck_number), name))
                    .setPositiveButton(OResource.string(this, R.string.dialog_confirm), new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
        } else {
            dialogPumping(name, rows.get(0).getInt("id"), NO_POSITION);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after){}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count){
        if (s.length() > 2){
            mTankTruckList.setVisibility(View.GONE);
            findViewById(R.id.loading_progress).setVisibility(View.VISIBLE);
            BaseAbstractListener listener = new BaseAbstractListener() {
                @Override
                public void OnSuccessful(List<ODataRow> listRow) {
                    findViewById(R.id.loading_progress).setVisibility(View.GONE);
                    mTankTruckList.setVisibility(View.VISIBLE);
                    records.clear();
                    records.addAll(listRow);
                    adapter.notifyDataSetChanged();
                }
            };
            OdooFields fields = getCommonFields();
            ODomain domain = getCommonDomain();
            domain.add("&");
            domain.add("name", "=ilike", "%" + mTankNUm.getText().toString() + "%");
            SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(deliveryRoute, fields, domain).setListener(listener).setShowDialog(false);
        } else {
            if (isNetwork){
                records.clear();
                getDataOnServer(0, false);
            } else {
                records.clear();
                getLocalData(0);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s){}

    @NonNull
    private OdooFields getCommonFields() {
        return new OdooFields(new String[] {"id", "name", "state"});
    }

    private ODomain getCommonDomain() {
        ODomain domain = new ODomain();
        domain.add("&");
        domain.add("state", "=", "truck_in");
        domain.add("&");
        domain.add("gross_weight", "<>", 0);
        domain.add("|");
        domain.add("truck_weight", "=", 0);
        domain.add("truck_weight", "=", null);
        return domain;
    }
}
