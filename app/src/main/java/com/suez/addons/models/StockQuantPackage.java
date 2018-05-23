package com.suez.addons.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-18.
 */

public class StockQuantPackage extends OModel {
    private static final String TAG = StockQuantPackage.class.getSimpleName();

    OColumn name = new OColumn(getContext(), R.string.column_name, OVarchar.class);

    public StockQuantPackage(Context context, OUser user) {
        super(context, "stock.quant.package", user);
    }
}
