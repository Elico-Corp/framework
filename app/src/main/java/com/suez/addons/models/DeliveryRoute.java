package com.suez.addons.models;

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
            OVarchar.class).setSize(64);
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

    @Override
    public Uri uri(){
        return buildURI(this.AUTHORITY);
    }
}
