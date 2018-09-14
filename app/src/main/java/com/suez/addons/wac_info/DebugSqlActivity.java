package com.suez.addons.wac_info;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jcodecraeer.xrecyclerview.ProgressStyle;
import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.utils.OPreferenceManager;
import com.suez.SuezActivity;
import com.suez.addons.adapters.CommonTextAdapter;
import com.suez.utils.ToastUtil;
import com.suez.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by joseph on 18-7-25.
 */

public class DebugSqlActivity extends SuezActivity implements CommonTextAdapter.OnItemClickListener, ClearEditText.ITextLengthChangeListener {

    private static final String TAG = DebugSqlActivity.class.getSimpleName();

    @BindView(R.id.btn_run)
    Button btnRun;
    @BindView(R.id.txt_sql)
    TextView txtSql;
    @BindView(R.id.res_list)
    XRecyclerView resList;
    @BindView(R.id.sql)
    ClearEditText sqlView;

    private String sql;
    private List<ODataRow> rows;
    private CommonTextAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_debug_sql_activity);
        ButterKnife.bind(this);
        initToolbar("Sql Debug");
        rows = new ArrayList<>();
        adapter = new CommonTextAdapter(rows, R.layout.suez_debug_res_layout,
                new String[]{"_id", "name", "id"}, new int[]{R.id.txt__id, R.id.txt_name, R.id.txt_id});
        adapter.setmOnItemClickListener(this);
        resList.setAdapter(adapter);
        resList.setLayoutManager(new LinearLayoutManager(this));
        resList.setPullRefreshEnabled(false);
        resList.setLoadingMoreEnabled(false);
        sqlView.setTextLengthChangeListener(this);
    }

    @OnClick(R.id.btn_run)
    public void onClick(View v) {
        if (sql == null || sql.equals("")) {
            return;
        }
        try {
            OModel model = new OModel(this, "res.partner", null);
            rows.clear();
            rows.addAll(model.query(sql));
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
            txtSql.setText(e.getMessage());
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.suez_menu_clear_version, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onItemClick(int position) {
        txtSql.setText(rows.get(position - 1).toString());
    }

    @Override
    public void onItemLongClick(int position) {

    }

    @Override
    public void onTextLength(int length) {
        sql = sqlView.getEditableText().toString();
        txtSql.setText(sql);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                finish();
                break;
            case R.id.menu_clear_version:
                OPreferenceManager preferenceManager = new OPreferenceManager(this);
                preferenceManager.putString("offline_version", "0");
                ToastUtil.toastShow(R.string.toast_successful, this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
