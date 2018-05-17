package com.suez.addons.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

import odoo.controls.OSelectionField;

/**
 * Created by joseph on 18-5-9.
 */

public class OfflineAction extends OModel {
    private static final String TAG = OfflineAction.class.getSimpleName();
    OColumn type = new OColumn(getContext(), R.string.column_action_type, OInteger.class).setRequired();
    OColumn model_name = new OColumn(getContext(), R.string.column_model_name, OVarchar.class).setSize(64).setRequired();
    OColumn isSynced = new OColumn(getContext(), R.string.column_sync_finished, OBoolean.class).setDefaultValue(false);
    private Context mContext;

    public OfflineAction (Context context, OUser user) {
        super(context, "offline.action", user);
        mContext = context;
    }
}
