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

    public OfflineAction (Context context, OUser user) {
        super(context, "offline.action", user);
    }
}
