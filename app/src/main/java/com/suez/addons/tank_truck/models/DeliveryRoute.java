package com.suez.addons.tank_truck.models;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.odoo.BaseAbstractListener;
import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OResource;
import com.suez.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by joseph on 18-4-28.
 */

public class DeliveryRoute extends OModel {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.delivery_route";
    private static final String TAG = DeliveryRoute.class.getSimpleName();
    OColumn name = new OColumn(getContext(), R.string.column_name, OVarchar.class).setSize(64);
    OColumn plate_number = new OColumn(getContext(), R.string.column_plate_number,
            PlateNumber.class, OColumn.RelationType.ManyToOne);
    OColumn state = new OColumn(getContext(), R.string.column_state, OVarchar.class);
    OColumn truck_weight = new OColumn(getContext(), R.string.column_truck_weight, OFloat.class)
            .setDefaultValue(0.0f);
    OColumn gross_weight = new OColumn(getContext(), R.string.column_gross_weight, OFloat.class)
            .setDefaultValue(0.0f);
    private Context mContext;

    public DeliveryRoute(Context context, OUser user){
        super(context, "delivery.route", user);
        this.mContext = context;
    }

    public void searchName(final BaseAbstractListener listener, final String name) {
        new AsyncTask<Void, Void, List<ODataRow>>() {

            @Override
            protected List<ODataRow> doInBackground(Void... params) {
                OdooFields fields = new OdooFields(new String[] {"id", "name", "state"});
                ODomain domain = new ODomain();
                domain.add("&");
                domain.add("state", "=", "truck_in");
                domain.add("&");
                domain.add("gross_weight", "<>", 0);
                domain.add("|");
                domain.add("truck_weight", "=", 0);
                domain.add("truck_weight", "=", null);
                return getServerDataHelper().searchRecords(fields, domain, 10);
            }

            @Override
            protected void onPostExecute(List<ODataRow> list) {
                super.onPostExecute(list);
                if (listener != null) {
                    listener.OnSuccessful(list);
                }
            }
        }.execute();
    }

    public void searchData(final BaseAbstractListener listener, final int offset, final Boolean offDialog){
        new AsyncTask<Void, Void, List<ODataRow>>(){
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute(){
                super.onPreExecute();
                if (offDialog){
                    dialog = new ProgressDialog(mContext);
                    dialog.setTitle(R.string.title_please_wait);
                    dialog.setMessage(OResource.string(mContext, R.string.title_searching));
                    dialog.setCancelable(false);
                    dialog.show();
                }
            }

            @Override
            protected List<ODataRow> doInBackground(Void... params){
                try{
                    OdooFields fields = new OdooFields(getColumns());
                    ODomain domain= new ODomain();
                    domain.add("&");
                    domain.add("state", "=", "truck_in");
                    domain.add("&");
                    domain.add("gross_weight", "!=", 0);
                    domain.add("|");
                    domain.add("truck_weight", "=", 0);
                    domain.add("truck_weight", "=", null);
                    List<ODataRow> results = getServerDataHelper().searchRecords(fields, domain, offset*30, 30, null);
                    return results;
                } catch (Exception e){
                    e.printStackTrace();
                    LogUtils.e(TAG, e.toString());
                }
                return null;
            }
            @Override
            protected void onPostExecute(List<ODataRow> list){
                super.onPostExecute(list);
                if (offDialog){
                    dialog.dismiss();
                }
                if (listener != null){
                    listener.OnSuccessful(list);
                }
            }
        }.execute();
    }

    public void setState(final BaseAbstractListener listener, final int id){
        new AsyncTask<Void, Void, Boolean>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute(){
                super.onPreExecute();
                dialog = new ProgressDialog(mContext);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(mContext, R.string.title_searching));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params){
                try {
                    OArguments loc = new OArguments();
                    loc.add(new JSONArray().put(id));
                    loc.add(new JSONObject());
                    return (Boolean) getServerDataHelper().callMethod("delivery.route", "action_pumping", loc, null,null);
                } catch (Exception e){
                    e.printStackTrace();
                    LogUtils.e(TAG, e.toString());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Boolean obj){
                super.onPostExecute(obj);
                dialog.dismiss();
                if (listener!=null){
                    listener.OnSuccessful(obj);
                }
            }
        }.execute();
    }
    @Override
    public Uri uri(){
        return buildURI(this.AUTHORITY);
    }
}
