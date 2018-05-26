package com.suez.addons.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-18.
 */

public class OperationsWizard extends OModel {
    private static final String TAG = OperationsWizard.class.getSimpleName();

    OColumn action = new OColumn("action", OVarchar.class);
    OColumn prodlot_id = new OColumn(getContext(), R.string.column_prodlot_id, StockProductionLot.class, OColumn.RelationType.ManyToOne);
    OColumn new_prodlot_id = new OColumn("New Prodlot Id", StockProductionLot.class, OColumn.RelationType.ManyToOne);
    OColumn new_prodlot_ids = new OColumn("New Prodlot Ids", OVarchar.class);
    OColumn quant_line_ids = new OColumn(getContext(), R.string.column_quant_ids,OVarchar.class).setLocalColumn();
    OColumn quant_line_location_ids = new OColumn("Locations", OVarchar.class).setLocalColumn();
    OColumn quant_line_qty = new OColumn("Qty for lines", OFloat.class);
    OColumn pretreatment_location_id = new OColumn(getContext(), R.string.column_pretreatment_location_id, StockLocation.class, OColumn.RelationType.ManyToOne).setLocalColumn().addDomain("is_pretreatment", "=", true);
    OColumn destination_location_id = new OColumn(getContext(), R.string.column_destination_location_id, StockLocation.class, OColumn.RelationType.ManyToOne).setLocalColumn().addDomain("usage", "=", "internal");
    OColumn pretreatment_type_id = new OColumn(getContext(), R.string.column_pretreatment_type_id, PretreatmentWac.class, OColumn.RelationType.ManyToOne);
    OColumn qty = new OColumn(getContext(), R.string.column_qty, OFloat.class).setLocalColumn().setDefaultValue(0);
    OColumn repacking_location_id = new OColumn(getContext(), R.string.column_repacking_location_id, StockLocation.class, OColumn.RelationType.ManyToOne).setLocalColumn().setDefaultValue(false);
    OColumn package_id = new OColumn(getContext(), R.string.column_packaging_id, StockQuantPackage.class, OColumn.RelationType.ManyToOne).setLocalColumn().setDefaultValue(false);
    OColumn package_mumber = new OColumn(getContext(), R.string.column_packaging_number, OInteger.class).setLocalColumn().setDefaultValue(0);
    OColumn remain_qty = new OColumn(getContext(), R.string.column_remain_qty, OFloat.class).setLocalColumn().setDefaultValue(0);
    OColumn synced = new OColumn("Synced?", OBoolean.class).setDefaultValue(false).setLocalColumn();

    public OperationsWizard(Context context, OUser user) {
        super(context, "operations.wizard", user);
    }

    @Override
    public boolean allowUpdateRecordOnServer() {
        return false;
    }

    @Override
    public boolean allowCreateRecordOnServer() {
        return false;
    }

    @Override
    public boolean allowDeleteRecordOnServer() {
        return false;
    }
}
