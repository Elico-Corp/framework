package com.suez.addons.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-14.
 */

public class ActualPackaging extends OModel {
    public static final String TAG = ActualPackaging.class.getSimpleName();

    OColumn package_ids = new OColumn(getContext(), R.string.column_actual_packaging, ProductPackaging.class, OColumn.RelationType.ManyToOne);
    OColumn qty = new OColumn(getContext(), R.string.column_qty, OInteger.class);
    OColumn remark = new OColumn(getContext(), R.string.column_remark, OVarchar.class);
    OColumn route_line_id = new OColumn(getContext(), R.string.column_delivery_route_line, DeliveryRouteLine.class, OColumn.RelationType.ManyToOne);

    public ActualPackaging(Context context, OUser user) {
        super(context, "actual.packaging", user);
    }

}
