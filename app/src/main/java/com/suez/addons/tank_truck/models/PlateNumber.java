package com.suez.addons.tank_truck.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-4-28.
 */

public class PlateNumber extends OModel {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
            ".core.provider.content.sync.plate_number";
    OColumn name = new OColumn(getContext(), R.string.column_name, OVarchar.class);

    public PlateNumber(Context context, OUser user) {
        super(context, "plate.number", user);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
}
