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

public class WmdsParameterMainComponent extends OModel {
    private static final String TAG = WmdsParameterMainComponent.class.getSimpleName();

    OColumn min = new OColumn(getContext(), R.string.column_min, OVarchar.class);
    OColumn max = new OColumn(getContext(), R.string.column_max, OVarchar.class);
    OColumn average = new OColumn(getContext(), R.string.column_average, OVarchar.class);
    OColumn component = new OColumn(getContext(), R.string.column_component, WmdsMainComponent.class, OColumn.RelationType.ManyToOne);
    OColumn wac_id = new OColumn(getContext(), R.string.column_wac_id, ProductWac.class, OColumn.RelationType.ManyToOne);

    public WmdsParameterMainComponent(Context context, OUser user) {
        super(context, "wmds.parameter.main_component", user);
    }
}
