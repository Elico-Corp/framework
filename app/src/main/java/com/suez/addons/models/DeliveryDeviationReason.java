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

public class DeliveryDeviationReason extends OModel {
    public static final String TAG = DeliveryDeviationReason.class.getSimpleName();

    OColumn name = new OColumn(getContext(), R.string.column_desc_en, OVarchar.class);

    public DeliveryDeviationReason(Context context, OUser user) {
        super(context, "delivery.deviation.reason", user);
    }
}
