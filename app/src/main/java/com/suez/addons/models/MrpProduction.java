package com.suez.addons.models;

import android.content.Context;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-11.
 */

public class MrpProduction extends OModel {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".core.provider.content.sync.mrp_production";
    public static final String TAG = MrpProduction.class.getSimpleName();

    OColumn name = new OColumn(getContext(), R.string.column_reference, OVarchar.class).setSize(64);
    OColumn product_id = new OColumn(getContext(), R.string.column_product, ProductProduct.class, OColumn.RelationType.ManyToOne);
    OColumn product_qty = new OColumn(getContext(), R.string.column_product_qty, OFloat.class);

    public MrpProduction(Context context, OUser user) {
        super(context, "mrp.production", user);
    }
}
