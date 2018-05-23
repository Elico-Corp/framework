package com.suez.addons.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-17.
 */

public class StockQuant extends OModel {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".core.provider.content.sync.stock_quant";
    private static final String TAG = StockQuant.class.getSimpleName();

    OColumn qty = new OColumn(getContext(), R.string.column_qty, OFloat.class);
    OColumn lot_id = new OColumn(getContext(), R.string.column_prodlot_id, StockProductionLot.class, OColumn.RelationType.ManyToOne);
    OColumn location_id = new OColumn(getContext(), R.string.column_location_id, StockLocation.class, OColumn.RelationType.ManyToOne);
    OColumn wizard_id  = new OColumn("Wizard Id", OperationsWizard.class, OColumn.RelationType.ManyToOne).setLocalColumn();

    public StockQuant(Context context, OUser user) {
        super(context, "stock.quant", user);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
}
