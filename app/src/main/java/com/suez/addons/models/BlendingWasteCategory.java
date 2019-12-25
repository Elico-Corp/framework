package com.suez.addons.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-30.
 */

public class BlendingWasteCategory extends OModel {
    public static final String TAG = BlendingWasteCategory.class.getSimpleName();

    OColumn name = new OColumn(getContext(), R.string.column_name, OVarchar.class);
    OColumn name_local = new OColumn(getContext(), R.string.column_name_local, OVarchar.class);

    public BlendingWasteCategory(Context context, OUser user) {
        super(context, "blending.waste.category", user);
    }
}
