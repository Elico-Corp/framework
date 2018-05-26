package com.suez.utils;

import android.content.Context;

import com.google.gson.internal.LinkedTreeMap;
import com.odoo.BaseAbstractListener;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OPreferenceManager;
import com.suez.SuezConstants;
import com.suez.addons.models.OfflineAction;
import com.suez.addons.models.OperationsWizard;
import com.suez.addons.models.StockProductionLot;
import com.suez.addons.models.StockQuant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-9.
 */

public class SuezSyncUtils {
    private static final String TAG = SuezSyncUtils.class.getSimpleName();
    private Context mContext;
    private OUser mUser;
    private String syncDate;
    private OperationsWizard wizard;
    private StockProductionLot stockProductionLot;
    private StockQuant stockQuant;
    private List<ODataRow> records;

    public SuezSyncUtils(Context context, OUser user, String date) {
        mUser = user;
        mContext = context;
        syncDate = date;
        wizard = new OperationsWizard(context, user);
        stockProductionLot = new StockProductionLot(mContext, mUser);
        stockQuant = new StockQuant(mContext, mUser);
    }

    public void sync() {
        records = wizard.select(null, "_create_date > ? and synced = ?",
                new String[] {syncDate, "false"}, "_create_date desc");
        for (ODataRow record: records) {
            final String[] quantLineIds = record.getString("quant_line_ids").split(",");
            String[] quantLineQty = record.getString("quant_line_qty").split(",");
            String[] quantLineLocationIds = record.getString("quant_line_location_ids").split(",");
            int lotId = record.getM2ORecord("prodlot_id").browse().getInt("id");
            final int newLotId = record.getM2ORecord("new_prodlot_id").getId();
            Float qty = record.getFloat("qty");
            HashMap<String, Object> map = new HashMap<>();
            switch (record.getString("action")) {
                // TODO
                case SuezConstants.PRETREATMENT_KEY:
                    int pretreatmentLocationId = record.getM2ORecord("pretreatment_location_id").browse().getInt("id");
                    int destinationLocationId = record.getM2ORecord("destination_location_id").browse().getInt("id");
                    String datetime = record.getString("_create_date");
                    HashMap <String, Object> kwargs = new HashMap<>();
                    kwargs.put("lot_id", lotId);
                    kwargs.put("product_qty", qty);
                    kwargs.put("date_planned_start", datetime);
                    List<HashMap> quantLines = new ArrayList<>();
                    for (int i=0; i<quantLineLocationIds.length && i<quantLineQty.length; i++) {
                        HashMap<String, Object> quantLine = new HashMap<>();
                        quantLine.put("location_id", Integer.parseInt(quantLineLocationIds[i]));
                        quantLine.put("quantity", Float.parseFloat(quantLineQty[i]));
                        quantLines.add(quantLine);
                    }
                    kwargs.put("quant_lines", quantLines);
                    kwargs.put("pretreatment_location", pretreatmentLocationId);
                    kwargs.put("dest_location", destinationLocationId);
                    map.put("action", SuezConstants.PRETREATMENT_KEY);
                    map.put("data", kwargs);
                    break;
            }
            CallMethodsOnlineUtils syncUtils = new CallMethodsOnlineUtils(stockProductionLot, "get_flush_data",
                    new OArguments(), null, map).setListener(new BaseAbstractListener(){
                @Override
                public void OnSuccessful(Object obj) {
                    OdooResult result = (OdooResult) obj;
                    if (result != null && result.has("result") && !result.getString("result").contains("error")) {
                        for (int i=0; i<quantLineIds.length && i<result.getMap("result").getArray("quant_id").size(); i++) {
                            OValues quantValues = new OValues();
                            quantValues.put("id", (int) result.getMap("result").getArray("quant_id").get(i));
                            stockQuant.update(Integer.parseInt(quantLineIds[i]), quantValues);
                        }
                        OValues lotValues = new OValues();
                        lotValues.put("id", Integer.parseInt(result.getMap("result").getString("lot_id")));
                        stockProductionLot.update(newLotId, lotValues);
                    }
                }
            });
            syncUtils.callMethodOnServer();
        }
    }

}
