package com.suez.addons.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-14.
 */

public class PretreatmentWac extends OModel {
    public static final String TAG = PretreatmentWac.class.getSimpleName();

    public PretreatmentWac(Context context, OUser user) {
        super(context, "processing.wac", user);
    }

    OColumn name = new OColumn(getContext(), R.string.column_name, OVarchar.class).setSize(64);
}
