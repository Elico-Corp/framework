package com.suez.addons.tank_truck.activity;

import com.jcodecraeer.xrecyclerview.ProgressStyle;
import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.utils.OResource;
import com.suez.SuezActivity;
import com.suez.addons.tank_truck.adapter.TankTruckListAdapter;
import com.suez.addons.tank_truck.jsonutils.TankTruckJsonUtils;
import com.suez.addons.tank_truck.models.DeliveryRoute;
import com.suez.utils.LogUtils;
import com.suez.utils.ToastUtil;
import com.suez.view.ClearEditText;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 18-4-28.
 */

public class TankTruckActivity extends SuezActivity implements View.OnClickListener,
        TextWatcher {
    private Button btnSuezConfirm;
    private XRecyclerView mTankTruckList;
    private TankTruckListAdapter adapter;
    private List<ODataRow> records;
    private ClearEditText mTankNUm;
    private DeliveryRoute deliveryRoute;
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

    private void getDataOnServer(int offset, Boolean offDialog){
        this.deliveryRoute.searchData(new BaseAbstractListener(){
            @Override
            public void OnSuccessful(List<ODataRow> listRow){
                records.addAll(TankTruckJsonUtils.setTankTruck(listRow));
                if (loadNumber == 0){
                    loadData();
                } else {
                    mTankTruckList.loadMoreComplete();
                    adapter.notifyDataSetChanged();
                }
            }
        }, offset, offDialog);
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

        adapter = new TankTruckListAdapter(this.records, R.layout.suez_tanktruck_list_items);
        mTankTruckList.setAdapter(adapter);
        adapter.setmOnItemClickListener(new TankTruckListAdapter.OnItemClickListener() {
            @Override
            public void ItemOnClick(int position) {
                dialogPumping(records.get(position).getString("name"), records.get(position).getInt("id"), position);
            }

            @Override
            public void ItemOnLongClick(int position) {}
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
                            Boolean row = false;
                            if (position == NO_POSITION){
                                row = deliveryRoute.update("id=?", new String[]{String.valueOf(id)},values)>0 ? true: false;
                            } else {
                                row = deliveryRoute.update(records.get(position).getInt("_id"), values);
                            }
                            if (row){
                                records.remove(position);
                                adapter.notifyDataSetChanged();
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
        this.deliveryRoute.setState(new BaseAbstractListener() {
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
        }, id);
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
                    this.deliveryRoute.searchName(new BaseAbstractListener() {
                        @Override
                        public void OnSuccessful(List<ODataRow> listRow){
                            confirmName(TankTruckJsonUtils.setTankTruck(listRow), textTankNum);
                        }
                    }, textTankNum);
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
            new LiveSearch().execute(mTankNUm.getText().toString());
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

    private class LiveSearch extends AsyncTask<String, Void, List<ODataRow>>{

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            findViewById(R.id.loading_progress).setVisibility(View.VISIBLE);
            mTankTruckList.setVisibility(View.GONE);
        }

        @Override
        protected List<ODataRow> doInBackground(String... params){
            try {
                List<ODataRow> results = null;
                if (isNetwork) {
                    OdooFields fields = new OdooFields("id", "name", "state", "truck_weight");
                    ODomain domain = new ODomain();
                    domain.add("&");
                    domain.add("name", "=ilike", "%" + params[0] + "%");
                    domain.add("&");
                    domain.add("state", "=", "truck_in");
                    domain.add("&");
                    domain.add("gross_weight", "<>", 0);
                    domain.add("|");
                    domain.add("truck_weight", "=", 0);
                    domain.add("truck_weight", "=", null); // is null
                    results = deliveryRoute.getServerDataHelper().searchRecords(
                            fields, domain, 30, null);
                } else {
                    results = deliveryRoute.select(new String[]{"id", "name", "state"},
                            "name like ? and state = ?, and truck_weight = ? and gross_weight != ?",
                            new String[]{"%" + params[0] + "%", "truck_in", "0", "0"}, "id limit 30");
                }
                return TankTruckJsonUtils.setTankTruck(results);
            } catch (Exception e){
                e.printStackTrace();
                LogUtils.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<ODataRow> result){
            super.onPostExecute(result);
            findViewById(R.id.loading_progress).setVisibility(View.GONE);
            mTankTruckList.setVisibility(View.VISIBLE);
            records.clear();
            records.addAll(result);
            adapter.notifyDataSetChanged();
        }
    }
}
