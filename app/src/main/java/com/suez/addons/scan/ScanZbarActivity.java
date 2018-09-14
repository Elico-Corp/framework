package com.suez.addons.scan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.utils.OResource;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.blending.AddBlendingActivity;
import com.suez.addons.blending.CreateBlendingActivity;
import com.suez.addons.models.ProductWac;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.processing.ProcessingQuantListActivity;
import com.suez.addons.processing.RepackingActivity;
import com.suez.addons.processing.WacMoveActivity;
import com.suez.addons.wac_info.WacInfoDrlLIstActivity;
import com.suez.addons.wac_info.WacInfoActivity;
import com.suez.utils.SearchRecordsOnlineUtils;
import com.suez.utils.SuezJsonUtils;
import com.suez.view.ClearEditText;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by joseph on 18-5-11.
 */

public class ScanZbarActivity extends SuezActivity {

    @BindView(R.id.txt_scan)
    ClearEditText txtScan;

    private String code;
    private StockProductionLot stockProductionLot;
    private ProductWac productWac;
    private AlertDialog scanDialog;
    private int prodlotId = 0;
    private String key;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_activity_scanzbar);
        ButterKnife.bind(this);
        checkNetwork(this);
        initToolbar(R.string.title_scan_sn);
        txtScan.setFocusable(true);
        txtScan.addTextChangedListener(new TextScanClass(R.id.txt_scan));
        stockProductionLot = new StockProductionLot(this, null);
        productWac = new ProductWac(this, null);
        key = getIntent().getStringExtra(SuezConstants.COMMON_KEY);
    }

    @Override
    protected void onDestroy() {
        if (scanDialog != null) {
            scanDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.suez_menu_scan, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                finish();
                break;
            case R.id.menu_scan_input_code:
                LinearLayout layout = new LinearLayout(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                layout.setLayoutParams(params);
                final EditText input = new EditText(this);
                input.setSingleLine();
                input.setLayoutParams(params);
                input.addTextChangedListener(new TextScanClass(R.id.menu_scan_input_code));
                layout.addView(input);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_title_input);
                builder.setView(layout);
                builder.setNegativeButton(R.string.dialog_cancel, null);
                builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        code = input.getText().toString().trim();
                        intentUtil(code);
                    }
                });
                scanDialog = builder.create();
                scanDialog.show();
                scanDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void checkNetwork(Context context) {
        if (app.networkState != app.inNetwork()) {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setIcon(R.drawable.ic_odoo)
                    .setTitle(R.string.title_network_not_match)
                    .setNegativeButton(R.string.label_close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setMessage(String.format(OResource.string(context, R.string.message_work_mode), app.networkState ? OResource.string(context, R.string.label_online) : OResource.string(context, R.string.label_offline)))
                    .create();
            dialog.show();
        }
    }

    private void intentUtil(final String code) {
        if (isNetwork) {
            OdooFields fields = new OdooFields(stockProductionLot.getColumns());
            ODomain domain = new ODomain();
            domain.add("name", "=", code.toUpperCase());
            BaseAbstractListener listener = new BaseAbstractListener() {

                @Override
                public void OnSuccessful(List<ODataRow> listRow) {
                    if (listRow != null && !listRow.isEmpty()) {
                        listRow = SuezJsonUtils.parseRecords(stockProductionLot, listRow);
                        prodlotId = listRow.get(0).getInt("id");
                        startIntent(listRow.get(0), listRow.get(0).getInt("delivery_route_line"));
                        scanDialog.dismiss();
                    } else if (key.equals(SuezConstants.WAC_INFO_KEY)){
                        OdooFields wacFields = new OdooFields(productWac.getColumns());
                        ODomain wacDomain = new ODomain();
                        wacDomain.add("wac_code", "=", code.toUpperCase());
                        BaseAbstractListener wacListener = new BaseAbstractListener() {
                            @Override
                            public void OnSuccessful(List<ODataRow> listRow) {
                                if (listRow != null && !listRow.isEmpty()) {
                                    listRow = SuezJsonUtils.parseRecords(productWac, listRow);
                                    key = SuezConstants.WAC_INFO_WAC_KEY;
                                    startIntent(listRow.get(0), listRow.get(0).getInt("id"));
                                    scanDialog.dismiss();
                                } else {
                                    alertWarning(String.format(OResource.string(ScanZbarActivity.this, R.string.message_no_wac_code), code));
                                }
                            }
                        };
                        SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(productWac, wacFields, wacDomain).setListener(wacListener);
                        utils.searchRecordsOnServer();
                    } else {
                        alertWarning(String.format(OResource.string(ScanZbarActivity.this, R.string.message_no_wac_code), code));
                    }
                }
            };
            SearchRecordsOnlineUtils utils = new SearchRecordsOnlineUtils(stockProductionLot, fields, domain).setListener(listener);
            utils.searchRecordsOnServer();
        } else {
            List<ODataRow> rows = stockProductionLot.select(null, "name = ?", new String[]{code.toUpperCase()});
            if (!rows.isEmpty()) {
                prodlotId = rows.get(0).getInt("_id");
                startIntent(rows.get(0), rows.get(0).getInt("delivery_route_line"));
            } else if (key.equals(SuezConstants.WAC_INFO_KEY)){
                List<ODataRow> wacRows = productWac.select(new String[]{"_id"}, "wac_code = ?", new String[]{code.toUpperCase()});
                if (!wacRows.isEmpty()) {
                    key = SuezConstants.WAC_INFO_WAC_KEY;
                    startIntent(rows.get(0), rows.get(0).getInt("_id"));
                } else {
                    alertWarning(String.format(OResource.string(this, R.string.message_no_wac_code), code));
                }
            } else {
                alertWarning(String.format(OResource.string(this, R.string.message_no_wac_code), code));
            }
        }
    }

    private void startIntent(ODataRow row, int id) {
        Intent intent;
        switch (key) {
            case SuezConstants.WAC_INFO_KEY:
                intent = new Intent(this, WacInfoActivity.class);
                intent.putExtra(SuezConstants.DELIVERY_ROUTE_LINE_ID_KEY, id);
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.PRODLOT_NAME_KEY, code);
                startActivity(intent);
                break;
            case SuezConstants.WAC_INFO_WAC_KEY:
                intent = new Intent(this, WacInfoDrlLIstActivity.class);
                intent.putExtra(SuezConstants.WAC_ID_KEY, id);
                intent.putExtra(SuezConstants.PRODLOT_NAME_KEY, code);
                startActivity(intent);
                break;
            case SuezConstants.CREATE_BLENDING_KEY:
                intent = new Intent(this, CreateBlendingActivity.class);
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.PRODLOT_NAME_KEY, code);
                startActivity(intent);
                break;
            case SuezConstants.ADD_BLENDING_KEY:
                if (!code.toUpperCase().startsWith("B")) {
                    alertWarning(String.format(OResource.string(this, R.string.message_not_blending_lot), code));
                    break;
                }
                if (row.getBoolean("is_finished")) {
                    alertWarning(String.format(OResource.string(this, R.string.message_is_finished), code));
                    break;
                }
                intent = new Intent(this, AddBlendingActivity.class);
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.PRODLOT_NAME_KEY, code);
                startActivity(intent);
                break;
            case SuezConstants.SCAN_BLENDING_KEY:
                intent = new Intent();
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.PRODLOT_NAME_KEY, code);
                setResult(RESULT_OK, intent);
                finish();
                break;
            case SuezConstants.DIRECT_BURN_KEY:
            case SuezConstants.WAC_MOVE_KEY:
            case SuezConstants.REPACKING_KEY:
                intent = new Intent(this, ProcessingQuantListActivity.class);
                intent.putExtra(SuezConstants.COMMON_KEY, key);
                intent.putExtra(SuezConstants.PRODLOT_ID_KEY, prodlotId);
                intent.putExtra(SuezConstants.PRODLOT_NAME_KEY, code);
                startActivity(intent);
                break;
        }
    }

    private void alertWarning(String message) {
        if (scanDialog != null) {
            scanDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_warning);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.label_close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                code = null;
                txtScan.setText(null);
                txtScan.setFocusable(true);
                dialog.dismiss();
            }
        });
        AlertDialog warningDialog = builder.create();
        warningDialog.show();
    }

    private class TextScanClass implements TextWatcher {
        private int id;

        public TextScanClass(int id) {
            this.id = id;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after){}

        @Override
        public void onTextChanged (CharSequence charSequence, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable editable) {
            if (id == R.id.txt_scan) {
                if (editable != null && !editable.toString().isEmpty() && code == null) {
                    code = editable.toString().trim();
                    txtScan.setFocusable(false);
                    intentUtil(code);
                }
            } else {
                if (editable.toString().isEmpty() || editable.length() <1) {
                    scanDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    scanDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        }
    }
}
